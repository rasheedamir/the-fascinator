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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * 
 * HarvestClient class to handle harvesting of objects to the storage
 * 
 * @author Oliver Lucido
 * 
 */
public class HarvestClient {

    /** Date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type */
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(HarvestClient.class);

    /** Configuration file */
    private File configFile;

    /** Configuration Digital Object */
    private DigitalObject configObject;

    /** Rule file */
    private File rulesFile;

    /** Rule Digital object */
    private DigitalObject rulesObject;

    /** Uploaded file */
    private File uploadedFile;

    /** Uploaded file object id */
    private String uploadedOid;

    /** File owner for the uploaded file */
    private String fileOwner;

    /** Json configuration */
    private JsonConfig config;

    /** Conveyer belt used for digital object transformation */
    private ConveyerBelt conveyerBelt;

    /** Storage to store the digital object */
    private Storage storage;

    /** Connection to Queue */
    private Connection connection;

    /** Session of the connection */
    private Session session;

    /** Message producer */
    private MessageProducer producer;

    /**
     * Harvest Client Constructor
     * 
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient() throws HarvesterException {
        this(null, null, null);
    }

    /**
     * Harvest Client Constructor
     * 
     * @param configFile configuration file
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient(File configFile) throws HarvesterException {
        this(configFile, null, null);
    }

    /**
     * Harvest Client Constructor
     * 
     * @param configFile Configuration file
     * @param uploadedFile Uploaded file
     * @param owner Owner of the file
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient(File configFile, File uploadedFile, String owner)
            throws HarvesterException {
        MDC.put("name", "client");

        this.configFile = configFile;
        this.uploadedFile = uploadedFile;
        fileOwner = owner;

        try {
            if (configFile == null) {
                config = new JsonConfig();
            } else {
                config = new JsonConfig(configFile);
                rulesFile = new File(configFile.getParent(), config
                        .get("indexer/script/rules"));
            }
        } catch (IOException ioe) {
            throw new HarvesterException("Failed to read configuration file: '"
                    + configFile + "'");
        }

        // initialise storage system
        String storageType = config.get("storage/type", DEFAULT_STORAGE_TYPE);
        storage = PluginManager.getStorage(storageType);
        if (storage == null) {
            throw new HarvesterException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            storage.init(config.toString());
            log.info("Loaded {}", storage.getName());
        } catch (PluginException pe) {
            throw new HarvesterException("Failed to initialise storage", pe);
        }

        initConnection();
    }

    /**
     * Initialising connection
     */
    private void initConnection() {
        try {
            String brokerUrl = config.get("messaging/url",
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    brokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session
                    .createQueue(HarvestQueueConsumer.HARVEST_QUEUE);
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        } catch (JMSException jmse) {
            log.error("Failed to start connection: {}", jmse.getMessage());
        }
    }

    /**
     * Start Harvesting Digital objects
     * 
     * @throws PluginException If harvest plugin not found
     */
    public void start() throws PluginException {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        // cache harvester config and indexer rules
        configObject = StorageUtils.storeFile(storage, configFile);
        rulesObject = StorageUtils.storeFile(storage, rulesFile);

        // initialise the harvester
        Harvester harvester = null;
        String harvesterType = config.get("harvester/type");
        harvester = PluginManager.getHarvester(harvesterType, storage);
        if (harvester == null) {
            throw new HarvesterException("Harvester plugin '" + harvesterType
                    + "'. Ensure it is in the classpath.");
        }
        harvester.init(configFile);
        log.info("Loaded harvester: " + harvester.getName());

        // initialise the extractor conveyer belt
        conveyerBelt = new ConveyerBelt(configFile, ConveyerBelt.EXTRACTOR);

        if (uploadedFile != null) {
            // process the uploaded file only
            try {
                Set<String> objectIds = harvester.getObjectId(uploadedFile);
                if (!objectIds.isEmpty()) {
                    uploadedOid = objectIds.iterator().next();
                    processObject(uploadedOid);
                }
            } catch (HarvesterException e) {
                throw new PluginException(e);
            }
        } else {
            // process harvested objects
            do {
                for (String oid : harvester.getObjectIdList()) {
                    processObject(oid);
                }
            } while (harvester.hasMoreObjects());
            // process deleted objects
            do {
                for (String oid : harvester.getDeletedObjectIdList()) {
                    storage.removeObject(oid);
                    queueDelete(oid, configFile);
                }
            } while (harvester.hasMoreObjects());
        }

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Reharvest Digital Object when there's request to reharvest from the
     * portal
     * 
     * @param oid Object Id
     * @throws IOException If necessary files not found
     * @throws PluginException If the harvester plugin not found
     */
    public void reharvest(String oid) throws IOException, PluginException {
        log.info("Reharvest '{}'...", oid);

        // get the object from storage
        DigitalObject object = storage.getObject(oid);

        // get its harvest config
        boolean usingTempFile = false;
        String configOid = object.getMetadata().getProperty("jsonConfigOid");
        if (configOid == null) {
            log.warn("No harvest config for '{}', using defaults...");
            configFile = JsonConfig.getSystemFile();
        } else {
            log.debug("Using config from '{}'", configOid);
            DigitalObject configObj = storage.getObject(configOid);
            Payload payload = configObj.getPayload(configObj.getSourceId());
            configFile = File.createTempFile("reharvest", ".json");
            OutputStream out = new FileOutputStream(configFile);
            IOUtils.copy(payload.open(), out);
            out.close();
            payload.close();
            configObj.close();
            usingTempFile = true;
        }

        // run extractor transformers
        conveyerBelt = new ConveyerBelt(configFile, ConveyerBelt.EXTRACTOR);
        object = conveyerBelt.transform(object);
        object.close();

        // queue for rendering
        queueHarvest(oid, configFile, true);
        log.info("Object '{}' now queued for reindexing...", oid);

        // cleanup
        if (usingTempFile) {
            configFile.delete();
        }
    }

    /**
     * Shutdown Harvester Client. Including: Storage, Message Producer, Session
     * and Connection
     */
    public void shutdown() {
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage", pe);
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close producer", jmse);
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close session", jmse);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close connection", jmse);
            }
        }
    }

    /**
     * Process/transform each objects
     * 
     * @param oid Object Id
     * @throws StorageException If storage is not found
     * @throws TransformerException If transformer fail to transform the object
     */
    private void processObject(String oid) throws TransformerException,
            StorageException {
        // get the object
        DigitalObject object = storage.getObject(oid);

        // transform it with just the extractor transformers
        object = conveyerBelt.transform(object);

        // update object metadata
        Properties props = object.getMetadata();
        // FIXME objectId is redundant now?
        props.setProperty("objectId", object.getId());
        props.setProperty("scriptType", config.get("indexer/script/type"));
        if (props.getProperty("rulesOid") == null) {
            props.setProperty("rulesOid", rulesObject.getId());
            props.setProperty("rulesPid", rulesObject.getSourceId());
        }
        if (props.getProperty("jsonConfigOid") == null) {
            props.setProperty("jsonConfigOid", configObject.getId());
            props.setProperty("jsonConfigPid", configObject.getSourceId());
        }
        if (fileOwner != null) {
            props.setProperty("owner", fileOwner);
        }
        Map<String, Object> params = config.getMap("indexer/params");
        for (String key : params.keySet()) {
            props.setProperty(key, params.get(key).toString());
        }

        // done with the object
        object.close();

        // queue the object for indexing
        queueHarvest(oid, configFile);
    }

    /**
     * To queue object to be processed
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     */
    private void queueHarvest(String oid, File jsonFile) {
        queueHarvest(oid, jsonFile, false);
    }

    /**
     * To queue object to be processed
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     */
    private void queueHarvest(String oid, File jsonFile, boolean commit) {
        try {
            JsonConfigHelper json = new JsonConfigHelper(jsonFile);
            json.set("oid", oid);
            if (commit) {
                json.set("commit", "true");
            }
            TextMessage message = session.createTextMessage(json.toString());
            producer.send(message);
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (JMSException jmse) {
            log.error("Failed to send message: {}", jmse.getMessage());
        }
    }

    /**
     * To delete object processing from queue
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     */
    private void queueDelete(String oid, File jsonFile) {
        try {
            JsonConfigHelper json = new JsonConfigHelper(jsonFile);
            json.set("oid", oid);
            json.set("deleted", "true");
            TextMessage message = session.createTextMessage(json.toString());
            producer.send(message);
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (JMSException jmse) {
            log.error("Failed to send message: {}", jmse.getMessage());
        }
    }

    /*
     * Useful only for uploaded files.
     * 
     * @return The object ID the uploaded file was given by harvester.
     */
    public String getUploadOid() {
        if (uploadedFile == null) {
            return null;
        } else {
            return uploadedOid;
        }
    }

    /**
     * Main method for Harvest Client
     * 
     * @param args Argument list
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: harvest <json-config>");
        } else {
            File jsonFile = new File(args[0]);
            try {
                HarvestClient harvest = new HarvestClient(jsonFile);
                harvest.start();
                harvest.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to initialise client: ", pe);
            }
        }
    }
}
