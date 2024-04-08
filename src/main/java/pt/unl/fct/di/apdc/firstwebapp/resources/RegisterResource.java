package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.util.RegistrationData;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    public static final int PASSWORD_MIN_LENGTH = 5;

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    public RegisterResource(){ }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doRegistration(RegistrationData data) {
        LOG.fine("Register attempt with username: " + data.username);

        if (data.password.length() < PASSWORD_MIN_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST).entity("At least " + PASSWORD_MIN_LENGTH +" characters for password.").build();
        }

        if (!data.validRegistration()){
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }


        Key k = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity existing_user = txn.get(k);
            if (existing_user != null) {
                txn.rollback();
                return Response.status(Response.Status.BAD_REQUEST).entity("User already exists.").build();
            }

            Entity.Builder builder = Entity.newBuilder(k)
                    .set("pwd", DigestUtils.sha512Hex(data.password))
                    .set("creation_time", Timestamp.now())
                    .set("email", data.email)
                    .set("name", data.name)
                    .set("tel_number", data.tel_number)
                    .set("profile_status", data.profile)
                    .set("has_photo", data.hasPhoto)
                    .set("role", LoginResource.USER)
                    .set("state", LoginResource.INACTIVE_STATE);
            if (!data.profession.isEmpty())
                builder.set("profession", data.profession);
            if (!data.workplace.isEmpty())
                builder.set("workplace", data.workplace);
            if (!data.address.isEmpty())
                builder.set("address", data.address);
            if (!data.postalCode.isEmpty())
                builder.set("postal_code", data.postalCode);
            if (!data.NIF.isEmpty())
                builder.set("NIF", data.NIF);

            Entity user = builder.build();

            txn.add(user);
            LOG.info("User registered " + data.username);
            txn.commit();
            return Response.ok("{}").build();
        } finally {
            if (txn.isActive()){
                txn.rollback();
            }
        }
    }
}
