package org.ajcarlyle.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ajcarlyle.utilities.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  public static void main(String args[]) throws Exception {

    JobProcessor jobProcessor = new JobProcessor(6,TimeUnit.SECONDS);

    List<String> messages = new ArrayList<>();

    for(int i=1;i<=50;i++) {
      messages.add(Strings.format("Working Job {}", i));
    }

   List<String> results = jobProcessor.SendMessages(messages);

   for(String result : results) {
    logger.info(result);
   } 

    System.exit(0);
  }
}


