import com.google.cloud.datastore.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class rootInitializer implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener {

    //root user info
    private static final String rootUsername = "root";
    private static final String rootHashedPassword = "99adc231b045331e514a516b4b7680f588e3823213abe901738bc3ad67b2f6fcb3c64efb93d18002588d3ccc1a49efbae1ce20cb43df36b38651f11fa75678e8";
    private static final String rootEmail = "root@gmail.com";
    private static final String rootName = "John Doe";
    private static final String rootTelNumber = "123456789";
    private static final String rootRole = "Super-User";
    private static final String rootState = "Active";

    public rootInitializer() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        /* This method is called when the servlet context is initialized(when the Web application is deployed). */
        ServletContext servletContext = sce.getServletContext();
        Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
        servletContext.setAttribute("datastore", datastore);
        Key k = datastore.newKeyFactory().setKind("User").newKey(rootUsername);
        Transaction txn = datastore.newTransaction();
        try {
            Entity rootUser = Entity.newBuilder(k)
                    .set("pwd", rootHashedPassword)
                    .set("creation_time", System.currentTimeMillis())
                    .set("email", rootEmail)
                    .set("name", rootName)
                    .set("tel_number", rootTelNumber)
                    .set("role", rootRole)
                    .set("state", rootState)
                    .set("profile_status", true)
                    .set("has_photo", false)
                    .set("profession", "System Administrator")
                    .set("workplace", "")
                    .set("address", "")
                    .set("postal_code", "")
                    .set("NIF", "")
                    .build();

            txn.put(rootUser);
            txn.commit();
        } finally {
            if (txn.isActive()){
                txn.rollback();
            }
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is called when the servlet Context is undeployed or Application Server shuts down. */
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        /* Session is created. */
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        /* Session is destroyed. */
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute is added to a session. */
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute is removed from a session. */
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute is replaced in a session. */
    }
}
