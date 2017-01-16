package core;

import com.researchworx.cresco.library.messaging.MsgEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static core.AgentEngine.pluginsconfig;

public class CommandExec {
    private static final Logger coreLogger = LoggerFactory.getLogger("Engine");
    private static final Logger logMessages = LoggerFactory.getLogger("Logging");

    public CommandExec() {

    }

    public MsgEvent cmdExec(MsgEvent ce) throws IOException, ConfigurationException {

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
                        if (Boolean.parseBoolean(ce.getParam("is_regional_controller"))) {
                            AgentEngine.isRegionalController = true;
                        }
                        else {
                            AgentEngine.isRegionalController = false;
                        }
                        if (Boolean.parseBoolean(ce.getParam("is_global_controller"))) {
                            AgentEngine.isGlobalController = true;
                        }
                        else {
                            AgentEngine.isGlobalController = false;
                        }


                    } else {
                        //System.out.println("CODY [" + ce.getParams().toString() + "]");
                        //failover startup
                        AgentEngine.region = ce.getParam("set_region");
                        AgentEngine.agent = ce.getParam("set_agent");
                        if (Boolean.parseBoolean(ce.getParam("is_regional_controller"))) {
                            AgentEngine.isRegionalController = true;
                        }
                        else {
                            AgentEngine.isRegionalController = false;
                        }
                        AgentEngine.LoadWatchDog();

                    }
                    return null;
                } else if (ce.getParam("configtype").equals("pluginadd")) {
                    //Map<String, String> hm = pluginsconfig.buildPluginMap(ce.getParam("configparams"));
                    Map<String, String> hm = pluginsconfig.getMapFromString(ce.getParam("configparams"),false);

                    if(hm.containsKey("inode_id")) {

                    }
                    hm.remove("configtype");
                    String plugin = pluginsconfig.addPlugin(hm);
                    ce.setParam("plugin", plugin);
                    boolean isEnabled = AgentEngine.enablePlugin(plugin, false);
                    if (!isEnabled) {
                        ce.setMsgBody("Failed to Add Plugin:" + plugin);
                        pluginsconfig.removePlugin(plugin);
                    } else {
                        ce.setMsgBody("Added Plugin:" + plugin);
                    }
                    ce.removeParam("configtype");
                    ce.removeParam("configparams");
                    return ce;

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
                    ce.setMsgBody("Removed Plugin:" + ce.getParam("plugin"));
                    ce.removeParam("configtype");
                    ce.removeParam("plugin");
                    return ce;
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
