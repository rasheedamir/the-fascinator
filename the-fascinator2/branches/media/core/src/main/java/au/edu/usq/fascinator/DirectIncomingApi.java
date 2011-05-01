/*
 * The Fascinator - Core - DiReCt Copyright Complete
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

import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

import java.io.File;
import java.io.IOException;

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
 * An external facing object of the DirectMessageService. This class
 * is solely designed to function as a stable API, allowing us to redesign
 * internal messaging systems if we decide to at a a later date, without
 * breaking API 'contracts'.
 *
 * @author Greg Pendlebury
 */
public class DirectIncomingApi implements MessageListener {

    public static final String EXTERNAL_QUEUE = "directComplete";
    public static final String INTERNAL_QUEUE =
            DirectMessageService.MESSAGE_QUEUE;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(DirectIncomingApi.class);

    private JsonConfig config;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer producer;

    public DirectIncomingApi() throws IOException {
        try {
            File sysFile = JsonConfig.getSystemFile();
            config = new JsonConfig(sysFile);
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    public void start() throws JMSException {
        log.info("Starting DiReCt Incoming API...");
        String brokerUrl = config.get("messaging/url",
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Destinations
        Destination destDirectComplete, destFromDirect;

        // Message coming from the DiReCt system
        destDirectComplete = session.createQueue(EXTERNAL_QUEUE);
        consumer = session.createConsumer(destDirectComplete);
        consumer.setMessageListener(this);

        // Message heading to our Internal message system
        destFromDirect = session.createQueue(INTERNAL_QUEUE);
        producer = session.createProducer(destFromDirect);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public void stop() {
        log.info("Stopping DiReCt Incoming API...");
        try {
            consumer.close();
        } catch (JMSException jmse) {
            log.warn("Failed to close consumer: {}", jmse.getMessage());
        }
        try {
            producer.close();
        } catch (JMSException jmse) {
            log.warn("Failed to close consumer: {}", jmse.getMessage());
        }
    }

    @Override
    public void onMessage(Message message) {
        MDC.put("name", EXTERNAL_QUEUE);
        try {
            String text = ((TextMessage) message).getText();
            // Verify the json is valid
            JsonConfigHelper jsonMessage = new JsonConfigHelper(text);
            log.debug("Incoming API : '{}'", text);
            // Send it to the internal system
            jsonMessage.set("messageType", EXTERNAL_QUEUE);
            TextMessage newMessage = session.createTextMessage(jsonMessage.toString());
            producer.send(newMessage);
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }
}
