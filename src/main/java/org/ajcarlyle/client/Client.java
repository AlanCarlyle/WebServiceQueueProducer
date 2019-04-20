package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.PingMeFault;
import org.apache.hello_world_soap_http.SOAPService;

public final class Client {

  private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

  private static URL wsdlUrl;
  private static Receiver receiver;

  private Client() throws IOException, TimeoutException {
   
    
    wsdlUrl = new URL( "src/main/resources/LessonOne.wsdl" );
  }

  public static void main(String args[]) throws Exception {
    

    System.out.println(wsdlUrl);
    SOAPService ss = new SOAPService(wsdlUrl, SERVICE_NAME);
    Greeter port = ss.getSoapPort();
    System.out.println(port);
    receiver = new Receiver();
    receiver.start();
    

    //System.out.println("Invoking createNewQueuedJob...");
    for(int i=0;i<5;i++) {
      String resp = port.greetMe(String.format("Message %d", i));
      System.out.println("Server responded with: " + resp);
    }
   // 
   // System.out.println();
 

   Thread.sleep(20000);
    System.exit(0);
  }
}