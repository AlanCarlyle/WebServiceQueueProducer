package org.ajcarlyle.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private final static Logger logger = LoggerFactory.getLogger(Main.class);

  private static Server server;

  public static void main(String[] args) {

    java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.stop();
      logger.info("Server Exited");
    }));

    try {
      server = new Server();
      logger.info("Server Started");

      server.start();

      //startUpTest();
      logger.info("Server ready...");

      for (;;) {
        Thread.sleep(3000);
      }
    } catch (Throwable e) {

      e.printStackTrace();
      logger.error("Server exited with error", e);
      System.exit(1);
    }
  }

  // private static void startUpTest() {

  //   JobServiceImpl jobService = new JobServiceImpl();
  //   JobRequest testRequest = new JobRequest();
  //   testRequest.setClientId("0000");
  //   testRequest.setContent("Direct Request Test");
  //   JobResponse response = jobService.executeJob(testRequest);

  //    if( response.getClientId() != "0000")
  //       throw new RuntimeException("Something is wrong");

  
  // }

}
