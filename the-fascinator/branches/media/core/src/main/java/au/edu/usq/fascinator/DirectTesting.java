/*
 * The Fascinator - Core - DiReCt Testing
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

public class DirectTesting implements MessageListener {

    public static final String INC_QUEUE = DirectOutgoingApi.EXTERNAL_QUEUE;
    public static final String OUT_QUEUE = DirectIncomingApi.EXTERNAL_QUEUE;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(DirectTesting.class);

    private JsonConfig config;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private MessageProducer producer;

    public DirectTesting() throws IOException {
        try {
            File sysFile = JsonConfig.getSystemFile();
            config = new JsonConfig(sysFile);
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    public void start() throws JMSException {
        log.info("Starting DiReCt Testing class...");
        String brokerUrl = config.get("messaging/url",
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Message coming from our Internal message system
        Destination destToDirect = session.createQueue(INC_QUEUE);
        consumer = session.createConsumer(destToDirect);
        consumer.setMessageListener(this);

        // Message heading out to the DiReCt system
        Destination destNewDirectItems = session.createQueue(OUT_QUEUE);
        producer = session.createProducer(destNewDirectItems);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public void stop() {
        log.info("Stopping DiReCt Testing...");
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
        MDC.put("name", INC_QUEUE);
        try {
            String newText;
            TextMessage newMessage;

            String text = ((TextMessage) message).getText();
            // Verify the json is valid
            JsonConfig msgJson = new JsonConfig(text);
            log.debug("DiReCt Testing : '{}'", text);

            // Sleep 10s and send confirmation to DiReCt 
            try {Thread.sleep(10000);} catch (Exception e) {}
            newText = prepareConfirmation(msgJson);
            if (newText != null) {
                newMessage = session.createTextMessage(newText);
                producer.send(newMessage);
            }
            // Sleep 30s and send completion to DiReCt 
            try {Thread.sleep(30000);} catch (Exception e) {}
            newText = prepareCompletion(msgJson);
            if (newText != null) {
                newMessage = session.createTextMessage(newText);
                producer.send(newMessage);
            }
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }

    private String prepareConfirmation(JsonConfig incoming) {
        String oid = incoming.get("ma.identifier");

        JsonConfigHelper outgoing = new JsonConfigHelper();
        outgoing.set("ma.identifier",   oid);
        outgoing.set("usq.direct_item_key", "ADIRECTITEMKEY");

        return outgoing.toString();
    }

    private String prepareCompletion(JsonConfig incoming) {
        String oid = incoming.get("ma.identifier");

        JsonConfigHelper outgoing = new JsonConfigHelper();
        outgoing.set("ma.identifier",   oid);
        outgoing.set("usq.direct_item_key", "ADIRECTITEMKEY");

        outgoing.set("usq.security",   "class_id1,class_id2,class_id3");
        outgoing.set("usq.exceptions", "tom,dick,harry");
        outgoing.set("usq.copyright",  "true");
        outgoing.set("usq.notice",     "Some sort of copyright notice");
        outgoing.set("usq.expiry",     "2010-12-31T23:59:59.00Z");

        return outgoing.toString();
    }
}
