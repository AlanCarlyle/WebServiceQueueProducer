package org.ajcarlyle.server;

import java.io.StringWriter;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", targetNamespace = "http://ajcarlyle.org/jobservice", 
endpointInterface = "org.ajcarlyle.jobservice.JobService")

public class JobServiceImpl implements JobService {

  private final static Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);
  private  JAXBContext jaxbContext;
  private  Marshaller jaxbMarshaller;

  private Server server;
  private QueuePublisher queuePublisher;

 


  public JobServiceImpl(Server server) throws JAXBException {
    this.server = server;
     jaxbContext = JAXBContext.newInstance(JobQueueMessage.class);
     jaxbMarshaller = jaxbContext.createMarshaller();
     queuePublisher = server.getQueuePublisher();
  }

  @Override
  public JobResponse executeJob(JobRequest request)  {
   
    String serverId = UUID.randomUUID().toString();
    String clientId = request.getClientId();
    JobResponse response = new JobResponse();
    response.setClientId(clientId);
    response.setServerId(serverId);
    try {
   
      JobQueueMessage queueMessage = new JobQueueMessage();
      queueMessage.setClientId(clientId);
      queueMessage.setServerId(serverId);
      queueMessage.setStatus("Success");
  
      StringWriter writer = new StringWriter();
      
      jaxbMarshaller.marshal(queueMessage, writer);
      String content = writer.toString();
  
      queuePublisher.SendMessageAsync(content);
  
     
    } catch (Exception e) {

    
      logger.error("Failed sending message", e);
      
    }
    return response;
  }

  

}