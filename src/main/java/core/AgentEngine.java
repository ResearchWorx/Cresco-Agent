package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import plugins.ConfigPlugins;
import plugins.PluginInterface;
import plugins.PluginLoader;
import security.RandomString;
import shared.LogEvent;
import channels.ControlChannel;
import channels.LogProducer;


public class AgentEngine {
    
	public static boolean isActive = false; //agent on/off
	public static boolean logProducerActive = false;  //log service on/off
	public static boolean ControlChannelEnabled = false; //control service on/off
	public static boolean logProducerEnabled = false; //thread on/off
	public static boolean watchDogActive = false; //agent watchdog on/off
	public static String agentVersion = null;
	private static Thread logProducerThread;
	
	public static Map<String, PluginInterface> pluginMap;
	public static ConcurrentLinkedQueue<LogEvent> logQueue;
	public static Config config;
	public static ConfigPlugins pluginsconfig;
	
    public static void main(String[] args) throws Exception {
    
    	try 
    	{
    		//Cleanup on Shutdown
    		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    	        public void run() {
    	            cleanup();	            
    	        }
    	    }, "Shutdown-thread"));
    		
    		
    		//Establish  a named map of plugin interfaces
    		pluginMap = new ConcurrentHashMap<String,PluginInterface>();
    		
    		//Make sure initial input is sane.	
        	String configFile = checkConfig(args);
        	
        	//Make sure config file
        	config = new Config(configFile);
    		
        	//Generate Random Agent String
    		if(config.getGenerateName())
        	{
        		RandomString rs = new RandomString(4);
        		String AgentName = "agent-" + rs.nextString();
        		config.setAgentName(AgentName);
        	}
        	System.out.println("AGENT NAME:[" +config.getAgentName() +"]");
        	
        	//set version name
    		agentVersion = new String(getVersion());
	    
        	
        	//Create log Queue wait to start
    		logQueue = new ConcurrentLinkedQueue<LogEvent>();
    		LogProducer v = new LogProducer(logQueue);
	    	logProducerThread = new Thread(v);
	    	logProducerThread.start();
	    	while(!logProducerEnabled)
	    	{
	    		Thread.sleep(1000);
	    		String msg = "Waiting for logProducer Initialization...";
	    		logQueue.offer(new LogEvent("INFO","CORE",msg));
		    	System.out.println(msg);
	    	}
	    	
	    	//Process Plugins
            processPlugins();
    		
	    	
	    	//create control channel after everything else has been loaded.
	    	ControlChannel c = new ControlChannel(logQueue);
	    	Thread ControlChannelThread = new Thread(c);
	    	ControlChannelThread.start();
	    	while(!ControlChannelEnabled)
	    	{
	    		Thread.sleep(1000);
	    		String msg = "Waiting for Control Channel Initialization...";
	    		logQueue.offer(new LogEvent("INFO","CORE",msg));
		    	System.out.println(msg);
	    	}
	    	
	    	//Notify agent start
	    	String msg = "Agent Core (" + agentVersion + ") Started";
	    	logQueue.offer(new LogEvent("INFO","CORE",msg));
	    	System.out.println(msg);
	    	
           //wait until shutdown occures
        	isActive = true;
     	   
        	//start core watchdog
	    	WatchDog wd = new WatchDog(logQueue);
	    	logQueue.offer(new LogEvent("CONFIG",config.getAgentName(),"enabled"));	    	   	    			
			
        	while(isActive) 
    	   {
        	   
    		//just sleep until isActive=false
        	//need to add ability to control other threads here.
        	//need to add upgrade ability
    		Thread.sleep(1000);
    		
    	   }
        	
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
		String errorMgs = "Cresco-Agent\n" +
    			"Version: " + new Version().getVersion() + "\n" +
    			"Usage: java -jar Cresco-Agent.jar" +
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
   
   public static void processPlugins() throws ClassNotFoundException, IOException
    {
    	try
    	{
    		String plugin_config_file = config.getPluginConfigFile();
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
    		
    		@SuppressWarnings("unchecked")
			List<String> enabledPlugins = pluginsconfig.getEnabledPluginList(1);//return enabled values in the config
    		
    		for(String pluginName : enabledPlugins)
    		{
    			System.out.println("Plugin Configuration: " + pluginName);
    			System.out.println("Plugin Location: " + pluginsconfig.getPluginJar(pluginName));
    			PluginLoader pl = new PluginLoader(pluginsconfig.getPluginJar(pluginName));
    	    	PluginInterface pi = pl.getPluginInterface();
    	    		   
    	    	if(pi.initialize(logQueue,pluginsconfig.getPluginConfig(pluginName),pluginName))
    	    	{
    	    		if(pluginsconfig.getPluginName(pluginName).equals(pi.getName()))
    	    		{
    	    			String msg = "Plugin Configuration: [" + pluginName + "] Initialized: (" + pi.getVersion() + ")";
    	    			System.out.println(msg);
    	    			//logQueue.offer(new LogEvent("INFO","CORE",msg));
    	    			logQueue.offer(new LogEvent("CONFIG",config.getAgentName() + "_" + pluginName,"enabled"));	    	   	    			
    	    			
    	    			pluginMap.put(pluginName, pi);
    	    		}
    	    		else
    	    		{
    	    			String msg = "Plugin Configuration: pluginname=" + pluginsconfig.getPluginName(pluginName) + " does not match Plugin Jar: " + pi.getVersion() + ")";
    	    			System.err.println(msg);
    	    	 		//logQueue.offer(new LogEvent("ERROR","CORE",msg));
    	    			logQueue.offer(new LogEvent("ERROR",config.getAgentName() + "_" + pluginName,msg));
    	    	 		pl = null;
    	    			pi = null;	    			
    	    		}
    	    	}
    	    	else
    	    	{
    	    		String msg = pluginName + " Failed Initialization";
    	    		System.err.println(msg);
	    	 		//logQueue.offer(new LogEvent("ERROR","CORE",msg));
	    	 		logQueue.offer(new LogEvent("ERROR",config.getAgentName(),msg));
    	    	}
    	    	
    		}
    		
    	}
    	catch(Exception ex)
    	{
    		String msg = "Failed to Process Plugins: " + ex.toString();
			System.err.println(msg);
	 		//logQueue.offer(new LogEvent("ERROR","CORE",msg));
	 		logQueue.offer(new LogEvent("ERROR",config.getAgentName(),msg));
    	}
    	
    }
   
   public static String disablePlugin(String plugin, boolean save) //loop through known plugins on agent
	{
	   StringBuilder sb = new StringBuilder();
		List<String> pluginListEnabled = pluginsconfig.getEnabledPluginList(1);
		
		if(pluginListEnabled.size() > 0)
		{
			boolean isFound = false;
			for(String pluginName : pluginListEnabled)
			{
				if(pluginName.equals(plugin))
				{
					try 
					{
						isFound = true;
						if(save)
						{
							pluginsconfig.setPluginStatus(plugin, 0);//save in config 
						}
						
						PluginInterface pi = pluginMap.get(pluginName);
    	    			String msg = "Plugin Configuration: [" + pluginName + "] Removed: (" + pi.getVersion() + ")";
    	    			pi.shutdown();
    	    			pi = null;
    	    			pluginMap.remove(pluginName);
    	    			System.out.println(msg);
    	    			logQueue.offer(new LogEvent("CONFIG",config.getAgentName() + "_" + pluginName,"disabled"));
    	    			sb.append(msg);
					}
					catch (Exception ex) 
					{
						sb.append("Error : disablePlugin : " + ex.toString());
					}
					break;
				}
			}
			if(!isFound)
			{
			sb.append("No Configuration Found for Plugin: [" + plugin + "]");
			}
		}
		else
		{
			sb.append("No Plugins Found!");
		}
		return sb.toString();
	}

   public static String enablePlugin(String plugin, boolean save) //loop through known plugins on agent
	{
		StringBuilder sb = new StringBuilder();
		List<String> pluginListDisabled = pluginsconfig.getEnabledPluginList(0);
		
		if(pluginListDisabled.size() > 0)
		{
			boolean isFound = false;
			for(String pluginName : pluginListDisabled)
			{
				if(pluginName.equals(plugin))
				{
					try 
					{
						isFound = true;
						sb.append("Plugin: [" + plugin + "] Enabled");
						if(save)
						{
							pluginsconfig.setPluginStatus(plugin, 1);
						}
						PluginLoader pl = new PluginLoader(pluginsconfig.getPluginJar(pluginName));
		    	    	PluginInterface pi = pl.getPluginInterface();
		    	    	
		    	    	if(pi.initialize(logQueue,pluginsconfig.getPluginConfig(pluginName),pluginName))
		    	    	{
		    	    		if(pluginsconfig.getPluginName(pluginName).equals(pi.getName()))
		    	    		{
		    	    			String msg = "Plugin Configuration: [" + pluginName + "] Initialized: (" + pi.getVersion() + ")";
		    	    			System.out.println(msg);
		    	    			logQueue.offer(new LogEvent("CONFIG",config.getAgentName() + "_" + pluginName,"enabled"));	    	   	    			
		    	    			pluginMap.put(pluginName, pi);
		    	    		}
		    	    		else
		    	    		{
		    	    			String msg = "Plugin Configuration: pluginname=" + pluginsconfig.getPluginName(pluginName) + " does not match Plugin Jar: " + pi.getVersion() + ")";
		    	    			System.err.println(msg);
		    	    	 		logQueue.offer(new LogEvent("ERROR",config.getAgentName() + "_" + pluginName,msg));
		    	    	 		pl = null;
		    	    			pi = null;
		    	    			
		    	    		}
		    	    	}
		    	    	else
		    	    	{
		    	    		String msg = pluginName + " Failed Initialization";
		    	    		System.err.println(msg);
			    	 		logQueue.offer(new LogEvent("ERROR",config.getAgentName(),msg));
		    	    	}
		    	    	
		    	    	
					} 
					catch (Exception ex) 
					{
						sb.append("Error : enablePlugin : " + ex.toString());
					}
					break;
				}
			}
			if(!isFound)
			{
			sb.append("No Configuration Found for Plugin: [" + plugin + "]");
			}
		}
		else
		{
			sb.append("No Plugins Found!");
		}
		return sb.toString();
   }
   
   static void cleanup()
   {
	   System.out.println("\nDisabling Plugins");
	       Iterator it = pluginMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        System.out.println(pairs.getKey() + " = " + pairs.getValue());
		        String plugin = pairs.getKey().toString();
		        disablePlugin(plugin,false);
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		    
		    logQueue.offer(new LogEvent("CONFIG",config.getAgentName(),"disabled"));	    	   	    			
			
	    	   //stop other threads
	    	   logProducerActive = false;
	    	   logProducerEnabled = false;
	    	   
		    while(logProducerThread.isAlive())
		    {
		    	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
   }
   public static String getVersion() //This should pull the version information from jar Meta data
   {
		   String version;
		   try{
		   String jarFile = AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		   System.out.println("JARFILE:" + jarFile);
		   //File file = new File(jarFile.substring(5, (jarFile.length() )));
		   File file = new File(jarFile);
          FileInputStream fis = new FileInputStream(file);
          @SuppressWarnings("resource")
		   JarInputStream jarStream = new JarInputStream(fis);
		   Manifest mf = jarStream.getManifest();
		   
		   Attributes mainAttribs = mf.getMainAttributes();
          version = mainAttribs.getValue("Implementation-Version");
		   }
		   catch(Exception ex)
		   {
			   String msg = "Unable to determine Plugin Version " + ex.toString();
			   System.err.println(msg);
			   version = "Unable to determine Version";
		   }
		   System.out.println("WOOT:" + version);
		   return config.getAgentName() + "." + version;
	   }
   
}


