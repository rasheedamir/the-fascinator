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
import java.io.IOException;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Consumer for Ingest Queue. Jobs in this queue should be short running
 * processes as they are run at harvest time.
 * 
 * @author Linda Octalina
 */
public class IngestQueueConsumer implements GenericListener {

    /** Harvest Queue name */
    public static final String INGEST_QUEUE = "ingest";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(IngestQueueConsumer.class);

    /** Render queue string */
    private String QUEUE_ID;

    /** Name identifier to be put in the queue */
    private String name;

    /** JSON configuration */
    private JsonConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

    // /** Render Queues */
    // private Map<String, Queue> renderers;

    // /** Render Queue Names */
    // private Map<String, String> rendererNames;

    /** Indexer object */
    private Indexer indexer;

    /** Storage */
    private Storage storage;

    /** Messaging Consumer */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** Thread reference */
    private Thread thread;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public IngestQueueConsumer() {
        thread = new Thread(this, INGEST_QUEUE);
    }

    /**
     * Start thread running
     * 
     */
    @Override
    public void run() {
        try {
            log.info("Starting {}", name);

            // Get a connection to the broker
            String brokerUrl = globalConfig.get("messaging/url",
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    brokerUrl);
            connection = connectionFactory.createConnection();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            consumer = session.createConsumer(session.createQueue(QUEUE_ID));
            consumer.setMessageListener(this);

            // renderers = new LinkedHashMap();
            // for (String selector : rendererNames.keySet()) {
            // renderers.put(selector, session.createQueue(rendererNames
            // .get(selector)));
            // }
            producer = session.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            connection.start();
        } catch (JMSException ex) {
            log.error("Error starting message thread!", ex);
        }
    }

    /**
     * Initialization method
     * 
     * @param config Configuration to use
     * @throws IOException if the configuration file not found
     */
    @Override
    public void init(JsonConfigHelper config) throws Exception {
        try {
            name = config.get("config/name");
            QUEUE_ID = name;
            thread.setName(name);

            globalConfig = new JsonConfig();
            File sysFile = JsonConfig.getSystemFile();
            indexer = PluginManager.getIndexer(globalConfig.get("indexer/type",
                    "solr"));
            indexer.init(sysFile);
            storage = PluginManager.getStorage(globalConfig.get("storage/type",
                    "file-system"));
            storage.init(sysFile);

            // // Setup render queue logic
            // rendererNames = new LinkedHashMap();
            // String userQueue = config.get("config/user-renderer");
            // rendererNames.put(ConveyerBelt.CRITICAL_USER_SELECTOR,
            // userQueue);
            // Map<String, Object> map =
            // config.getMap("config/normal-renderers");
            // for (String selector : map.keySet()) {
            // rendererNames.put(selector, (String) map.get(selector));
            // }

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }
    }

    /**
     * Return the ID string for this listener
     * 
     */
    @Override
    public String getId() {
        return INGEST_QUEUE;
    }

    /**
     * Start the ingest queue consumer
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Stop the Ingest Queue consumer. Including: indexer and storage
     */
    @Override
    public void stop() throws Exception {
        log.info("Stopping {}...", name);
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
                throw pe;
            }
        }
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
                throw pe;
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close producer: {}", jmse);
            }
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer: {}", jmse.getMessage());
                throw jmse;
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer session: {}", jmse);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close connection: {}", jmse);
            }
        }
    }

    /**
     * Callback function for incoming messages.
     * 
     * @param message The incoming message
     */
    @Override
    public void onMessage(Message message) {
        MDC.put("name", name);
        try {
            // Incoming message
            String text = ((TextMessage) message).getText();
            JsonConfigHelper config = new JsonConfigHelper(text);
            String oid = config.get("oid");
            log.info("Received job, object id={}, text={}", oid, text);

            File configFile = new File(config.get("configFile"));
            File uploadedFile = new File(oid);

            Boolean deleted = Boolean.parseBoolean(config.get("deleted",
                    "false"));
            try {
                HarvestClient harvestClient = new HarvestClient(configFile,
                        uploadedFile, "guest");
                if (!deleted) {
                    harvestClient.start();
                }
            } catch (PluginException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Delete object
            if (deleted) {
                String objectId = StorageUtils.generateOid(uploadedFile);
                log.info("Removing object {}...", oid);
                storage.removeObject(objectId);
                indexer.remove(objectId);
                indexer.annotateRemove(objectId);
                return;
            }

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        } catch (StorageException e) {
            log.error("Failed to delete object: {}", e.getMessage());
        }
    }

    /**
     * Sets the priority level for the thread. Used by the OS.
     * 
     * @param newPriority The priority level to set the thread at
     */
    @Override
    public void setPriority(int newPriority) {
    }
}
