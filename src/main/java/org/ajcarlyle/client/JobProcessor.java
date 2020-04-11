package org.ajcarlyle.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import org.ajcarlyle.jobservice.JobService;
import org.ajcarlyle.jobservice.SOAPService;
import org.ajcarlyle.jobservice.types.JobRequest;
import org.ajcarlyle.utilities.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobProcessor {

  private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);

  private static final QName SERVICE_NAME = new QName("http://ajcarlyle.org/jobservice", "SOAPService");

  private final int maxParallelRequests = 4;
  private ExecutorService executor;

  private Receiver receiver;
  private Receiver.JobConsumer jobConsumer;

  private SOAPService wsService;
  private JobService wsPort;

  private long timeout;
  private TimeUnit timeUnit;

  private CancellationNotifier cancellationNotifier;

  /**
   * Job Processor that submits jobs to web service and waits for result sent to
   * queue. If any job fails then all pending jobs that have not started are
   * terminated.
   *
   * @param timeout  Amount of time to wait for each individual job to complete.
   * @param timeUnit Time unit to use for timeout
   * @throws TimeoutException Took to long to connect to queue Receiver.
   * @throws IOException Can read Web Service WSDL or queue Receiver is invalid.
   */
  public JobProcessor(long timeout, TimeUnit timeUnit) throws IOException, TimeoutException {
    //try {
      this.timeout = timeout;
      this.timeUnit = timeUnit;

      File wsdlFile = new File("src/main/resources/JobService.wsdl");
      URL wsdlURL = wsdlFile.toURI().toURL();
      logger.debug("WSDL: {}", wsdlURL);

      wsService = new SOAPService(wsdlURL, SERVICE_NAME);
      wsPort = wsService.getSoapPort();

      receiver = new Receiver();

      executor = Executors.newFixedThreadPool(maxParallelRequests);
      cancellationNotifier = new CancellationNotifier(executor, timeout, timeUnit);    
  }

  private void startJobConsumer() throws IOException {    
    jobConsumer = receiver.startJobConsumer(cancellationNotifier);
    logger.info("Started Queue Consumer");
  }

  private void stopJobConsumer() throws IOException, TimeoutException {
    cancellationNotifier = null;
    receiver.stopJobConsumer(jobConsumer);

    logger.info("Stopped Queue Consumer");
  }

  public List<String> SendMessages(List<String> messages) throws Exception {
    cancellationNotifier = new CancellationNotifier(executor, timeout, timeUnit);
    List<JobProcessTask> jobProcessTasks = prepareJobProcessTasks(messages);
    return invokeJobProcessTasks(jobProcessTasks);
  }

  private List<JobProcessTask> prepareJobProcessTasks(List<String> messages) throws IOException {

    startJobConsumer();

    List<JobProcessTask> jobs = new ArrayList<>();

    JobProcessTaskFactory factory = new JobProcessTaskFactory(wsPort, jobConsumer, cancellationNotifier);
    logger.info("Processing {} Jobs", messages.size());

    int jobId = 1;
    for (String message : messages) {

      JobRequest request = new JobRequest();
      request.setContent(message);
      request.setClientId(Integer.toString(jobId));

      jobs.add(factory.getJobProcessTask(request));
      jobId++;
    }

    return jobs;
  }

  private List<String> invokeJobProcessTasks(List<JobProcessTask> jobProcessTasks) throws Exception {
    try {
     
      List<String> results = new ArrayList<>();
      logger.info("Invoking {} jobs", jobProcessTasks.size());
      // Invokes all the jobs on the executor and waits for them all to complete. 
      // Times out if overall time to complete is much greater than that allowed for the sum on the 
      // individual jobs. Should not time out of running jobs as threads on parallel processes.
      List<Future<JobProcessResult>> jobProcessResults = executor.invokeAll(jobProcessTasks);
        //  (timeout * jobProcessTasks.size()) + timeout, timeUnit);
     
      for(Future<JobProcessResult> task : jobProcessResults) {
        try {

          // if (!task.isDone() && !task.isCancelled())
          //   task.cancel(false);
          
          JobProcessResult result = task.get();
          if (result.isCompleted()) {
            results.add(Strings.format("Completed [{}]: ServerId: {}", result.getQueueMessage().getClientId(),
                result.getQueueMessage().getServerId()));
          } else {
            results.add(Strings.format("Failed [{}]: Message: {}", result.getWsRequest().getClientId(),
                result.getFailureMessage()));
          }
        } catch (InterruptedException | ExecutionException e) {
          logger.error("Processing Job failed",e);
          // Unforced shutdown that allows already queued jobs to complete
          cancellationNotifier.cancel();
        }
      }
      logger.info("Result gathering done with {} results", results.size());
      stopJobConsumer();

      return results;
    } catch (InterruptedException | IOException | TimeoutException e) {
      // Forced shutdown of executor due to serious problem
    //  cancellationNotifier.cancel(true);
      throw e;
    }
  }
}