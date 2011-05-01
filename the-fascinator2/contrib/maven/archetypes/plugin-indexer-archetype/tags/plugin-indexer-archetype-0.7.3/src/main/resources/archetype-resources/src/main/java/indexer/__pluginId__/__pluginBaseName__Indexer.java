/*
 * The Fascinator - Plugin - Indexer - ${pluginName}
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
package ${package}.indexer.${pluginId};

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
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
public class ${pluginBaseName}Indexer implements Indexer {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(${pluginBaseName}Indexer.class);

    /** Json config file **/
    private JsonConfigHelper config;

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for indexer
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
     * @param jsonString of configuration for indexer
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
    private void init() throws IndexerException {
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
     * Index an annotation
     * 
     * @param oid : The identifier of the annotation's object
     * @param pid : The identifier of the annotation
     * @throws IndexerException if there were errors during indexing
     */
    @Override
    public void annotate(String oid, String pid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Remove all annotations from the index against an object
     * 
     * @param oid : The identifier of the object
     * @throws IndexerException if there were errors during removal
     */
    @Override
    public void annotateRemove(String oid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Remove the specified annotation from the index
     * 
     * @param oid : The identifier of the object
     * @param annoId : The identifier of the annotation
     * @throws IndexerException if there were errors during removal
     */
    @Override
    public void annotateRemove(String oid, String annoId)
            throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Search for annotations and return the result to the provided stream
     * 
     * @param request : The SearchRequest object
     * @param response : The OutputStream to send responses to
     * @throws IndexerException if there were errors during indexing
     */
    @Override
    public void annotateSearch(SearchRequest request, OutputStream response)
            throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Call a manual commit against the index
     * 
     */
    @Override
    public void commit() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Index an object and all of its payloads
     * 
     * @param oid : The identifier of the object
     * @throws IndexerException if there were errors during indexing
     */
    @Override
    public void index(String oid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Index a specific payload
     * 
     * @param oid : The identifier of the payload's object
     * @param pid : The identifier of the payload
     * @throws IndexerException if there were errors during indexing
     */
    @Override
    public void index(String oid, String pid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Remove the specified object from the index
     * 
     * @param oid : The identifier of the object to remove
     * @throws IndexerException if there were errors during removal
     */
    @Override
    public void remove(String oid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Remove the specified payload from the index
     * 
     * @param oid : The identifier of the payload's object
     * @param pid : The identifier of the payload to remove
     * @throws IndexerException if there were errors during removal
     */
    @Override
    public void remove(String oid, String pid) throws IndexerException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Perform a Solr search and stream the results into the provided output
     * 
     * @param request : A prepared SearchRequest object
     * @param response : The OutputStream to send results to
     * @throws IndexerException if there were errors during the search
     */
    @Override
    public void search(SearchRequest request, OutputStream response)
            throws IndexerException {
        // TODO Auto-generated method stub
        
    }
    
    
}
