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
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Consumer for harvest transformers. Jobs in this queue should be short running
 * processes as they are run at harvest time.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class HarvestQueueConsumer implements MessageListener {

    public static final String HARVEST_QUEUE = "harvest";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HarvestQueueConsumer.class);

    private JsonConfig config;

    private Indexer indexer;

    private Storage storage;

    private Connection connection;

    private Session session;

    private MessageConsumer consumer;

    private MessageProducer producer;

    private MessageSender sender;

    public HarvestQueueConsumer()
            throws IOException, JAXBException, PluginException {
        try {
            config = new JsonConfig();
            File sysFile = JsonConfig.getSystemFile();
            indexer = PluginManager.getIndexer(config.get("indexer/type",
                    "solr"));
            indexer.init(sysFile);
            storage = PluginManager.getStorage(config.get("storage/type",
                    "file-system"));
            storage.init(sysFile);
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }
    }

    public void start() throws JMSException {
        log.info("Starting harvest consumer...");
        String brokerUrl = config.get("messaging/url",
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination harvestDest = session.createQueue(HARVEST_QUEUE);
        consumer = session.createConsumer(harvestDest);
        consumer.setMessageListener(this);
        Destination renderDest = session
                .createQueue(RenderQueueConsumer.RENDER_QUEUE);
        producer = session.createProducer(renderDest);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);

        sender = new MessageSender(MessageSender.MESSAGE_QUEUE);
    }

    public void stop() {
        log.info("Stopping harvest consumer...");
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
            }
        }
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
            }
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer: {}", jmse.getMessage());
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close producer: {}", jmse.getMessage());
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close session: {}", jmse.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close connection: {}", jmse.getMessage());
            }
        }
        if (sender != null) {
            sender.close();
        }
    }

    @Override
    public void onMessage(Message message) {
        MDC.put("name", "harvest");
        try {
            String text = ((TextMessage) message).getText();
            JsonConfig config = new JsonConfig(text);
            String oid = config.get("oid");
            String source = config.get("source");
            String path = config.get("sourceFile", oid);
            log.info("Received job, object id={}", oid, text);
            boolean deleted = Boolean.parseBoolean(config.get("deleted",
                    "false"));
            if (deleted) {
                log.info("Removing object {}...", oid);
                indexer.remove(oid);
            } else {
                // Exctraction
                if (source != null) {
                    log.info("Adding new object {} from {}...", oid, source);
                    File rulesFile = new File(config.get("configDir"), config
                            .get("indexer/script/rules"));
                    processObject(rulesFile, text, oid, path);
                }
                // Indexing
                boolean doIndex = Boolean.parseBoolean(
                        config.get("transformer/indexOnHarvest", "true"));
                if (doIndex) {
                    sendNotification(oid, "indexStart", "Indexing '" + oid + "' started");
                    log.info("Indexing object {}...", oid);
                    indexer.index(oid);
                    sendNotification(oid, "indexComplete", "Index of '" + oid + "' completed");
                }
                log.info("Queuing render job...");
                queueRenderJob(oid, text);
            }
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (StorageException se) {
            log.error("Failed to update storage: {}", se.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        }
    }

    private void sendNotification(String oid, String status, String message) {
        JsonConfigHelper jsonMessage = new JsonConfigHelper();
        jsonMessage.set("id", oid);
        jsonMessage.set("idType", "object");
        jsonMessage.set("status", status);
        jsonMessage.set("message", message);
        sender.sendMessage(jsonMessage.toString());
    }

    private void queueRenderJob(String oid, String json) {
        try {
            TextMessage message = session.createTextMessage(json);
            producer.send(message);
        } catch (JMSException jmse) {
            log.error("Failed to send message: {}", jmse.getMessage());
        }
    }

    private void processObject(File rulesFile, String jsonStr, String oid,
            String path) throws StorageException, IOException {
        try {
            log.info("Processing " + oid + "...");

            // cache the rules file
            StorageUtils.storeFile(storage, rulesFile);

            // get the object
            DigitalObject object = storage.getObject(oid);

            // transform it with just the extractor transformers
            ConveyerBelt conveyerBelt = new ConveyerBelt(jsonStr,
                    ConveyerBelt.EXTRACTOR);
            object = conveyerBelt.transform(object);

            // update object metadata
            Properties props = object.getMetadata();
            // FIXME objectId is redundant now?
            props.setProperty("objectId", object.getId());
            props.setProperty("scriptType", config.get("indexer/script/type"));
            props.setProperty("rulesOid", rulesFile.getAbsolutePath());
            props.setProperty("rulesPid", rulesFile.getName());
            props.setProperty("render-pending", "true");
            props.setProperty("owner", "system");
            Map<String, Object> params = config.getMap("indexer/params");
            for (String key : params.keySet()) {
                props.setProperty(key, params.get(key).toString());
            }

            // done with the object
            object.close();
        } catch (StorageException re) {
            throw new IOException(re);
        } catch (TransformerException te) {
            throw new IOException(te);
        }
    }
}
