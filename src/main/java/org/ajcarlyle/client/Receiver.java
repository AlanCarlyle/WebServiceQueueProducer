package org.ajcarlyle.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.ajcarlyle.AbortedException;
import org.ajcarlyle.jobservice.types.JobQueueMessage;
import org.ajcarlyle.jobservice.types.JobResponse;
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

    public JobConsumer startJobConsumer(CancellationNotifier cancellationNotifier) throws IOException {
        JobConsumer consumer = new JobConsumer(channel,cancellationNotifier);
        channel.basicConsume(QUEUE_NAME, true, consumer);

        return consumer;
    }

    public void stopJobConsumer(JobConsumer consumer) throws IOException, TimeoutException {
        if (consumer != null) {
            channel.basicCancel(consumer.getConsumerTag());
        }
    }

    public static class JobConsumer extends CancelNotificationConsumer {

        private ConcurrentHashMap<String, CompletableFuture<JobQueueMessage>> wsJobCallbacks;

        protected JobConsumer(Channel channel, CancellationNotifier cancellationNotifier) {
            super(channel,cancellationNotifier);
            wsJobCallbacks = new ConcurrentHashMap<String, CompletableFuture<JobQueueMessage>>();
        }

        public CompletableFuture<JobQueueMessage> GetCallback(String serverId) {
            CompletableFuture<JobQueueMessage> callback = new CompletableFuture<>();

            wsJobCallbacks.put(serverId, callback);
            return callback;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                byte[] body) {
            try {
                String message = new String(body, "UTF-8");
                StringReader reader = new StringReader(message);

                Object maybeJobQueueMessage = jaxbUnmarshaller.unmarshal(reader);
                JobQueueMessage jobQueueMessage = (maybeJobQueueMessage instanceof JobQueueMessage)
                        ? (JobQueueMessage) maybeJobQueueMessage
                        : null;

                if (jobQueueMessage == null) {
                    logger.error("Body invalid");
                    getCancellationNotifier().cancel();
                } else {
                    String serverId = jobQueueMessage.getServerId();
                    CompletableFuture<JobQueueMessage> callback = wsJobCallbacks.remove(serverId);

                    if (callback == null) {
                        logger.error("Callback null");
                       getCancellationNotifier().cancel();

                    } else if (!callback.complete(jobQueueMessage)) {
                        logger.error("Callback failed");
                        getCancellationNotifier().cancel();               
                    }
                }

            } catch (JAXBException | IOException e) {
                logger.error("Error handling delivered message", e.fillInStackTrace());
                getCancellationNotifier().cancel();
            }
        }
    }

    static abstract class CancelNotificationConsumer extends DefaultConsumer {

       
        protected CancellationNotifier cancellationNotifier;
        public CancelNotificationConsumer(Channel channel, CancellationNotifier cancellationNotifier) {
            super(channel);
            this.cancellationNotifier = cancellationNotifier;
            
        }

        public CancellationNotifier getCancellationNotifier() {
            return cancellationNotifier;
        }
        
       
    }
}
