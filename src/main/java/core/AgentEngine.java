package core;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import plugins.ConfigPlugins;
import plugins.PluginInterface;
import plugins.PluginLoader;
import shared.LogEvent;
import channels.LogProducer;


public class AgentEngine {
    
	public static boolean isActive = false; //agent on/off
	public static boolean logProducerActive = false;  //log service on/off 
	public static boolean logProducerEnabled = false; //thread on/off
	public static boolean watchDogActive = false; //agent watchdog on/off
	
	public static Map<String, PluginInterface> pluginMap;
	//public static Map<String, ConcurrentLinkedQueue<LogEvent>> channelMap;
	public static ConcurrentLinkedQueue<LogEvent> logQueue;
	public static Config config;
	public static ConfigPlugins pluginsconfig;
	
    public static void main(String[] args) throws Exception {
    
    	try 
    	{
    		//Establish  a named map of plugin interfaces
    		pluginMap = new ConcurrentHashMap<String,PluginInterface>();
    		
    		//Make sure initial input is sane.	
        	String configFile = checkConfig(args);
        	
        	//Make sure config file
        	config = new Config(configFile);
    		
        	//Create log Queue wait to start
    		logQueue = new ConcurrentLinkedQueue<LogEvent>();
    		LogProducer v = new LogProducer(logQueue);
	    	Thread logProducerThread = new Thread(v);
	    	logProducerThread.start();
	    	while(!logProducerEnabled)
	    	{
	    		Thread.sleep(1000);
	    		String msg = "Waiting for logProducer Initialization...";
	    		logQueue.offer(new LogEvent("INFO","CORE",msg));
		    	System.out.println(msg);
	    	}
	    	
	    	//start core watchdog
	    	WatchDog wd = new WatchDog(logQueue);
	    	
	    	//Notify agent start
	    	String msg = "Agent Core (" + new Version().getVersion() + ") Started";
	    	logQueue.offer(new LogEvent("INFO","CORE",msg));
	    	System.out.println(msg);
	    	
	    	//Process Plugins
        	processPlugins(config);
    		
        	//printMap(pluginMap);
    	   //isActive = true;
    	   //wait until shutdown occures
        	System.out.println(pluginMap.get("plugin_0").getCommandSet());
     	   System.out.println(pluginMap.get("plugin_0").executeCommand("command-ls-la"));
     	   
        	while(isActive) 
    	   {
        	   
    		//just sleep until isActive=false
    		Thread.sleep(1000);
    		
    	   }
   		   Thread.sleep(20000);           	
   	    
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
    			String msg = "The specified configuration file: " + plugin_config_file + " is invalid";
    			System.err.println(msg);
    	 		logQueue.offer(new LogEvent("ERROR","CORE",msg));	    	
    			System.exit(1);	
    		}
    	
    		//pull in plugin configuration
    		pluginsconfig = new ConfigPlugins(plugin_config_file);
    		List<String> enabledPlugins = pluginsconfig.getEnabledPluginList();
    		
    		for(String pluginName : enabledPlugins)
    		{
    			System.out.println("Plugin Configuration: " + pluginName);
    			System.out.println("Plugin Location: " + pluginsconfig.getPluginJar(pluginName));
    			PluginLoader pl = new PluginLoader(pluginsconfig.getPluginJar(pluginName));
    	    	PluginInterface pi = pl.getPluginInterface();
    	    		   
    	    	if(pi.initialize(logQueue,pluginsconfig.getPluginConfig(pluginName)))
    	    	{
    	    		if(pluginsconfig.getPluginName(pluginName).equals(pi.getName()))
    	    		{
    	    			String msg = "Plugin Configuration: [" + pluginName + "] Initialized: (" + pi.getVersion() + ")";
    	    			System.out.println(msg);
    	    			logQueue.offer(new LogEvent("INFO","CORE",msg));	    	   	    			
    	    			pluginMap.put(pluginName, pi);
    	    		}
    	    		else
    	    		{
    	    			String msg = "Plugin Configuration: pluginname=" + pluginsconfig.getPluginName(pluginName) + " does not match Plugin Jar: " + pi.getVersion() + ")";
    	    			System.err.println(msg);
    	    	 		logQueue.offer(new LogEvent("ERROR","CORE",msg));
    	    	 		pl = null;
    	    			pi = null;
    	    			
    	    		}
    	    	}
    	    	else
    	    	{
    	    		String msg = pluginName + " Failed Initialization";
    	    		System.err.println(msg);
	    	 		logQueue.offer(new LogEvent("ERROR","CORE",msg));
    	    	}
    	    	
    		}
    		
    	}
    	catch(Exception ex)
    	{
    		System.err.println("Failed to Process Plugins: " + ex.toString());
    	}
    	
    }
    
    //to check plugin modules
    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            //System.out.println(pairs.getKey() + " = " + pairs.getValue());
            String pluginName = pairs.getKey().toString();
            PluginInterface pi = (PluginInterface)pairs.getValue();
            System.out.println("Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion());
    		it.remove(); // avoids a ConcurrentModificationException
        }
    }
    
}


