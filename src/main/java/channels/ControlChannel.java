package channels;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Queue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import shared.CmdEvent;
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
	private Marshaller CmdEventMarshaller;
	private Unmarshaller CmdEventUnmarshaller;
	private CommandExec commandExec;
    
    
	
		 public ControlChannel(Queue<LogEvent> log) {
	        this.logQueue = log;
	    	this.RPC_QUEUE_NAME = AgentEngine.config.getRegion() + "_control_" + AgentEngine.config.getAgentName();
	    	commandExec = new CommandExec();
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

		
		//XML Output
		JAXBContext jaxbContext = JAXBContext.newInstance(CmdEvent.class);
	    CmdEventUnmarshaller = jaxbContext.createUnmarshaller();
	    CmdEventMarshaller = jaxbContext.createMarshaller();
	    CmdEventMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		
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
		    
		    
		    //create CmdEvent 
		    InputStream stream = new ByteArrayInputStream(message.getBytes());		        
		    JAXBElement<CmdEvent> rootUm = CmdEventUnmarshaller.unmarshal(new StreamSource(stream), CmdEvent.class);		        
		    
		    //exec command
		    CmdEvent ce = commandExec.cmdExec(rootUm.getValue());
		    
		    String msg = "Control Channel Command:" + ce.getCmdType() + " Arguement(s):" + ce.getCmdArg() + " result:" + ce.getCmdResult();
		    LogEvent le2 = new LogEvent("INFO","CORE",msg);
		    logQueue.offer(le2);
		    
		    
		  //create rootXML for marshaler & create XML output
			StringWriter CmdEventXMLString = new StringWriter();
	        QName qName = new QName("com.researchworx.cresco.shared", "CmdEvent");
	        JAXBElement<CmdEvent> root = new JAXBElement<CmdEvent>(qName, CmdEvent.class, ce);
	        CmdEventMarshaller.marshal(root, CmdEventXMLString);
	        
	        //put responce on queue
		    channel.basicPublish( "", props.getReplyTo(), replyProps, CmdEventXMLString.toString().getBytes());
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
}
