package org.ajcarlyle.server;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.Endpoint;

import org.ajcarlyle.jobservice.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    

    public void start() throws IOException, TimeoutException {
        logger.info("Starting Server");
        JobService implementor = new JobServiceImpl();
        Endpoint endpoint = Endpoint.publish("http://localhost:9000/SoapContext/SoapPort", implementor);

        logger.info("Endpoint: {}",endpoint.toString());
    }

}