package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.util.*;

@Path("/operation")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class OperationResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

    public static final String AM = LoginResource.AM;
    public static final String BACKOFFICE = LoginResource.BACKOFFICE;
    public static final String USER = LoginResource.USER;
    public static final String SUPERUSER = LoginResource.SUPERUSER;
    public static final String ACTIVE_STATE = LoginResource.ACTIVE_STATE;
    public static final String INACTIVE_STATE = LoginResource.INACTIVE_STATE;
    public static final String COOKIE_NAME = LoginResource.COOKIE_NAME;
    public static final int PAGE_LIMIT = 10;    //limit on user listing per page
    public static final int PASSWORD_MIN_LENGTH = RegisterResource.PASSWORD_MIN_LENGTH;

    private final Gson g = new Gson();

    @Context
    HttpServletRequest request;


    private final Datastore datastore = LoginResource.datastore;

    public OperationResource() {
    }

    @POST
    @Path("/change_role")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doChangeRole(ChangeRoleData data, @CookieParam(COOKIE_NAME) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);
        if (!data.role.equals(AM) && !data.role.equals(SUPERUSER) && !data.role.equals(BACKOFFICE) && !data.role.equals(USER))
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid role").build();

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("Change role attempt for user: " + data.username + " to + '" + data.role + "', attempt made by user: " + cookieValue[0]);

        if (LoginResource.convertRole(cookieValue[2]) < LoginResource.convertRole(AM))
            return Response.status(Response.Status.FORBIDDEN).entity("You do not have permission to use this operation.").build();

        boolean su_access = cookieValue[2].equals(SUPERUSER);
        if (!su_access && (data.role.equals(SUPERUSER) || data.role.equals(AM)))
            return Response.status(Response.Status.FORBIDDEN).entity("You do not have permission to change anyone to this role.").build();

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("User " + data.username + " does not exist.");
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String user_role = user.getString("role");
            if (user_role.equals(data.role) || (!su_access && (user_role.equals(SUPERUSER) || user_role.equals(AM)))) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity("You do not have permission to change this user.").build();
            }
            user = Entity.newBuilder(user)
                    .set("role", data.role)
                    .build();
            txn.put(user);
            LOG.info("User role updated " + data.username);
            txn.commit();
            return Response.ok().entity("User updated").build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    @POST
    @Path("/change_state")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doChangeState(ChangeStateData data, @CookieParam(COOKIE_NAME) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);
        if (!data.state.equals(INACTIVE_STATE) && !data.state.equals(ACTIVE_STATE))
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid state").build();

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("Change state attempt for user: " + data.username + " to + '" + data.state + "', attempt made by user: " + cookieValue[0]);

        if (cookieValue[2].equals(USER)) {
            return Response.status(Response.Status.FORBIDDEN).entity("You do not have permissions to change account statuses.").build();
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("User " + data.username + " does not exist.");
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String user_role = user.getString("role");
            switch (cookieValue[2]) {
                case AM:
                    if (user_role.equals(SUPERUSER) || user_role.equals(AM)) {
                        txn.rollback();
                        return Response.status(Response.Status.FORBIDDEN).entity("You do not have permissions to change this account statuses.").build();
                    }
                    break;
                case BACKOFFICE:
                    if (!user_role.equals(USER)) {
                        txn.rollback();
                        return Response.status(Response.Status.FORBIDDEN).entity("You do not have permissions to change this account statuses.").build();
                    }
            }
            user = Entity.newBuilder(user)
                    .set("state", data.state)
                    .build();
            txn.put(user);
            LOG.info("User role updated " + data.username);
            txn.commit();
            return Response.ok().entity("User updated").build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    @POST
    @Path("/remove_user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRemoveUser(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("username") String username) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("User removal attempt for user: " + username + ", attempt made by user: " + cookieValue[0]);

        if (cookieValue[2].equals(BACKOFFICE)) {
            return Response.status(Response.Status.FORBIDDEN).entity("You do not have permissions to remove any account.").build();
        }
        if (cookieValue[2].equals(USER) && !cookieValue[0].equals(username))
            return Response.status(Response.Status.FORBIDDEN).entity("You can only remove your own account.").build();

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("User '" + username + "' does not exist.");
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            String user_role = user.getString("role");
            if (cookieValue[2].equals(AM) && (user_role.equals(SUPERUSER) || user_role.equals(AM))) {
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).entity("You do not have permissions to remove this account.").build();
            }
            txn.delete(userKey);
            LOG.info("User '" + username + "' removed ");
            txn.commit();
            return Response.ok().entity("User updated").build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    @GET
    @Path("/list_users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListUsers(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("cursor") String startCursor) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("User listing attempt for user made by user: " + cookieValue[0]);
        EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder().setKind("User").setLimit(PAGE_LIMIT);

        if (startCursor != null && !startCursor.isEmpty()) {
            queryBuilder.setStartCursor(Cursor.fromUrlSafe(startCursor));
        }
        switch (cookieValue[2]) {
            case SUPERUSER:
                break;
            case AM:
                queryBuilder
                        .setFilter(PropertyFilter.neq("role", SUPERUSER));
                break;
            case BACKOFFICE:
                queryBuilder
                        .setFilter(PropertyFilter.eq("role", USER));
                break;
            case USER:
            default:
                queryBuilder
                        .setFilter(CompositeFilter.and(
                                PropertyFilter.eq("profile_status", false),
                                PropertyFilter.eq("state", ACTIVE_STATE),
                                PropertyFilter.eq("role", USER)
                        ));
                break;
        }
        QueryResults<Entity> results = datastore.run(queryBuilder.build());

        JsonObject jsonResponse = new JsonObject();
        JsonArray usersArray = new JsonArray();
        while (results.hasNext()) {
            Entity userEntity = results.next();
            // Process user entity as needed
            JsonObject userJson = new JsonObject();
            userJson.addProperty("username", userEntity.getKey().getName());
            userJson.addProperty("email", userEntity.getString("email"));
            userJson.addProperty("name", userEntity.getString("name"));
            if (!cookieValue[2].equals(USER)) {
                userJson.addProperty("role", userEntity.getString("role"));
                userJson.addProperty("state", userEntity.getString("state"));
                userJson.addProperty("pwd", userEntity.getString("pwd"));
                userJson.addProperty("profile_status", userEntity.getBoolean("profile_status"));
                userJson.addProperty("tel_number", userEntity.getString("tel_number"));
                userJson.addProperty("creation_time", userEntity.getLong("creation_time"));
                userJson.addProperty("has_photo", userEntity.getBoolean("has_photo"));
                userJson.addProperty("profession", userEntity.getString("profession"));
                userJson.addProperty("workplace", userEntity.getString("workplace"));
                userJson.addProperty("address", userEntity.getString("address"));
                userJson.addProperty("postal_code", userEntity.getString("postal_code"));
                userJson.addProperty("NIF", userEntity.getString("NIF"));
            }
            usersArray.add(userJson);
        }
        jsonResponse.add("users", usersArray);
        // If there are more results, include next page cursor
        if (results.hasNext()) {
            Cursor endCursor = results.getCursorAfter();
            jsonResponse.addProperty("nextCursor", endCursor.toUrlSafe());
        }
        String json = new Gson().toJson(jsonResponse);
        return Response.ok().entity(json).type("application/json").build();
    }

    @POST
    @Path("/modify_attribute")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doModifyAttribute(@CookieParam(COOKIE_NAME) Cookie cookie, RegistrationData data) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("User removal attempt for user: " + data.username + ", attempt made by user: " + cookieValue[0]);

        if (cookieValue[2].equals(USER) && !cookieValue[0].equals(data.username))
            return Response.status(Response.Status.FORBIDDEN).entity("You can only change your own account.").build();

        Key k = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity existing_user = txn.get(k);
            if (existing_user == null) {
                txn.rollback();
                return Response.status(Response.Status.BAD_REQUEST).entity("User doesn't exist.").build();
            }
            if (cookieValue[2].equals(BACKOFFICE)) {
                if (!existing_user.getString("role").equals(USER)) {
                    txn.rollback();
                    return Response.status(Response.Status.BAD_REQUEST).entity("Cannot change that user.").build();
                }
            } else if (cookieValue[2].equals(AM)) {
                if (existing_user.getString("role").equals(AM) || existing_user.getString("role").equals(SUPERUSER)) {
                    txn.rollback();
                    return Response.status(Response.Status.BAD_REQUEST).entity("Cannot change that user.").build();
                }
            } else if (cookieValue[2].equals(SUPERUSER)) {
                if (existing_user.getString("role").equals(SUPERUSER)) {
                    txn.rollback();
                    return Response.status(Response.Status.BAD_REQUEST).entity("Cannot change that user.").build();
                }
            }
            Entity.Builder builder = Entity.newBuilder(k)
                    .set("pwd", existing_user.getString("pwd"))
                    .set("role", existing_user.getString("role"))
                    .set("creation_time", existing_user.getLong("creation_time"))
                    .set("state", existing_user.getString("state"));

            builder.set("email", data.email)
                .set("name", data.name)
                .set("tel_number", data.tel_number)
                .set("profile_status", data.profile)
                .set("has_photo", data.hasPhoto)
                .set("profession", data.profession)
                .set("workplace", data.workplace)
                .set("address", data.address)
                .set("postal_code", data.postalCode)
                .set("NIF", data.NIF);

            Entity user = builder.build();

            txn.put(user);
            LOG.info("User modified " + data.username);
            txn.commit();
            return Response.ok("{}").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/change_password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doChangePassword(ChangePasswordData data, @CookieParam(COOKIE_NAME) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);
        if (!data.validate())
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid data passed, check if passwords match").build();
        if (data.password.length() < PASSWORD_MIN_LENGTH)
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid password, minimum " + PASSWORD_MIN_LENGTH + "characters").build();

        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();

        LOG.fine("Change password attempt for user: " + cookieValue[0] + ", attempt made by user: " + cookieValue[0]);

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(cookieValue[0]);
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("User " + cookieValue[0] + " does not exist.");
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            if (!user.getString("pwd").equals(DigestUtils.sha512Hex(data.password))){
                txn.rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            user = Entity.newBuilder(user)
                    .set("pwd", DigestUtils.sha512Hex(data.password))
                    .build();
            txn.put(user);
            LOG.info("User '" + cookieValue[0] + "' password changed.");
            txn.commit();
            return Response.ok().entity("User updated").build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    @POST
    @Path("/logout")
    public Response doLogout(@CookieParam(COOKIE_NAME) Cookie cookie) {
        if (cookie == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cookie not found, please log in.").build();
        }
        String[] cookieValue = LoginResource.extractCookieValues(cookie);
        if (!LoginResource.cookieIsValid(cookie))
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid cookie, please log in again.").build();
        LOG.fine("Logout attempt made by user: " + cookieValue[0]);

        String newValue = cookieValue[0]+"."+cookieValue[1]+"."+cookieValue[2]+"."+ System.currentTimeMillis()+"."+(-1);     //expired validity cookie made to rewrite the old one

        NewCookie newCookie = new NewCookie(COOKIE_NAME, newValue, "/", ".my-project-416118.oa.r.appspot.com", "comment", 60*60*2, false, false);
        return Response.ok().cookie(newCookie).build();
    }
}
