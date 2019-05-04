package org.ajcarlyle.client;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.ajcarlyle.client.Receiver.JobConsumer;
import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.ajcarlyle.utilities.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobProcessTask implements Callable<JobProcessResult> {

  private static final Logger logger = LoggerFactory.getLogger(JobProcessTask.class);

  private JobService wsPort;
  private JobRequest request;
  private JobConsumer queue;
  private CancellationNotifier cancellationNotifier;

  public JobProcessTask(JobService wsPort, JobRequest request, JobConsumer queue,
      CancellationNotifier cancellationNotifier) {
  
    this.wsPort = wsPort;
    this.queue = queue;
    this.request = request;
    this.cancellationNotifier = cancellationNotifier;
  
  }

  public JobRequest getRequest() {
    return request;
  }

  @Override
  public JobProcessResult call() {
    JobProcessResult result = new JobProcessResult(request);
    try {

      result.setCompleted(false);

      if (cancellationNotifier.isCanceled()) {
        result.setFailureMessage("Canceled");
        Thread.currentThread().interrupt();

      } else {

        JobResponse response = wsPort.executeJob(request);
        if (response == null) {
          result.setFailureMessage("Web Service Response is null");
          cancellationNotifier.cancel();
        } else {

          result.setWsResponse(response);
          String serverId = response.getServerId();
          String clientId = response.getClientId();
          CompletableFuture<JobQueueMessage> callback = queue.GetCallback(serverId);          
          JobQueueMessage queueMessage = callback.get(10, TimeUnit.SECONDS);
          
          if (queueMessage == null) {
            result.setFailureMessage("Queue Message is null");
            cancellationNotifier.cancel();
          } else {
            result.setQueueMessage(queueMessage);
            String status = queueMessage.getStatus();
            logger.debug("Queue Response {} for {}-{} ", status,queueMessage.getClientId(), queueMessage.getServerId());
            
            if (status.equals("Success")) {
              result.setCompleted(true);
            } else {
              if (status.equals("Failed")) {
                result.setFailureMessage(Strings.format("Status: Failed"));
              } else {
                result.setFailureMessage(Strings.format("Invalid Status: {}}", status));
              }
              cancellationNotifier.cancel();
            }
          }
        }
      }
    } catch (Throwable e) {
      result.setFailureMessage("Exception: " + e.getMessage());

      cancellationNotifier.cancel();
     
    } finally {
       // Always return the result with details about job processing.
      return result;
    }
  }
}