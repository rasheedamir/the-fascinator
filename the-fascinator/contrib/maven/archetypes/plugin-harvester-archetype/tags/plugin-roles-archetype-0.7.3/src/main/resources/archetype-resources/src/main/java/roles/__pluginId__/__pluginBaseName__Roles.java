/*
 * The Fascinator - Plugin - Roles - ${pluginName}
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
package ${package}.roles.${pluginId};

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
import au.edu.usq.fascinator.api.roles.Roles;
import au.edu.usq.fascinator.api.roles.RolesException;
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
 * Implementing....
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
public class ${pluginBaseName}Roles implements Roles {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}Roles.class);

    /** Json config file **/
    private JsonConfigHelper config;

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for Roles
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
     * @param jsonString of configuration for Roles
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
    private void init() throws RolesException {
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
     * Create a role.
     *
     * @param rolename The name of the new role.
     * @throws RolesException if there was an error creating the role.
     */
    @Override
    public void createRole(String arg0) throws RolesException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Delete a role.
     *
     * @param rolename The name of the role to delete.
     * @throws RolesException if there was an error during deletion.
     */
    @Override
    public void deleteRole(String arg0) throws RolesException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Find and return all roles this user has.
     *
     * @param username The username of the user.
     * @return An array of role names (String).
     */
    @Override
    public String[] getRoles(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns a list of users who have a particular role.
     *
     * @param role The role to search for.
     * @return An array of usernames (String) that have that role.
     */
    @Override
    public String[] getUsersInRole(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Remove a role from a user.
     *
     * @param username The username of the user.
     * @param oldrole The role to remove from the user.
     * @throws RolesException if there was an error during removal.
     */
    @Override
    public void removeRole(String arg0, String arg1) throws RolesException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Rename a role.
     *
     * @param oldrole The name role currently has.
     * @param newrole The name role is changing to.
     * @throws RolesException if there was an error during rename.
     */
    @Override
    public void renameRole(String arg0, String arg1) throws RolesException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Returns a list of roles matching the search.
     *
     * @param search The search string to execute.
     * @return An array of role names that match the search.
     * @throws RolesException if there was an error searching.
     */
    @Override
    public String[] searchRoles(String arg0) throws RolesException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Assign a role to a user.
     *
     * @param username The username of the user.
     * @param newrole The new role to assign the user.
     * @throws RolesException if there was an error during assignment.
     */
    @Override
    public void setRole(String arg0, String arg1) throws RolesException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Method for testing if the implementing plugin allows
     * the creation, deletion and modification of roles.
     *
     * @return true/false reponse.
     */
    @Override
    public boolean supportsRoleManagement() {
        // TODO Auto-generated method stub
        return false;
    }
}
