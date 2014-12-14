package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.commons.configuration.ConfigurationException;

import channels.MsgInQueue;
import channels.MsgOutQueue;
import plugins.ConfigPlugins;
import plugins.PluginLoader;
import shared.Clogger;
import shared.MsgEvent;
import shared.MsgEventType;
import shared.PluginInterface;
import shared.RandomString;



public class AgentEngine {
    
	public static boolean isActive = false; //agent on/off
	public static WatchDog wd;
	public static boolean hasChannel = false;
	public static String channelPluginSlot;
	public static boolean isController = false;
	public static String controllerPluginSlot;
	
	public static ConcurrentLinkedQueue<MsgEvent> msgInQueue;
	public static Thread MsgInQueueThread;
	public static boolean MsgInQueueEnabled = false; //control service on/off	
	public static boolean MsgInQueueActive = false; //control service on/off	
	
	public static ConcurrentLinkedQueue<MsgEvent> msgOutQueue;
	public static Thread MsgOutQueueThread;
	public static boolean MsgOutQueueEnabled = false; //control service on/off	
	public static boolean MsgOutQueueActive = false; //control service on/off	
	
	public static boolean ControllerEnabled = false; //control service on/off	
	public static boolean ControllerActive = false; //control service on/off	
	
	public static CommandExec commandExec;
	
	public static boolean watchDogActive = false; //agent watchdog on/off
	public static String agentVersion = null;
	
	public static Map<String, PluginInterface> pluginMap;
	
	public static Config config;
	public static ConfigPlugins pluginsconfig;
	
	public static String region;
	public static String agent;
	
	public static Clogger clog;
	
	public static DelayedShutdown ds; //delayed shutdown command
	
