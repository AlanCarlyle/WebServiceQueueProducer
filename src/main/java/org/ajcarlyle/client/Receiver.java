package org.ajcarlyle.client;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Receiver {

    private static final String QUEUE_NAME = "products_queue";
    ConnectionFactory factory;
    Connection connection;
    Channel channel;

    public Receiver() throws IOException, TimeoutException {
        factory = new ConnectionFactory();
        factory.setHost("localhost");
         connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        
    }

    public void start() throws IOException {
        Consumer consumer = new MyConsumer(channel);
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    public static class MyConsumer extends DefaultConsumer {

        public MyConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag,
                                   Envelope envelope, AMQP.BasicProperties properties,
                                   byte[] body) throws IOException {


            String message = new String(body, "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        }
    }
}
