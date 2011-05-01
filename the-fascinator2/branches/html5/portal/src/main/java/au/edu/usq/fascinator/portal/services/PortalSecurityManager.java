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
package au.edu.usq.fascinator.portal.services;

import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.api.authentication.AuthManager;
import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.api.roles.RolesManager;
import au.edu.usq.fascinator.common.authentication.GenericUser;
import au.edu.usq.fascinator.portal.JsonSessionState;
import java.util.List;
import java.util.Map;

/**
 * The security manager coordinates access to various security plugins
 * when cross plugin awareness is required, and executes some server side
 * logic required for features such as single sign-on.
 *
 * @author Greg Pendlebury
 */
public interface PortalSecurityManager {

    /**
     * Return the Access Control Manager
     *
     * @return AccessControlManager
     */
    public AccessControlManager getAccessControlManager();

    /**
     * Return the Authentication Manager
     *
     * @return AuthManager
     */
    public AuthManager getAuthManager();

    /**
     * Return the Role Manager
     *
     * @return RolesManager
     */
    public RolesManager getRoleManager();

    /**
     * Get the list of roles possessed by the current user.
     *
     * @param user The user object of the current user
     * @return String[] A list of roles
     */
    public String[] getRolesList(GenericUser user);

    /**
     * Retrieve the details of a user by username
     *
     * @param username The username of a user to retrieve
     * @param source The authentication source if known
     * @return User The user requested
     * @throws AuthenticationException if any errors occur
     */
    public User getUser(String username, String source)
            throws AuthenticationException;

    /**
     * Logout the provided user
     *
     * @return user The user to logout
     */
    public void logout(User user) throws AuthenticationException;

    /**
     * Initialize the SSO Service, prepare a login if required
     *
     * @param session The server session data
     * @throws Exception if any errors occur
     */
    public String ssoInit(JsonSessionState session) throws Exception;

    /**
     * Retrieve the login URL for redirection against a given provider.
     *
     * @return String The URL used by the SSO Service for logins
     */
    public String ssoGetRemoteLogonURL(String source);

    /**
     * Get user details from SSO connection and set them in the user session.
     *
     */
    public void ssoCheckUserDetails();

    /**
     * Build a Map of Maps of on-screen string values for each SSO provider.
     * Should be enough to generate a login interface.
     *
     * @return Map Containing the data structure of valid SSO interfaces.
     */
    public Map<String, Map<String, String>> ssoBuildLogonInterface();
}
