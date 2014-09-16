package core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import plugins.ConfigPlugins;
import plugins.PluginLoader;
import channels.LogProducer;
import channels.logEvent;


public class agentEngine {
    
	public static boolean isActive = false;
	public static boolean logProducerActive = false;
	public static boolean logProducerEnabled = false;
	public static boolean watchDogActive = false;
	public static long startTS = System.currentTimeMillis();
	
	public static Map<String, ConcurrentLinkedQueue<logEvent>> channelMap;
	public static Config config;
	public static ConfigPlugins pluginsconfig;
	
    public static void main(String[] args) throws Exception {
    
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
	    	
    	   //isActive = true;
    	   //wait until shutdown occures
	       while(isActive) 
    	   {
    		//just sleep until isActive=false
    		Thread.sleep(1000);           	
    	   }
    	   //stop other threads
    	   
    	   logProducerActive = false;
    	   logProducerEnabled = false;
    	   
    	   System.exit(0);
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
    	try
    	{
    		String plugin_config_file = conf.getPluginConfigFile();
    		File f = new File(plugin_config_file);
    		if(!f.exists())
    		{
    			System.err.println("The specified configuration file: " + plugin_config_file + " is invalid");
    			System.exit(1);	
    		}
    	
    		//pull in plugin configuration
    		pluginsconfig = new ConfigPlugins(plugin_config_file);
    		//System.out.println(pluginsconfig.getModuleInstances())
    		List<String> enabledPlugins = pluginsconfig.getEnabledPluginList();
    		
    		for(String pluginName : enabledPlugins)
    		{
    			System.out.println("Plugin: " + pluginName);
    		}
    		
    	}
    	catch(Exception ex)
    	{
    		System.err.println("Failed to Process Plugins: " + ex.toString());
    	}
    	
    	/*
    	String str = "/Users/vcbumg2/Documents/Mesh/Work/Development/Cresco/Cresco-Agent/plugins/cresco-agent-dummy-plugin.jar";
    	PluginLoader pl = new PluginLoader(str);
    	
    	System.out.println(new Version().getVersion());
    	System.out.println(pl.getPluginVersion());
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


