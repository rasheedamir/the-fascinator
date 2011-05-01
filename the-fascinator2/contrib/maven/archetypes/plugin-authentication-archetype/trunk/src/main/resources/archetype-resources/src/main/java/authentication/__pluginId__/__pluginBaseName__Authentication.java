/*
 * The Fascinator - Plugin - Authentication - ${pluginName}
 * Copyright (C) <your copyright here>
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
package ${package}.authentication.${pluginId};

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.authentication.Authentication;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * <h3>Introduction</h3>
 * <p>
 * This plugin ...
 * </p>
 * 
 * <h3>Configuration</h3>
 * 
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td></td>
 * <td></td>
 * <td></td>
 * <td></td>
 * </tr>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Authenticating ...
 * 
 * <pre>
 *   
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p></p>
 * 
 * @author 
 */
public class ${pluginBaseName}Authentication implements Authentication {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}Authentication.class);

    /** Json config file **/
    private JsonConfigHelper config;

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for Authentication
     * @throws PluginException if fail to parse the config
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonString);
            init();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for Authentication
     * @throws PluginException if fail to parse the config
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonFile);
            init();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Initialise the plugin
     */
    private void init() throws AuthenticationException {
        // configure plugin if required
    }

    /**
     * Gets plugin Id
     * 
     * @return pluginId
     */
    @Override
    public String getId() {
        return "${pluginId}";
    }

    /**
     * Gets plugin name
     * 
     * @return pluginName
     */
    @Override
    public String getName() {
        return "${pluginName}";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Overridden shutdown method
     * 
     * @throws PluginException
     */
    @Override
    public void shutdown() throws PluginException {
        // clean up any resources if required
    }

    /**
     * Change a user's password.
     * 
     * @param username The user changing their password.
     * @param password The new password for the user.
     * @throws AuthenticationException if there was an error changing the
     * password.
     */
    @Override
    public void changePassword(String arg0, String arg1)
            throws AuthenticationException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Create a user.
     * 
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @return A user object for the newly created in user.
     * @throws AuthenticationException if there was an error creating the user.
     */
    @Override
    public User createUser(String arg0, String arg1)
            throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Delete a user.
     * 
     * @param username The username of the user to delete.
     * @throws AuthenticationException if there was an error during deletion.
     */
    @Override
    public void deleteUser(String arg0) throws AuthenticationException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Describe the metadata the implementing class needs/allows for a user.
     * 
     * TODO: This is a placeholder of possible later SQUIRE integration.
     * 
     * @return TODO: possibly a JSON string.
     */
    @Override
    public String describeUser() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns a User object if the implementing class supports user queries
     * without authentication.
     * 
     * @param username The username of the user required.
     * @return An user object of the requested user.
     * @throws AuthenticationException if there was an error retrieving the
     * object.
     */
    @Override
    public User getUser(String arg0) throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Tests the user's username/password validity.
     * 
     * @param username The username of the user logging in.
     * @param password The password of the user logging in.
     * @return A user object for the newly logged in user.
     * @throws AuthenticationException if there was an error logging in.
     */
    @Override
    public User logIn(String arg0, String arg1) throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Optional logout method if the implementing class wants to do any
     * post-processing.
     * 
     * @param username The username of the logging out user.
     * @throws AuthenticationException if there was an error logging out.
     */
    @Override
    public void logOut(User arg0) throws AuthenticationException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Modify one of the user's properties. Available properties should match up
     * with the return value of describeUser().
     * 
     * @param username The user being modified.
     * @param property The user property being modified.
     * @param newValue The new value to be assigned to the property.
     * @return An updated user object for the modifed user.
     * @throws AuthenticationException if there was an error during
     * modification.
     */
    @Override
    public User modifyUser(String username, String property, String newValue)
            throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User modifyUser(String username, String property, int newValue)
            throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User modifyUser(String username, String property, boolean newValue)
            throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns a list of users matching the search.
     * 
     * @param search The search string to execute.
     * @return A list of usernames (String) that match the search.
     * @throws AuthenticationException if there was an error searching.
     */
    @Override
    public List<User> searchUsers(String arg0) throws AuthenticationException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Method for testing if the implementing plugin allows the creation,
     * deletion and modification of users.
     * 
     * @return true/false reponse.
     */
    @Override
    public boolean supportsUserManagement() {
        // TODO Auto-generated method stub
        return false;
    }
}
