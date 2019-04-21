package org.ajcarlyle.server;

import org.ajcarlyle.server.QueuePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.Endpoint;

import org.apache.hello_world_soap_http.Greeter;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private QueuePublisher queue;

    protected Server() throws IOException, TimeoutException {
        queue = new QueuePublisher();
    }

    public QueuePublisher getQueuePublisher() {
        return queue;
    }

    public void start() throws Exception {
        logger.info("Starting Server");
        Greeter implementor = new GreeterImpl(this);
        Endpoint endpoint = Endpoint.publish("http://localhost:9000/SoapContext/SoapPort", implementor);

        logger.info("Endpoint: {}",endpoint.toString());
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();

        server.start();

        System.out.println("Server ready...");
        int upTime = 5 * 60;

        System.out.println(String.format("Server will run for %d seconds", upTime));
        while(true) {
            Thread.sleep(upTime * 1000);
        }
        
        // System.out.println("Server exiting");
        // System.exit(0);
    }
}