package org.ajcarlyle.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.management.monitor.Monitor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Receiver {

    public static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private static final String QUEUE_NAME = "products_queue";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public Receiver() throws IOException, TimeoutException {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

    public JobConsumer startJobConsumer() throws IOException {
        JobConsumer consumer = new JobConsumer(channel);
        consumer.setConsumerTag(channel.basicConsume(QUEUE_NAME, true, consumer));
        return consumer;

    }

    public void stopJobConsumer(JobConsumer consumer) throws IOException, TimeoutException {
        if (consumer != null && consumer.getConsumerTag() != null) {
            channel.basicCancel(consumer.getConsumerTag());
        }
    }

    public static class JobConsumer extends DefaultConsumer {

        private BlockingQueue<String> jobQueue = new LinkedBlockingDeque<String>(2);
        private String consumerTag;
        public boolean isAborted;

        protected void setConsumerTag(String consumerTag) {
            this.consumerTag = consumerTag;

        }

        public String getConsumerTag() {
            return consumerTag;
        }

        public JobConsumer(Channel channel) {
            super(channel);
            logger.info("CREATE JOB CONSUMER");
            isAborted = false;
        }

        public BlockingQueue<String> getJobQueue() {
            return jobQueue;
        }

        public void AddJob(String jobid) throws AbortedException {
            try {

                if (!jobQueue.offer(jobid, 60, TimeUnit.SECONDS)) {
                    isAborted = true;
                    jobQueue.clear();
                    throw new AbortedException("Timeout adding job");
                }
            } catch (InterruptedException e) {
                isAborted = true;
                jobQueue.clear();
                throw new AbortedException("Adding job failed", e);
            }
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                byte[] body) {
            try {
                String message = new String(body, "UTF-8");

                if (jobQueue.contains(message)) {
                    jobQueue.remove(message);
                }

                logger.info(" [x] Received '{} '", message);
            } catch (UnsupportedEncodingException e) {
                isAborted = true;
                jobQueue.clear();
            }
        }
    }
}
