package org.ajcarlyle.server;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", targetNamespace = "http://ajcarlyle.org/jobservice", endpointInterface = "org.ajcarlyle.jobservice.JobService")

public class JobServiceImpl implements JobService {

  private final static Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);
  private static JAXBContext jaxbContext;
  private static Marshaller jaxbMarshaller;

  private QueuePublisher queue;

  private static ExecutorService executor;
  static {
    int cores = Runtime.getRuntime().availableProcessors();
    executor = Executors.newWorkStealingPool(cores);
  }

  static {
    try {
      jaxbContext = JAXBContext.newInstance(JobQueueMessage.class);
      jaxbMarshaller = jaxbContext.createMarshaller();
    } catch (JAXBException e) {
      logger.error("Failed to initialise", e);
      System.exit(1);
    }
  }

  public JobServiceImpl() throws IOException, TimeoutException {

    this.queue = new QueuePublisher();
  }

  @Override
  public JobResponse executeJob(JobRequest request) {

    String serverId = UUID.randomUUID().toString();
    String clientId = request.getClientId();
    JobResponse response = new JobResponse();
    response.setClientId(clientId);
    response.setServerId(serverId);
    try {

      logger.info("Received: {}", request.getContent());
      logger.info("Sending: {}", serverId);

      doProcessing(serverId, clientId);

      logger.info("Processing Completed");
    } catch (Exception e) {
      e.printStackTrace();

    }
    return response;
  }

  private final static Random random = new Random();

  private void doProcessing(String serverId, String clientId) throws Exception {

  
    executor.execute(() -> {
      try {
        JobQueueMessage queueMessage = new JobQueueMessage();
        queueMessage.setClientId(clientId);
        queueMessage.setServerId(serverId);

        StringWriter writer = new StringWriter();

        int failStatus = random.nextInt(7);
        if (failStatus == 0) {
          queueMessage.setStatus("Failed");
        } else {
          queueMessage.setStatus("Success");
        }

        jaxbMarshaller.marshal(queueMessage, writer);
        String content = writer.toString();

        long wait = (1000 * (1 + random.nextInt(6))) - 800;
        logger.debug("Waiting {} seconds", wait / (float) 1000);

        Thread.sleep(wait);

        queue.SendMessage(content);

      } catch (Exception e) {
        e.fillInStackTrace();
        logger.error("Error queuing message", e);
      }
    });
  }

}