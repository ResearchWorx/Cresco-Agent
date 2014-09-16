package channels;
import java.io.IOException;
import java.util.Queue;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import core.agentEngine;

public class LogProducer implements Runnable {

    private final Queue<logEvent> log;
    private static Channel channel_log;
    private static Connection connection;
    private static ConnectionFactory factory;
    
    private String EXCHANGE_NAME_LOG;
    
    
    public LogProducer(Queue<logEvent> log) {
    	this.log = log;
        this.EXCHANGE_NAME_LOG = agentEngine.config.getAMPQLogExchange();
    }
    
    public void run() {
    	
    	try{
    	factory = new ConnectionFactory();
	    factory.setHost(agentEngine.config.getAMPQHost());
	    factory.setUsername(agentEngine.config.getAMPQUser());
	    factory.setPassword(agentEngine.config.getAMPQPassword());
	    
	    connection = factory.newConnection();
	    channel_log = connection.createChannel();
	    
	    channel_log.exchangeDeclare(EXCHANGE_NAME_LOG, "fanout");
	    
    	}
    	catch(Exception ex)
    	{
    		System.err.println("LogProducer Init Failed:  Exiting");
    		System.err.println(ex);
    		System.exit(1);
    	}
    
    	agentEngine.logProducerActive = true;
    	logEvent le = new logEvent("INFO","LogProducer Started");
    	log.offer(le);
    	
    	while (agentEngine.logProducerEnabled) {
        	try {
        		if(agentEngine.logProducerActive)
        		{
        			log();
        		}
        		
				Thread.sleep(100);
	        } catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.out.println(e1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				le = new logEvent("INFO","LogProducer Stopped");
		    	log.offer(le);
		    	try{
		    	if(channel_log.isOpen())
	 			{
	 				channel_log.close();
	 			}
	 			if(connection.isOpen())
	 			{
	        		connection.close();
	 			}
		    	}
		    	catch(Exception ex)
		    	{
		    		le = new logEvent("ERROR","LogProducer Interupted" + ex.toString());
			    	log.offer(le);
		    	}		    	
			}
        	
        }
    	le = new logEvent("INFO","LogProducer Interupted ");
    	log.offer(le);
    	Thread.currentThread().interrupt();
    	return;
    }

    private void log() throws IOException {
    	try{
    	if(!connection.isOpen())
		{
			System.out.println("Reconnecting Connection");
			connection = factory.newConnection();
		}
		if(!channel_log.isOpen())
		{
			System.out.println("Reconnecting Channel");
			channel_log = connection.createChannel();
		    channel_log.exchangeDeclare(EXCHANGE_NAME_LOG, "fanout");
		}
    	}
    	catch(Exception ex)
    	{
    		System.out.println(ex);
    	}
    	try{
    		
    	synchronized(log) {
    		while ((!log.isEmpty())) {
    			
    			logEvent le = log.poll();
    			channel_log.basicPublish(EXCHANGE_NAME_LOG, "", null, le.toString().getBytes());
    		}
    	}
    	}
    	catch(Exception ex)
    	{
    		if(channel_log.isOpen())
 			{
 				channel_log.close();
 			}
 			if(connection.isOpen())
 			{
        		connection.close();
 			}
    		System.out.println(ex);
    	}
         			
         			
    	}
    
}