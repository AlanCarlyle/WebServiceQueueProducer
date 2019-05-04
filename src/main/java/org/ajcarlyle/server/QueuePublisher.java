package org.ajcarlyle.server;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueuePublisher {

    private final static Logger logger = LoggerFactory.getLogger(QueuePublisher.class);

    private final static String QUEUE_NAME = "products_queue";

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

  

    public QueuePublisher() throws IOException, TimeoutException {

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

    public synchronized void SendMessage(String message) throws IOException {
        byte[] body = message.getBytes();
        channel.basicPublish("", QUEUE_NAME, null, body);
    }

    public void Close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    private static final Random random = new Random();

    public synchronized Runnable SendMessageAsync(String message) {
        return () -> {
            try {
                byte[] body = message.getBytes();
               
                channel.basicPublish("", QUEUE_NAME, null, body);

            } catch (IOException  e) {
                logger.error("Publishing Failed", e);
            }            
        };
    }
}
