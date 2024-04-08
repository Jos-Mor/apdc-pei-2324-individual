import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.*;
import javax.servlet.http.*;

public class rootInitializer implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener {

    //root user info
    private static final String rootUsername = "root";
    private static final String rootHashedPassword = "2b64f2e3f9fee1942af9ff60d40aa5a719db33b8ba8dd4864bb4f11e25ca2bee00907de32a59429602336cac832c8f2eeff5177cc14c864dd116c8bf6ca5d9a9";
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
                    .set("pwd", DigestUtils.sha512Hex(rootHashedPassword))
                    .set("creation_time", Timestamp.now())
                    .set("email", rootEmail)
                    .set("name", rootName)
                    .set("tel_number", rootTelNumber)
                    .set("role", rootRole)
                    .set("state", rootState)
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
