package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.SOAPService;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.ajcarlyle.AbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Client {

  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private static final QName SERVICE_NAME = new QName("http://ajcarlyle.org/jobservice", "SOAPService");

  private URL wsdlURL;
  private Receiver receiver;

  private SOAPService wsService;
  private JobService wsPort;

  public Client() {

    try {
      File wsdlFile = new File("src/main/resources/JobService.wsdl");
      wsdlURL = wsdlFile.toURI().toURL();
      logger.debug("WSDL: {}", wsdlURL);

      wsService = new SOAPService(wsdlURL, SERVICE_NAME);
      wsPort = wsService.getSoapPort();

      receiver = new Receiver();

    } catch (IOException | TimeoutException e) {
      logger.error("Starting Queue Receiver Failed: ", e);
    }
  }

  public void SendMessages() throws IOException, TimeoutException {
    int totalJobs = 20;
    int jobCount = 0;
    int maxParallelRequests = 2;
    int maxJobCompletionWait = 2; // minutes
    Receiver.JobConsumer jobConsumer = receiver.startJobConsumer(maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(maxParallelRequests);

    logger.info("Processing {} Jobs", totalJobs);

    while (jobCount < totalJobs) {
      try {

        // Check if the Job Queue Consumer has been aborted while handling jobs.
        if (jobConsumer.isAborted()) {          
          logger.error("Job Consumer is aborted");
          throw new AbortedException("Job Queue Consumer Aborted");

      }
        // Prepare the Web Service request and submit it using a Callable that returns
        // the job id.
        String message = String.format("Do Job %d", jobCount);

        JobRequest request = new JobRequest();
        request.setContent(message);
        request.setClientId(Integer.toString(jobCount));
        //request.set( Integer.toString(jobCount));
        // Executing the web service task to get the job id should not need to be waited for.
        JobResponse response  = executor.submit(new JobServiceRequestCallable(wsPort, request)).get();

        logger.debug("Web service task sent for job {} queued", response.getServerId());
        // As the max number of jobs that can be in the job Queue is 2 this will
        // not complete the future until a job has been received thus delaying
        // execution of next request to web service and allowing to such
        // request to be run at a time.
      

        jobConsumer.QueueJobWithTimeOut(response.getServerId(), maxJobCompletionWait, TimeUnit.MINUTES);

        logger.debug("Task for job {} queued", jobCount);
      } catch (InterruptedException | CancellationException | ExecutionException | AbortedException e) {
        // If for whatever reason the attempt to add the job failed or the
        // current consumed job reported an error then terminate the service.
        logger.error("Exception processing job",e );
        executor.shutdownNow();
        break;
      }
      jobCount++;
    }

    if (jobCount != 0)
      logger.info("Failed to add {} Jobs", totalJobs - jobCount);

    // Wait for any pending jobs to complete to clear the queue. 
    // This assumes client does not have administrative control to purge the queue and 
    // may want to record any web service calls that are still running and waiting.
    int maxWaitClearTime = 5;
    while (jobConsumer.jobsWaiting() > 0)
      try {
        maxWaitClearTime--;
        if (maxWaitClearTime == 0)
          jobConsumer.getJobQueue().clear();
        else
          Thread.sleep(1000);
      } catch (InterruptedException e) {
       logger.error("Interrupted while waiting to clear Job Consumer Queue", e);
      }

    receiver.stopJobConsumer(jobConsumer);
  }

  static class JobServiceRequestCallable implements Callable<JobResponse> {

    private JobService wsPort;
    private JobRequest request;

    public JobServiceRequestCallable(JobService wsPort, JobRequest request) {
      this.wsPort = wsPort;
      this.request = request;
    }

    @Override
    public JobResponse call() throws Exception {
      JobResponse jobResponse = wsPort.executeJob(request);
      return jobResponse;
    }
  }

}