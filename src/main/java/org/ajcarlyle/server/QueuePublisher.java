package org.ajcarlyle.server;

import com.rabbitmq.client.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.Random;
import java.util.concurrent.ExecutorService;

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

    public  QueuePublisher() throws IOException, TimeoutException {

        factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();

        
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    }

   public void SendMessage(String message) throws IOException {

    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
    System.out.println( String.format(" [x] Sent '\"%s\"'",message));
   }
   public void Close() throws IOException, TimeoutException {
    channel.close();
    connection.close();
   }


   public void SendMessageAsync(String message) {

    Runnable runnable = () -> {
        try {

            Random random = new Random();
            int wait = 5000 + (1000 *  random.nextInt(10)); 
            logger.info("Waiting {} Seconds before queueing message",wait/1000);
            // Sleep for between 5 to 35 Seconds before sending message to queue.
            Thread.sleep(wait);
            
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            logger.info( String.format(" [x] Queued '\"%s\"'",message));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };
    executor.submit(runnable);
   }
}
