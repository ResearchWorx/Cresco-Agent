package core;

import channels.MsgInQueue;
import channels.MsgRoute;
import channels.RPCCall;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.utilities.CLogger;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugins.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPOutputStream;

public class AgentEngine {
    private static Logger coreLogger;
    private static Logger pluginsLogger;
    public static boolean isActive = false;
    public static WatchDog wd;
    public static boolean hasChannel = false;
    public static String channelPluginSlot;
    public static boolean isRegionalController = false;
    public static boolean isGlobalController = false;
    public static PluginExport pluginexport;

    public static boolean regionUpdate = false;

    public static String controllerPluginSlot;

    public static boolean isCommInit = false;

    public static ExecutorService msgInProcessQueue;

    public static BlockingQueue<MsgEvent> msgInQueue;

    public static Thread MsgInQueueThread;
    public static boolean MsgInQueueEnabled = false; //control service on/off
    public static boolean MsgInQueueActive = false; //control service on/off

    public static boolean ControllerEnabled = false; //control service on/off
    public static boolean ControllerActive = false; //control service on/off

    public static CommandExec commandExec;
    public static ConcurrentMap<String, MsgEvent> rpcMap;

    public static ConcurrentMap<String, Object> rpcReceipts;

    public static PluginHealthWatcher phw;

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

            //msgInProcessQueue = Executors.newFixedThreadPool(4);
            msgInProcessQueue = Executors.newCachedThreadPool();
            //msgInProcessQueue = Executors.newSingleThreadExecutor();

            //create logger and base queue
            msgInQueue = new LinkedBlockingQueue<>();

            //rpcMap = new java.util.WeakHashMap<>();
            rpcMap = new ConcurrentHashMap<>();
            rpcReceipts = new ConcurrentHashMap<>();

