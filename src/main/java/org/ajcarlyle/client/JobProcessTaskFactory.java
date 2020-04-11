package org.ajcarlyle.client;

import org.ajcarlyle.client.Receiver.JobConsumer;
import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.types.JobRequest;

public class JobProcessTaskFactory {

  private CancellationNotifier cancellationNotifier;
  private JobService wsPort;
  private JobConsumer consumer;

  public JobProcessTaskFactory(JobService wsPort, JobConsumer consumer, CancellationNotifier cancellationNotifier) {
      this.wsPort = wsPort;
      this.consumer = consumer;
      this.cancellationNotifier = cancellationNotifier;
  }


  public JobProcessTask getJobProcessTask(JobRequest request) {
    return new JobProcessTask(wsPort,request, consumer, cancellationNotifier);
  }

}