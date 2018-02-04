package core;

import channels.RPCCall;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.researchworx.cresco.library.messaging.MsgEvent;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
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

import static core.AgentEngine.isCommInit;
import static core.AgentEngine.pluginsconfig;

public class CommandExec {
    private static final Logger logger = LoggerFactory.getLogger("Engine");
    private static final Logger logMessages = LoggerFactory.getLogger("Logging");
    private Gson gson;
    public CommandExec() {
        gson = new Gson();
    }

    public MsgEvent cmdExec(MsgEvent ce) throws IOException, ConfigurationException {

        try {
            if(ce.getMsgType() == MsgEvent.Type.EXEC) {

                switch (ce.getParam("action")) {

                    case "ping":
                        return pingReply(ce);

                    default:
                        logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());
                        logger.error("Unknown message {}",ce.getParams());
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
                    case "pluginadd":
                        return pluginAdd(ce);

                    case "pluginremove":
                        return pluginRemove(ce);

                    default:
                        logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());
                        break;
                }

            } else if(ce.getMsgType() == MsgEvent.Type.WATCHDOG) {
                watchdogUpdate(ce);
            } else if (ce.getMsgType() == MsgEvent.Type.LOG) {
                logMessage(ce);
            }

        } catch (Exception ex) {
            System.out.println("AgentEngine : CommandExec Error : " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            System.out.println(sStackTrace);
            logger.error(sStackTrace);
            logger.error("Error Message: " + ce.getParams());
        }
        return null;
    }

    private MsgEvent pingReply(MsgEvent msg) {

        if(msg.getParam("reversecount") != null) {

            long starttime = System.currentTimeMillis();
            int count = 1;

            int samples = Integer.parseInt(msg.getParam("reversecount"));

            while(count < samples) {
                MsgEvent me = new MsgEvent(MsgEvent.Type.EXEC, AgentEngine.region, AgentEngine.agent, null, "external");
                me.setParam("action","ping");
                //me.setParam("action","noop");
                me.setParam("src_region", AgentEngine.region);
                me.setParam("src_agent", AgentEngine.agent);
                me.setParam("dst_region", me.getParam("src_region"));
                me.setParam("dst_agent", me.getParam("src_agent"));
                if(msg.getParam("src_plugin") != null) {
                    me.setParam("dst_plugin", msg.getParam("src_plugin"));
                }
                me.setParam("count",String.valueOf(count));
                //msgIn(me);
                //System.out.print(".");
                MsgEvent re = new RPCCall().call(me);
                logger.error("REC MESSAGE FROM CONTROLLER : " + re.getParams());
                count++;
            }

            long endtime = System.currentTimeMillis();
            long elapsed = (endtime - starttime);
            float timemp = elapsed/samples;
            float mps = samples/((endtime - starttime)/1000);
            msg.setParam("elapsedtime",String.valueOf(elapsed));
            msg.setParam("timepermessage",String.valueOf(timemp));
            msg.setParam("mps",String.valueOf(mps));
            msg.setParam("samples",String.valueOf(samples));
        }

        msg.setParam("action","pong");
        logger.debug("ping message type found");
        msg.setParam("remote_ts", String.valueOf(System.currentTimeMillis()));
        msg.setParam("type", "agent");
        return msg;
    }

    void watchdogUpdate(MsgEvent ce) {
        String src_agent = ce.getParam("src_agent");
        String src_region = ce.getParam("src_region");
        String src_plugin = ce.getParam("src_plugin");
        if(src_agent.equals(AgentEngine.agent) && src_region.equals(AgentEngine.region)) {

            AgentEngine.pluginMap.get(src_plugin).setWatchDogTS(System.currentTimeMillis());
            AgentEngine.pluginMap.get(src_plugin).setRuntime(Long.parseLong(ce.getParam("runtime")));
            logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus_code());
        } else {
            if (AgentEngine.isCommInit) {
                logger.error("Can't update watchdog plugin: {} for remote host: {} {} on {} {}", src_plugin, src_region, src_agent, AgentEngine.region, AgentEngine.agent);
            }
        }
    }

    void enablePlugin(MsgEvent ce) {

        String src_plugin = ce.getParam("src_plugin");

        AgentEngine.pluginMap.get(src_plugin).setStatus_code(10);

            if(ce.getParam("watchdogtimer") == null) {
                ce.setParam("watchdogtimer","5000");
            }

        AgentEngine.pluginMap.get(src_plugin).setWatchDogTimer(Long.parseLong(ce.getParam("watchdogtimer")));
        AgentEngine.pluginMap.get(src_plugin).setWatchDogTS(System.currentTimeMillis());

        logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus_code());

        //forward to region
        //ce.removeParam("dst_agent");
        //AgentEngine.regionUpdate = true;
    }

    MsgEvent pluginRemove(MsgEvent ce) {

        try {
            String plugin = ce.getParam("plugin");
            boolean isDisabled = AgentEngine.disablePlugin(ce.getParam("plugin"), true);
            if(isDisabled) {
                pluginsconfig.removePlugin(ce.getParam("plugin"));
                ce.setMsgBody("Removed Plugin:" + plugin);
                ce.setParam("status_code", "7");
                ce.setParam("status_desc", "Plugin Removed");
                ce.setParam("region",AgentEngine.region);
                ce.setParam("agent",AgentEngine.agent);
            } else {
                ce.setMsgBody("Failed to Remove Plugin:" + plugin);
                ce.setParam("status_code", "9");
                ce.setParam("status_desc", "Plugin Could Not Be Removed");
            }

        } catch(Exception ex) {
            logger.error("pluginremove Error: " + ex.getMessage());
            ce.setMsgBody("Failed to Remove Plugin Exception :" + ex.getMessage());
            ce.setParam("status_code", "9");
            ce.setParam("status_desc", "Plugin Could Not Be Removed Exception");

        }
        return ce;
    }

    MsgEvent pluginAdd(MsgEvent ce) {
            try {

                //String mapString = AgentEngine.stringUncompress(ce.getParam("configparams"));
                //Map<String, String> hm = pluginsconfig.getMapFromString(mapString, false);

                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> hm = gson.fromJson(ce.getCompressedParam("configparams"), type);
                //String pluginName = hm.get("pluginname");
                //String jarFile = hm.get("jarfile");

                    String plugin = pluginsconfig.addPlugin(hm);
                    boolean isEnabled = AgentEngine.enablePlugin(plugin, false);
                    ce.setParam("plugin", plugin);
                    if (!isEnabled) {
                        ce.setMsgBody("Failed to Add Plugin:" + plugin);
                        ce.setParam("status_code", "9");
                        ce.setParam("status_desc", "Plugin Could Not Be Added");
                        pluginsconfig.removePlugin(plugin);
                    } else {
                        ce.setMsgBody("Added Plugin:" + plugin);
                        ce.setParam("status_code", "10");
                        ce.setParam("status_desc", "Plugin Added");
                        ce.setParam("region",AgentEngine.region);
                        ce.setParam("agent",AgentEngine.agent);
                        hm = pluginsconfig.getPluginConfigMap(plugin);
                        ce.setCompressedParam("configparams", gson.toJson(hm));
                    }

            } catch(Exception ex) {
                logger.error("pluginadd Error: " + ex.getMessage());
                ce.setParam("status_code", "9");
                ce.setParam("status_desc", "Plugin Could Not Be Added Exception");
            }

            //logger.error("Agent: pluginAdd: Type:" + ce.getMsgType() + " params:[" + ce.getParams() +"]");
            return ce;
    }


    void disablePlugin(MsgEvent ce) {
        String src_agent = ce.getParam("src_agent");
        String src_region = ce.getParam("src_region");
        String src_plugin = ce.getParam("src_plugin");
        if(src_agent.equals(AgentEngine.agent) && src_region.equals(AgentEngine.region)) {
            //status = 10, plugin enabled
            AgentEngine.pluginMap.get(src_plugin).setStatus_code(8);
            logger.debug("Plugin {} status {}",src_plugin, AgentEngine.pluginMap.get(src_plugin).getStatus_code());
        } else {
            logger.error("Can't enable plugin: {} for remote host: {} {} on {} {}",src_plugin, src_region, src_agent, AgentEngine.region, AgentEngine.agent);
        }
        //AgentEngine.regionUpdate = true;
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
