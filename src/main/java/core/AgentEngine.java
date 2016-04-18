package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import channels.MsgRoute;
import channels.RPCCall;
import org.apache.commons.configuration.ConfigurationException;

import channels.MsgInQueue;
import plugins.ConfigPlugins;
import plugins.Plugin;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.utilities.CLogger;

public class AgentEngine {
    public static boolean isActive = false; //agent on/off
    public static WatchDog wd;
    public static boolean hasChannel = false;
    public static String channelPluginSlot;
    public static boolean isRegionalController = false;
    public static String controllerPluginSlot;

    public static boolean isCommInit = false;

    public static ExecutorService msgInProcessQueue;

    public static ConcurrentLinkedQueue<MsgEvent> msgInQueue;
    public static Thread MsgInQueueThread;
    public static boolean MsgInQueueEnabled = false; //control service on/off
    public static boolean MsgInQueueActive = false; //control service on/off

    public static boolean ControllerEnabled = false; //control service on/off
    public static boolean ControllerActive = false; //control service on/off

    public static CommandExec commandExec;
    public static Map<String, MsgEvent> rpcMap;


    public static boolean watchDogActive = false; //agent watchdog on/off
    public static String agentVersion = null;

    public static Map<String, Plugin> pluginMap;

    public static Config config;
    public static ConfigPlugins pluginsconfig;

    public static String region;
    public static String agent;

    public static CLogger clog;

    public static DelayedShutdown ds; //delayed shutdown command

