package au.edu.usq.fascinator.portal.services.impl;

import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.api.authentication.AuthManager;
import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.api.roles.RolesManager;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.authentication.GenericUser;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.PortalSecurityManager;
import au.edu.usq.fascinator.portal.sso.SSOInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The security manager coordinates access to various security plugins
 * when cross plugin awareness is required, and executes some server side
 * logic required for features such as single sign-on.
 *
 * @author Greg Pendlebury
 */
public class PortalSecurityManagerImpl implements PortalSecurityManager {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(
            PortalSecurityManagerImpl.class);

    /** User entry point for SSO logon */
    private String SSO_LOGIN_PAGE = "/sso";

    /** Session data */
    private JsonSessionState sessionState;

    /** Access Manager - item level security */
    @Inject
    private AccessControlManager accessManager;

    /** Authentication Manager - logging in */
    @Inject
    private AuthManager authManager;

    /** Role Manager - user groups */
    @Inject
    private RolesManager roleManager;

    /** HTTP Request */
    @Inject
    private Request request;

    /** Request globals */
    @Inject
    private RequestGlobals rg;

    /** Single Sign-On providers */
    private Map<String, SSOInterface> sso;

    /** Server public URL base */
    private String serverUrlBase;

    /** Default Portal */
    private String defaultPortal;

    /** SSO Login URL */
    private String ssoLoginUrl;

    /**
     * Basic constructor, should be run automatically by Tapestry.
     *
     */
    public PortalSecurityManagerImpl() throws IOException {
        // Get system configuration
        JsonConfigHelper config = new JsonConfigHelper(
                JsonConfig.getSystemFile());

        // For all SSO providers configured
        sso = new LinkedHashMap();
        List<Object> ssoProviders = config.getList("sso");
        for (Object ssoId : ssoProviders) {
            // Instantiate from the ServiceLoader
            SSOInterface valid = getSSOProvider((String) ssoId);
            if (valid == null) {
                log.error("Invalid SSO Implementation requested: '{}'", (String) ssoId);
            } else {
                // Store valid implementations
                sso.put((String) ssoId, valid);
                log.info("SSO Provider instantiated: '{}'", ssoId);
            }
        }

        defaultPortal = config.get("portal/defaultView",
                    PortalManager.DEFAULT_PORTAL_NAME);
        serverUrlBase = config.get("urlBase");
        ssoLoginUrl = serverUrlBase + defaultPortal + SSO_LOGIN_PAGE;
    }

