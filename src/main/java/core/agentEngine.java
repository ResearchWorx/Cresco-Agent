package core;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import plugins.PluginLoader;
import channels.LogProducer;
import channels.logEvent;


public class agentEngine {
    
	public static boolean isActive = false;
	public static boolean logProducerActive = false;
	public static boolean watchDogActive = false;
	public static long startTS = System.currentTimeMillis();
	
	public static Map<String, ConcurrentLinkedQueue<logEvent>> channelMap;
	public static Config config;
	
    public static void main(String[] args) throws Exception {
    
    	//Create Core Threads
    	//testing plugins
    	//processPlugins(config);
		//System.exit(1);
    	try 
    	{
    		
    		//Establish a named map of concurrent queues as defined in the config
    		channelMap = new ConcurrentHashMap<String,ConcurrentLinkedQueue<logEvent>>(); 
    		//Create log Queue
    		ConcurrentLinkedQueue<logEvent> logQueue = new ConcurrentLinkedQueue<logEvent>();
    		channelMap.put("log", logQueue);
    		
    		//Make sure initial input is sane.	
        	String configFile = checkConfig(args);
        	//String configFile = "Cresco-Agent.ini";
    		
        	//Make sure config file
        	config = new Config(configFile);
    		
    	    //Create Core Threads
        	processPlugins(config);
    		
    		//log producer is bound to log queue and ampq_log_exchange
    		LogProducer v = new LogProducer(channelMap.get("log"));
	    	Thread logProducerThread = new Thread(v);
	    	logProducerThread.start();
	    	
	    	//start core watchdog
	    	WatchDog wd = new WatchDog(channelMap.get("log"));
	    	
	    	
	    	//Notify agent start
	    	channelMap.get("log").offer(new logEvent("INFO","Agent Core Started"));
	    	
    	   isActive = true;
    	   //wait until shutdown occures
    	   while(isActive) 
    	   {
    		//just sleep until isActive=false
    		Thread.sleep(1000);           	
    	   }
    	   //stop other threads
    	   logProducerThread.interrupt();
   	    
    	}
    	catch (Exception e)
    	{
            System.out.println("Error: " + e.getMessage());
    	}
    	finally 
    	{
    	
    	}
    }

    
   public static String checkConfig(String[] args)
	{
		String errorMgs = "Cresco-Agent-Netflow\n" +
    			"Version: " + new Version().getVersion() + "\n" +
    			"Usage: java -jar Cresco-Agent-Netflow.jar" +
    			" -f <configuration_file>\n";
    			
    	if (args.length != 2)
    	{
    	  System.err.println(errorMgs);
    	  System.err.println("ERROR: Invalid number of arguements.");
      	  System.exit(1);
    	}
    	else if(!args[0].equals("-f"))
    	{
    	  System.err.println(errorMgs);
    	  System.err.println("ERROR: Must specify configuration file.");
      	  System.exit(1);
    	}
    	else
    	{
    		File f = new File(args[1]);
    		if(!f.exists())
    		{
    			System.err.println("The specified configuration file: " + args[1] + " is invalid");
    			System.exit(1);	
    		}
    	}
    return args[1];	
	}
   
    public static void processPlugins(Config conf) throws ClassNotFoundException, IOException
    {
    	/*
    	String str = "/Users/vcbumg2/Desktop/cresco/Cresco-Agent/plugins/cresco-agent-dummy-plugin.jar";
    	PluginLoader pl = new PluginLoader(str);
    	
    	System.out.println(pl.getPluginName());
    	System.out.println(pl.getPluginVersion());
    	Version vr = new Version();
    	System.out.println(vr.getVersion());
    	System.out.println(pl.getPluginCommandSet());
    	*/
    	  	
		/*
    	PluginLoader pl = new PluginLoader("location to jar);
    	System.out.println(pl.getPluginName());
    	PluginLoader pl2 = new PluginLoader("location to jar");
    	System.out.println(pl2.getPluginName());
    	*/
		
		/*
		if(config.getInstances() > 0)
		{
			
		}
		*/
    }
    
}


