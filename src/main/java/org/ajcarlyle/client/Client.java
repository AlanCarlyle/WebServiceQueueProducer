package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.print.attribute.standard.JobStateReason;
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

  private Receiver.JobConsumer jobConsumer;

  public void SendMessages() throws IOException, TimeoutException {
    int totalJobs = 20;
    int jobCount = 0;
    int maxParallelRequests = 4;

    jobConsumer = receiver.startJobConsumer(maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(maxParallelRequests);

    List<Callable<Boolean>> jobs = new ArrayList<>();

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
        // request.set( Integer.toString(jobCount));
        // Executing the web service task to get the job id should not need to be waited
        // for.

        jobs.add(new JobServiceRequestCallable(request));

        // As the max number of jobs that can be in the job Queue is 2 this will
        // not complete the future until a job has been received thus delaying
        // execution of next request to web service and allowing to such
        // request to be run at a time.

        logger.debug("Task for job {} queued", jobCount);
      } catch (CancellationException | AbortedException e) {
        // If for whatever reason the attempt to add the job failed or the
        // current consumed job reported an error then terminate the service.
        logger.error("Exception processing job", e);
        executor.shutdownNow();
        break;
      }
      jobCount++;
    }
    try {
      StringBuilder sb = new StringBuilder();
      List<Future<Boolean>> results = executor.invokeAll(jobs);
      results.forEach(action -> {
        try {
          Boolean result = action.get();
          sb.append(result).append(',');
        } catch (InterruptedException | ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      });
      logger.info("Results: {}", sb);
    } catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    if (jobCount != 0)
      logger.info("Failed to add {} Jobs", totalJobs - jobCount);

    receiver.stopJobConsumer(jobConsumer);
  }

  private class JobServiceRequestCallable implements Callable<Boolean> {

    private JobRequest request;
    private JobResponse response;

    public JobServiceRequestCallable(JobRequest request) {
      this.request = request;
    }

    @Override
    public Boolean call() {

      ReentrantLock lock = jobConsumer.getLock();
      lock.lock();
      try {
        if (jobConsumer.isAborted())
          return false;
        response = wsPort.executeJob(request);
        String serverId = response.getServerId();
        logger.debug("Web service task sent for job {} queued", serverId);

        Condition condition = jobConsumer.QueueJob(serverId);
        return condition.await(1, TimeUnit.MINUTES) && !jobConsumer.isAborted();

      } catch (InterruptedException e) {
        return false;
      } finally {
        lock.unlock();
      }
    }
  }
}