package org.ajcarlyle.server;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeverApplication {

  private final static Logger logger = LoggerFactory.getLogger(SeverApplication.class);

  private static Server server;

  public static void main(String[] args) {

    java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Server Exited");
    }));

    try {
      server = new Server();
      logger.info("Server Started");

    
      server.start();

      startUpTest();
      logger.info("Server ready...");

      for (;;) {
        Thread.sleep(3000);
      }
    } catch (IOException | TimeoutException | InterruptedException e) {

      logger.error("Server exited with error", e);
      System.exit(0);
    }
  }

  private static void startUpTest() {

    JobServiceImpl jobService = new JobServiceImpl(server);
    JobRequest testRequest = new JobRequest();
    testRequest.setClientId("0000");
    testRequest.setContent("Direct Request Test");
    JobResponse response = jobService.executeJob(testRequest);

     if( response.getClientId() != "0000")
        throw new RuntimeException("Something is wrong");

  
  }

}
