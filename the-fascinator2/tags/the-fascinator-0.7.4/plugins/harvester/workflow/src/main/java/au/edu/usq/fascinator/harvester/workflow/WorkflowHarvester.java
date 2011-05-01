/*
 * The Fascinator - Plugin - Harvester - Workflows
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
package au.edu.usq.fascinator.harvester.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * <p>
 * This plugin is a basic harvester for ingesting uploaded content into
 * workflows. It creates the DigitalObject of a source object for the standard
 * harvest/transform/index stack.
 * </p>
 * 
 * <p>
 * A trimmed down version of the file-system harvester but doesn't need
 * recursion or caching.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for workflow harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/workflow/src/main/resources/harvest/workflows/workflow-harvester.json"
 * >workflow-harvester.json</a>
 * </p>
 * 
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Below is the example of workflow stages:
 * 
 * <pre>
 *   "stages": [
 *         {
 *             "name": "pending",
 *             "label": "Pending",
 *             "security": ["metadata", "admin"],
 *             "visibility": ["metadata", "editor", "admin"]
 *         },
 *         {
 *             "name": "metadata",
 *             "label": "Basic Metadata Check",
 *             "security": ["editor", "admin"],
 *             "visibility": ["metadata", "editor", "admin"],
 *             "template": "workflows/basic-init"
 *         },
 *         {
 *             "name": "live",
 *             "label": "Live",
 *             "security": ["editor", "admin"],
 *             "visibility": ["guest"],
 *             "template": "workflows/basic-live"
 *         }
 *     ]
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the workflow harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/workflow/src/main/resources/harvest/workflows/workflow-harvester.py"
 * >workflow-harvester.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <a href=
 * "https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Harvester/Workflow"
 * >https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/
 * Harvester/Workflow</a>
 * </p>
 * 
 * @author Greg Pendlebury
 */
public class WorkflowHarvester extends GenericHarvester {

    /** logging */
    private Logger log = LoggerFactory.getLogger(WorkflowHarvester.class);

    /** flag for forcing local storage */
    private boolean forceLocalStorage;

    /** flag for forcing local update */
    private boolean forceUpdate;

    /** Render chains */
    private Map<String, Map<String, List<String>>> renderChains;

    public WorkflowHarvester() {
        super("workflow-harvester", "Workflow Harvester");
    }

    @Override
    public void init() throws HarvesterException {
        forceLocalStorage = Boolean.parseBoolean(getJsonConfig().get(
                "harvester/workflow-harvester/force-storage", "true"));
        forceUpdate = Boolean.parseBoolean(getJsonConfig().get(
                "harvester/workflow-harvester/force-update", "false"));

        // Order is significant
        renderChains = new LinkedHashMap();
        Map<String, JsonConfigHelper> renderTypes = getJsonConfig().getJsonMap(
                "renderTypes");
        for (String name : renderTypes.keySet()) {
            Map<String, List<String>> details = new HashMap();
            JsonConfigHelper chain = renderTypes.get(name);
            details.put("fileTypes", getList(chain, "fileTypes"));
            details.put("harvestQueue", getList(chain, "harvestQueue"));
            details.put("indexOnHarvest", getList(chain, "indexOnHarvest"));
            details.put("renderQueue", getList(chain, "renderQueue"));
            renderChains.put(name, details);
        }
    }

    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getObjectId(File uploadedFile) throws HarvesterException {
        Set<String> objectIds = new HashSet<String>();
        try {
            objectIds.add(createDigitalObject(uploadedFile));
        } catch (StorageException se) {
            throw new HarvesterException(se);
        }
        return objectIds;
    }

    @Override
    public boolean hasMoreObjects() {
        return false;
    }

    private String createDigitalObject(File file) throws HarvesterException,
            StorageException {
        String objectId;
        DigitalObject object;
        if (forceUpdate) {
            object = StorageUtils.storeFile(getStorage(), file,
                    !forceLocalStorage);
        } else {
            String oid = StorageUtils.generateOid(file);
            String pid = StorageUtils.generatePid(file);
            object = getStorage().createObject(oid);
            if (forceLocalStorage) {
                try {
                    object.createStoredPayload(pid, new FileInputStream(file));
                } catch (FileNotFoundException ex) {
                    throw new HarvesterException(ex);
                }
            } else {
                object.createLinkedPayload(pid, file.getAbsolutePath());
            }

        }
        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("file.path",
                FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
        objectId = object.getId();

        // Store rendition information if we have it
        String ext = FilenameUtils.getExtension(file.getName());
        for (String chain : renderChains.keySet()) {
            Map<String, List<String>> details = renderChains.get(chain);
            if (details.get("fileTypes").contains(ext)) {
                storeList(props, details, "harvestQueue");
                storeList(props, details, "indexOnHarvest");
                storeList(props, details, "renderQueue");
            }
        }

        object.close();
        return objectId;
    }

    /**
     * Get a list of strings from configuration
     * 
     * @param json Configuration object to retrieve from
     * @param field The path to the list
     * @return List<String> The resulting list
     */
    private List<String> getList(JsonConfigHelper json, String field) {
        List<String> result = new ArrayList();
        List<Object> list = json.getList(field);
        for (Object object : list) {
            result.add((String) object);
        }
        return result;
    }

    /**
     * Take a list of strings from a Java Map, concatenate the values together
     * and store them in a Properties object using the Map's original key.
     * 
     * @param props Properties object to store into
     * @param details The full Java Map
     * @param field The key to use in both objects
     */
    private void storeList(Properties props, Map<String, List<String>> details,
            String field) {
        Set<String> valueSet = new LinkedHashSet<String>();
        // merge with original property value if exists
        String currentValue = props.getProperty(field, "");
        if (!"".equals(currentValue)) {
            String[] currentList = currentValue.split(",");
            valueSet.addAll(Arrays.asList(currentList));
        }
        valueSet.addAll(details.get(field));
        String joinedList = StringUtils.join(valueSet, ",");
        props.setProperty(field, joinedList);
    }
}