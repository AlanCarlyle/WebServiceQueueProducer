package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.SOAPService;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.jobservice.types.JobResponse;
import org.ajcarlyle.AbortedException;
import org.ajcarlyle.client.Receiver.JobConsumer;
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
  //  int maxJobCompletionWait = 2; // minutes
    Receiver.JobConsumer jobConsumer = receiver.startJobConsumer();
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

        logger.debug("Running job {} queued", jobCount);

        // Executing the web service task to get the job id should not need to be waited for.
        Future<JobResponse> responseFuture  = executor.submit(new JobServiceRequestCallable(wsPort, request, jobConsumer));
       
        
        
        // As the max number of jobs that can be in the job Queue is 2 this will
        // not complete the future until a job has been received thus delaying
        // execution of next request to web service and allowing to such
        // request to be run at a time.
      
        //JobResponse response = responseFuture.get();

        
  
      
      } catch ( CancellationException |  AbortedException e) {
        // If for whatever reason the attempt to add the job failed or the
        // current consumed job reported an error then terminate the service.
        logger.error("Exception processing job",e );
        executor.shutdownNow();
        break;
      }
      jobCount++;
    }


    logger.info("{} jobs failed to complete", totalJobs - jobCount);

    // Wait for any pending jobs to complete to clear the queue. 
    // This assumes client does not have administrative control to purge the queue and 
    // may want to record any web service calls that are still running and waiting.
    //int maxWaitClearTime = 5;
    // while (jobConsumer.jobsWaiting() > 0)
    //   try {
    //     maxWaitClearTime--;
    //     if (maxWaitClearTime == 0)
    //       jobConsumer.getJobQueue().clear();
    //     else
    //       Thread.sleep(1000);
    //   } catch (InterruptedException e) {
    //    logger.error("Interrupted while waiting to clear Job Consumer Queue", e);
    //   }

    receiver.stopJobConsumer(jobConsumer);
  }

  static class JobServiceRequestCallable implements Callable<JobResponse> {

    private JobService wsPort;
    private JobRequest request;
    private JobConsumer jobConsumer;

    public JobServiceRequestCallable(JobService wsPort, JobRequest request,JobConsumer jobConsumer) {
      this.wsPort = wsPort;
      this.request = request;
      this.jobConsumer = jobConsumer;
    }

    @Override
    public JobResponse call() throws Exception {
      JobResponse jobResponse = wsPort.executeJob(request);

      // Get a lock to use to wait for this job to complete.
      // If this returns null it means the job has already completed.
    //  Semaphore lock = jobConsumer.getNotificationLock(jobResponse.getServerId());
    //  if (lock != null)
    //     lock.tryAcquire(5, TimeUnit.SECONDS);
      // Block until the Job Consumer receives the result.
      logger.debug("Completed job {} with server id {}", jobResponse.getClientId(), jobResponse.getServerId());
      return jobResponse;
    }
  }

}