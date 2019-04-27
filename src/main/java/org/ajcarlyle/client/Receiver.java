package org.ajcarlyle.client;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.ajcarlyle.AbortedException;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Receiver {

    public static final Logger logger = LoggerFactory.getLogger(Receiver.class);
    private static JAXBContext jaxbContext;
    private static Unmarshaller jaxbUnmarshaller;

    private static final String QUEUE_NAME = "products_queue";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(JobQueueMessage.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            logger.error("Failed to initialise", e);
            System.exit(1);
        }
    }

    public Receiver() throws IOException, TimeoutException {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

    public JobConsumer startJobConsumer() throws IOException {
        JobConsumer consumer = new JobConsumer(channel);
        channel.basicConsume(QUEUE_NAME, true, consumer);

        return consumer;
    }

    public void stopJobConsumer(JobConsumer consumer) throws IOException, TimeoutException {
        if (consumer != null) {
            channel.basicCancel(consumer.getConsumerTag());
        }
    }

    public static class JobConsumer extends DefaultConsumer {

        // private BlockingQueue<String> jobQueue;

        //private ConcurrentHashMap<String, Semaphore> notificationLocks;

        private boolean isAborted;

        public JobConsumer(Channel channel) {
            super(channel);
            logger.info("CREATE JOB CONSUMER");
            isAborted = false;
         //   notificationLocks = new ConcurrentHashMap<>();
            // jobQueue = new LinkedBlockingDeque<String>(maxJobsAllowed);
        }

        // public BlockingQueue<String> getJobQueue() {
        // return jobQueue;
        // }

        public synchronized boolean isAborted() {
            return isAborted;
        }

        public synchronized void Abort() {
            if (!isAborted)
                isAborted = true;
        }

        // public synchronized void QueueJobWithTimeOut(String clientId, int timeout,
        // TimeUnit timeUnit) throws AbortedException {
        // try {
        // logger.debug("Queue Size: {}",jobQueue.size());
        // if (!jobQueue.offer(clientId, timeout, timeUnit)) {
        // Abort();
        // throw new AbortedException("Timeout adding job");
        // }
        // } catch (InterruptedException e) {
        // Abort();
        // throw new AbortedException("Interrupted while adding job", e);
        // }
        // }

        // public synchronized Semaphore getNotificationLock(String id) throws InterruptedException {
        //     Semaphore lock = notificationLocks.remove(id);
        //     if (lock != null)
        //         return lock;
        //     lock = new Semaphore(0, true);
        //     logger.debug("Acquiring Lock");
        //     lock.acquire();
            
        //     return notificationLocks.put(id, lock);
        // }

        // public synchronized boolean releaseNotificationLock(String id) {
        //     Semaphore lock = notificationLocks.remove(id);
        //     logger.debug("Releasing Lock");
        //     if (lock == null) {
        //         return false;
        //     } else {
        //         lock.release();
        //         return true;
        //     }

        // }

        // private synchronized void addReleasedLock(String id) {
        //     logger.debug("Adding Released Lock");
        //     Semaphore lock = notificationLocks.putIfAbsent(id, new Semaphore(0, true));
        //     if (lock != null)
        //         lock.release();
        // }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                byte[] body) {
            try {

                logger.info("[x] Receiving Message");
                String message = new String(body, "UTF-8");

                StringReader reader = new StringReader(message);

                Object maybeJobQueueMessage = jaxbUnmarshaller.unmarshal(reader);
                JobQueueMessage jobQueueMessage = (maybeJobQueueMessage instanceof JobQueueMessage)
                        ? (JobQueueMessage) maybeJobQueueMessage
                        : null;

                if (jobQueueMessage == null)
                    throw new InvalidObjectException("Queue message is not valid type");

                String serverId = jobQueueMessage.getServerId();
                logger.info("[x] Received '{}'", serverId);
                // Attempt to release the existing lock else add an already released lock
                // that can be immediately acquired by the caller.
                // if (!releaseNotificationLock(serverId))
                //     addReleasedLock(serverId);

                
            } catch (JAXBException | InvalidObjectException | UnsupportedEncodingException e) {

                // Abort();
                // Thread.currentThread().interrupt();
            }
        }
    }
}
