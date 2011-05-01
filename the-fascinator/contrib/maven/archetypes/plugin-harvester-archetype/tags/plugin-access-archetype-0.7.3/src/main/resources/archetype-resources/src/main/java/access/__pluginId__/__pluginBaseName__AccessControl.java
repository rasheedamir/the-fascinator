/*
 * The Fascinator - Plugin - Access Control - ${pluginName}
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
package ${package}.access.${pluginId};

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
import au.edu.usq.fascinator.api.access.AccessControl;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
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
 * Controlling ...
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
public class ${pluginBaseName}AccessControl implements AccessControl {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}AccessControl.class);

    /** Json config file **/
    private JsonConfigHelper config;

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for access control
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
     * @param jsonString of configuration for access control
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
    private void init() throws AccessControlException {
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
     * Apply/store a new security implementation. The schema will already have
     * a recordId as a property.
     *
     * @param newSecurity The new schema to apply.
     * @throws AccessControlException if storage of the schema fails.
     */
    @Override
    public void applySchema(AccessControlSchema arg0)
            throws AccessControlException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Return an empty security schema for the portal to investigate and/or
     * populate.
     *
     * @return An empty security schema
     */
    @Override
    public AccessControlSchema getEmptySchema() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Retrieve a list of possible field values for a given field if the plugin
     * supports this feature.
     *
     * @param field The field name.
     * @return A list of String containing possible values
     * @throws AccessControlException if the field doesn't exist or there
     *          was an error during retrieval
     */
    @Override
    public List<String> getPossibilities(String arg0)
            throws AccessControlException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * A basic wrapper for getSchemas() to return just the roles of the schemas.
     * Useful during index and/or audit when this is the only data required.
     *
     * @param recordId The record to retrieve roles for.
     * @return A list of Strings containing role names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<String> getRoles(String arg0) throws AccessControlException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get a list of schemas that have been applied to a record.
     *
     * @param recordId The record to retrieve information about.
     * @return A list of access control schemas, possibly zero length.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<AccessControlSchema> getSchemas(String arg0)
            throws AccessControlException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Remove a security implementation. The schema will already have
     * a recordId as a property.
     *
     * @param oldSecurity The schema to remove.
     * @throws AccessControlException if removal of the schema fails.
     */
    @Override
    public void removeSchema(AccessControlSchema arg0)
            throws AccessControlException {
        // TODO Auto-generated method stub
        
    }
}
