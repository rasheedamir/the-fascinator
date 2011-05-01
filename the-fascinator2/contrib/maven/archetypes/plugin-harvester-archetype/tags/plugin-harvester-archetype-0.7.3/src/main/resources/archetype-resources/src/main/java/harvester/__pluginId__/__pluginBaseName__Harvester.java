/*
 * The Fascinator - Plugin - Harvester - ${pluginName}
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

package ${package}.harvester.${pluginId};

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.StorageUtils;

import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;

/**
 * <h3>Introduction</h3>
 * <p>
 * This plugin harvests ...
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
 * Harvesting ...
 * 
 * <pre>
 *   "harvester": {
 *         "type": "..."
 *         }
 *     }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p></p>
 * 
 * <h3>Wiki Link</h3>
 * <p></p>
 * 
 * @author 
 */

public class ${pluginBaseName}Harvester extends GenericHarvester {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}Harvester.class);

    /**
     * Harvester Constructor
     */
    public ${pluginBaseName}Harvester() {
        super("${pluginId}", "${pluginName}");
    }
    
    /**
     * Initialise the plugin
     */
    @Override
    public void init() throws HarvesterException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Harvest the next set of objects, and return their Object IDs
     * 
     * @return Set<String> The set of object IDs just harvested
     * @throws HarvesterException If there are errors
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Check if there is more object
     * 
     * @return true of there is more object
     */
    @Override
    public boolean hasMoreObjects() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Create digital object and the payload for the harvester
     * 
     * NOTE: this function is customised based on the usage of the 
     * harvester
     */
    private String createObject(String[] objectDetail) {
        // TODO Auto-generated method stub
        return null;
    }
}
