/* 
 * The Fascinator - Plugin API
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
package au.edu.usq.fascinator.api;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;

/**
 * Factory class to get plugin instances
 * 
 * @author Oliver Lucido
 */
public class PluginManager {

    /**
     * Gets a harvester plugin
     * 
     * @param id plugin identifier
     * @return a harvester plugin, or null if not found
     */
    public static Harvester getHarvester(String id) {
        ServiceLoader<Harvester> plugins = ServiceLoader.load(Harvester.class);
        for (Harvester plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    public static Map<String, Harvester> getHarvesterPlugins() {
        Map<String, Harvester> harvesters = new HashMap<String, Harvester>();
        ServiceLoader<Harvester> plugins = ServiceLoader.load(Harvester.class);
        for (Harvester plugin : plugins) {
            harvesters.put(plugin.getId(), plugin);
        }
        return harvesters;
    }

    /**
     * Gets a indexer plugin
     * 
     * @param id plugin identifier
     * @return a indexer plugin, or null if not found
     */
    public static Indexer getIndexer(String id) {
        ServiceLoader<Indexer> plugins = ServiceLoader.load(Indexer.class);
        for (Indexer plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    public static Map<String, Indexer> getIndexerPlugins() {
        Map<String, Indexer> indexers = new HashMap<String, Indexer>();
        ServiceLoader<Indexer> plugins = ServiceLoader.load(Indexer.class);
        for (Indexer plugin : plugins) {
            indexers.put(plugin.getId(), plugin);
        }
        return indexers;
    }

    /**
     * Gets a storage plugin
     * 
     * @param id plugin identifier
     * @return a storage plugin, or null if not found
     */
    public static Storage getStorage(String id) {
        ServiceLoader<Storage> plugins = ServiceLoader.load(Storage.class);
        for (Storage plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    public static Map<String, Storage> getStoragePlugins() {
        Map<String, Storage> storageMap = new HashMap<String, Storage>();
        ServiceLoader<Storage> plugins = ServiceLoader.load(Storage.class);
        for (Storage plugin : plugins) {
            storageMap.put(plugin.getId(), plugin);
        }
        return storageMap;
    }

    /**
     * Gets a transformer plugin
     * 
     * @param id plugin identifier
     * @return a transformer plugin, or null if not found
     */
    public static Transformer getTransformer(String id)
            throws TransformerException {
        ServiceLoader<Transformer> plugins = ServiceLoader
                .load(Transformer.class);
        for (Transformer plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    public static Map<String, Transformer> getTransformerPlugins() {
        Map<String, Transformer> transformers = new HashMap<String, Transformer>();
        ServiceLoader<Transformer> plugins = ServiceLoader
                .load(Transformer.class);
        for (Transformer plugin : plugins) {
            transformers.put(plugin.getId(), plugin);
        }
        return transformers;
    }
}
