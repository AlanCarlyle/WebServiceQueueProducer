package org.ajcarlyle.client;

import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;

public class JobProcessResult {

  private JobRequest wsRequest;
  private JobResponse wsResponse;
  private JobQueueMessage queueMessage;

  private boolean completed;
  private String failureMessage;
  public JobProcessResult(JobRequest wsRequest) {
    this.wsRequest = wsRequest;
    completed = false;
  }

  /**
   * @return the wsRequest
   */
  public JobRequest getWsRequest() {
    return wsRequest;
  }

  /**
   * @param wsRequest the wsRequest to set
   */
  public void setWsRequest(JobRequest wsRequest) {
    this.wsRequest = wsRequest;
  }

  /**
   * @return the wsResponse
   */
  public JobResponse getWsResponse() {
    return wsResponse;
  }

  /**
   * @param wsResponse the wsResponse to set
   */
  public void setWsResponse(JobResponse wsResponse) {
    this.wsResponse = wsResponse;
  }

  /**
   * @return the queueMessage
   */
  public JobQueueMessage getQueueMessage() {
    return queueMessage;
  }

  /**
   * @param queueMessage the queueMessage to set
   */
  public void setQueueMessage(JobQueueMessage queueMessage) {
    this.queueMessage = queueMessage;
  }

  /**
   * @return the completed
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * @param completed the completed to set
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  /**
   * @return the failureMessage
   */
  public String getFailureMessage() {
    return failureMessage;
  }

  /**
   * @param failureMessage the failureMessage to set
   */
  public void setFailureMessage(String failureMessage) {
    this.failureMessage = failureMessage;
  }
  
}