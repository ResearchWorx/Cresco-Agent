package core;

import org.apache.commons.configuration.ConfigurationException;
import com.researchworx.cresco.library.messaging.MsgEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Map;

public class CommandExec {
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
                        AgentEngine.region = ce.getParam("set_region");
                        if (!AgentEngine.config.getGenerateRegion())
                            AgentEngine.config.setRegionName(AgentEngine.region);
                        AgentEngine.agent = ce.getParam("set_agent");
                        if (!AgentEngine.config.getGenerateName())
                            AgentEngine.config.setAgentName(AgentEngine.agent);
                        AgentEngine.isCommInit = true;
                        if (Boolean.parseBoolean(ce.getParam("is_regional_controller"))) {
                            AgentEngine.isRegionalController = true;
                        }

                    }
                    return null;
                } else if (ce.getParam("configtype").equals("pluginadd")) {
                    Map<String, String> hm = AgentEngine.pluginsconfig.buildPluginMap(ce.getParam("configparams"));

                    hm.remove("configtype");
                    String plugin = AgentEngine.pluginsconfig.addPlugin(hm);
                    ce.setParam("plugin", plugin);
                    boolean isEnabled = AgentEngine.enablePlugin(plugin, false);
                    if (!isEnabled) {
                        ce.setMsgBody("Failed to Add Plugin:" + plugin);
                        AgentEngine.pluginsconfig.removePlugin(plugin);
                    } else {
                        ce.setMsgBody("Added Plugin:" + plugin);
                    }
                    return ce;
                } else if (ce.getParam("configtype").equals("plugininventory")) {
                    String pluginList = "";
                    File jarLocation = new File(AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                    String parentDirName = jarLocation.getParent(); // to get the parent dir name

                    File folder = new File(parentDirName + "/plugins");
                    if (folder.exists()) {
                        File[] listOfFiles = folder.listFiles();

                        for (int i = 0; i < listOfFiles.length; i++) {
                            if (listOfFiles[i].isFile()) {
                                System.out.println("Found Plugin: " + listOfFiles[i].getName());
                                pluginList = pluginList + listOfFiles[i].getName() + ",";
                            }

                        }
                        if (pluginList.length() > 0) {
                            pluginList = pluginList.substring(0, pluginList.length() - 1);
                            System.out.println("pluginList=" + pluginList);
                            ce.setParam("pluginlist", pluginList);
                            ce.setMsgBody("There were " + listOfFiles.length + " plugins found.");
                        }

                    } else {
                        ce.setMsgBody("No plugin directory exist to inventory");
                    }

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
                            FileOutputStream fos = new FileOutputStream(AgentEngine.config.getPluginPath() + ce.getParam("plugin"));

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
                    AgentEngine.pluginsconfig.removePlugin(ce.getParam("plugin"));
                    ce.setMsgBody("Removed Plugin:" + ce.getParam("plugin"));
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
                    sb.append("\nAgent " + AgentEngine.config.getAgentName() + " Help\n");
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
        String logMessage = "[" + log.getParam("src_plugin") + ": " + AgentEngine.pluginsconfig.getPluginName(log.getParam("src_plugin")) + "] - " + log.getMsgBody();
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

}
