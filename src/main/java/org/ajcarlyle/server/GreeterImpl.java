package org.ajcarlyle.server;

import java.io.IOException;

import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.PingMeFault;

@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", targetNamespace = "http://apache.org/hello_world_soap_http", endpointInterface = "org.apache.hello_world_soap_http.Greeter")

public class GreeterImpl implements Greeter {

  private Server server;

  public GreeterImpl(Server server) {
    this.server = server;
  }

  public String greetMe(String message) {

    java.util.UUID jobUUID = java.util.UUID.randomUUID();
    String jobid = jobUUID.toString();

    server.getQueuePublisher().SendMessageAsync(jobid);

    // System.out.println("Executing operation greetMe");
    // System.out.println("Message received: " + message + "\n");
    return jobid;
  }

}