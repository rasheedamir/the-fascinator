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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class HarvestClient {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private static Logger log = LoggerFactory.getLogger(HarvestClient.class);

    private JsonConfig config;

    private File configFile;

    private File rulesFile;

    private ConveyerBelt cb;

    public HarvestClient(File jsonFile) throws IOException {
        configFile = jsonFile;
        config = new JsonConfig(jsonFile);
        cb = new ConveyerBelt(jsonFile);
    }

    public void run() {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        Storage storage, realStorage;
        try {
            realStorage = PluginManager.getStorage(config.get("storage/type",
                    DEFAULT_STORAGE_TYPE));
            Indexer indexer = PluginManager.getIndexer(config.get(
                    "indexer/type", DEFAULT_INDEXER_TYPE));
            storage = new IndexedStorage(realStorage, indexer);
            storage.init(configFile);
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());
        } catch (Exception e) {
            log.error("Failed to initialise storage", e);
            return;
        }

        rulesFile = new File(configFile.getParentFile(), config
                .get("indexer/script/rules"));
        log.debug("rulesFile=" + rulesFile);

        String rulesOid;
        try {
            log.debug("Caching rules file " + rulesFile);
            DigitalObject rulesObject = new RulesDigitalObject(rulesFile);
            realStorage.addObject(rulesObject);
            rulesOid = rulesObject.getId();
        } catch (StorageException se) {
            log.error("Failed to cache indexing rules, stopping", se);
            return;
        }

        String harvesterType = config.get("harvester/type");
        Harvester harvester;
        try {
            harvester = PluginManager.getHarvester(harvesterType);
            if (harvester == null) {
                throw new PluginException("Harvester plugin not found: "
                        + harvesterType);
            }
            harvester.init(configFile);
            log.info("Loaded harvester: " + harvester.getName());
        } catch (PluginException pe) {
            log.error("Failed to initialise harvester plugin", pe);
            return;
        }

        do {
            try {
                for (DigitalObject item : harvester.getObjects()) {
                    try {
                        processObject(storage, item, rulesOid);
                    } catch (Exception e) {
                        log.warn("Processing failed: " + item.getId(), e);
                    }
                }
            } catch (HarvesterException he) {
                log.error("Failed to harvest", he);
            }
        } while (harvester.hasMoreObjects());

        try {
            storage.shutdown();
        } catch (PluginException e) {
            log.error("Failed to shutdown storage", e);
        }

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    private String processObject(Storage storage, DigitalObject object,
            String rulesOid) throws StorageException, IOException {
        String oid = object.getId();
        String sid = null;
        try {
            log.info("Processing " + oid + "...");

            // Calling conveyer
            object = cb.transform(object);

            Properties sofMeta = new Properties();
            sofMeta.setProperty("objectId", oid);
            Payload metadata = object.getMetadata();
            if (metadata != null) {
                sofMeta.setProperty("metaPid", metadata.getId());
            }
            sofMeta.setProperty("scriptType", config.get("indexer/script/type",
                    "python"));
            sofMeta.setProperty("rulesOid", rulesOid);
            sofMeta.setProperty("rulesPid", rulesFile.getName());

            Map<String, Object> indexerParams = config.getMap("indexer/params");
            for (String key : indexerParams.keySet()) {
                sofMeta.setProperty(key, indexerParams.get(key).toString());
            }
            ByteArrayOutputStream sofMetaOut = new ByteArrayOutputStream();
            sofMeta.store(sofMetaOut, "The Fascinator Indexer Metadata");
            GenericPayload sofMetaDs = new GenericPayload("SOF-META",
                    "The Fascinator Indexer Metadata", "text/plain");
            sofMetaDs.setInputStream(new ByteArrayInputStream(sofMetaOut
                    .toByteArray()));
            sofMetaDs.setType(PayloadType.Annotation);
            storage.addPayload(oid, sofMetaDs);

            storage.addObject(object);

        } catch (StorageException re) {
            throw new IOException(re.getMessage());
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sid;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: harvest <json-config>");
        } else {
            File jsonFile = new File(args[0]);
            try {
                HarvestClient harvest = new HarvestClient(jsonFile);
                harvest.run();
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            }
        }
    }
}
