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
    
    new JobProcessor(channel,message).run();
   }


   public static class JobProcessor implements Runnable {

    private final static Random random = new Random();
    private String message;

    private Channel channel;
    public JobProcessor(Channel channel, String message){
        this.message = message;
        this.channel = channel;
    }

        @Override
        public void run() {
            try {
                logger.info(" [x] Queing Message '{}''",message);
                byte[] body = message.getBytes();
              
               int wait = (1000 + 1000 * random.nextInt(2)); 
           //     logger.info("Waiting {} Seconds before queueing message '{}'",wait/1000,message);
                // Sleep for between 1 to 5 Seconds before sending message to queue.
                Thread.sleep(wait);
                
                channel.basicPublish("", QUEUE_NAME, null, body );
                
            } catch (IOException | InterruptedException e) {
                logger.error("Error consuming message", e);
            }
        }
    


   } 
}