            //rpcMap = Collections.synchronizedMap(new HashMap());
            //todo there is still a problem with entries not being removed under load #memoryleak
            /*
            rpcMap = new MapMaker()
                    //.concurrencyLevel(1)
                    .weakValues()
                    .weakKeys()
                    .makeMap();
            */
            //Cleanup on Shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        cleanup();
                    } catch (Exception ex) {
                        coreLogger.error("Exception Shutting Down: {}", ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }, "Shutdown-thread"));



            coreLogger.debug("Building MsgInQueue");
            MsgInQueue miq = new MsgInQueue();
            MsgInQueueThread = new Thread(miq);
            MsgInQueueThread.start();
            while (!MsgInQueueEnabled) {
                Thread.sleep(1000);
            }

            //region and agent names might be changed by the controller
            //region = "region-" + java.util.UUID.randomUUID().toString();
            //agent = "agent-" + java.util.UUID.randomUUID().toString();
            region = "init";
            agent = "init";


            //Establish  a named map of plugin interfaces
            pluginMap = new ConcurrentHashMap<>();

            //build initial plugin list
            processPlugins();

            //class to export plugin configs
            pluginexport = new PluginExport();

            //delay and waiting for network init.
            if(config.getStringParams("general", "startupdelay") != null) {
                int startupdelay = Integer.parseInt(config.getStringParams("general", "startupdelay"));
                Thread.sleep(startupdelay);
            }

            LoadControllerPlugin();

            isActive = true;


            enableStaticPlugins();

            wd = new WatchDog();

            while(!watchDogActive)
            {
                Thread.sleep(1000);
            }

            phw = new PluginHealthWatcher();

            Scanner scanner = new Scanner(System.in);
            boolean noConsole = false;
            try {
                String input = scanner.nextLine();

                System.out.println("Name of Agent to message [q to quit]: ");


                while (!input.toLowerCase().equals("q")) {


                    if (input.length() > 0) {
                        try {

                            //do a pong!

                            long starttime = System.currentTimeMillis();
                            int count = 0;

                            int samples = Integer.parseInt(input);

                            //RPCCall rpc = new RPCCall();

                            while(count < samples) {
                                MsgEvent me = new MsgEvent(MsgEvent.Type.EXEC, region, agent, controllerPluginSlot, "external");
                                me.setParam("action","ping");
                                //me.setParam("action","noop");
                                me.setParam("src_region", region);
                                me.setParam("src_agent", agent);
                                me.setParam("dst_region", region);
                                me.setParam("dst_agent", agent);
                                me.setParam("dst_plugin", controllerPluginSlot);
                                //me.setParam("reversecount","10");
                                me.setParam("count",String.valueOf(count));
                                //msgIn(me);

                                //System.out.print(" " + count + " ");

                                MsgEvent re = new RPCCall().call(me);

                                //System.out.println(re.getParams());

                                //AgentEngine.msgInQueue.add(me);
                                //AgentEngine.pluginMap.get(AgentEngine.controllerPluginSlot) .Message(me);
                                count++;
                            }

                            System.out.println("RPC SIZE: " + AgentEngine.rpcMap.size());

                            //System.out.println(".");

                            long endtime = System.currentTimeMillis();
                            long elapsed = (endtime - starttime);
                            //float timemp = elapsed/samples;
                            float mps = (samples/elapsed)*1000;
                            System.out.println("elapsed time: " + elapsed);
                            //System.out.println("time per message: " + timemp);
                            System.out.println("Samples: " + samples + " MPS: " + mps);


                            /*
                            String[] sstr = input.split("_");
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
                            */

                        } catch (Exception ex) {
                            ex.printStackTrace();

                        }

                    }

                    System.out.println("Plugins:");
                    List<String> pluginList = pluginsconfig.getPluginList();
                    for(String pluginId : pluginList) {
                        System.out.println("PluginId: " + pluginId);
                        System.out.println("Config: " +  pluginsconfig.getPluginConfigParams(pluginId));
                    }


                    System.out.println("Name of Agent to message [q to quit]: ");


                    //Thread.sleep(1);
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

            } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error AgentCore: " + e.getMessage());
        } finally {
            System.out.println("Agent : Main :Finally Statement");
        }
    }

    public static void LoadWatchDog() {
        try {
            if(wd != null) {
                wd.shutdown(false);
            }
            wd = new WatchDog();

            while (!watchDogActive) {
                Thread.sleep(1000);
            }
        }
        catch(Exception ex) {
            System.out.println("FAILED TO LOAD WatchDog!");
        }
    }

    public static void LoadControllerPlugin()  {
        try {
            MsgInQueueActive = true; //allow incoming message

            controllerPluginSlot = "plugin/0";

            boolean isComm = enablePlugin(controllerPluginSlot, false);
            if (!isComm) {
                System.out.println("failed to load");
                System.exit(0);
            } else {


                Plugin plugin = AgentEngine.pluginMap.get(controllerPluginSlot);
                MsgEvent me = new MsgEvent(MsgEvent.Type.CONFIG, region, agent, controllerPluginSlot, "comminit");
                me.setParam("action", "comminit");
                me.setParam("src_region", region);
                me.setParam("src_agent", agent);
                me.setParam("dst_region", region);
                me.setParam("dst_agent", agent);
                me.setParam("dst_plugin", "plugin/0");
                plugin.Message(me); //send msg to plugin

                while (!isCommInit) {
                    Thread.sleep(500);
                }
                if (isGlobalController) {
                    coreLogger.info("* Global Controller *");
                }
                if (isRegionalController)
                    coreLogger.info("Region: [" + AgentEngine.region + "] * Regional Controller *");
                else
                    coreLogger.info("Region: [" + AgentEngine.region + "]");
                coreLogger.info(" Agent: [" + AgentEngine.agent + "]");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void msgIn(MsgEvent me) {
        msgInProcessQueue.execute(new MsgRoute(me));
    }

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
                    try {
                        plugin.PreStop();
                    } catch (Exception e) {
                        pluginsLogger.error("[{}] - PreShutdown error - [Exception: {}]", pluginID, e.getMessage());
                    }
                    try {
                        plugin.Stop();
                    } catch (Exception e) {
                        pluginsLogger.error("[{}] - Shutdown error - [Exception: {}]", pluginID, e.getMessage());
                    }
                    int status = pluginMap.get(pluginID).getStatus();

                    if(status != 8) {
                        pluginsLogger.error("[{}] unable to confirm disabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                    } else {
                        pluginsLogger.info("[{}] disabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                    }
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

                int status = pluginMap.get(pluginID).getStatus();
                if(status != 10) {
                    pluginsLogger.error("[{}] unable to confirm enable. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                } else {
                    pluginsLogger.info("[{}] enabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());

                }
                /*
                if(isCommInit) { //let controller plugin come up without watchdog enable
                    int count = 0;
                    while((status != 10) && (count < 120)) {
                        pluginsLogger.debug("Waiting on enable for plugin {} current status: {}", pluginID, status);
                        Thread.sleep(500);
                        status = pluginMap.get(pluginID).getStatus();
                    }
                    if(count == 120) {
                        pluginsLogger.error("[{}] unable to confirm enable. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                    } else {
                        pluginsLogger.info("[{}] enabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());

                    }
                }
                else {
                    pluginsLogger.info("[{}] enabled. [Name: {}, Version: {}]", pluginID, plugin.getName(), plugin.getVersion());
                }
                */

                return true;

            } catch (IOException e) {
                pluginsLogger.error("Loading failed - Could not read plugin jar file. [Jar: {}]", pluginsconfig.getPluginJar(pluginID));
                e.printStackTrace();
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
        //return config.getAgentName() + "." + version;
        return AgentEngine.agent + "." + version;
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

    public static byte[] stringCompress(String str) {
        byte[] dataToCompress = str.getBytes(StandardCharsets.UTF_8);
        byte[] compressedData = null;
        try
        {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream(dataToCompress.length);
            try
            {
                GZIPOutputStream zipStream =
                        new GZIPOutputStream(byteStream);
                try
                {
                    zipStream.write(dataToCompress);
                }
                finally
                {
                    zipStream.close();
                }
            }
            finally
            {
                byteStream.close();
            }

            compressedData = byteStream.toByteArray();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return compressedData;
    }

    static void cleanup() throws ConfigurationException, IOException, InterruptedException {
        try {
            coreLogger.info("Shutdown initiated");
            phw.timer.cancel();

            wd.shutdown(true);

            List<String> pluginList = getActivePlugins();

                for (String plugin : pluginList) {
                    if (disablePlugin(plugin, false)) {
                        pluginsLogger.info("{} is shutdown", plugin);
                    }
                }
            coreLogger.info("Shutdown Complete.");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Shutdown Error: " + ex.getMessage());
        }
    }
}


