/*
 * The Fascinator - Portal
 * Copyright (C) 2010 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package au.edu.usq.fascinator.portal.sso;

import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.portal.JsonSessionState;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fascinator and OpenID integration.
 * @author Greg Pendlebury
 *
 */

public class OpenID implements SSOInterface {
    /** Logging */
    private Logger log = LoggerFactory.getLogger(OpenID.class);

    /** Session data */
    private JsonSessionState sessionState;

    /** The incoming server request */
    private HttpServletRequest request;

    /** OpenID Manager */
    private OpenIdManager manager;

    /** Association MAC */
    private byte[] oidAssocicationMac;

    /** Endpoint Association */
    private String oidEndpointAlias;

    /** Remote logon URL */
    private String oidRemoteLogonUrl;

    /** Return address used */
    private String oidReturnAddress;

    /** Portal base url */
    private String portalUrl;

    /** OpenID Provider in use */
    private String oidProvider;

    /**
     * Return the OpenID ID. Must match configuration at instantiation.
     *
     * @return String The SSO implementation ID.
     */
    @Override
    public String getId() {
        return "OpenID";
    }

    /**
     * Return the on-screen label to describing this implementation.
     *
     * @return String The SSO implementation label.
     */
    @Override
    public String getLabel() {
        return "Login via OpenID";
    }

    /**
     * Return the HTML snippet to use in the interface.
     *
     * Implementations can append additional params to URLs.
     * Like so: "?ssoId=OpenID&{customString}"
     * eg: "?ssoId=OpenID&provider=Google"
     *
     * @param ssoUrl The basic ssoUrl for the server.
     * @return String The string to display as link text.
     */
    @Override
    public String getInterface(String ssoUrl) {
        String html = "";

        // TODO: This is starting to get too big for this file.
        // Should be refactored to refer to a velocity template.

        // Google
        html += "<a href=\"" + ssoUrl + "&provider=Google\">" +
                "<img title=\"Google\" src=\"" + portalUrl +
                "/images/google.png\"/></a>";

        html += "&nbsp;&nbsp;&nbsp; OR &nbsp;&nbsp;&nbsp;";

        // Yahoo
        html += "<a href=\"" + ssoUrl + "&provider=Yahoo\">" +
                "<img title=\"Yahoo\" src=\"" + portalUrl +
                "/images/yahoo.png\"/></a>";

        html += "<hr/>OR<br/>";

        // Custom OpenID
        String txt = " My own OpenID provider ";
        html += "<img title=\"Yahoo\" src=\"" + portalUrl +
                "/images/openid.png\"/> " +
                "<input class=\"custom\" type=\"text\" id=\"ssoProvider\"" +
                " onblur=\"blurOpenId(this);\" onfocus=\"focusOpenId(this);\"" +
                " value=\""+txt+"\"/> " +
                "<input type=\"hidden\" id=\"ssoUrl\" value=\""
                + ssoUrl + "&provider=\"/>" +
                "<input type=\"button\" name=\"openIdGo\"" +
                " onclick=\"doSso();\" value=\"GO\"/>" +
                "<br/>eg. 'http://yourname.myopenid.com'";

        // Custom OpenID scripts
        html += "<script type=\"text/javascript\">" +
                "function focusOpenId(obj) {" +
                "if (obj.value == \"" + txt + "\") {obj.value = \"\";}}" +
                "function blurOpenId(obj) {" +
                "if (obj.value == \"\") {obj.value = \"" + txt + "\";}}" +
                "function doSso() {" +
                "var ssoUrl = document.getElementById('ssoUrl').value;" +
                "var provider = document.getElementById('ssoProvider').value;" +
                "location.href=ssoUrl + provider;}" +
                "</script>";

        return html;
    }

    /**
     * Get the current user details in a User object.
     *
     * @return User A user object containing the current user.
     */
    @Override
    public User getUserObject() {
        String username = (String) sessionState.get("oidSsoIdentity");
        String fullname = (String) sessionState.get("oidSsoDisplayName");
        String email = (String) sessionState.get("oidSsoEmail");

        if (username == null) {
            return null;
        }

        OpenIDUser user = new OpenIDUser();
        user.setUsername(username);
        user.setSource("OpenID");
        user.set("name", fullname);
        user.set("email", email);
        return user;
    }

