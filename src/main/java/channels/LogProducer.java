package channels;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Queue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;

import shared.LogEvent;

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
    private Marshaller LogEventMarshaller;
    
    public LogProducer(Queue<LogEvent> logQueue) {
    	this.logQueue = logQueue;
        this.EXCHANGE_NAME_LOG = AgentEngine.config.getRegion() + "_log";
    }
    
    public void run() {
        
    	try{
    	
    		//Queue AMPQ Output
    		factory = new ConnectionFactory();
    		factory.setHost(AgentEngine.config.getAMPQControlHost());
    	    factory.setUsername(AgentEngine.config.getAMPQControlUser());
    	    factory.setPassword(AgentEngine.config.getAMPQControlPassword());
    	    factory.setConnectionTimeout(10000);
    	    
    	    connection = factory.newConnection();
    		channel_log = connection.createChannel();
    		channel_log.exchangeDeclare(EXCHANGE_NAME_LOG, "fanout");
    		
    		//XML Output
    		JAXBContext context = JAXBContext.newInstance(LogEvent.class); 
    		LogEventMarshaller = context.createMarshaller();
    		LogEventMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    		
    	}
    	catch(Exception ex)
    	{
    		System.err.println("LogProducer Initialization Failed:  Exiting");
    		System.err.println(ex);
    		return; //don't kill app if log can't be established
    		//System.exit(1);
    	}
    
    	AgentEngine.logProducerActive = true;
    	AgentEngine.logProducerEnabled = true;
    	
    	LogEvent le = new LogEvent("INFO",AgentEngine.config.getAgentName(),"LogProducer Started");
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
				le = new LogEvent("INFO",AgentEngine.config.getAgentName(),"LogProducer Stopped");
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
		    		le = new LogEvent("ERROR",AgentEngine.config.getAgentName(),"LogProducer Interupted" + ex.toString());
			    	logQueue.offer(le);
		    	}		    	
			}
        	
        }
    	System.out.println("LogProducer Disabled");   	
    	le = new LogEvent("INFO",AgentEngine.config.getAgentName(),"LogProducer Disabled");
    	logQueue.offer(le);
    	try {
			log(); //one last call
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //
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
    			
    			LogEvent le = logQueue.poll(); //get logevent
    			le.setEventAgent(AgentEngine.config.getAgentName()); //set agent name
    			
    			//create rootXML for marshaler & create XML output
    			StringWriter LogEventXMLString = new StringWriter();
    	        QName qName = new QName("com.researchworx.cresco.shared", "LogEvent");
    	        JAXBElement<LogEvent> root = new JAXBElement<LogEvent>(qName, LogEvent.class, le);
    	        LogEventMarshaller.marshal(root, LogEventXMLString);
    	        
    			//System.out.println(LogEventXMLString.toString());
    	        //put XML on queue
    	        
    			channel_log.basicPublish(EXCHANGE_NAME_LOG, "", null, LogEventXMLString.toString().getBytes());
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