    public static void main(String[] args) throws Exception {
    
    	try 
    	{
    		//create command group
    		commandExec = new CommandExec();
    		
    		//create logger and base queue
    		msgInQueue = new ConcurrentLinkedQueue<MsgEvent>();
    		msgOutQueue = new ConcurrentLinkedQueue<MsgEvent>();
    		
    		//Cleanup on Shutdown
    		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    	        public void run() {
    	            try{
    	        	cleanup();
    	            }
    	            catch(Exception ex)
    	            {
    	            	System.out.println("Exception Shutting Down:" + ex.toString());
    	            }
    	        }
    	    }, "Shutdown-thread"));
    		
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
    		//set region and agent
    		agent = config.getAgentName();
    		region = config.getRegion();
        	
    		clog = new Clogger(msgInQueue,region,agent,null);
    		
    		
    		System.out.println("Building MsgOutQueue");
    		//start outgoing queue
    		
    		MsgOutQueue moq = new MsgOutQueue();
    		MsgOutQueueThread = new Thread(moq);
    		MsgOutQueueThread.start();
        	while(!MsgOutQueueEnabled)
        	{
        		Thread.sleep(100);
        	}
        	System.out.println("Building MsgInQueue");
    		//start incoming queue
        	MsgInQueue miq = new MsgInQueue();
        	MsgInQueueThread = new Thread(miq);
        	MsgInQueueThread.start();
        	while(!MsgInQueueEnabled)
        	{
        		Thread.sleep(100);
        	}
        	
        	//Establish  a named map of plugin interfaces
    		pluginMap = new ConcurrentHashMap<String,PluginInterface>();
        	
    		//build plugin list and launch startup plugins
        	processPlugins();
    		
        	//if channel was not configured during startup try and establish
        	if(!hasChannel)
        	{
        		getChannel(); //currently AMPQ and REST plugins
        	}
        	
        	//if controller was not configured during startup try and establish
        	if(!ControllerActive)
        	{
        		getController();	    	
        	}
        	
	    	//start core watchdog
	    	wd = new WatchDog();
	    	
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
            System.out.println("Error AgentCore: " + e.getMessage());
    	}
    	finally 
    	{
    		System.out.println("Agent : Main :Finally Statement");
    	}
    }

    public static void msgIn(MsgEvent me)
	{
		final MsgEvent ce = me;
		try
		{
		Thread thread = new Thread(){
		    public void run(){ //command request go in new threads
		      
		    	try 
		        {
		        	if(ce == null)
		        	{
		        		System.out.println("Agent : msgIn : Incoming message NULL!!!!");
		        	}
		        	
					MsgEvent re = commandExec.cmdExec(ce); //execute command
					if(re != null)
					{
						re.setReturn(); //reverse to-from for return
						msgInQueue.offer(re);
					}
					
				} 
		        catch(Exception ex)
		        {
		        	clog.error("Agent : AgentEngine : msgIn Thread: " + ex.toString());
		        }
		    }
		  };
		  thread.start(); //start the exec thread
		}
		catch(Exception ex)
		{
			
			clog.error("Agent : AgentEngine : msgIn : " + ex.toString());
		}
		
	}
    
    public static void getController() throws InterruptedException, ConfigurationException, IOException
    {
    	
    	if(MsgInQueueActive && MsgOutQueueActive)
    	{
    		int tryController = 1;
    		int controllerDiscoveryTimeout = config.getControllerDiscoveryTimeout();
    		int controllerLaunchTimeout = Math.round(controllerDiscoveryTimeout/2);
    		//give the controller 20 sec to respond. First 10 for possibe existing, 
    		//last 10 for one we try and start 
    		while((!ControllerActive) && (tryController < controllerDiscoveryTimeout))
    		{
    			System.out.println("Waiting for Controller Response Initialization...");
	    		
    	    	 if(AgentEngine.channelPluginSlot != null) //controller plugin was found 
   	 			 {
   					MsgEvent de = clog.getLog("enabled");
   					de.setMsgType(MsgEventType.CONFIG);
   					de.setMsgAgent(AgentEngine.agent); //route to this agent
   					de.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin
   					de.setParam("attempt", String.valueOf(tryController));
   					AgentEngine.commandExec.cmdExec(de);
       			 }
    	    	
    	    	Thread.sleep(1000);
     			tryController++; //give X trys for controller to respond
     			
    			//Notify controler of agent enable wait for controller contact
    			
		    	if((tryController > controllerLaunchTimeout) && !isController)
    			{
    				String controllerPlugin = findPlugin("ControllerPlugin",0);
    				if(controllerPlugin != null)
    				{
    					System.out.println("Try and Start our own controller");
    					//start controller but don't save the config.
    					isController = enablePlugin(controllerPlugin, false);
    					if(!isController)
    					{
    						System.out.println("Controller Plugin NOT Loaded");
    					}
    					else
    					{
    						System.out.println("Controller Plugin Loaded");
    						controllerPluginSlot = controllerPlugin;
    						isActive = true;
    						//else if(ce.getParam("cmd").equals("enablelogconsumer"))
    						//enable incoming logs
    						MsgEvent me = new MsgEvent(MsgEventType.EXEC,region,agent,channelPluginSlot,"Enable Incoming Log");
    						me.setParam("cmd", "enablelogconsumer");
    						me.setSrc(region, agent, null);
    						me.setDst(region, agent, channelPluginSlot);
    						AgentEngine.msgInQueue.offer(me); //enable the incoming queue
    						//if controller is active enable REST interface
    						String RESTPlugin = findPlugin("RESTPlugin",0);
    	    				if(controllerPlugin != null)
    	    				{
    	    					System.out.println("Start RESTChannel for Controller on port 32001");
    	    					boolean isREST = enablePlugin(RESTPlugin, false);
    	    					if(!isREST)
    	    					{
    	    						System.out.println("RESTChannel Plugin NOT Loaded");
    	    					}
    	    				}
    						
    					}
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
     	   
        
	    	//Notify log that agent has started
			String msg = "Agent Core (" + agentVersion + ") Started";
			clog.log(msg);
				
    	}
    	else
    	{
    		System.out.println("Agent is a Zombie!\nNo Active Log or Control Channels!\nAgent will now shutdown.");
    		isActive = false;
    	}

    	
    }
    
    public static void getChannel()
    {
    	//Try and load some plugins to establish comm
    	try
    	{
    		String ampqPlugin = findPlugin("AMPQPlugin",0);
    		if(ampqPlugin != null)
    		{
    			//start controller but don't save the config.
    			boolean isAMPQ = enablePlugin(ampqPlugin, false);
    			if(!isAMPQ)
    			{
    				System.out.println("AMPQ is not enabled");
    			}
    			else
    			{
    				//turn on queues and set channel
    				MsgInQueueActive = true;
    				MsgOutQueueActive = true;
    				hasChannel = true;
    				channelPluginSlot = ampqPlugin;
    			}
    		}
    		else
    		{
    			System.out.println("NO AMPQPlugin found.. falling back to REST");
    		}
    		
    		if(!hasChannel)
    		{
    			System.out.println("Starting REST Plugin");
		
    			String restPlugin = findPlugin("RESTPlugin",0);
    			if(restPlugin != null)
    			{
    				//start controller but don't save the config.
    				boolean isREST = enablePlugin(restPlugin, false);
    				//System.out.println("Enabled AMPQ Module [" + ampqSlot + "]");
    				if(!isREST)
    				{
    					System.out.println("REST is not enabled.. No communication channel.. exiting");
        				System.exit(0);
    				}
    				else
    				{
    					MsgInQueueActive = true;
    					MsgOutQueueActive = true;
    					hasChannel = true;
    					channelPluginSlot = ampqPlugin;
    				}
    			}
    			else
    			{
    				System.out.println("NO RESTPlugin found.. No communication channel.. exiting");
    				System.exit(0);
    			}
    		}
    	}
    	catch(Exception ex)
    	{
    		System.out.println("Error creating communication channel.. exiting");
    		System.exit(0);
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
   			clog.error(msg);
   			System.exit(1);	
   		}
   	
   		//pull in plugin configuration
   		pluginsconfig = new ConfigPlugins(plugin_config_file);
   		
   		@SuppressWarnings("unchecked")
			List<String> enabledPlugins = pluginsconfig.getPluginList(1);//return enabled values in the config
   		
   		for(String pluginName : enabledPlugins) //process list of plugins that should be enabled
   		{
   			
   			boolean isLoaded = enablePlugin(pluginName, false);
			if(!isLoaded)
			{
				System.out.println("Failed Loading Required Plugin: " + pluginName + " " + pluginsconfig.getPluginJar(pluginName) + " exiting..");
				System.exit(0); //exit if required plugin fails
			}
			    	
   		}
   		
   	}
   	catch(Exception ex)
   	{
   		String msg = "Failed to Process Plugins: Agent=" + AgentEngine.agent + " ERROR:" + ex.toString();
			clog.error(msg);
   	}   	
   }
   
   public static boolean disablePlugin(String plugin, boolean save) //loop through known plugins on agent
	{
	   try
	   {
	    if(pluginMap.containsKey(plugin))
	    {
	    	//PluginNode pn = pluginMap.get(plugin);
	    	PluginInterface pi = pluginMap.get(plugin);
	    	pi.shutdown();
			String msg = "Plugin Configuration: [" + plugin + "] Removed: (" + pi.getVersion() + ")";
			pi = null;
			System.out.println(msg);
			pluginMap.remove(plugin);
			
			if(save)
			{
				pluginsconfig.setPluginStatus(plugin, 0);//save in config 
			}
			
   			return true;	
	    }
	    else
	    {
	    	//already disabled
	    	System.out.println("Plugin " + plugin  + " is already disabled");
	    	return false;   
	    }
	   }
	   catch(Exception ex)
	   {
		   String msg = "Plugin Failed Disable: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin);
			clog.error(msg);
			return false;
	   }
	   
	}
   
   public static boolean enablePlugin(String plugin, boolean save) //loop through known plugins on agent
	{
	   try
	   {
	    if(!pluginMap.containsKey(plugin))
	    {
	    	PluginLoader pl = new PluginLoader(pluginsconfig.getPluginJar(plugin));
	    	PluginInterface pi = pl.getPluginInterface();
	    	
	    	if(pi.initialize(msgOutQueue,msgInQueue,pluginsconfig.getPluginConfig(plugin),AgentEngine.config.getRegion(),AgentEngine.config.getAgentName(),plugin))
	    	{
	    		if(pluginsconfig.getPluginName(plugin).equals(pi.getName()))
	    		{
	    			String msg = "Plugin Configuration: [" + plugin + "] Initialized: (" + pi.getVersion() + ")";
	    			clog.log(msg);
	    			
	    			pluginMap.put(plugin, pi);
	    			if(save)
					{
						pluginsconfig.setPluginStatus(plugin, 1);
					}
	    			return true;
	    		}
	    		else
	    		{
	    			String msg = "Plugin Configuration: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin) + " does not match Plugin Jar: " + pi.getVersion() + ")";
	    			clog.error(msg);
	    			pl = null;
	    			pi = null;
	    			return false;
	    		}
	    	}
	    	else
	    	{
	    		String msg = "Plugin Failed Initialization: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin) + " does not match Plugin Jar: " + pi.getVersion() + ")";
    			clog.error(msg);
    			return false;
	    	}
	    }
	    else
	    {
	    	System.out.println("Plugin " + plugin  + " is already active");
	    	return false;
	    }
	   }
	   catch(Exception ex)
	   {
		   String msg = "Plugin Failed Initialization: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin);
			clog.error(msg);
			return false;
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
		   return config.getAgentName() + "." + version;
	   }
   
   //This needs to be redone to account for active but not configured
   public static String findPlugin(String searchPluginName, int isActive) //loop through known plugins on agent
	{
	   System.out.println("Searching for plugin: " + searchPluginName);
		StringBuilder sb = new StringBuilder();
		List<String> pluginList = AgentEngine.pluginsconfig.getPluginList(isActive);
		
		if(pluginList.size() > 0)
		{
			for(String pluginName : pluginList)
			{
					if(AgentEngine.pluginsconfig.getPluginName(pluginName).equals(searchPluginName))
					{
						if((isActive == 0) && !AgentEngine.pluginMap.containsKey(pluginName))
						{
							System.out.println("PluginFound: " + searchPluginName + "=" + pluginName);
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
      
		List<String> pluginListEnabled = new ArrayList<String>(AgentEngine.pluginsconfig.getPluginList(1));
		List<String> pluginListDisabled = new ArrayList<String>(AgentEngine.pluginsconfig.getPluginList(0));
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
					PluginInterface pi = pluginMap.get(pluginName);
					sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
				}
			}
			for(String pluginName : pluginListActive)
			{
				PluginInterface pi = pluginMap.get(pluginName);
				
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
   
   static void cleanup() throws ConfigurationException, IOException, InterruptedException
   {
	   System.out.println("Shutdown:Cleaning Active Agent Resources");
	   wd.timer.cancel();
   	
	       List<String> pluginList = getActivePlugins(); 
	   	   /*
	       for(String plugin : pluginList)
	   	   {
	   		   if(((isController) && (plugin.equals(channelPluginSlot))))
	   		   {
	   			   
	   		   }
	   		   else
	   		   {
	   			   //disablePlugin(plugin,false);
	   		   }
	   	   }
	   	   */
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
	       
	    	
		    if(!isController)
		   	{
		    	
		    	for(String plugin : pluginList)
			   	{
		    	   if(!plugin.equals(channelPluginSlot))
			   	   {
			   		   disablePlugin(plugin,false);   
			   	   }
			   	}
		    	
		   		if(msgInQueue != null)
			    {
		    		MsgEvent de = clog.getLog("disabled");
		    		de.setMsgType(MsgEventType.CONFIG);
		    		de.setMsgAgent(AgentEngine.agent); //route to this agent
		    		de.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin
		    		de.setParam("src_region",region);
					de.setParam("src_agent",agent);
					de.setParam("dst_region",region);
					
		    		AgentEngine.commandExec.cmdExec(de);
		    		Thread.sleep(5000);
		    		disablePlugin(channelPluginSlot,false);
		    		/*
		    		int time = 0;
				    int timeout = 30; //give 30sec to timeout from RPC request
				    
				    
			    	if(MsgInQueueThread != null)
			    	{
			    		//disablePlugin(controllerPluginSlot,false);
			    		//disablePlugin(controllerPluginSlot,false);
			   		 	disablePlugin(channelPluginSlot,false);
			    		while((MsgInQueueThread.isAlive()) && (time < timeout))
			    		{
				    		try 
				    		{
								Thread.sleep(1000);
							} 
				    		catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				    		time++;
				    	}
			    	}
			    	*/
			    	
		    	}
			}
		    else
		    {
		    	//cleanup controller here
		    	for(String plugin : pluginList)
			   	{
		    		
			   	     disablePlugin(plugin,false);   
			   	     
			   	}
		    	
		    }
	    	
   }
}