    /**
     * Get a SSO Provider from the ServiceLoader
     *
     * @param id SSO Implementation ID
     * @return SSOInterface implementation matching the ID, if found
     */
    private SSOInterface getSSOProvider(String id) {
        ServiceLoader<SSOInterface> providers =
                ServiceLoader.load(SSOInterface.class);
        for (SSOInterface provider : providers) {
            if (id.equals(provider.getId())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Return the Access Control Manager
     *
     * @return AccessControlManager
     */
    @Override
    public AccessControlManager getAccessControlManager() {
        return accessManager;
    }

    /**
     * Return the Authentication Manager
     *
     * @return AuthManager
     */
    @Override
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * Return the Role Manager
     *
     * @return RolesManager
     */
    @Override
    public RolesManager getRoleManager() {
        return roleManager;
    }

    /**
     * Get the list of roles possessed by the current user.
     *
     * @param user The user object of the current user
     * @return String[] A list of roles
     */
    @Override
    public String[] getRolesList(GenericUser user) {
        String source = user.getSource();
        List<String> ssoRoles = new ArrayList();

        // SSO Users
        if (sso.keySet().contains(source)) {
            ssoRoles = sso.get(source).getRolesList();
        }

        // Standard Users
        String[] standardRoles = roleManager.getRoles(user.getUsername());
        for (String role : standardRoles) {
            // Merge the two
            if (!ssoRoles.contains(role)) {
                ssoRoles.add(role);
            }
        }

        // Cast to array and return
        return ssoRoles.toArray(standardRoles);
    }

    /**
     * Retrieve the details of a user by username
     *
     * @param username The username of a user to retrieve
     * @param source The authentication source if known
     * @return User The user requested
     * @throws AuthenticationException if any errors occur
     */
    @Override
    public User getUser(String username, String source)
            throws AuthenticationException {
        // SSO Users
        if (sso.keySet().contains(source)) {
            GenericUser user = (GenericUser) sso.get(source).getUserObject();
            // Sanity check our data
            if (user == null || !user.getUsername().equals(username)) {
                throw new AuthenticationException(
                        "Unknown user '" + username + "'");
            }
            return user;

        // Standard users
        } else {
            authManager.setActivePlugin(source);
            return authManager.getUser(username);
        }
    }

    /**
     * Logout the provided user
     *
     * @return user The user to logout
     */
    @Override
    public void logout(User user) throws AuthenticationException {
        String source = user.getSource();
        // SSO Users
        if (sso.keySet().contains(source)) {
            sso.get(source).logout();
            sessionState.remove("username");
            sessionState.remove("source");

        // Standard users
        } else {
            authManager.logOut(user);
            sessionState.remove("username");
            sessionState.remove("source");
        }
    }

    /**
     * Initialize the SSO Service, prepare a login if required
     *
     * @param session The server session data
     * @throws Exception if any errors occur
     */
    @Override
    public String ssoInit(JsonSessionState session) throws Exception {
        // Keep track of the session
        sessionState = session;

        // Keep track of the user switching portals for
        // link building in other methods
        String portalId = (String) sessionState.get("portalId", defaultPortal);
        ssoLoginUrl = serverUrlBase + portalId + SSO_LOGIN_PAGE;

        // Find out what page we are on
        String path = request.getAttribute("RequestURI").toString();
        String currentAddress = serverUrlBase + path;

        // Store the portal URL, might be required by implementers to build
        //  an interface (images etc).
        sessionState.set("ssoPortalUrl", serverUrlBase + portalId);

        // Makes sure all SSO plugins get initialised
        for (String ssoId : sso.keySet()) {
            sso.get(ssoId).ssoInit(sessionState, rg.getHTTPServletRequest());
        }

        // If we aren't logging in right now
        if (!currentAddress.contains(SSO_LOGIN_PAGE)) {
            // Store the current address for use later
            sessionState.set("returnAddress", currentAddress);
            // We're done now
            return null;
        }

        // Get the last address to return the user to
        String returnAddress = (String) sessionState.get("returnAddress");
        if (returnAddress == null) {
            // Or use the home page
            returnAddress = serverUrlBase + portalId + "/home";
        }

        // Which SSO provider did the user request?
        String ssoId = request.getParameter("ssoId");
        if (ssoId == null) {
            log.error("==== SSO: No SSO ID found!");
            return null;
        }

        // The main event... finally
        sso.get(ssoId).ssoPrepareLogin(returnAddress, serverUrlBase);
        return ssoId;
    }

    /**
     * Get user details from SSO connection and set them in the user session.
     *
     */
    @Override
    public void ssoCheckUserDetails() {
        for (String ssoId : sso.keySet()) {
            sso.get(ssoId).ssoCheckUserDetails();
            GenericUser user = (GenericUser) sso.get(ssoId).getUserObject();
            if (user != null) {
                sessionState.set("username", user.getUsername());
                sessionState.set("source", ssoId);
            }
        }
    }

    /**
     * Build a Map of Maps of on-screen string values for each SSO provider.
     * Should be enough to generate a login interface.
     *
     * @return Map Containing the data structure of valid SSO interfaces.
     */
    @Override
    public Map<String, Map<String, String>> ssoBuildLogonInterface() {
        Map<String, Map<String, String>> ssoInterface = new LinkedHashMap();
        for (String ssoId : sso.keySet()) {
            SSOInterface provider = sso.get(ssoId);
            Map<String, String> map = new HashMap();
            map.put("label", provider.getLabel());
            map.put("interface", provider.getInterface(
                    ssoLoginUrl + "?ssoId=" + ssoId));
            ssoInterface.put(ssoId, map);
        }
        return ssoInterface;
    }

    /**
     * Retrieve the login URL for redirection against a given provider.
     *
     * @return String The URL used by the SSO Service for logins
     */
    @Override
    public String ssoGetRemoteLogonURL(String source) {
        if (!sso.containsKey(source)) {
            return null;
        } else {
            return sso.get(source).ssoGetRemoteLogonURL();
        }
    }
}
