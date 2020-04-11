package org.ajcarlyle.server;

import java.util.UUID;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", targetNamespace = "http://ajcarlyle.org/jobservice", endpointInterface = "org.ajcarlyle.jobservice.JobService")
public class JobServiceImpl implements JobService {

  private final static Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

  public  Server server;

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

      logger.info("Received: {}", request.getContent());
      logger.info("Sending: {}", serverId);

      server.doProcessing(serverId, clientId);

      logger.info("Web Service response sent {}-{}",clientId, serverId);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response;
  }
}