    public static void main(String[] args) throws Exception {
        //Make sure initial input is sane.
        String configFile = checkConfig(args);

        try {
            //create command group
            commandExec = new CommandExec();

            msgInProcessQueue = Executors.newFixedThreadPool(4);
            //create logger and base queue
            msgInQueue = new ConcurrentLinkedQueue<>();
            //msgOutQueue = new ConcurrentLinkedQueue<>();
            rpcMap = new ConcurrentHashMap<>();

            //Cleanup on Shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        cleanup();
                    } catch (Exception ex) {
                        System.out.println("Exception Shutting Down:" + ex.toString());
                    }
                }
            }, "Shutdown-thread"));


            //Make sure config file
            config = new Config(configFile);

        	/*
            System.out.println("Building MsgOutQueue");
    		//start outgoing queue
    		
    		MsgOutQueue moq = new MsgOutQueue();
    		MsgOutQueueThread = new Thread(moq);
    		MsgOutQueueThread.start();
        	while(!MsgOutQueueEnabled)
        	{
        		Thread.sleep(100);
        	}
        	*/

            System.out.println("Building MsgInQueue");
            //start incoming queue
            MsgInQueue miq = new MsgInQueue();
            MsgInQueueThread = new Thread(miq);
            MsgInQueueThread.start();
            while (!MsgInQueueEnabled) {
                Thread.sleep(100);
            }

            region = "init"; //set temp setting to allow routing
            agent = "init"; //region and agent will come from controller

            //Establish  a named map of plugin interfaces
            pluginMap = new ConcurrentHashMap<>();

            //build initial plugin list
            processPlugins();
            //and launch static plugins
            //enableStaticPlugins()

            //delay and waiting for network init.
            int startupdelay = Integer.parseInt(config.getStringParams("general", "startupdelay"));
            Thread.sleep(startupdelay);

            LoadControllerPlugin();

            isActive = true;
            enableStaticPlugins();

            Scanner scanner = new Scanner(System.in);

            System.out.println("Name of Agent to message [q to quit]: ");
            String input = scanner.nextLine();


            while (!input.toLowerCase().equals("q")) {


                if (input.length() > 0) {
                    try {
                        String[] sstr = input.split(",");
                        //System.out.println("region: " + sstr[0] + " agent=" + sstr[1] + " plugin=" + sstr[2]);
                        //System.out.println("controllerPluginSlot=" + controllerPluginSlot);
                        MsgEvent me = new MsgEvent(MsgEvent.Type.EXEC, region, agent, controllerPluginSlot, "external");
                        me.setParam("cmd", "show_name");
                        me.setParam("src_region", region);
                        me.setParam("src_agent", agent);
                        me.setParam("dst_region", sstr[0]);
                        me.setParam("dst_agent", sstr[1]);
                        if (sstr.length == 3) {
                            me.setParam("dst_plugin", sstr[2]);
                        }
                        MsgEvent re = new RPCCall().call(me);
                        if (re != null) {
                            System.out.println("MESSAGE RETURNED : " + re.getParams());
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
                System.out.println("Name of Agent to message [q to quit]: ");
                input = scanner.nextLine();
            }

			/*
			while(isActive)
			{
				//just sleep until isActive=false
				//need to add ability to control other threads here.
				//need to add upgrade ability
				Thread.sleep(1000);
			}
    		*/
            //Die here
            System.out.println("SYSTEM EXIT");
            System.exit(0);


            //set version name
            agentVersion = getVersion();
            //set region and agent
            agent = config.getAgentName();
            region = config.getRegion();

            clog = new CLogger(msgInQueue, region, agent, null, CLogger.Level.Info);

            //if channel was not configured during startup try and establish
            if (!hasChannel) {
                getChannel(); //currently AMPQ and REST plugins
            }

            //if controller was not configured during startup try and establish
            if (!ControllerActive) {
                getController();
            }

            //start core watchdog
            wd = new WatchDog();
	    	/*
        	while(isActive) 
    	   {
        	   //just sleep until isActive=false
        		//need to add ability to control other threads here.
        		//need to add upgrade ability
        		Thread.sleep(1000);
    	   }
        	*/
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error AgentCore: " + e.getMessage());
        } finally {
            System.out.println("Agent : Main :Finally Statement");
        }
    }

    public static void LoadControllerPlugin() throws InterruptedException {
        controllerPluginSlot = "plugin/0";
        boolean isComm = enablePlugin(controllerPluginSlot, false);
        if (!isComm) {
            System.out.println("failed to load");
            System.exit(0);
        } else {
            MsgInQueueActive = true; //allow incoming message

            Plugin plugin = AgentEngine.pluginMap.get(controllerPluginSlot);
            MsgEvent me = new MsgEvent(MsgEvent.Type.CONFIG, region, agent, controllerPluginSlot, "comminit");
            me.setParam("configtype", "comminit");
            me.setParam("src_region", region);
            me.setParam("src_agent", agent);
            me.setParam("dst_region", region);
            me.setParam("dst_agent", agent);
            me.setParam("dst_plugin", "plugin/0");
            plugin.Message(me); //send msg to plugin

            while (!isCommInit) {
                Thread.sleep(1000);
            }
        }

    }

    public static void msgIn(MsgEvent me) {
        msgInProcessQueue.execute(new MsgRoute(me));
        //new MsgRoute(me).run();
		/*
		final MsgEvent ce = me;
		try
		{
		Thread thread = new Thread(){
		    public void run(){ //command request go in new threads
		    	MsgEvent re = null;
		    	try 
		        {
		        	if(ce == null)
		        	{
		        		System.out.println("Agent : msgIn : Incoming message NULL!!!!");
		        	}
		        	
					re = commandExec.cmdExec(ce); //execute command
					if(re != null)
					{
						System.out.println("Agent : msgIn : pre-reverse: " + re.getParams());
						re.setReturn(); //reverse to-from for return
						System.out.println("Agent : msgIn : reverse: " + re.getParams());
						msgInQueue.offer(re);
					}
					
				} 
		        catch(Exception ex)
		        {
		        	clog.error("Agent : AgentEngine : msgIn Thread: " + ex.toString());
		        	clog.error("Agent : AgentEngine : msgIn ce EventMsg =" + ce.getParamsString());
		        	
		        	if(re != null)
		        	{
		        		clog.error("Agent : AgentEngine : msgIn re EventMsg =" + re.getParamsString());
		        	}
		        	
		        }
		    }
		  };
		  thread.start(); //start the exec thread
		}
		catch(Exception ex)
		{
			
			clog.error("Agent : AgentEngine : msgIn : " + ex.toString());
		}
		*/
    }

    public static void getController() throws InterruptedException, ConfigurationException, IOException {

        if (MsgInQueueActive/* && MsgOutQueueActive*/) {
            int tryController = 1;
            int controllerDiscoveryTimeout = config.getControllerDiscoveryTimeout();
            //add random delay on startup
            Random r = new Random();
            int Low = 0;
            int High = 10000;
            int R = r.nextInt(High - Low) + Low;
            //
            controllerDiscoveryTimeout = (controllerDiscoveryTimeout + R) / 1000;//add random delay to startup so only one controller starts
            int controllerLaunchTimeout = Math.round(controllerDiscoveryTimeout / 2);
            //give the controller 20 sec to respond. First 10 for possibe existing,
            //last 10 for one we try and start
            System.out.println("Waiting for Controller Response Initialization");
            while ((!ControllerActive) && (tryController < controllerDiscoveryTimeout)) {
                System.out.print(".");

                if (AgentEngine.channelPluginSlot != null) //controller plugin was found
                {
                    //MsgEvent de = clog.getLog("enabled");
                    MsgEvent de = new MsgEvent(MsgEvent.Type.INFO, region, null, null, "enabled");
                    de.setParam("src_region", region);
                    de.setParam("src_agent", agent);
                    de.setParam("dst_region", region);
                    de.setMsgType(MsgEvent.Type.CONFIG);
                    de.setMsgAgent(AgentEngine.agent); //route to this agent
                    de.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin
                    de.setParam("attempt", String.valueOf(tryController));
                    AgentEngine.commandExec.cmdExec(de);
                }

                Thread.sleep(1000);
                tryController++; //give X trys for controller to respond

                //Notify controler of agent enable wait for controller contact

                if ((tryController > controllerLaunchTimeout) && !isRegionalController) {
                    String controllerPlugin = findPlugin("ControllerPlugin", 0);
                    if (controllerPlugin != null) {
                        System.out.println("Try and Start our own controller");
                        //start controller but don't save the config.
                        isRegionalController = enablePlugin(controllerPlugin, false);
                        if (!isRegionalController) {
                            System.out.println("Controller Plugin NOT Loaded");
                        } else {
                            System.out.println("Controller Plugin Loaded");
                            controllerPluginSlot = controllerPlugin;
                            isActive = true;
                            //else if(ce.getParam("cmd").equals("enablelogconsumer"))
                            //enable incoming logs
                            MsgEvent me = new MsgEvent(MsgEvent.Type.EXEC, region, agent, channelPluginSlot, "Enable Incoming Log");
                            me.setParam("cmd", "enablelogconsumer");
                            me.setSrc(region, agent, null);
                            me.setDst(region, agent, channelPluginSlot);
                            AgentEngine.msgInQueue.offer(me); //enable the incoming queue
                            //if controller is active enable REST interface
    						
    						/*
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
    	    				*/

                        }
                    }

                }
            }

            if (ControllerActive) {
                System.out.println("Region:" + config.getRegion() + " Controller found:");
            } else {
                System.out.println("Region:" + config.getRegion() + " *NOT* Controller found:");
            }
            //wait until shutdown occures
            isActive = true;


            //Notify log that agent has started
            String msg = "Agent Core (" + agentVersion + ") Started";
            clog.log(msg);

        } else {
            System.out.println("Agent is a Zombie!\nNo Active Log or Control Channels!\nAgent will now shutdown.");
            isActive = false;
        }


    }

    public static void getChannel() {
        //Try and load some plugins to establish comm
        //while here not explicit names
        //commpluginlist="cresco-agent-ampqchannel-plugin"
        String commpluginlist = AgentEngine.config.getStringParams("communication", "commpluginlist");
        String[] commList = commpluginlist.split(",");
        int i = 0;
        while ((i < commList.length) && !hasChannel) {
            try {
                String commPlugin = findPlugin(commList[i], 0);
                if (commPlugin != null) {
                    //start controller but don't save the config.
                    boolean isComm = enablePlugin(commPlugin, false);
                    if (!isComm) {
                        System.out.println(commList[i] + " is not enabled");
                    } else {
                        //turn on queues and set channel
                        MsgInQueueActive = true;
                        //MsgOutQueueActive = true;
                        hasChannel = true;
                        channelPluginSlot = commPlugin;
                    }
                } else {
                    System.out.println("NO configuration found for communication plugin " + commList[i]);
                }

            } catch (Exception ex) {
                System.out.println("Error creating communication channel using plugin " + commList[i]);
            }
            i++;
        }
        if (!hasChannel) {
            System.out.println("Unable to create communication channel.. exiting");
            System.exit(0);
        }
    }

    /*public static void getChannel2() {
        //Try and load some plugins to establish comm
        //while here not explicit names

        try {
            String ampqPlugin = findPlugin("cresco-agent-ampqchannel-plugin", 0);
            if (ampqPlugin != null) {
                //start controller but don't save the config.
                boolean isAMPQ = enablePlugin(ampqPlugin, false);
                if (!isAMPQ) {
                    System.out.println("AMPQ is not enabled");
                } else {
                    //turn on queues and set channel
                    MsgInQueueActive = true;
                    //MsgOutQueueActive = true;
                    hasChannel = true;
                    channelPluginSlot = ampqPlugin;
                }
            } else {
                System.out.println("NO AMPQPlugin found.. falling back to REST");
            }

            if (!hasChannel) {
                System.out.println("Starting REST Plugin");

                String restPlugin = findPlugin("RESTPlugin", 0);
                if (restPlugin != null) {
                    //start controller but don't save the config.
                    boolean isREST = enablePlugin(restPlugin, false);
                    //System.out.println("Enabled AMPQ Module [" + ampqSlot + "]");
                    if (!isREST) {
                        System.out.println("REST is not enabled.. No communication channel.. exiting");
                        System.exit(0);
                    } else {
                        MsgInQueueActive = true;
                        //MsgOutQueueActive = true;
                        hasChannel = true;
                        channelPluginSlot = ampqPlugin;
                    }
                } else {
                    System.out.println("NO RESTPlugin found.. No communication channel.. exiting");
                    System.exit(0);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error creating communication channel.. exiting");
            System.exit(0);
        }
    }*/

    public static String checkConfig(String[] args) {
        String errorMgs = "Cresco-Agent\n" +
                "Usage: java -jar Cresco-Agent.jar" +
                " -f <configuration_file>\n";

        if (args.length != 2) {
            System.err.println(errorMgs);
            System.err.println("ERROR: Invalid number of arguements.");
            System.exit(1);
        } else if (!args[0].equals("-f")) {
            System.err.println(errorMgs);
            System.err.println("ERROR: Must specify configuration file.");
            System.exit(1);
        } else {
            File f = new File(args[1]);
            if (!f.exists()) {
                System.err.println("The specified configuration file: " + args[1] + " is invalid");
                System.exit(1);
            }
        }
        return args[1];
    }

    public static void processPlugins() throws ClassNotFoundException, IOException {
        try {
            //System.out.println("Start process plugins");
            String plugin_config_file = config.getPluginConfigFile();
            System.out.println("Start process plugins " + plugin_config_file);

            File f = new File(plugin_config_file);
            if (!f.exists()) {
                String msg = "The specified configuration file: " + plugin_config_file + " is invalid";
                clog.error(msg);
                System.exit(1);
            }
            System.out.println("PluginFile=" + plugin_config_file);
            //pull in plugin configuration
            pluginsconfig = new ConfigPlugins(plugin_config_file);

        } catch (Exception ex) {
            String msg = "Failed to Process Plugins: Agent=" + AgentEngine.agent + " ERROR:" + ex.toString();
            clog.error(msg);
        }
    }

    public static void enableStaticPlugins() {
        try {
            if (pluginsconfig != null) {
                @SuppressWarnings("unchecked")
                List<String> enabledPlugins = pluginsconfig.getPluginList(1);//return enabled values in the config

                for (String pluginName : enabledPlugins) //process list of plugins that should be enabled
                {
                    if (pluginName.equals(controllerPluginSlot)) continue;
                    boolean isLoaded = enablePlugin(pluginName, false);
                    if (!isLoaded) {
                        System.out.println("Failed Loading Required Plugin: " + pluginName + " " + pluginsconfig.getPluginJar(pluginName) + " exiting..");
                        System.exit(0); //exit if required plugin fails
                    }

                }
            } else {
                String msg = "No static plugins to load!";
                clog.error(msg);
            }
        } catch (Exception ex) {
            String msg = "Failed to Process Plugins: Agent=" + AgentEngine.agent + " ERROR:" + ex.toString();
            clog.error(msg);
        }
    }

    public static boolean disablePlugin(String plugin, boolean save) //loop through known plugins on agent
    {
        try {
            if (pluginMap.containsKey(plugin)) {
                //PluginNode pn = pluginMap.get(plugin);
                Plugin pi = pluginMap.get(plugin);
                pi.Stop();
                String msg = "Plugin Configuration: [" + plugin + "] Removed: (" + pi.getVersion() + ")";
                //pi = null;
                System.out.println(msg);
                pluginMap.remove(plugin);

                if (save) {
                    pluginsconfig.setPluginStatus(plugin, 0);//save in config
                }

                return true;
            } else {
                //already disabled
                System.out.println("Plugin " + plugin + " is already disabled");
                return false;
            }
        } catch (Exception ex) {
            String msg = "Plugin Failed Disable: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin);
            clog.error(msg);
            return false;
        }

    }

    public static boolean enablePlugin(String pluginID, boolean save) {
        try {
            if (!pluginMap.containsKey(pluginID)) {
                Plugin plugin;
                try {
                    plugin = new Plugin(pluginsconfig.getPluginJar(pluginID));
                } catch (Exception e) {
                    System.out.println("Plugin Generation: " + e.getMessage());
                    return false;
                }
                if (!pluginsconfig.getPluginName(pluginID).equals(plugin.getName())) {
                    String msg = "Plugin Configuration: Agent=" + AgentEngine.agent + " pluginname=" + pluginsconfig.getPluginName(pluginID) + " does not match reported plugin Jar name: " + plugin.getName();
                    //pluginMap.put(pluginID, plugin);
                    //clog.error(msg);
                    System.out.println(msg);
                    plugin.Dispose();
                    plugin = null;
                    return false;
                }
                if (plugin.Start(msgInQueue, pluginsconfig.getPluginConfig(pluginID), AgentEngine.region, AgentEngine.agent, pluginID)) {
                    String msg = "Plugin Configuration: [" + pluginID + "] Initialized: (" + plugin.getName() + " Version: " + plugin.getVersion() + ")";
                    //clog.log(msg);
                    System.out.println(msg);
                    pluginMap.put(pluginID, plugin);
                    if (save)
                        pluginsconfig.setPluginStatus(pluginID, 1);
                    return true;
                } else {
                    String msg = "Plugin Failed Initialization: Agent=" + AgentEngine.agent + " pluginname=" + pluginsconfig.getPluginName(pluginID) + " does not match Plugin Jar: " + plugin.getVersion() + ")";
                    //clog.error(msg);
                    System.out.println(msg);
                    plugin.Dispose();
                    plugin = null;
                    return false;
                }
            } else {
                String msg = "Plugin Failed Initialization: Plugin [" + pluginID + "] is already active";
                System.out.println(msg);
                //clog.error(msg);
                return false;
            }
        } catch (Exception ex) {
            //String msg = "Plugin Failed Initialization: Agent=" + AgentEngine.agent + "pluginname=" + pluginsconfig.getPluginName(plugin) + " Error: " + ex.toString();
            String msg = "Plugin Failed Initialization: [" + pluginID + "] Error: " + ex.getMessage();
            ex.printStackTrace();
            //clog.error(msg);
            System.out.println(msg);
            return false;
        }
    }

    public static String getVersion() {
        String version;
        try {
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
        } catch (Exception ex) {
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

        if (pluginList.size() > 0) {
            for (String pluginName : pluginList) {
                if (AgentEngine.pluginsconfig.getPluginName(pluginName).equals(searchPluginName)) {
                    if ((isActive == 0) && !AgentEngine.pluginMap.containsKey(pluginName)) {
                        System.out.println("PluginFound: " + searchPluginName + "=" + pluginName);
                        return pluginName;
                    }
                }
            }
            return null;
        } else {
            return null;
        }

    }

    public static String listPlugins() {
        StringBuilder sb = new StringBuilder();

        List<String> pluginListEnabled = new ArrayList<>(AgentEngine.pluginsconfig.getPluginList(1));
        List<String> pluginListDisabled = new ArrayList<>(AgentEngine.pluginsconfig.getPluginList(0));
        List<String> pluginListActive = getActivePlugins();

        if ((pluginListEnabled.size() > 0) || (pluginListDisabled.size() > 0)) {
            if ((pluginListEnabled.size() > 0) || (pluginListActive.size() > 0)) {
                sb.append("Enabled Plugins:\n");
            }
            for (String pluginName : pluginListEnabled) {
                if ((AgentEngine.pluginMap.containsKey(pluginName)) && !pluginListActive.contains(pluginName)) {
                    Plugin pi = pluginMap.get(pluginName);
                    sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
                }
            }
            for (String pluginName : pluginListActive) {
                Plugin pi = pluginMap.get(pluginName);

                sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
            }
            if (pluginListDisabled.size() > 0) {
                sb.append("Disabled Plugins:\n");
            }
            for (String pluginName : pluginListDisabled) {
                if (!pluginListActive.contains(pluginName)) {
                    sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + "\n");
                }
            }
        } else {
            sb.append("No Plugins Found!\n");

        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    static List<String> getActivePlugins() {
        List<String> pluginList = new ArrayList<>();
        if (pluginMap != null) {
            Iterator it = pluginMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                System.out.println(pairs.getKey() + " = " + pairs.getValue());
                String plugin = pairs.getKey().toString();
                //disablePlugin(plugin,false);
                pluginList.add(plugin);
                //it.remove(); // avoids a ConcurrentModificationException
            }
        }
        return pluginList;
    }

    static void cleanup() throws ConfigurationException, IOException, InterruptedException {
        try {
            System.out.println("Shutdown:Cleaning Active Agent Resources");
            //wd.timer.cancel();

            MsgEvent de = new MsgEvent(MsgEvent.Type.CONFIG, AgentEngine.config.getRegion(), null, null, "disabled");
            de.setParam("src_region", region);
            de.setParam("src_agent", agent);
            de.setParam("dst_region", region);
            //AgentEngine.commandExec.cmdExec(de);
            AgentEngine.msgInQueue.offer(de);

            List<String> pluginList = getActivePlugins();
	   	    /*
	        for(String plugin : pluginList) {
	   		   if(((isController) && (plugin.equals(channelPluginSlot)))) {
	   			   
	   		    } else {
	   			   //disablePlugin(plugin,false);
	   		    }
	   	    }
	   	    */
	        /*
	   	    if(pluginMap != null) {
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
            if (!isRegionalController) {
                for (String plugin : pluginList) {
                    if (!plugin.equals(channelPluginSlot)) {
                        disablePlugin(plugin, false);
                    }
                }
                if (msgInQueue != null) {

                    //disablePlugin(channelPluginSlot, false);

                /*
                MsgEvent de = clog.getLog("disabled");
                de.setMsgType(MsgEvent.Type.CONFIG);
                de.setMsgAgent(AgentEngine.agent); //route to this agent
                de.setMsgPlugin(AgentEngine.channelPluginSlot); //route to controller plugin
                de.setParam("src_region", region);
                de.setParam("src_agent", agent);
                de.setParam("dst_region", region);

                //AgentEngine.commandExec.cmdExec(de);
                AgentEngine.msgInQueue.offer(de);
                Thread.sleep(5000);
                disablePlugin(channelPluginSlot, false);
                */
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
            } else {
                //cleanup controller here
                for (String plugin : pluginList) {
                    disablePlugin(plugin, false);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Shutdown Error: " + ex.getMessage());
        }
    }
}


