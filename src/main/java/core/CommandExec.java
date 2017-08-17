package core;

import com.researchworx.cresco.library.messaging.MsgEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plugins.Plugin;
import sun.management.Agent;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static core.AgentEngine.main;
import static core.AgentEngine.pluginsconfig;

public class CommandExec {
    private static final Logger logger = LoggerFactory.getLogger("Engine");
    private static final Logger logMessages = LoggerFactory.getLogger("Logging");

    public CommandExec() {

    }

    public MsgEvent cmdExec(MsgEvent ce) throws IOException, ConfigurationException {

        try {
            if(ce.getMsgType() == MsgEvent.Type.EXEC) {

                switch (ce.getParam("action")) {
                    default:
                        logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());
                        return null;
                }

            } else if(ce.getMsgType() == MsgEvent.Type.CONFIG) {

                switch (ce.getParam("action")) {

                    case "comminit":
                        commInit(ce);
                        break;

                    case "enable":
                        enablePlugin(ce);
                        break;

                    case "disable":
                        disablePlugin(ce);
                        break;

                    default:
                        logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());
                        break;
                }

            } else if(ce.getMsgType() == MsgEvent.Type.WATCHDOG) {
                watchdogUpdate(ce);
            }

        } catch (Exception ex) {
            System.out.println("AgentEngine : CommandExec Error : " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    void watchdogUpdate(MsgEvent ce) {
        String src_agent = ce.getParam("src_agent");
        String src_region = ce.getParam("src_region");
        String src_plugin = ce.getParam("src_plugin");
        if(src_agent.equals(AgentEngine.agent) && src_region.equals(AgentEngine.region)) {
            AgentEngine.pluginMap.get(src_plugin).setWatchDogTS(System.currentTimeMillis());
            AgentEngine.pluginMap.get(src_plugin).setRuntime(Long.parseLong(ce.getParam("runtime")));
            logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus());
        } else {
            logger.error("Can't update watchdog plugin: {} for remote host: {} {} on {} {}",src_plugin, src_region, src_agent, AgentEngine.region, AgentEngine.agent);
        }
    }

    void enablePlugin(MsgEvent ce) {
        String src_agent = ce.getParam("src_agent");
        String src_region = ce.getParam("src_region");
        String src_plugin = ce.getParam("src_plugin");
        if(src_agent.equals(AgentEngine.agent) && src_region.equals(AgentEngine.region)) {
            //status = 10, plugin enabled
            AgentEngine.pluginMap.get(src_plugin).setStatus(10);
            AgentEngine.pluginMap.get(src_plugin).setWatchDogTimer(Long.parseLong(ce.getParam("watchdogtimer")));
            AgentEngine.pluginMap.get(src_plugin).setWatchDogTS(System.currentTimeMillis());
            logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus());
        } else {
            logger.error("Can't enable plugin: {} for remote host: {} {} on {} {}",src_plugin, src_region, src_agent, AgentEngine.region, AgentEngine.agent);
        }
    }

    void disablePlugin(MsgEvent ce) {
        String src_agent = ce.getParam("src_agent");
        String src_region = ce.getParam("src_region");
        String src_plugin = ce.getParam("src_plugin");
        if(src_agent.equals(AgentEngine.agent) && src_region.equals(AgentEngine.region)) {
            //status = 10, plugin enabled
            AgentEngine.pluginMap.get(src_plugin).setStatus(8);
            logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus());
        } else {
            logger.error("Can't enable plugin: {} for remote host: {} {} on {} {}",src_plugin, src_region, src_agent, AgentEngine.region, AgentEngine.agent);
        }
    }

    void commInit(MsgEvent ce) {
        logger.debug("comminit message type found");
        if (Boolean.parseBoolean(ce.getParam("is_active"))) {

            //init startup
            AgentEngine.region = ce.getParam("set_region");
            AgentEngine.agent = ce.getParam("set_agent");

            //IS COMMINIT?
            AgentEngine.isCommInit = true;
            AgentEngine.isRegionalController = Boolean.parseBoolean(ce.getParam("is_regional_controller"));
            AgentEngine.isGlobalController = Boolean.parseBoolean(ce.getParam("is_global_controller"));


        } else {
            //failover startup
            AgentEngine.region = ce.getParam("set_region");
            AgentEngine.agent = ce.getParam("set_agent");
            AgentEngine.isRegionalController = Boolean.parseBoolean(ce.getParam("is_regional_controller"));
            AgentEngine.LoadWatchDog();

        }
    }


    public MsgEvent cmdExec2(MsgEvent ce) throws IOException, ConfigurationException {

        try {

            if ((ce.getMsgType() == MsgEvent.Type.CONFIG) && (ce.getParam("configtype") != null)) //this is only for controller detection
            {
                //create for initial discovery
                //if ((ce.getMsgBody() != null) && (ce.getParam("set_region") != null) && (ce.getParam("set_agent") != null)) {
                if (ce.getParam("configtype").equals("comminit")) {
                    if (Boolean.parseBoolean(ce.getParam("is_active"))) {

                        //init startup
                        AgentEngine.region = ce.getParam("set_region");
                        /*
                        if (!AgentEngine.config.getGenerateRegion())
                            AgentEngine.config.setRegionName(AgentEngine.region);
                        */
                        AgentEngine.agent = ce.getParam("set_agent");
                        /*
                        if (!AgentEngine.config.getGenerateName())
                            AgentEngine.config.setAgentName(AgentEngine.agent);
                        */
                        //IS COMMINIT?
                        AgentEngine.isCommInit = true;
                        AgentEngine.isRegionalController = Boolean.parseBoolean(ce.getParam("is_regional_controller"));
                        AgentEngine.isGlobalController = Boolean.parseBoolean(ce.getParam("is_global_controller"));


                    } else {
                        //System.out.println("CODY [" + ce.getParams().toString() + "]");
                        //failover startup
                        AgentEngine.region = ce.getParam("set_region");
                        AgentEngine.agent = ce.getParam("set_agent");
                        AgentEngine.isRegionalController = Boolean.parseBoolean(ce.getParam("is_regional_controller"));
                        AgentEngine.LoadWatchDog();

                    }
                    return null;
                } else if (ce.getParam("configtype").equals("pluginadd")) {
//
                    //Map<String, String> hm = pluginsconfig.buildPluginMap(ce.getParam("configparams"));
                    Map<String, String> hm = pluginsconfig.getMapFromString(ce.getParam("configparams"),false);
                    String hostAddressString = ce.getParam("http_host");
                    //String pluginName = ce.getParam("pluginname");
                    String pluginName = hm.get("pluginname");
                    String jarFile = hm.get("jarfile");
                    String jarMD5 = ce.getParam("jarmd5");
                    if(getPlugin(hostAddressString,pluginName,jarFile,jarMD5)) {

                        hm.remove("configtype");
                        String plugin = pluginsconfig.addPlugin(hm);
                        ce.setParam("plugin", plugin);
                        boolean isEnabled = AgentEngine.enablePlugin(plugin, false);
                        if (!isEnabled) {
                            ce.setMsgBody("Failed to Add Plugin:" + plugin);
                            pluginsconfig.removePlugin(plugin);
                        } else {
                            ce.setMsgBody("Added Plugin:" + plugin);
                            ce.setParam("status_code","10");
                        }
                    }
                    else {
                        System.out.println("plugin add failed getPlugin");
                        ce.setMsgBody("Failed to Download Plugin: " + pluginName);
                    }
                    ce.removeParam("configtype");
                    ce.removeParam("configparams");
                    //return ce;
                    return null;
                } else if (ce.getParam("configtype").equals("plugininventory")) {
                       //dirty.. fake message from plugin.. so bad
                       //This needs to be change on the Cresco Library
                       if(ce.getParam("plugin") != null) {
                           //send enable message from agent for plugin with configParams
                           String configParams = pluginsconfig.getPluginConfigParams(ce.getParam("plugin"));
                           ce.setParam("configparams",configParams);
                           ce.setParam("dst_plugin",ce.getParam("plugin"));
                           ce.setParam("is_active", Boolean.TRUE.toString());
                           String watchdogtimer = pluginsconfig.getPluginConfigParam(ce.getParam("plugin"),"watchdogtimer");
                           if(watchdogtimer !=null) {
                               ce.setParam("watchdogtimer",watchdogtimer);
                           }
                           else {
                               ce.setParam("watchdogtimer","5000");
                           }
                           return ce;
                       }


                } else if (ce.getParam("configtype").equals("activeplugininventory")) {


                } else if (ce.getParam("configtype").equals("plugininventoryfull")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    int pluginsFound = 0;
                    for (int i = 0; i < 1000; i++) {
                        String plugin = "plugin/" + i;
                        if (AgentEngine.pluginsconfig.getPluginName(plugin) == null)
                            continue;

                        pluginsFound++;

                        sb.append("{\"id\":\"");
                        sb.append(plugin);
                        sb.append("\",");

                        sb.append("\"status\":");
                        sb.append(String.valueOf(AgentEngine.pluginsconfig.getPluginStatus(plugin)));
                        sb.append(",");

                        sb.append("\"config\":{");
                        boolean hasKeys = false;
                        Iterator<String> keys = AgentEngine.pluginsconfig.getPluginConfig(plugin).getKeys();
                        while (keys.hasNext()) {
                            hasKeys = true;
                            String key = keys.next();
                            sb.append("\"");
                            sb.append(key);
                            sb.append("\":\"");
                            sb.append(AgentEngine.pluginsconfig.getPluginConfig(plugin).getString(key));
                            sb.append("\",");
                        }
                        if (hasKeys)
                            sb.deleteCharAt(sb.lastIndexOf(","));
                        sb.append("}},");
                    }
                    sb.append("]");
                    if (pluginsFound > 0)
                        sb.deleteCharAt(sb.lastIndexOf(","));
                    ce.setParam("pluginlist", sb.toString());
                    return ce;
                } else if (ce.getParam("configtype").equals("plugindownload")) {
                    try {
                        String baseUrl = ce.getParam("pluginurl");
                        if (!baseUrl.endsWith("/")) {
                            baseUrl = baseUrl + "/";
                        }

                        URL website = new URL(baseUrl + ce.getParam("plugin"));
                        ReadableByteChannel rbc = Channels.newChannel(website.openStream());

                        String pluginFile = AgentEngine.config.getPluginPath() + ce.getParam("plugin");
                        boolean forceDownload = false;
                        if (ce.getParam("forceplugindownload") != null) {
                            forceDownload = true;
                            System.out.println("Forcing Plugin Download");
                        }

                        File pluginFileObject = new File(pluginFile);
                        if (!pluginFileObject.exists() || forceDownload) {
                            FileOutputStream fos = new FileOutputStream(pluginFile);

                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                            fos.close();

                            ce.setMsgBody("Downloaded Plugin:" + ce.getParam("plugin"));
                            System.out.println("Downloaded Plugin:" + ce.getParam("plugin"));
                        } else {
                            ce.setMsgBody("Plugin already exists:" + ce.getParam("plugin"));
                            System.out.println("Plugin already exists:" + ce.getParam("plugin"));
                        }

                    } catch (Exception ex) {
                        System.out.println("CommandExec : plugindownload " + ex.toString());
                    }
                    return ce;
                } else if (ce.getParam("configtype").equals("pluginremove")) {
                    //disable if active
                    AgentEngine.disablePlugin(ce.getParam("plugin"), true);
                    //remove configuration
                    pluginsconfig.removePlugin(ce.getParam("plugin"));

                    /*
                    ce.setMsgBody("Removed Plugin:" + ce.getParam("plugin"));
                    ce.removeParam("configtype");
                    ce.removeParam("plugin");
                    */

                    /*
                    MsgEvent le = new MsgEvent(MsgEvent.Type.CONFIG, AgentEngine.region, null, null, "disabled");
                    le.setParam("src_region", AgentEngine.region);
                    le.setParam("src_agent", AgentEngine.agent);
                    le.setParam("dst_region", AgentEngine.region);
                    //le.setParam("is_active", Boolean.FALSE.toString());
                    le.setParam("action", "disable");
                    */
                    return null;
                    //return ce;
                } else if (ce.getParam("configtype").equals("componentstate")) {
                    if (ce.getMsgBody().equals("disabled")) {
                        //System.exit(0);//shutdown agent
                        AgentEngine.ds = new DelayedShutdown(5000l);
                        ce.setMsgBody("Shutting Down");
                        return ce;
                    }


                }
            } else if (ce.getMsgType() == MsgEvent.Type.EXEC) {//Execute and respond to execute commands

                if (ce.getParam("cmd").equals("show") || ce.getParam("cmd").equals("?") || ce.getParam("cmd").equals("help")) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("\nAgent " + AgentEngine.agent + " Help\n");
                    sb.append("-\n");
                    sb.append("help\t\t\t\t Shows This Message\n");
                    sb.append("show address\t\t\t\t Shows IP address of local host\n");
                    sb.append("show agent\t\t\t\t Shows Agent Info\n");
                    sb.append("show name\t\t\t\t Shows Name of Agent\n");
                    sb.append("show plugins\t\t\t\t Shows Plugins Info\n");
                    sb.append("show version\t\t\t\t Shows Agent Version\n");
                    sb.append("enable  plugin [plugin/(id)]\t\t\t\t Enables a Plugin\n");
                    sb.append("disable plugin [plugin/(id}]\t\t\t\t Disables a Plugin\n");
                    sb.append("---");
                    sb.append("plugin/[number of plugin]\t\t To access Plugin Info");

                    ce.setMsgBody(sb.toString());
                    return ce;
                } else if (ce.getParam("cmd").equals("show_name")) {
                    ce.setMsgBody(AgentEngine.agent);
                    return ce;
                } else if (ce.getParam("cmd").equals("show_plugins")) {
                    ce.setMsgBody(AgentEngine.listPlugins());
                    return ce;
                } else if (ce.getParam("cmd").equals("getactiveplugins")) {
                    //getActivePlugins
                    String activePluginList = "";
                    List<String> activePlugins = AgentEngine.getActivePlugins();
                    for (String pluginName : activePlugins) {
                        activePluginList += pluginsconfig.getPluginName(pluginName) + "=" + pluginName + ",";
                    }
                    if (activePluginList.length() > 1) {
                        activePluginList = activePluginList.substring(0, activePluginList.length() - 1);
                    }
                    ce.setParam("activepluginlist", activePluginList);

                    return ce;
                } else if (ce.getParam("cmd").equals("show_version")) {
                    ce.setMsgBody(AgentEngine.agentVersion);
                    return ce;
                } else if (ce.getParam("cmd").equals("show_address")) {

                    StringBuilder sb = new StringBuilder();
                    try {
                        InetAddress localhost = InetAddress.getLocalHost();
                        sb.append(" IP Addr: " + localhost.getHostAddress() + "\n");
                        // Just in case this host has multiple IP addresses....
                        InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
                        if (allMyIps != null && allMyIps.length > 1) {
                            sb.append(" Full list of IP addresses:\n");
                            for (int i = 0; i < allMyIps.length; i++) {
                                sb.append("    " + allMyIps[i]);
                            }
                        }
                        return ce;
                    } catch (UnknownHostException e) {
                        sb.append(" (error retrieving server host name)\n");
                    }

                    try {
                        sb.append("Full list of Network Interfaces:\n");
                        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                            NetworkInterface intf = en.nextElement();
                            sb.append("    " + intf.getName() + " " + intf.getDisplayName() + "\n");
                            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                                sb.append("        " + enumIpAddr.nextElement().toString() + "\n");
                            }
                            return ce;
                        }
                    } catch (SocketException e) {
                        sb.append(" (error retrieving network interface list)\n");
                    }

                    ce.setMsgBody(sb.toString());

                } else if (ce.getParam("cmd").startsWith("enable")) {
                    if (ce.getParam("plugin") != null) {
                        boolean isEnabled = AgentEngine.enablePlugin(ce.getParam("plugin"), true);
                        if (isEnabled) {
                            ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " enabled");
                        } else {
                            ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " failed to enable");
                        }

                    } else {
                        ce.setMsgBody("Agent Enable Command [" + ce.getParam("cmd") + "] unknown");
                    }
                    return ce;
                } else if (ce.getParam("cmd").startsWith("disable")) {
                    if (ce.getParam("plugin") != null) {
                        //ce.setMsgBody(AgentEngine.disablePlugin(ce.getParam("plugin"),true));
                        boolean isDisabled = AgentEngine.enablePlugin(ce.getParam("plugin"), true);
                        if (isDisabled) {
                            ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " disabled");
                        } else {
                            ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " failed to disable");
                        }
                    } else {
                        ce.setMsgBody("Agent Disable Command [" + ce.getParam("cmd") + "] unknown");
                    }
                    return ce;
                }
            } else if (ce.getMsgType() == MsgEvent.Type.LOG) {
                logMessage(ce);
            }
        } catch (Exception ex) {
            System.out.println("AgentEngine : CommandExec Error : " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    private void logMessage(MsgEvent log) {
        String className = log.getParam("full_class");
        String logMessage = "[" + log.getParam("src_plugin") + ": " + pluginsconfig.getPluginName(log.getParam("src_plugin")) + "]";
        if (className != null)
            logMessage = logMessage + "[" + formatClassName(className) + "]";
        logMessage = logMessage + " " + log.getMsgBody();
        switch (log.getParam("log_level").toLowerCase()) {
            case "error":
                logMessages.error(logMessage);
                break;
            case "warn":
                logMessages.warn(logMessage);
                break;
            case "info":
                logMessages.info(logMessage);
                break;
            case "debug":
                logMessages.debug(logMessage);
                break;
            case "trace":
                logMessages.trace(logMessage);
                break;
            default:
                logMessages.error("Unknown log_level [{}]", log.getParam("log_level"));
                break;
        }
    }

    public List<String> getPluginInventory() {
        List<String> pluginFiles = null;
        try
        {
            String pluginDirectory = AgentEngine.config.getPluginPath();

            File folder = new File(pluginDirectory);
            if(folder.exists())
            {
                pluginFiles = new ArrayList<String>();
                File[] listOfFiles = folder.listFiles();

                for (int i = 0; i < listOfFiles.length; i++)
                {
                    if (listOfFiles[i].isFile())
                    {
                        pluginFiles.add(listOfFiles[i].getAbsolutePath());
                    }

                }
                if(pluginFiles.isEmpty())
                {
                    pluginFiles = null;
                }
            }
        }
        catch(Exception ex)
        {
            pluginFiles = null;
        }
        return pluginFiles;
    }

    public String getPluginName(String jarFile) {
        String version = null;
        try{
            //String jarFile = AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            //logger.debug("JARFILE:" + jarFile);
            //File file = new File(jarFile.substring(5, (jarFile.length() )));
            File file = new File(jarFile);

            boolean calcHash = true;
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            long fileTime = attr.creationTime().toMillis();

            FileInputStream fis = new FileInputStream(file);
            @SuppressWarnings("resource")
            JarInputStream jarStream = new JarInputStream(fis);
            Manifest mf = jarStream.getManifest();

            Attributes mainAttribs = mf.getMainAttributes();
            version = mainAttribs.getValue("artifactId");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();

        }
        return version;
    }

    public String getJarMD5(String pluginFile) {
        String jarString = null;
        try
        {
            Path path = Paths.get(pluginFile);
            byte[] data = Files.readAllBytes(path);

            MessageDigest m= MessageDigest.getInstance("MD5");
            m.update(data);
            jarString = new BigInteger(1,m.digest()).toString(16);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return jarString;
    }

    private String verifyPlugin(String requestedPlugin) {
        String returnPluginfile = null;
        //String requestedPlugin = ce.getParam("pluginname");
        List<String> pluginMap = getPluginInventory();
        for(String pluginfile : pluginMap) {
        //    logger.debug("plugin = " + pluginfile);
        //    logger.debug("plugin name = " + getPluginName(pluginfile));
            String pluginName = getPluginName(pluginfile);
            if(pluginName != null) {
                if (requestedPlugin.equals(pluginName)) {
                    returnPluginfile = pluginfile;
                }
            }
        }

        return returnPluginfile;
    }

    private boolean getPlugin(String hostAddressString, String pluginName, String jarFile, String pluginMD5) {
        boolean isFound = false;
        try {
            String pluginFile = verifyPlugin(pluginName);
            if(pluginFile != null) {
                String jarMD5 = getJarMD5(pluginFile);

                if(pluginMD5.equals(jarMD5)) {
                    isFound = true;
                }
            }

            if(!isFound) {
                String[] hostAddresses = null;
                if (hostAddressString.contains(",")) {
                    hostAddresses = hostAddressString.split(",");
                } else {
                    hostAddresses = new String[1];
                    hostAddresses[0] = hostAddressString;
                }

                for(String hostAddress : hostAddresses) {
                    if (serverListening(hostAddress)) {
                        try {

                            String pluginDirectory = AgentEngine.config.getPluginPath();
                            if (pluginDirectory.endsWith("/")) {
                                pluginDirectory = pluginDirectory.substring(0, pluginDirectory.length() - 1);
                            }

                            String downloadFile = pluginDirectory + "/" + jarFile + ".test";
                            URL website = new URL(hostAddress + jarFile);


                            //ReadableByteChannel rbc = Channels.newChannel(website.openStream());

                            File pluginFileObject = new File(downloadFile);
                            if (pluginFileObject.exists()) {
                                pluginFileObject.delete();
                            } else {
                                pluginFileObject.createNewFile();
                            }

                            //FileOutputStream fos = new FileOutputStream(downloadFile);

                            //fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                            //fos.close();
                            System.out.println(website.toString());
                            java.io.BufferedInputStream in = new java.io.BufferedInputStream(website.openStream());
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(downloadFile);
                            java.io.BufferedOutputStream bout = new BufferedOutputStream(fos);
                            byte data[] = new byte[1024];
                            int read;
                            while((read = in.read(data,0,1024))>=0)
                            {
                                bout.write(data, 0, read);
                            }
                            bout.close();
                            in.close();

                            String jarMD5 = getJarMD5(pluginFile);

                            if (pluginMD5.equals(jarMD5)) {
                                return true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }
            /*
            String baseUrl = ce.getParam("pluginurl");
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }

            URL website = new URL(baseUrl + ce.getParam("plugin"));
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());

            String pluginFile = AgentEngine.config.getPluginPath() + ce.getParam("plugin");
            boolean forceDownload = false;
            if (ce.getParam("forceplugindownload") != null) {
                forceDownload = true;
                System.out.println("Forcing Plugin Download");
            }

            File pluginFileObject = new File(pluginFile);
            if (!pluginFileObject.exists() || forceDownload) {
                FileOutputStream fos = new FileOutputStream(pluginFile);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();

                ce.setMsgBody("Downloaded Plugin:" + ce.getParam("plugin"));
                System.out.println("Downloaded Plugin:" + ce.getParam("plugin"));
            } else {
                ce.setMsgBody("Plugin already exists:" + ce.getParam("plugin"));
                System.out.println("Plugin already exists:" + ce.getParam("plugin"));
            }
            */
        }
        catch(Exception ex) {
            //System.out.println("getPlugin " + ex.getMessage());
            ex.printStackTrace();
        }
        return isFound;
    }

    public static boolean serverListening(String hosturl) {
        Socket s = null;
        //http://address:port
        try
        {

            String[] tmphosturl = hosturl.split(":");
            int port = Integer.parseInt(tmphosturl[2].substring(0,tmphosturl[2].indexOf("/")));
            String host = tmphosturl[1].substring(tmphosturl[1].lastIndexOf("/") + 1,tmphosturl[1].length());
            s = new Socket();
            s.connect(new InetSocketAddress(host,port),2000);
            //s = new Socket(host, port);

            return true;
        }
        catch (Exception e)
        {
            //System.out.println("serverlistening error " + e.getMessage());
            //e.printStackTrace();
            return false;
        }
        finally
        {
            if(s != null)
                try {s.close();}
                catch(Exception e){}
        }
    }

    private String formatClassName(String className) {
        String newName = "";
        int lastIndex = 0;
        int nextIndex = className.indexOf(".", lastIndex + 1);
        while (nextIndex != -1) {
            newName = newName + className.substring(lastIndex, lastIndex + 1) + ".";
            lastIndex = nextIndex + 1;
            nextIndex = className.indexOf(".", lastIndex + 1);
        }
        return newName + className.substring(lastIndex);
    }

}
