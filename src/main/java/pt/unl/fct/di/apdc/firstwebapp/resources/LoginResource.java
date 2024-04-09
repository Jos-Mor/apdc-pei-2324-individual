package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.Authentication.SignatureUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	
	//Settings that must be in the database
	public static final String AM = "Application Manager";
	public static final String BACKOFFICE = "Backoffice";
	public static final String USER = "User";
	public static final String SUPERUSER = "Super-User";
	public static final String ACTIVE_STATE = "Active";
	public static final String INACTIVE_STATE= "Inactive";
	public static final String COOKIE_NAME = "session::apdc";

	private static final String key = "dhsjfhndkjvnjdsdjhfkjdsjfjhdskjhfkjsdhfhdkjhkfajkdkajfhdkmc";

	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	@Context HttpServletRequest request;
	@Context HttpHeaders headers;
	private final Gson g = new Gson();
	
	public LoginResource() {}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		LOG.fine("Login attempt by user: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username)).setKind("UserStats").newKey("counters");
		Key logKey = datastore.allocateId((datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username))).setKind("UserLog").newKey());
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if (user == null) {
				LOG.warning("Failed login attempt for username " + data.username + ".");
				return Response.status(Response.Status.FORBIDDEN).build();
			}
			if (user.getString("state").equals(INACTIVE_STATE)) {
				LOG.warning("User " + data.username + " does not have the account activated.");
				return Response.status(Response.Status.FORBIDDEN).build();
			}
			Entity stats = txn.get(ctrsKey);
			if (stats == null){
				stats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", 0L)
						.set("user_stats_failed", 0L)
						.set("user_first_login", Timestamp.now())
						.set("user_last_login", Timestamp.now())
						.build();
			}
			if (user.getString("pwd").equals(DigestUtils.sha512Hex(data.password))) {

				String id = UUID.randomUUID().toString();
				String role = user.getString("role");
				Timestamp created_at = Timestamp.now();
				String fields = data.username+"."+ id +"."+role+"."+created_at+"."+60*60*2;
				String signature = SignatureUtils.calculateHMac(key, fields);

				if(signature == null) {
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while signing token. See logs.").build();
				}

				String value =  fields + "." + signature;
				NewCookie cookie = new NewCookie(COOKIE_NAME, value, "/", ".my-project-416118.oa.r.appspot.com", "comment", 60*60*2, false, false);

				updateStatLog(true, txn, logKey, ctrsKey, stats);
				LOG.info("User " + data.username + " logged in successfully.");
				return Response.ok().cookie(cookie).build();
			}
			else {
				updateStatLog(false, txn, logKey, ctrsKey, stats);
				LOG.info("Attempted login in user account '" + data.username + "'");
				return Response.status(Response.Status.FORBIDDEN).entity("Incorrect username or password.").build();
			}
		} catch (Exception e){
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	private void updateStatLog (Boolean success, Transaction txn, Key logKey, Key statsKey, Entity stats){
		Entity usStats = Entity.newBuilder(statsKey)
				.set("user_stats_logins", ((success) ? 1L : 0L) + stats.getLong("user_stats_logins"))
				.set("user_stats_failed", ((success) ? 0L : 1L) + stats.getLong("user_stats_failed"))
				.set("user_first_login", stats.getTimestamp("user_first_login"))
				.set("user_last_login", Timestamp.now())
				.set("user_last_attempt", Timestamp.now())
				.build();
		if (success) {
			Entity log = Entity.newBuilder(logKey)
					.set("user_login_ip", request.getRemoteAddr())
					.set("user_login_host", request.getRemoteHost())
					.set("user_login_latlon", (headers == null) ? null : StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong"))
							.setExcludeFromIndexes(true).build())
					.set("user_login_city", (headers == null) ? null : headers.getHeaderString("X-AppEngine-City"))
					.set("user_login_country", (headers == null) ? null : headers.getHeaderString("X-AppEngine-Country"))
					.build();
			txn.put(log, usStats);
		}
		else
			txn.put(usStats);
		txn.commit();
	}
/*
	private static boolean checkPassword(LoginData data)  {
		UserData user = users.get(data.username);

		if(user == null || !user.password.equals(data.password)) {
			return false;
		}

		return true;
	}
*/

	public static boolean cookieIsValid(Cookie cookie) {
		if(cookie == null || cookie.getValue() == null) {
			return false;
		}
		
		String[] values = extractCookieValues(cookie);
		for (int i = 0; i < values.length; i++)
			LOG.warning("COOKIE VALUES: " + values[i]);
			
		String signatureNew = SignatureUtils.calculateHMac(key, values[0]+"."+values[1]+"."+values[2]+"."+values[3]+"."+values[4]+"."+values[5]);
		String signatureOld = values[6];

		LOG.warning("ZÉ: old = " + values[6]);
		
		if(signatureNew == null || !signatureNew.equals(signatureOld)) {
			return false;
		}
		
		String timestamp = values[3] + "." + values[4];
		Timestamp creation_time = Timestamp.parseTimestamp(timestamp);
		Timestamp expiry_time = Timestamp.ofTimeSecondsAndNanos(creation_time.getSeconds() + Long.getLong(values[5]), creation_time.getNanos());
		
		LOG.warning("ZÉ: ts = " + timestamp);

		if(Timestamp.now().compareTo(expiry_time) > 0) //current time is after expiry
			return false;
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(values[0]);
		Entity user = datastore.get(userKey);

		LOG.warning("ZÉ: user = " + user);

		return user.getString("state").equals(ACTIVE_STATE);
	}

	public static int convertRole(String role) {
		switch(role) {
			case SUPERUSER:
				return 3;
			case AM:
				return 2;
			case BACKOFFICE:
				return 1;
			case USER:
				return 0;
		}
		LOG.warning("Roles not converting properly");
		return -1;
	}
	
	@GET
	@Path("/{username}")
	public Response checkUsernameAvailable(@PathParam("username") String username) {
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Entity user = datastore.get(userKey);
		
		if(user != null) {
			return Response.ok().entity(g.toJson(false)).build();
		}
		
		return Response.ok().entity(g.toJson(true)).build();
	}

	public static String[] extractCookieValues(Cookie cookie){
		String value = cookie.getValue();
		return value.split("\\.");
	}
}
