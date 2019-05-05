package org.ajcarlyle.server;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.ws.Endpoint;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final static Random random = new Random();

    private Endpoint endpoint;

    private JAXBContext jaxbContext;
    private Marshaller jaxbMarshaller;
    private ExecutorService executor;

    private QueuePublisher queue;

    public Server() throws IOException, TimeoutException, JAXBException {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            executor = Executors.newWorkStealingPool(cores);

            jaxbContext = JAXBContext.newInstance(JobQueueMessage.class);
            jaxbMarshaller = jaxbContext.createMarshaller();
            queue = new QueuePublisher();
        } catch (Throwable e) {
            logger.error("Failed to initialise server ", e);
           throw e;
        }
    }

    public void start() throws IOException, TimeoutException {
        logger.info("Starting Server");
        JobService implementor = new JobServiceImpl(this);
         endpoint = Endpoint.publish("http://localhost:9000/SoapContext/SoapPort", implementor);
        
        logger.info("Started: {}", endpoint.toString());
    }

    public void stop() throws InterruptedException {

        logger.info("Stopping server");
        executor.awaitTermination(5, TimeUnit.MINUTES);
        if (endpoint !=  null){
            logger.info("Stopping: {}", endpoint.toString());
            endpoint.stop();
            queue = null;
        }
    }
    

    public void doProcessing(String serverId, String clientId)  {

       executor.execute(() -> {
            try {
                JobQueueMessage queueMessage = new JobQueueMessage();
                queueMessage.setClientId(clientId);
                queueMessage.setServerId(serverId);

                StringWriter writer = new StringWriter();

                // int failStatus = random.nextInt(7);
                // if (failStatus == Integer.MIN_VALUE) {
                //     queueMessage.setStatus("Failed");
                // } else {
                //     queueMessage.setStatus("Success");
                // }
                queueMessage.setStatus("Success");

                logger.debug("Marshaller is null: {}",jaxbMarshaller == null);
                logger.debug("JobQueueMessage is null: {}",queueMessage == null);
                logger.debug("StringWriter is null: {}",writer == null);
                jaxbMarshaller.marshal(queueMessage, writer);
                
                String content = writer.toString();
                logger.debug("Content is null or empty:  {}",  content == null || content.length() == 0);

                long wait = (1000 * (1 + random.nextInt(6))) - 800;
                logger.debug("Waiting {} seconds", wait / (float) 1000);

                Thread.sleep(wait);
                logger.debug("Queue is null: {}",queue == null);
             
                queue.SendMessage(content);

            } catch (Throwable e) {
               
              logger.error("Error queuing message",e);
               e.printStackTrace();
               
            }
        });
    }

}