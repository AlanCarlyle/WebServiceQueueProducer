package org.ajcarlyle.server;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceFeature;

import org.apache.hello_world_soap_http.Greeter;

public class Server {

    private QueuePublisher queue;

    protected Server() throws IOException, TimeoutException {
        queue = new QueuePublisher();
    }

    public QueuePublisher getQueuePublisher() {
        return queue;
    }

    public void start() throws Exception {
        System.out.println("Starting Server");
        Greeter implementor = new GreeterImpl(this);
        Endpoint.publish("http://localhost:9000/SoapContext/SoapPort", implementor);
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