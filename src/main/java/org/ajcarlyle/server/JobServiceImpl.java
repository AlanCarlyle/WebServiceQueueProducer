package org.ajcarlyle.server;

import java.io.StringWriter;
import java.util.Random;
import java.util.UUID;

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

  private Server server;

  static {
    try {
      jaxbContext = JAXBContext.newInstance(JobQueueMessage.class);
      jaxbMarshaller = jaxbContext.createMarshaller();
    } catch (JAXBException e) {
      logger.error("Failed to initialise", e);
      System.exit(1);
    }
  }

  public JobServiceImpl(Server server) {
    this.server = server;
  }

  @Override
  public JobResponse executeJob(JobRequest request) {

    String serverId = UUID.randomUUID().toString();
    String clientId = request.getClientId();
    JobResponse response = new JobResponse();
    response.setClientId(clientId);
    response.setServerId(serverId);
    try {

      queueMessage(serverId, clientId);

    } catch (Exception e) {
      queueFailure(serverId, clientId, e.toString());
    }
    return response;
  }

  private final static Random random = new Random();

  private void queueMessage(String serverId, String clientId) throws JAXBException {

    JobQueueMessage queueMessage = new JobQueueMessage();
    queueMessage.setClientId(clientId);
    queueMessage.setServerId(serverId);

    int failStatus = random.nextInt(6);
    if (failStatus == 3) {
      queueMessage.setStatus("Failed");
    } else {
      queueMessage.setStatus("Success");
    }
    StringWriter writer = new StringWriter();

    jaxbMarshaller.marshal(queueMessage, writer);
    String content = writer.toString();

    server.getQueuePublisher().SendMessageAsync(content);

  }

  private void queueFailure(String serverId, String clientId, String error) {
    server.getQueuePublisher().SendMessageAsync(String.format("ERROR (%s-%s): %s", clientId, serverId, error));
  }

}