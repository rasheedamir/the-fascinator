/*
 * The Fascinator - Core - DiReCt New Items 'Api'
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
 * A queue that 'publishes' new items to DiReCt. A polling script
 * on the DiReCt server will check for new content periodically.
 *
 * @author Greg Pendlebury
 */
public class DirectOutgoingApi implements MessageListener {

    public static final String EXTERNAL_QUEUE = "newDirectItems";
    public static final String INTERNAL_QUEUE =
            DirectMessageService.DIRECT_QUEUE;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(DirectOutgoingApi.class);

    private JsonConfig config;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer producer;

    public DirectOutgoingApi() throws IOException {
        try {
            File sysFile = JsonConfig.getSystemFile();
            config = new JsonConfig(sysFile);
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    public void start() throws JMSException {
        log.info("Starting DiReCt Outgoing API...");
        String brokerUrl = config.get("messaging/url",
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Message coming from our Internal message system
        Destination destToDirect = session.createQueue(INTERNAL_QUEUE);
        consumer = session.createConsumer(destToDirect);
        consumer.setMessageListener(this);

        // Message heading out to the DiReCt system
        Destination destNewDirectItems = session.createQueue(EXTERNAL_QUEUE);
        producer = session.createProducer(destNewDirectItems);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public void stop() {
        log.info("Stopping DiReCt Outgoing API...");
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
        MDC.put("name", INTERNAL_QUEUE);
        try {
            String text = ((TextMessage) message).getText();
            // Verify the json is valid
            JsonConfig msgJson = new JsonConfig(text);
            log.debug("Outgoing API : '{}'", text);
            // Send it to DiReCt
            TextMessage newMessage = session.createTextMessage(text);
            producer.send(newMessage);
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }
}
