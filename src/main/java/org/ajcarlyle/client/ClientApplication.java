package org.ajcarlyle.client;

public class ClientApplication {


  public static void main(String args[]) throws Exception {

    Client client = new Client();

    client.SendMessages();

    Thread.sleep(1000 * 60 * 5);

    System.exit(0);
  }
}
