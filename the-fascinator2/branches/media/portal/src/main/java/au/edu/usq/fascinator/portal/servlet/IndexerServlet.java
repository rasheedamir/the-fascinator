/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.python.core.PySystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.DirectIncomingApi;
import au.edu.usq.fascinator.DirectMessageService;
import au.edu.usq.fascinator.DirectOutgoingApi;
import au.edu.usq.fascinator.DirectTesting;
import au.edu.usq.fascinator.HarvestQueueConsumer;
import au.edu.usq.fascinator.RenderQueueConsumer;
import au.edu.usq.fascinator.common.FascinatorHome;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * Starts an embedded ActiveMQ message broker, harvest and render queue
 * consumers.
 * 
 * @author Oliver Lucido
 */
@SuppressWarnings("serial")
public class IndexerServlet extends HttpServlet {

    public static final String DEFAULT_MESSAGING_HOME = FascinatorHome
            .getPath("activemq-data");

    private Logger log = LoggerFactory.getLogger(IndexerServlet.class);

    private Timer timer;

    private DirectMessageService directLogic;
    private DirectIncomingApi directIn;
    private DirectOutgoingApi directOut;
    private DirectTesting directTest;

    private HarvestQueueConsumer harvester;

    private List<RenderQueueConsumer> renderers;

    private JsonConfig config;

    /**
     * activemq broker NOTE: Will use Fedora's broker if fedora is running,
     * otherwise use activemq broker
     */
    private BrokerService broker;

    @Override
    public void init() throws ServletException {
        // configure the broker
        try {
            config = new JsonConfig();
            String dataDir = config.get("messaging/home",
                    DEFAULT_MESSAGING_HOME);
            broker = new BrokerService();
            broker.setDataDirectory(dataDir);
            broker.addConnector(config.get("messaging/url",
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL));
            String stompUrl = config.get("messaging/stompUrl");
            if (stompUrl != null) {
                broker.addConnector(stompUrl);
            }
            broker.start();
        } catch (Exception e) {
            log.error("Failed to start broker: {}", e.getMessage());
        }

        // add jars for jython to scan for packages
        String realPath = getServletContext().getRealPath("/");
        if (!realPath.endsWith("/")) {
            realPath += "/";
        }

        String pythonHome = realPath + "WEB-INF/lib";
        Properties props = new Properties();
        props.setProperty("python.home", pythonHome);

        PySystemState.initialize(PySystemState.getBaseProperties(), props,
                new String[] { "" });
        PySystemState.add_classdir(realPath + "WEB-INF/classes");
        PySystemState.add_classdir(realPath + "../../../target/classes");
        PySystemState.add_extdir(pythonHome, true);

        // use a timer to start the indexer because tomcat's deployment order
        // is not reliable, fedora may not have started yet thus the indexer
        // is not able to subscribe to fedora messages. the timer will retry
        // every 15 secs if it was unable to start initially

        timer = new Timer("StartIndexer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                startIndexer();
            }
        }, 0, 15000);
    }

    private void startIndexer() {
        log.info("Starting The Fascinator indexer...");
        try {
            if (directLogic == null) {
                directLogic = new DirectMessageService();
            }
            directLogic.start();
            if (directIn == null) {
                directIn = new DirectIncomingApi();
            }
            directIn.start();
            if (directOut == null) {
                directOut = new DirectOutgoingApi();
            }
            directOut.start();
            if (directTest == null) {
                directTest = new DirectTesting();
            }
            directTest.start();

            if (harvester == null) {
                harvester = new HarvestQueueConsumer();
            }
            harvester.start();
            // start the render consumers
            if (renderers == null) {
                renderers = new ArrayList<RenderQueueConsumer>();
                for (int i = 0; i < Integer.parseInt(config.get(
                        "messaging/render-threads", "3")); i++) {
                    renderers.add(new RenderQueueConsumer("render" + i));
                }
            }
            for (RenderQueueConsumer rqc : renderers) {
                rqc.start();
            }
            log.info("The Fascinator indexer was started successfully");
            timer.cancel();
            timer = null;
        } catch (Exception e) {
            log.warn("Will retry in 15 seconds.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }

    @Override
    public void destroy() {
        try {
            broker.stop();
        } catch (Exception e) {
            log.error("Failed to stop message broker: {}", e.getMessage());
        }
        if (directLogic != null) {
            try {
                directLogic.stop();
            } catch (Exception e) {
                log.error("Failed to stop the DiReCt message service: {}", e.getMessage());
            }
        }
        if (directIn != null) {
            try {
                directIn.stop();
            } catch (Exception e) {
                log.error("Failed to stop the DiReCt incoming API: {}", e.getMessage());
            }
        }
        if (directOut != null) {
            try {
                directOut.stop();
            } catch (Exception e) {
                log.error("Failed to stop the DiReCt outgoing API: {}", e.getMessage());
            }
        }
        if (directTest != null) {
            try {
                directTest.stop();
            } catch (Exception e) {
                log.error("Failed to stop the DiReCt outgoing API: {}", e.getMessage());
            }
        }
        if (harvester != null) {
            try {
                harvester.stop();
                log.info("The Fascinator indexer stopped");
            } catch (Exception e) {
                log.error("Failed to stop the harvester: {}", e.getMessage());
            }
        }
        if (renderers != null) {
            for (RenderQueueConsumer rqc : renderers) {
                try {
                    rqc.stop();
                } catch (Exception e) {
                    log.error("Failed to stop renderer: {}", e.getMessage());
                }
            }
        }
        if (timer != null) {
            timer.purge();
            timer = null;
        }
        super.destroy();
    }
}
