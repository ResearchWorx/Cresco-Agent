package channels;
import java.io.IOException;
import java.util.Queue;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import core.AgentEngine;

public class LogProducer implements Runnable {

    private final Queue<LogEvent> logQueue;
    private Channel channel_log;
    private Connection connection;
    private ConnectionFactory factory;
    
    private String EXCHANGE_NAME_LOG;
    
    
    public LogProducer(Queue<LogEvent> logQueue) {
    	this.logQueue = logQueue;
        this.EXCHANGE_NAME_LOG = AgentEngine.config.getAMPQLogExchange();
    }
    
    public void run() {
        
    	try{
    	
    		factory = new ConnectionFactory();

        	factory.setHost(AgentEngine.config.getAMPQLogHost());
    	    factory.setUsername(AgentEngine.config.getAMPQLogUser());
    	    factory.setPassword(AgentEngine.config.getAMPQLogPassword());
    	    factory.setConnectionTimeout(10000);
    	    connection = factory.newConnection();
    		channel_log = connection.createChannel();
    		channel_log.exchangeDeclare(EXCHANGE_NAME_LOG, "fanout");
	    
    	}
    	catch(Exception ex)
    	{
    		System.err.println("LogProducer Initialization Failed:  Exiting");
    		System.err.println(ex);
    		System.exit(1);
    	}
    
    	AgentEngine.logProducerActive = true;
    	AgentEngine.logProducerEnabled = true;
    	
    	System.out.println("*LogProducer Started*");
    	LogEvent le = new LogEvent("INFO","LogProducer Started");
    	logQueue.offer(le);
    	
    	while (AgentEngine.logProducerEnabled) {
        	try {
        		
        		if(AgentEngine.logProducerActive)
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
				le = new LogEvent("INFO","LogProducer Stopped");
		    	logQueue.offer(le);
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
		    		System.out.println("LogProducer Interupted" + ex.toString());	        	
		    		le = new LogEvent("ERROR","LogProducer Interupted" + ex.toString());
			    	logQueue.offer(le);
		    	}		    	
			}
        	
        }
    	System.out.println("LogProducer Disabled");   	
    	le = new LogEvent("INFO","LogProducer Disabled");
    	logQueue.offer(le);
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
    		
    	synchronized(logQueue) {
    		while ((!logQueue.isEmpty())) {
    			LogEvent le = logQueue.poll();
    			System.out.println(le.toString());
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