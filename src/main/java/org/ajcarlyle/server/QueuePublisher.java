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

    private static ExecutorService executor;
    static {
        int cores = Runtime.getRuntime().availableProcessors();
        executor = Executors.newWorkStealingPool(cores);
    }

    public QueuePublisher() throws IOException, TimeoutException {

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }


   public void SendMessage(String message) throws IOException {

    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
   }

   public void Close() throws IOException, TimeoutException {
    channel.close();
    connection.close();
   }


   public void SendMessageAsync(String message) {

    Runnable runnable = () -> {
        try {

            Random random = new Random();
            int wait = (2000 * (1 + random.nextInt(5))); 
            logger.debug("Waiting {} seconds",wait/1000);
            
            // Sleep for between 2 to 10 Seconds before sending message to queue.
            Thread.sleep(wait);
            
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            logger.info(" [x] Queued {}",message);
        } catch (IOException | InterruptedException e) {
            logger.error("Error consuming message", e);
        }
    };
    executor.submit(runnable);
   }
}
