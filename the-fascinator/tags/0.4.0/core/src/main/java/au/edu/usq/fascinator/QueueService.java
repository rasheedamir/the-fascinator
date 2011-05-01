package au.edu.usq.fascinator;

import java.io.IOException;

import org.apache.activemq.broker.BrokerService;

import au.edu.usq.fascinator.common.JsonConfig;

public class QueueService {

    public static void main(String[] args) throws IOException {
        BrokerService broker = new BrokerService();

        JsonConfig json = new JsonConfig();
        // configure the broker
        try {
            broker.addConnector(json.get("messaging/url",
                    "tcp://localhost:61616"));
            broker.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
