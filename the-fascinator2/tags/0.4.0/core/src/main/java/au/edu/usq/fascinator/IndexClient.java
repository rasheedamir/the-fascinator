/* 
 * The Fascinator - Core
 * Copyright (C) 2009 University of Southern Queensland
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

package au.edu.usq.fascinator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

/**
 * Index Client class to Re-index the storage
 * 
 * @author Linda Octalina
 * 
 */
public class IndexClient {
    /** Date format **/
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format **/
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default indexer type will be used if none defined **/
    private static final String DEFAULT_INDEXER_TYPE = "solr";

    /** Default storage type will be used if none defined **/
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Logging **/
    private static Logger log = LoggerFactory.getLogger(IndexClient.class);

    /** configuration file **/
    private File configFile;

    /** JsonConfiguration for the configuration file **/
    private JsonConfig config;

    /** rules file **/
    private File rulesFile;

    /** Indexer **/
    private Indexer indexer;

    /** Indexed storage and real storage **/
    private Storage storage, realStorage;

    /**
     * IndexClient Constructor
     * 
     * @throws IOException
     */
    public IndexClient() throws IOException {
        config = new JsonConfig();
        configFile = config.getSystemFile();
        setSetting();
    }

    /**
     * IndexClient Constructor
     * 
     * @param jsonFile
     * @throws IOException
     */
    public IndexClient(File jsonFile) throws IOException {
        configFile = jsonFile;
        config = new JsonConfig(jsonFile);
        setSetting();
    }

