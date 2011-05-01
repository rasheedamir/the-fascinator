/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services.impl;

import java.io.File;
import java.io.OutputStream;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.portal.services.IndexerService;

public class IndexerServiceImpl implements IndexerService {

    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private Indexer indexer;

    public IndexerServiceImpl() {
        try {
            JsonConfig config = new JsonConfig();
            indexer = PluginManager.getIndexer(config.get("indexer/type",
                    DEFAULT_INDEXER_TYPE));
            indexer.init(JsonConfig.getSystemFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void index(String oid) throws IndexerException {
        indexer.index(oid);
    }

    @Override
    public void index(String oid, String pid) throws IndexerException {
        indexer.index(oid, pid);
    }

    @Override
    public void commit() {
        indexer.commit();
    }

    @Override
    public void annotate(String oid, String pid) throws IndexerException {
        indexer.annotate(oid, pid);
    }

    @Override
    public void annotateSearch(SearchRequest request, OutputStream result)
            throws IndexerException {
        indexer.annotateSearch(request, result);
    }

    @Override
    public void annotateRemove(String oid) throws IndexerException {
        indexer.annotateRemove(oid);
    }

    @Override
    public void annotateRemove(String oid, String pid) throws IndexerException {
        indexer.annotateRemove(oid, pid);
    }

    @Override
    public void remove(String oid) throws IndexerException {
        indexer.remove(oid);
    }

    @Override
    public void remove(String oid, String pid) throws IndexerException {
        indexer.remove(oid, pid);
    }

    @Override
    public void search(SearchRequest request, OutputStream result)
            throws IndexerException {
        indexer.search(request, result);
    }

    @Override
    public String getId() {
        return indexer.getId();
    }

    @Override
    public String getName() {
        return indexer.getName();
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

    @Override
    public void init(File jsonFile) throws PluginException {
        indexer.init(jsonFile);
    }

    @Override
    public void shutdown() throws PluginException {
        indexer.shutdown();
    }

    @Override
    public void init(String jsonString) throws PluginException {
        indexer.init(jsonString);
    }
}
