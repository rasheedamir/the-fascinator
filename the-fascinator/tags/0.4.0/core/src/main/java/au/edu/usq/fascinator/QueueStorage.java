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
import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

/**
 * Wraps a storage plugin and queues jobs when modifying the storage layer
 * 
 * @author Oliver Lucido
 */
public class QueueStorage implements Storage {

    private Logger log = LoggerFactory.getLogger(QueueStorage.class);

    private Storage storage;

    private File jsonFile;

    private Connection connection;

    private Session session;

    private MessageProducer producer;

    public QueueStorage(Storage storage, File jsonFile) {
        this.storage = storage;
        this.jsonFile = jsonFile;
    }

    public String addObject(DigitalObject object) throws StorageException {
        String sid = storage.addObject(object);
        queueHarvest(object.getId(), jsonFile);
        return sid;
    }

    public void addPayload(String oid, Payload payload) {
        storage.addPayload(oid, payload);
    }

    public DigitalObject getObject(String oid) {
        return storage.getObject(oid);
    }

    public Payload getPayload(String oid, String pid) {
        return storage.getPayload(oid, pid);
    }

    public void removeObject(String oid) {
        storage.removeObject(oid);
        queueDelete(oid, jsonFile);
    }

    public void removePayload(String oid, String pid) {
        storage.removePayload(oid, pid);
    }

    public String getId() {
        return storage.getId();
    }

    public String getName() {
        return storage.getName();
    }

    public void init(File jsonFile) throws PluginException {
        storage.init(jsonFile);
        try {
            initConnection(new JsonConfig(jsonFile));
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    @Override
    public void init(String jsonString) throws PluginException {
        storage.init(jsonString);
        try {
            initConnection(new JsonConfig(jsonString));
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    public void shutdown() throws PluginException {
        storage.shutdown();
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
    }

    @Override
    public List<DigitalObject> getObjectList() {
        return storage.getObjectList();
    }

    private void initConnection(JsonConfig config) {
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

    private void queueHarvest(String oid, File jsonFile) {
        try {
            JsonConfigHelper json = new JsonConfigHelper(jsonFile);
            json.set("oid", oid);
            TextMessage message = session.createTextMessage(json.toString());
            producer.send(message);
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (JMSException jmse) {
            log.error("Failed to send message: {}", jmse.getMessage());
        }
    }

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
}
