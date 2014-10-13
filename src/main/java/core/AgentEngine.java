package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
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
	public static boolean ControllerActive = false; //control service on/off
	public static boolean ControllerEnabled = false; //control service on/off	
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
	    	while((!logProducerEnabled) && (logProducerThread.isAlive()))
	    	{
	    		Thread.sleep(1000);
	    		String msg = "Waiting for logProducer Initialization...";
	    		logQueue.offer(new LogEvent("INFO",AgentEngine.config.getAgentName(),msg));
		    	System.out.println(msg);
	    	}
	    	
	    	//Process Plugins
            processPlugins();
    		
	    	
	    	//create control channel after everything else has been loaded.
	    	ControlChannel c = new ControlChannel(logQueue);
	    	Thread ControlChannelThread = new Thread(c);
	    	ControlChannelThread.start();
	    	while((!ControlChannelEnabled) && (ControlChannelThread.isAlive()))
	    	{
	    		Thread.sleep(1000);
	    		String msg = "Waiting for Control Channel Initialization...";
	    		logQueue.offer(new LogEvent("INFO",AgentEngine.config.getAgentName(),msg));
		    	System.out.println(msg);
	    	}
	    	
	    	if(ControlChannelEnabled && logProducerEnabled)
	    	{
	    		System.out.println("Waiting for Controller Response Initialization...");
	    		int tryController = 1;
	    		int controllerDiscoveryTimeout = config.getControllerDiscoveryTimeout();
	    		int controllerLaunchTimeout = Math.round(controllerDiscoveryTimeout/2);
	    		//give the controller 20 sec to respond. First 10 for possibe existing, 
	    		//last 10 for one we try and start 
	    		while((!ControllerActive) && (tryController < controllerDiscoveryTimeout))
	    		{
	    			//Notify controler of agent enable wait for controller contact
	    			logQueue.offer(new LogEvent("CONFIG",config.getAgentName(),"enabled"));	
			    	Thread.sleep(1000);
	    			tryController++; //give 30 atempts for controller to respond
	    			if(tryController > controllerLaunchTimeout)
	    			{
	    				String controllerPlugin = findPlugin("ControllerPlugin",0);
	    				if(controllerPlugin != null)
	    				{
	    					System.out.println("Try and Start our own controller");
	    					//start controller but don't save the config.
	    					String controllerSlot = enablePlugin(controllerPlugin, false);
	    					System.out.println("Enabled Controller Module [" + controllerSlot + "]");	
	    				}
	    				
	    			}
	    		}
	    		
	    		if(ControllerActive)
	    		{
	    			System.out.println("Region:" + config.getRegion() + " Controller found:");
	    		}
	    		else
	    		{
	    			System.out.println("Region:" + config.getRegion() + " *NOT* Controller found:");
	    		}
	    		//wait until shutdown occures
	        	isActive = true;
	     	   
	        	//start core watchdog
		    	WatchDog wd = new WatchDog(logQueue);
		    	
		    	//Notify log that agent has started
    			String msg = "Agent Core (" + agentVersion + ") Started";
    			logQueue.offer(new LogEvent("INFO",config.getAgentName(),msg));
    			System.out.println(msg);
    			
	    	}
	    	else
	    	{
	    		System.out.println("Agent is a Zombie!\nNo Active Log or Control Channels!\nAgent will now shutdown.");
	    		isActive = false;
	    	}
	    	
           
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
    	 		logQueue.offer(new LogEvent("ERROR",AgentEngine.config.getAgentName(),msg));	    	
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
    	    		   
    	    	if(pi.initialize(logQueue,pluginsconfig.getPluginConfig(pluginName),pluginName,AgentEngine.config.getAgentName()))
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
    		
    	}
    	catch(Exception ex)
    	{
    		String msg = "Failed to Process Plugins: " + ex.toString();
			System.err.println(msg);
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
		    	    	
		    	    	if(pi.initialize(logQueue,pluginsconfig.getPluginConfig(pluginName),pluginName,AgentEngine.config.getAgentName()))
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
		   return config.getAgentName() + "." + version;
	   }
   //This needs to be redone to account for active but not configured
   public static String findPlugin(String searchPluginName, int isActive) //loop through known plugins on agent
	{
		StringBuilder sb = new StringBuilder();
       
		List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList(isActive);
		
		
		
		if(pluginList.size() > 0)
		{
			for(String pluginName : pluginList)
			{
					if(AgentEngine.pluginsconfig.getPluginName(pluginName).equals(searchPluginName))
					{
						if((isActive == 0) && !AgentEngine.pluginMap.containsKey(pluginName))
						{
							return pluginName;
						}
						
					}
				
			}
			return null;
		}
		else
		{
			return null;
		}
		
   }
   
   //needs to be redone to account for active and configured.
   public static String listPlugins() //loop through known plugins on agent
	{
	   StringBuilder sb = new StringBuilder();
      
		List<String> pluginListEnabled = new ArrayList<String>(AgentEngine.pluginsconfig.getEnabledPluginList(1));
		List<String> pluginListDisabled = new ArrayList<String>(AgentEngine.pluginsconfig.getEnabledPluginList(0));
		List<String> pluginListActive = getActivePlugins(); 
	   	   
		
		
		if((pluginListEnabled.size() > 0) || (pluginListDisabled.size() > 0))
		{
			if((pluginListEnabled.size() > 0) || (pluginListActive.size() > 0))
			{
				sb.append("Enabled Plugins:\n");
			}
			for(String pluginName : pluginListEnabled)
			{
				if((AgentEngine.pluginMap.containsKey(pluginName)) && !pluginListActive.contains(pluginName))
				{
					PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
					sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
				}
			}
			for(String pluginName : pluginListActive)
			{
				PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
				sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
			}
			if(pluginListDisabled.size() > 0)
			{
				sb.append("Disabled Plugins:\n");
			}
			for(String pluginName : pluginListDisabled)
			{
				if(!pluginListActive.contains(pluginName))
				{
				sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName)  + "\n");
				}
			}		
		}
		else
		{
			sb.append("No Plugins Found!\n");
			
		}
		return sb.toString().substring(0,sb.toString().length()-1);
  }
   
   static List<String> getActivePlugins()
   {
	   List<String> pluginList = new ArrayList<String>();
	   if(pluginMap != null)
   	   {
   		   Iterator it = pluginMap.entrySet().iterator();
	    	while (it.hasNext()) {
	        	Map.Entry pairs = (Map.Entry)it.next();
	        	System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        	String plugin = pairs.getKey().toString();
	        	//disablePlugin(plugin,false);
	        	pluginList.add(plugin);
	        	//it.remove(); // avoids a ConcurrentModificationException
	    	}
   	    }
	   return pluginList;
   }
   
   static void cleanup()
   {
	   System.out.println("Shutdown:Cleaning Active Agent Resources");
	       List<String> pluginList = getActivePlugins(); 
	   	   for(String plugin : pluginList)
	   	   {
	   		   disablePlugin(plugin,false);
	   	   }
	       /*
	   	   if(pluginMap != null)
	   	   {
	   		   Iterator it = pluginMap.entrySet().iterator();
		    	while (it.hasNext()) {
		        	Map.Entry pairs = (Map.Entry)it.next();
		        	System.out.println(pairs.getKey() + " = " + pairs.getValue());
		        	String plugin = pairs.getKey().toString();
		        	disablePlugin(plugin,false);
		        	it.remove(); // avoids a ConcurrentModificationException
		    	}
	   	    }
	   	    */
		    if(logQueue != null)
		    {
		    logQueue.offer(new LogEvent("CONFIG",config.getAgentName(),"disabled"));	    	   	    			
		    }
	    	   //stop other threads
	    	   logProducerActive = false;
	    	   logProducerEnabled = false;
	    	if(logProducerThread != null)
	    	{
	    		while(logProducerThread.isAlive())
	    		{
		    		try 
		    		{
						Thread.sleep(1000);
					} 
		    		catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
	    	}
   }
}


