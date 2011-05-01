/*
 * The Fascinator - Core - DiReCt Messaging Service
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

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * A message processing class for both incoming and outgoing messages to the
 * DiReCt server. Messaging framework adapted from the RenderQueueConsumer.
 *
 * @author Greg Pendlebury
 */
public class DirectMessageService implements MessageListener {

    public static final String MESSAGE_QUEUE = "direct";
    public static final String DIRECT_QUEUE  = "toDirect";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(DirectMessageService.class);

    private JsonConfig config;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer producer;
    private Indexer indexer;
    private Storage storage;

    public DirectMessageService() throws IOException {
        try {
            File sysFile = JsonConfig.getSystemFile();
            config = new JsonConfig(sysFile);
            storage = PluginManager.getStorage(config.get("storage/type", "file-system"));
            storage.init(sysFile);
            indexer = PluginManager.getIndexer(config.get("indexer/type", "solr"));
            indexer.init(sysFile);
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    public void start() throws JMSException {
        log.info("Starting DiReCt message service...");
        String brokerUrl = config.get("messaging/url",
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Message coming from either direction
        Destination destination = session.createQueue(MESSAGE_QUEUE);
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);

        // Message heading out to our DiReCt API
        Destination destNewDirectItems = session.createQueue(DIRECT_QUEUE);
        producer = session.createProducer(destNewDirectItems);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public void stop() {
        log.info("Stopping DiReCt message service...");
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer: {}", jmse.getMessage());
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
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
            }
        }
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        MDC.put("name", MESSAGE_QUEUE);
        try {
            String text = ((TextMessage) message).getText();
            // Verify the json is valid
            JsonConfig msgJson = new JsonConfig(text);
            String messageType = msgJson.get("messageType");
            if (messageType != null && messageType.equals(
                    DirectIncomingApi.EXTERNAL_QUEUE)) {
                // Messages coming back from DiReCt
                log.debug("Incoming message from DiReCt : '{}'", text);
                if(!processDiReCtMessage(msgJson)) {
                    log.error("Failed processing DiReCt Message: {}", text);
                }
            } else {
                // Messages coming from the workflow system
                log.debug("Sending message to DiReCt : '{}'", text);
                // Send it to DiReCt
                String newText = prepareNewDiReCtSubmission(msgJson);
                if (newText != null) {
                    TextMessage newMessage = session.createTextMessage(newText);
                    producer.send(newMessage);
                }
            }
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }

    private String prepareNewDiReCtSubmission(JsonConfig incoming) {
        String oid = incoming.get("oid");
        JsonConfigHelper indexData;
        try {
            DigitalObject object = storage.getObject(oid);
            Payload directMetadata = object.getPayload("direct.index");
            indexData = new JsonConfigHelper(directMetadata.open());
            directMetadata.close();
        } catch (Exception ex) {
            log.error("Error retrieving object metadata: {}", ex.getMessage());
            return null;
        }

        JsonConfigHelper outgoing = new JsonConfigHelper();
        outgoing.set("ma.identifier",   oid);
        outgoing.set("ma.locator",      config.get("url_base") + "default/detail/" + oid);
        outgoing.set("ma.title",        indexData.get("dc_title"));
        outgoing.set("ma.description",  indexData.get("dc_description"));
        outgoing.set("ma.format",       indexData.get("dc_format"));
        outgoing.set("ma.creator",      indexData.get("dc_creator"));
        outgoing.set("ma.contributor",  indexData.get("dc_contributor"));
        outgoing.set("ma.location",     indexData.get("dc_location"));
        outgoing.set("ma.language",     null);
        outgoing.set("ma.duration",     indexData.get("dc_duration"));
        outgoing.set("ma.frameSize",    indexData.get("dc_size"));
        outgoing.set("usq.credits",     indexData.get("usq_credits"));
        outgoing.set("dc.available",    indexData.get("dc_available"));
        outgoing.set("usq.data_source", null);
        outgoing.set("usq.notes",       indexData.get("notes"));
        outgoing.set("usq.course",      indexData.get("course_code"));
        outgoing.set("usq.year",        indexData.get("course_year"));
        outgoing.set("usq.semester",    indexData.get("course_semester"));

        return outgoing.toString();
    }

    private boolean processDiReCtMessage(JsonConfig incoming) {
        String oid = incoming.get("ma.identifier");
        String key = incoming.get("usq.direct_item_key");
        String copyright = incoming.get("usq.copyright");
        JsonConfigHelper workflow;
        DigitalObject object;

        // Invalid message data
        if (oid == null || key == null) {
            return false;
        }

        try {
            object = storage.getObject(oid);
            Payload wfMeta = object.getPayload("workflow.metadata");
            workflow = new JsonConfigHelper(wfMeta.open());
            wfMeta.close();
        } catch (Exception ex) {
            log.error("Error retrieving workflow metadata: {}", ex.getMessage());
            return false;
        }

        // Confirmation messages
        if (copyright == null) {
            workflow.set("directItemKey", key);
            workflow.set("targetStep", "direct");

        // Completion messages
        } else {
            workflow.set("directItemKey", key);
            workflow.set("targetStep", "live");

            // Security Data
            String security = incoming.get("usq.security");
            if (security != null) {
                workflow.set("directSecurity", security);
            }
            String exceptions = incoming.get("usq.exceptions");
            if (exceptions != null) {
                workflow.set("directSecurityExceptions", exceptions);
            }
            // Copyright Data
            workflow.set("copyright", copyright);
            String notice = incoming.get("usq.notice");
            if (copyright != null) {
                workflow.set("copyrightNotice", notice);
            } else {
                log.error("Copyright flag set, but no notice supplied: ({})", oid);
            }
            // Expiry date
            String expiry = incoming.get("usq.expiry");
            if (expiry != null) {
                workflow.set("expiryDate", expiry);
            }
        }

        // Save updated workflow metadata
        try {
            ByteArrayInputStream inStream =
                new ByteArrayInputStream(workflow.toString().getBytes("UTF-8"));
            StorageUtils.createOrUpdatePayload(object, "workflow.metadata", inStream);
        } catch(StorageException ex) {
            log.error("Error saving workflow data: {}", ex.getMessage());
        } catch(UnsupportedEncodingException ex) {
            log.error("Error decoding workflow data: {}", ex.getMessage());
        }

        try {
            indexer.index(oid);
            return true;

        } catch(IndexerException ex) {
            log.error("Error during index: {}", ex.getMessage());
            return false;
        }
    }
}
