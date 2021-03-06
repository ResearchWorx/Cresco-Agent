package core;

import channels.MsgInQueue;
import channels.MsgRoute;
import channels.RPCCall;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.utilities.CLogger;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugins.ConfigPlugins;
import plugins.Plugin;
import plugins.PluginManager;

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

public class AgentEngine {
    private static Logger coreLogger;
    private static Logger pluginsLogger;
    public static boolean isActive = false;
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

    public static PluginManager pluginManager;

    public static void main(String[] args) throws Exception {
        //Make sure initial input is sane.
        String configFile = checkConfig(args);

        try {
            //Make sure config file
            config = new Config(configFile);

            System.setProperty("cresco.logs.path", config.getLogPath());

            coreLogger = LoggerFactory.getLogger("Engine");
            pluginsLogger = LoggerFactory.getLogger("Plugins");

            coreLogger.info("");
            coreLogger.info("       ________   _______      ________   ________   ________   ________");
            coreLogger.info("      /  _____/  /  ___  |    /  _____/  /  _____/  /  _____/  /  ___   /");
            coreLogger.info("     /  /       /  /__/  /   /  /__     /  /___    /  /       /  /  /  /");
            coreLogger.info("    /  /       /  __   /    /  ___/    /____   /  /  /       /  /  /  /");
            coreLogger.info("   /  /____   /  /  |  |   /  /____   _____/  /  /  /____   /  /__/  /");
            coreLogger.info("  /_______/  /__/   |__|  /_______/  /_______/  /_______/  /________/");
            coreLogger.info("");
            coreLogger.info("      Configuration File: {}", configFile);
            coreLogger.info("      Plugin Configuration File: {}", config.getPluginConfigFile());
            coreLogger.info("");
            //create command group
            commandExec = new CommandExec();

            msgInProcessQueue = Executors.newFixedThreadPool(4);
            //create logger and base queue
            msgInQueue = new ConcurrentLinkedQueue<>();
            rpcMap = new ConcurrentHashMap<>();

            //Cleanup on Shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        cleanup();
                    } catch (Exception ex) {
                        coreLogger.error("Exception Shutting Down: {}", ex.getMessage());
                    }
                }
            }, "Shutdown-thread"));



            coreLogger.debug("Building MsgInQueue");
            MsgInQueue miq = new MsgInQueue();
            MsgInQueueThread = new Thread(miq);
            MsgInQueueThread.start();
            while (!MsgInQueueEnabled) {
                Thread.sleep(100);
            }

            if((config.getStringParams("general","agentname") != null) && (config.getStringParams("general","regionname") != null)) {
                region = config.getStringParams("general","regionname");
                agent = config.getStringParams("general","agentname");
            }
            else {
                region = "init"; //set temp setting to allow routing
                agent = "init"; //region and agent will come from controller
            }
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
            boolean noConsole = false;
            try {
                String input = scanner.nextLine();

                System.out.println("Name of Agent to message [q to quit]: ");


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
            } catch (java.util.NoSuchElementException nse) {
                //Ignore if no stdin
                coreLogger.info("Agent Startup Complete.");
                noConsole = true;
            }

			if(noConsole) {
                while (isActive) {
                    //just sleep until isActive=false
                    //need to add ability to control other threads here.
                    //need to add upgrade ability
                    Thread.sleep(1000);
                }
            }
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
            //wd = new WatchDog();
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
                Thread.sleep(10);
            }
            if (isRegionalController)
                coreLogger.info("Region: [" + AgentEngine.config.getRegion() + "] * Regional Controller *");
            else
                coreLogger.info("Region: [" + AgentEngine.config.getRegion() + "]");
            coreLogger.info(" Agent: [" + AgentEngine.config.getAgentName() + "]");
        }

    }

    public static void msgIn(MsgEvent me) {
        msgInProcessQueue.execute(new MsgRoute(me));
    }

    public static void getController() throws InterruptedException, ConfigurationException, IOException {

        if (MsgInQueueActive) {
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
            coreLogger.info(msg);

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
            pluginsLogger.info("Processing configuration file");

            File f = new File(plugin_config_file);
            if (!f.exists()) {
                String msg = "The specified configuration file: " + plugin_config_file + " is invalid";
                pluginsLogger.error(msg);
                System.exit(1);
            }
            pluginsconfig = new ConfigPlugins(plugin_config_file);

        } catch (Exception ex) {
            String msg = "Failed to Process Plugins: Agent=" + AgentEngine.agent + " ERROR:" + ex.toString();
            pluginsLogger.error(msg);
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
                pluginsLogger.error(msg);
            }
        } catch (Exception ex) {
            String msg = "Failed to Process Plugins: Agent=" + AgentEngine.agent + " ERROR:" + ex.toString();
            pluginsLogger.error(msg);
        }
    }

    public static boolean disablePlugin(String pluginID, boolean save) {
        try {
            if (pluginMap.containsKey(pluginID)) {
                Plugin plugin = pluginMap.get(pluginID);
                //if (plugin.Stop()) {
                    plugin.Stop();
                    pluginsLogger.info("[{}] disabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                    pluginMap.remove(pluginID);
                    if (save)
                        pluginsconfig.setPluginStatus(pluginID, 0);
                    return true;
                /*} else {
                    pluginsLogger.error("[{}] failed to shutdown. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                    return false;
                }*/
            } else {
                pluginsLogger.error("[{}] is not currently enabled.", pluginID);
                return false;
            }
        } catch (Exception e) {
            pluginsLogger.error("[{}] failed to stop. [Exception: {}]", pluginID, e.getMessage());
            return false;
        }
    }

    public static boolean enablePlugin(String pluginID, boolean save) {
        try {
            if (pluginMap.containsKey(pluginID)) {
                Plugin plugin = pluginMap.get(pluginID);
                pluginsLogger.error("Plugin is already loaded. [Name: {}, Version: {}]", plugin.getName(), plugin.getVersion());
                return false;
            }
            try {
                Plugin plugin = new Plugin(pluginID, pluginsconfig.getPluginJar(pluginID));
                if (!pluginsconfig.getPluginName(pluginID).equals(plugin.getName())) {
                    pluginsLogger.error("Configuration error - plugin name [{}] does not match configuration {}]", plugin.getName(), pluginsconfig.getPluginName(pluginID));
                    return false;
                }
                try {
                    plugin.PreStart();
                } catch (Exception e) {
                    pluginsLogger.error("[{}] - PreStart error - [Exception: {}]", pluginID, e.getMessage());
                }
                if (!plugin.Start(msgInQueue, pluginsconfig.getPluginConfig(pluginID), region, agent, pluginID)) {
                    return false;
                }
                pluginMap.put(pluginID, plugin);
                try {
                    plugin.PostStart();
                } catch (Exception e) {
                    pluginsLogger.error("[{}] - PostStart error - [Exception: {}]", pluginID, e.getMessage());
                }
                if (save)
                    pluginsconfig.setPluginStatus(pluginID, 1);
                pluginsLogger.info("[{}] enabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                return true;
            } catch (IOException e) {
                pluginsLogger.error("Loading failed - Could not read plugin jar file. [Jar: {}]", pluginsconfig.getPluginJar(pluginID));
            } catch (ClassNotFoundException e) {
                pluginsLogger.error("Loading failed - Plugin class [{}] not found.", pluginsconfig.getCPluginClass(pluginID));
            } catch (InstantiationException e) {
                pluginsLogger.error("Loading failed - Failed to instantiate plugin class [{}].", pluginsconfig.getCPluginClass(pluginID));
            } catch (IllegalAccessException e) {
                pluginsLogger.error("Loading failed - Could not access plugin class [{}].", pluginsconfig.getCPluginClass(pluginID));
            }
            return false;
        } catch (Exception e) {
            pluginsLogger.error("Loading failed - Exception raised on [{}]: [{}]", pluginID, e.getMessage());
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
                //System.out.println(pairs.getKey() + " = " + pairs.getValue());
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
            coreLogger.info("Shutdown initiated");
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

            if (!isRegionalController) {
                for (String plugin : pluginList) {
                    if (!plugin.equals(channelPluginSlot)) {
                        disablePlugin(plugin, false);
                        Thread.sleep(2000);
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


                }
            } else {*/
                //cleanup controller here
                for (String plugin : pluginList) {
                    if (disablePlugin(plugin, false)) {
                        pluginsLogger.info("{} is shutdown", plugin);
                    }
                }
            //}
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Shutdown Error: " + ex.getMessage());
        }
    }
}


