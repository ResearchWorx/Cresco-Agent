package core;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.util.Iterator;

public class Config {

    private HierarchicalINIConfiguration iniConfObj;

    public Config(String configFile) throws ConfigurationException {
        iniConfObj = new HierarchicalINIConfiguration(configFile);
        iniConfObj.setDelimiterParsingDisabled(true);
        iniConfObj.setAutoSave(true);

    }

    public String getPluginConfigString() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        //final Map<String,String> result=new TreeMap<String,String>();
        StringBuilder sb = new StringBuilder();
        final Iterator it = sObj.getKeys();
        while (it.hasNext()) {
            final Object key = it.next();
            final String value = sObj.getString(key.toString());
            //result.put(key.toString(),value);
            sb.append(key.toString() + "=" + value + ",");

        }
        return sb.toString().substring(0, sb.length() - 1);
        //return result;
    }


    public int getIntParams(String section, String param) {
        int return_param = -1;
        try {
            SubnodeConfiguration sObj = iniConfObj.getSection(section);
            return_param = Integer.parseInt(sObj.getString(param));
        } catch (Exception ex) {
            System.out.println("AgentEngine : Config : Error : " + ex.toString());
        }
        return return_param;
    }

    public String getStringParams(String section, String param) {
        String return_param = null;
        try {
            SubnodeConfiguration sObj = iniConfObj.getSection(section);
            return_param = sObj.getString(param);
        } catch (Exception ex) {
            System.out.println("AgentEngine : Config : Error : " + ex.toString());
        }
        return return_param;
    }

    public String getPluginPath() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        String pluginPath = sObj.getString("pluginpath");
        if (!pluginPath.endsWith("/")) {
            pluginPath = pluginPath + "/";
        }
        return pluginPath;
    }

    public String getLogPath() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        String logPath = sObj.getString("logpath");
        if (logPath == null)
            logPath = "./logs";
        if (logPath.endsWith("/") || logPath.endsWith("\\\\"))
            logPath = logPath.substring(0, logPath.length() - 1);
        return new File(logPath).getAbsolutePath();
    }

    public String getAgentName() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("agentname");
    }

    public boolean setAgentName(String agentname) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("agentname", agentname);
        return true;
    }

    public boolean setRegionName(String region) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("regionname", region);
        return true;
    }

    public boolean getGenerateName() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        int value = Integer.parseInt(sObj.getString("generatename"));
        if (value == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getGenerateRegion() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        int value = Integer.parseInt(sObj.getString("generateregion"));
        if (value == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean setGenerateName(boolean genname) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        if (genname) {
            sObj.setProperty("generatename", 1);
        } else {
            sObj.setProperty("generatename", 0);
        }
        return true;
    }

    public int getWatchDogTimer() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getInt("watchdogtimer");
    }

    public int getControllerDiscoveryTimeout() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        int tmpTime = Math.round(sObj.getInt("controllerdiscoverytimeout"));
        return tmpTime;
    }

    public int getLogProducerTimeout() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        int tmpTime = Math.round(sObj.getInt("logproducertimeout") / 1000);
        return tmpTime;
    }

    public String getPluginConfigFile() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("plugin_config_file");
    }

    public String getRegion() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("regionname");
    }

    public boolean setRegion(String region) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("regionname", region);
        iniConfObj.save();
        return true;
    }

    public String getAMPQControlHost() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("ampq_control_host");
    }

    public boolean setAMPQControlHost(String host) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("ampq_control_host", host);
        iniConfObj.save();
        return true;
    }

    public String getAMPQControlUser() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("ampq_control_username");
    }

    public boolean setAMPQControlUser(String userName) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("ampq_control_username", userName);
        iniConfObj.save();
        return true;
    }

    public String getAMPQControlPassword() {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        return sObj.getString("ampq_control_password");
    }

    public boolean setAMPQControlPassword(String password) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("general");
        sObj.setProperty("ampq_control_password", password);
        iniConfObj.save();
        return true;
    }

}