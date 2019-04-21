package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Client {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

  private URL wsdlURL;
  private Receiver receiver;

  private SOAPService wsService;
  private Greeter wsPort;

  Client() {

    try {
      File wsdlFile = new File("src/main/resources/LessonOne.wsdl");
      wsdlURL = wsdlFile.toURI().toURL();

      logger.debug("WSLD: {}", wsdlURL);

      wsService = new SOAPService(wsdlURL, SERVICE_NAME);
      wsPort = wsService.getSoapPort();

      receiver = new Receiver();

    } catch (IOException | TimeoutException e) {
      logger.error("Starting Queue Reciver Failed: ", e);
    }
  }

  public void SendMessages() throws IOException, TimeoutException {
    int totalJobs = 20;
    int jobCount = 0;
    Receiver.JobConsumer jobConsumer = receiver.startJobConsumer();

    ExecutorService executor = Executors.newFixedThreadPool(2);

   
    logger.info("Processing {} Jobs", totalJobs);

    while (jobCount < totalJobs) {

      executor.submit(() -> {
        try {
          if (!jobConsumer.isAborted) {
            String jobid = wsPort.greetMe(String.format("Job Request %d", 2));
            logger.info("Job {} has server job id: {}", 2, jobid);
            
            
        
            jobConsumer.AddJob(jobid);
          } else {
            logger.info("ABORTED");
          }

        } catch (AbortedException e) {
          logger.error("Adding Jobs Aborted", e);
        } 
      });
      jobCount++;
    }

    try {
      executor.awaitTermination(totalJobs, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    if (jobCount != 0)
      logger.info("Failed to add {} Jobs", totalJobs - jobCount);

     receiver.stopJobConsumer(jobConsumer);
  }


}