    /**
     * We cannot log the user out of UConnect, but we can clear Fascinator
     * session data regarding this user.
     *
     */
    @Override
    public void logout() {
        sessionState.remove("oidAssocicationMac");
        sessionState.remove("oidEndpointAlias");
        sessionState.remove("oidRemoteLogonUrl");
        sessionState.remove("oidReturnAddress");
        sessionState.remove("oidSsoIdentity");
        sessionState.remove("oidSsoEmail");
        sessionState.remove("oidSsoName");
        sessionState.remove("oidProvider");
    }

    /**
     * Initialize the SSO Service
     *
     * @param session The server session data
     * @param request The incoming servlet request
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoInit(JsonSessionState session, HttpServletRequest request)
            throws Exception {
        sessionState = session;
        this.request = request;
        manager = new OpenIdManager();

        // Get/Cache/Retrieve the OpenID provider string
        String provider = request.getParameter("provider");
        if (provider != null) {
            oidProvider = provider;
            sessionState.set("oidProvider", oidProvider);
        }
        oidProvider = (String) sessionState.get("oidProvider");

        // Make sure our link is up-to-date
        portalUrl = (String) sessionState.get("ssoPortalUrl");
    }

    /**
     * Prepare the SSO Service to receive a login from the user
     *
     * @param returnAddress The address to come back to after the login
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoPrepareLogin(String returnAddress, String server)
            throws Exception {
        // Set our data
        manager.setReturnTo(returnAddress);
        manager.setRealm(server);
        sessionState.set("oidReturnAddress", returnAddress);
    }

    /**
     * Retrieve the login URL for redirection.
     *
     * @return String The URL used by the SSO Service for logins
     */
    @Override
    public String ssoGetRemoteLogonURL() {
        if (oidProvider == null) {
            return null;
        }

        // Get the provider's data
        try {
            Endpoint endpoint = manager.lookupEndpoint(oidProvider);
            Association association = manager.lookupAssociation(endpoint);
            oidAssocicationMac = association.getRawMacKey();
            oidEndpointAlias = endpoint.getAlias();
            oidRemoteLogonUrl = manager.getAuthenticationUrl(endpoint, association);

            // Make sure we don't forget it
            sessionState.set("oidAssocicationMac", oidAssocicationMac);
            sessionState.set("oidEndpointAlias", oidEndpointAlias);
            sessionState.set("oidRemoteLogonUrl", oidRemoteLogonUrl);
        } catch (OpenIdException ex) {
            log.error("OpenID Error: ", ex.getMessage());
            return null;
        }

        return oidRemoteLogonUrl;
    }

    /**
     * Get user details from the SSO Service and set them in the user session.
     *
     */
    @Override
    public void ssoCheckUserDetails() {
        // Check if already logged in
        String username = (String) sessionState.get("oidSsoIdentity");
        if (username != null) {
            return;
        }

        // SSO Service details
        oidReturnAddress = (String) sessionState.get("oidReturnAddress");
        oidAssocicationMac = (byte[]) sessionState.get("oidAssocicationMac");
        oidEndpointAlias = (String) sessionState.get("oidEndpointAlias");
        if (oidAssocicationMac == null || oidEndpointAlias == null) {
            return;
        }

        // Extract any data that was returned
        try {
            manager.setReturnTo(oidReturnAddress);
            Authentication authentication = manager.getAuthentication(request,
                    oidAssocicationMac, oidEndpointAlias);
            String identity = authentication.getIdentity();
            String name = authentication.getFullname();
            String email = authentication.getEmail();
            log.debug("=== SSO: Provider: '{}', Data: '{}'", oidProvider, authentication.toString());

            if (identity != null) {
                sessionState.set("oidSsoIdentity", identity);
            }
            if (email != null && !email.equals("null")) {
                sessionState.set("oidSsoEmail", email);
            }
            // We try to set the display name as:
            // 1) Name > 2) Email > 3) ID
            if (name != null && !name.equals("nullnull")) {
                sessionState.set("oidSsoName", name);
                sessionState.set("oidSsoDisplayName", name);
            } else {
                if (email != null && !email.equals("null")) {
                    sessionState.set("oidSsoDisplayName", email);
                } else {
                    sessionState.set("oidSsoDisplayName", identity);
                }
            }
        } catch (OpenIdException ex) {
            log.error("Login event expected, but not found: '{}'", ex.getMessage());
        }
    }

    /**
     * Get a list of roles possessed by the current user from the SSO Service.
     *
     * @return List<String> Array of roles.
     */
    @Override
    public List<String> getRolesList() {
        // Not supported by this provider
        return new ArrayList();
    }
}