    /**
     * Set the default setting
     */
    public void setSetting() {
        // Get the storage type to be indexed...
        try {
            realStorage = PluginManager.getStorage(config.get("storage/type",
                    DEFAULT_STORAGE_TYPE));
            indexer = PluginManager.getIndexer(config.get("indexer/type",
                    DEFAULT_INDEXER_TYPE));
            storage = new IndexedStorage(realStorage, indexer);
            storage.init(configFile);
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());
        } catch (Exception e) {
            log.error("Failed to initialise storage", e);
            return;
        }
    }

    /**
     * Start to run the indexing
     */
    public void run() {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        rulesFile = new File(configFile.getParentFile(), config
                .get("indexer/script/rules"));
        log.debug("rulesFile=" + rulesFile);

        String rulesOid;
        try {
            log.debug("Caching rules file " + rulesFile);
            DigitalObject rulesObject = new RulesDigitalObject(rulesFile);
            realStorage.addObject(rulesObject);
            log.debug("Realstorage: " + realStorage.getId());
            rulesOid = rulesObject.getId();
        } catch (StorageException se) {
            log.error("Failed to cache indexing rules, stopping", se);
            return;
        }

        // List all the DigitalObject in the storages
        List<DigitalObject> objectList = realStorage.getObjectList();
        for (DigitalObject object : objectList) {
            try {
                processObject(object, rulesOid, config.getMap("indexer/params"));
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Indexing single object
     * 
     * TODO: Might let the user to fill in form in the portal regards to which
     * rules to be used
     * 
     * @param objectId
     */
    public void indexObject(String objectId) {
        DigitalObject object = realStorage.getObject(objectId);
        // Get the rules from SOF-META
        Properties sofMeta = getSofMeta(object);
        String sofMetaRulesOid = sofMeta.getProperty("rulesOid");
        rulesFile = new File(sofMetaRulesOid);

        String rulesOid;
        try {
            log.debug("Caching rules file " + rulesFile);
            DigitalObject rulesObject = new RulesDigitalObject(rulesFile);
            realStorage.addObject(rulesObject);
            log.debug("Realstorage: " + realStorage.getId());
            rulesOid = rulesObject.getId();
        } catch (StorageException se) {
            log.error("Failed to cache indexing rules, stopping", se);
            return;
        }
        try {
            processObject(object, rulesOid, null);
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Index objects found in the portal
     * 
     * @param portalName
     */
    public void indexPortal(String portalQuery) {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        // Get all the records from solr
        int startRow = 0;
        int numPerPage = 5;
        int numFound = 0;
        do {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            SearchRequest request = new SearchRequest("*:*");
            request.addParam("rows", String.valueOf(numPerPage));
            request.addParam("fq", "item_type:\"object\"");
            request.setParam("start", String.valueOf(startRow));

            if (portalQuery != "" && portalQuery != null) {
                request.addParam("fq", portalQuery);
            }

            try {
                indexer.search(request, result);
                JsonConfigHelper js;

                js = new JsonConfigHelper(result.toString());
                for (Object oid : js.getList("response/docs/id")) {
                    DigitalObject object = realStorage
                            .getObject(oid.toString());
                    log.info("Indexing: " + object.getId());
                    Properties sofMeta = getSofMeta(object);
                    String sofMetaRulesOid = sofMeta.getProperty("rulesOid");
                    rulesFile = new File(sofMetaRulesOid);
                    String rulesOid;
                    try {
                        log.debug("Caching rules file " + rulesFile);
                        DigitalObject rulesObject = new RulesDigitalObject(
                                rulesFile);
                        realStorage.addObject(rulesObject);
                        log.debug("Realstorage: " + realStorage.getId());
                        rulesOid = rulesObject.getId();
                        processObject(object, rulesOid, null);
                    } catch (StorageException se) {
                        log.error("Failed to cache indexing rules, stopping",
                                se);
                        return;
                    }
                }

                startRow += numPerPage;
                numFound = Integer.parseInt(js.get("response/numFound"));

            } catch (IndexerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (startRow < numFound);

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Start to process indexing
     * 
     * @param object
     * @param rulesOid
     * @param indexerParams
     * @return
     * @throws StorageException
     * @throws IOException
     */
    private String processObject(DigitalObject object, String rulesOid,
            Map<String, Object> indexerParams) throws StorageException,
            IOException {
        String oid = object.getId();
        String sid = null;

        log.info("Processing " + oid + "...");
        // Properties oldSofMeta = getSofMeta(object);
        // Properties sofMeta = new Properties(oldSofMeta);
        // sofMeta.setProperty("objectId", oid);
        // Payload metadata = object.getMetadata();
        // String metaPid = "";
        // if (metadata != null) {
        // metaPid = metadata.getId();
        // } else {
        // // get the meta id and the repository information from the old
        // // sof-meta
        // if (oldSofMeta != null) {
        // metaPid = oldSofMeta.getProperty("metaPid");
        // log.info("************* " + sofMeta + oldSofMeta
        // + oldSofMeta.getProperty("repository.name"));
        // sofMeta.setProperty("repository.name", oldSofMeta
        // .getProperty("repository.name"));
        // sofMeta.setProperty("repository.type", oldSofMeta
        // .getProperty("repository.type"));
        // }
        // }
        // sofMeta.setProperty("metaPid", metaPid);
        // sofMeta.setProperty("scriptType", config.get("indexer/script/type",
        // "python"));
        // sofMeta.setProperty("rulesOid", rulesOid);
        // sofMeta.setProperty("rulesPid", rulesFile.getName());
        //
        // if (indexerParams != null) {
        // for (String key : indexerParams.keySet()) {
        // sofMeta.setProperty(key, indexerParams.get(key).toString());
        // }
        // }
        //
        // ByteArrayOutputStream sofMetaOut = new ByteArrayOutputStream();
        // // Remove the old sof-meta
        // realStorage.removePayload(object.getId(), "SOF-META");
        //
        // // Store new sof-meta
        // sofMeta.store(sofMetaOut, "The Fascinator Indexer Metadata2");
        // GenericPayload sofMetaDs = new GenericPayload("SOF-META",
        // "The Fascinator Indexer Metadata", "text/plain");
        // sofMetaDs.setInputStream(new ByteArrayInputStream(sofMetaOut
        // .toByteArray()));
        // sofMetaDs.setType(PayloadType.Annotation);
        // storage.addPayload(oid, sofMetaDs);
        try {
            indexer.index(oid);
        } catch (IndexerException e) {
            e.printStackTrace();
        }

        return sid;
    }

    /**
     * Main function of IndexClient
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: index <json-config>");
        } else {
            File jsonFile = new File(args[0]);
            try {
                IndexClient index = new IndexClient(jsonFile);
                index.run();
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            }
        }
    }

    /**
     * Getting the sofMeta properties for the DigitalObject
     * 
     * @param object
     * @return properties
     */
    private Properties getSofMeta(DigitalObject object) {
        try {
            Payload sofMetaPayload = object.getPayload("SOF-META");
            Properties sofMeta = new Properties();
            sofMeta.load(sofMetaPayload.getInputStream());
            return sofMeta;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
