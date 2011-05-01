/*
 * The Fascinator - Plugin - Storage - ${pluginName}
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
package ${package}.storage.${pluginId};

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfigHelper;

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
public class ${pluginBaseName}Storage implements Storage {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}Storage.class);

    /** Json config file **/
    private JsonConfigHelper config;

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for storage
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
     * @param jsonString of configuration for storage
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
    private void init() throws StorageException {
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
     * To create DigitalObject in the storage
     * @param oid : The identifier of the object
     * @return DigitalObject : The created DigitalObject
     * @throws StorageException if there were errors during creation
     */
    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get the DigitalObject based on the id
     * @param oid : The identifier of the object
     * @return DigitalObject as the found object 
     * @throws StorageException if there were errors during searching
     */
    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get the object id list
     * @return Set of DigitalObject ids
     */
    @Override
    public Set<String> getObjectIdList() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Remove object based on the id
     * @param oid : The identifier of the object
     * @throws StorageException if there were errors during removal
     */
    @Override
    public void removeObject(String oid) throws StorageException {
        // TODO Auto-generated method stub
        
    }
}
