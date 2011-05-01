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
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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

/**
 * Consumer for rendering transformers. Jobs in this queue are generally longer
 * running running processes and are started after the initial harvest.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class RenderQueueConsumer implements MessageListener {

    /** Render queue string */
    public static final String RENDER_QUEUE = "render";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(RenderQueueConsumer.class);

    /** JSON configuration */
    private JsonConfig config;

    /** Indexer object */
    private Indexer indexer;

    /** Storage */
    private Storage storage;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Messaging service instance */
    private MessagingServices services;

    /** Name identifier to be put in the queue */
    private String name;

    /**
     * Render Queue Consumer constructor
     * 
     * @param name name identifier
     * @throws IOException if the configuration file not found
     */
    public RenderQueueConsumer(String name) throws IOException {
        this.name = name;
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
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
        }
    }

    /**
     * Start the queue based on the name identifier
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    public void start() throws JMSException {
        log.info("Starting {}...", name);
        services = MessagingServices.getInstance();
        Session session = services.getSession();
        Destination destination = session.createQueue(RENDER_QUEUE);
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
    }

    /**
     * Stop the Render Queue Consumer. Including stopping the storage and
     * indexer
     */
    public void stop() {
        log.info("Stopping {}...", name);
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
        services.release();
    }

    @Override
    public void onMessage(Message message) {
        MDC.put("name", name);
        try {
            String text = ((TextMessage) message).getText();
            JsonConfig config = new JsonConfig(text);
            String oid = config.get("oid");
            String renderList = config.get("renderList");
            boolean commit = Boolean.parseBoolean(config.get("commit", "false"));

            sendNotification(oid, "renderStart", "Renderer starting : '" + oid
                    + "'");
            log.info("Received job, object id={}", oid);
            log.info("Updating object...");
            DigitalObject object = storage.getObject(oid);
            ConveyerBelt conveyerBelt = new ConveyerBelt(text,
                    ConveyerBelt.RENDER);
            if (renderList != null) {
                object = conveyerBelt.transform(object, renderList);
            } else {
                object = conveyerBelt.transform(object);
            }
            log.info("Indexing object...");
            indexer.index(object.getId());
            if (commit) {
                indexer.commit();
            }
            sendNotification(oid, "renderComplete", "Renderer complete : '"
                    + oid + "'");
            // update object metadata
            Properties props = object.getMetadata();
            props.setProperty("render-pending", "false");

            object.close();
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (StorageException se) {
            log.error("Failed to update storage: {}", se.getMessage());
        } catch (TransformerException te) {
            log.error("Failed to transform object: {}", te.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        }
    }

    /**
     * Send the notification to the queue
     * 
     * @param oid Object id
     * @param status Status of the object
     * @param message Message to be sent
     */
    private void sendNotification(String oid, String status, String message) {
        JsonConfigHelper jsonMessage = new JsonConfigHelper();
        jsonMessage.set("id", oid);
        jsonMessage.set("idType", "object");
        jsonMessage.set("status", status);
        jsonMessage.set("message", message);
        services.publishMessage(MessagingServices.MESSAGE_TOPIC, jsonMessage
                .toString());
    }
}
