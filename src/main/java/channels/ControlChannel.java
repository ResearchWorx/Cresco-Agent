package channels;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import plugins.PluginInterface;
import shared.LogEvent;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import core.AgentEngine;


public class ControlChannel implements Runnable {

	private String RPC_QUEUE_NAME;
	private final Queue<LogEvent> logQueue;
    
	
		 public ControlChannel(Queue<LogEvent> log) {
	        this.logQueue = log;
	    	this.RPC_QUEUE_NAME = AgentEngine.config.getAMPQControlExchange();
	    }

	
	public void run()
	{
		try{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(AgentEngine.config.getAMPQControlHost());
	    factory.setUsername(AgentEngine.config.getAMPQControlUser());
	    factory.setPassword(AgentEngine.config.getAMPQControlPassword());
	    factory.setConnectionTimeout(10000);
	      
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

		channel.basicQos(1);

		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

		//System.out.println(" [x] Awaiting RPC requests");
		LogEvent le = new LogEvent("INFO","CORE","Controller Started");
		logQueue.offer(le);
		AgentEngine.ControlChannelEnabled = true;
		while (true) {
			
		    QueueingConsumer.Delivery delivery = consumer.nextDelivery();

		    BasicProperties props = delivery.getProperties();
		    
		    BasicProperties replyProps = new BasicProperties
		                                     .Builder()
		                                     .correlationId(props.getCorrelationId())
		                                     .build();
			
		    String message = new String(delivery.getBody());
		    //int n = Integer.parseInt(message);

		    //System.out.println("Server: " + message);
		    //String response = message + " pong";
		    
		    LogEvent le2 = new LogEvent("INFO","CORE","Controller Action Request: " + message);
		    logQueue.offer(le2);
		    
		    LogEvent ae = controlAction(message);
		    String response = null;
		    
		    if(ae.getEventType().equals("ERROR"))
		    {
		    	response = "1";
		    }
		    else if(ae.getEventType().equals("CONFIG"))
		    {
		    	response = "\nSettings:\n" + ae.getEventMsg(); 
		    }
		    else
		    {
		    	response = "0";
		    }
		    
		    logQueue.offer(ae);
	    	
		    channel.basicPublish( "", props.getReplyTo(), replyProps, response.getBytes());

		    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		}
		}
		catch(Exception ex)
		{
			System.out.println(ex);
			LogEvent le = new LogEvent("ERROR","CORE","Controller ERROR: " + ex.toString());
			logQueue.offer(le);
			
		}
	}
	
	private LogEvent controlAction(String message)
	{
		LogEvent le = new LogEvent("INFO","CORE","Controller Action Request: " + message);
		
		try{
			String actionLog = null;
			int action = Integer.parseInt(message);
			
				switch (action) {
				case 0:  {le.setEventType("CONFIG"); le.setEventMsg(getSettings()); return le;}
				case 1:  {le.setEventType("PLUGINCONFIG"); le.setEventMsg(getPlugins()); return le;}
				case 2:  {AgentEngine.logProducerActive = false; actionLog="logProducerActive=false";}
				break;
				case 3:  {AgentEngine.logProducerActive = true; actionLog="logProducerActive=true";}
				break;
				case 4:  {AgentEngine.watchDogActive = false; actionLog="watchDogActive=false";}
				break;
				case 5:  {AgentEngine.watchDogActive = true; actionLog="watchDogActive=true";}
				break;
							
			}
			le.setEventMsg(le.getEventMsg() + " Action Result: " + actionLog);
		return le;
		}
		catch(Exception ex)
		{
			System.out.println(ex);
			
			LogEvent le2 = new LogEvent("ERROR","CORE","Controller action: " + message + " ERROR: " + ex.toString());
			return le2;
		}
	}
	
	private String getSettings()
	{
		String settings = "logProducerActive=" + AgentEngine.logProducerActive + "\n";
		settings = settings + "watchDogActive=" + AgentEngine.watchDogActive + "\n";
		return settings;
	}
	public static String getPlugins() 
	{
		Map mp = AgentEngine.pluginMap;
		String str = null;
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            //System.out.println(pairs.getKey() + " = " + pairs.getValue());
            String pluginName = pairs.getKey().toString();
            PluginInterface pi = (PluginInterface)pairs.getValue();
            str = str + "Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion();
            		
            //System.out.println("Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion());
    		it.remove(); // avoids a ConcurrentModificationException
        }
        return str;
    }
	
}
