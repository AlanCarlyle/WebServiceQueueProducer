package org.ajcarlyle.server;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class QueuePublisher {

    private final static String QUEUE_NAME = "products_queue";

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    


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
     new Thread(new QueueMessageRunnable(this, message)).start();
   }

   public static class QueueMessageRunnable implements Runnable {

    private QueuePublisher publisher;
        private String message;
        public QueueMessageRunnable(QueuePublisher publisher, String message)
        {
            this.publisher = publisher;
            this.message = message;
        }
        public void run() {
            try {
                Thread.sleep(5000);
                
                publisher.channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
              //  System.out.println( String.format(" [x] Sent '\"%s\"'",message));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
       
   }

}
