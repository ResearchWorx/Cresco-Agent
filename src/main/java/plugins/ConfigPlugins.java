package plugins;

import core.AgentEngine;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.net.URLDecoder;
import java.util.*;

public class ConfigPlugins {

    private HierarchicalINIConfiguration iniConfObj;

    public ConfigPlugins(String configFile) throws ConfigurationException {
        iniConfObj = new HierarchicalINIConfiguration(configFile);
        iniConfObj.setAutoSave(true);

    }

    public void removePlugin(String pluginID) {
        iniConfObj.clearProperty("plugins." + pluginID);
        iniConfObj.clearTree(pluginID);
        try {
            iniConfObj.save();
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String addPlugin(Map<String, String> params) throws ConfigurationException {
        Boolean isFound = false;
        int pluginNum = 0;
        String pluginID = null;
        while (!isFound)
            try {
                pluginID = "plugin/" + String.valueOf(pluginNum);
                SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
                if (sObj.isEmpty()) {
                    if (!AgentEngine.pluginMap.containsKey(pluginID)) {
                        isFound = true;
                        iniConfObj.addProperty("plugins." + pluginID, "0");
                        //System.out.println("added plugin record for:" + pluginID);
                        iniConfObj.save(); //problems with duplicates
                    }

                }
                pluginNum++;
            } catch (Exception ex) {
                System.out.println("ConfigPlugins : Problem searching for open plugin slot");
            }

        //System.out.println("adding param records for=" + pluginID);
        Iterator it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            //System.out.println(pairs.getKey() + " = " + pairs.getValue());
            iniConfObj.addProperty(pluginID + "." + pairs.getKey().toString(), pairs.getValue().toString());
            //System.out.println("plugin:" + pluginID + "." + pairs.getKey().toString() + " " + pairs.getValue().toString() );
            it.remove(); // avoids a ConcurrentModificationException
        }
        return pluginID;
    }

    public Map<String, String> buildPluginMap(String configparams) {

        Map<String, String> configMap = new HashMap<String, String>();
        try {
            String[] configLines = configparams.split(",");
            for (String config : configLines) {
                String[] configs = config.split("=");
                configMap.put(configs[0], configs[1]);
            }
        } catch (Exception ex) {
            System.out.println("Controller : PluginConfig : buildconfig ERROR : " + ex.toString());
        }
        return configMap;
    }

    public List<String> getPluginList() {
        //isEnabled : 0=disabled , 1 enabled

        List<String> enabledPlugins = new ArrayList<>();
        SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
        Iterator it = sObj.getKeys();
        while (it.hasNext()) {
            Object key = it.next();
            enabledPlugins.add(key.toString());
        }
        return enabledPlugins;
    }

    public List<String> getPluginList(int isEnabled) {
        //isEnabled : 0=disabled , 1 enabled

        List<String> enabledPlugins = new ArrayList<>();
        SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
        Iterator it = sObj.getKeys();
        while (it.hasNext()) {
            Object key = it.next();
            int value = Integer.parseInt(sObj.getString(key.toString()));
            //result.put(key.toString(), value);
            if (value == isEnabled) {
                enabledPlugins.add(key.toString());
            }
        }
        return enabledPlugins;
    }
    //String configParams = "pluginname=MD5Plugin,jarfile=/opt/Cresco/plugins/cresco-agent-MD5processor-plugin-0.5.0-SNAPSHOT-jar-with-dependencies.jar,ampq_control_host=" + server + ",ampq_control_username=cresco,ampq_control_password=u$cresco01,watchdogtimer=5000,dataqueuedelay=" + delay + ",perfapp=MD5out,dataqueue=md5data,dataqueuedelay=1000,md5producerrate=" + rate + ",enablemd5consumer=1,enablemd5producer=0";

    public String getPluginConfigParams(String pluginID) {
        StringBuilder sb = new StringBuilder();
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        Iterator it = sObj.getKeys();
        while (it.hasNext()) {
            String key = (String)it.next();
            String value = sObj.getString(key);
            sb.append(key + "=" + value + ",");

        }
        return sb.toString().substring(0,sb.length()-1);
    }

    public Map<String,String> getMapFromString(String param, boolean isRestricted) {
        Map<String,String> paramMap = null;


        try{
            String[] sparam = param.split(",");

            paramMap = new HashMap<String,String>();

            for(String str : sparam)
            {
                String[] sstr = str.split(":");

                if(isRestricted)
                {
                    paramMap.put(URLDecoder.decode(sstr[0], "UTF-8"), "");
                }
                else
                {
                    if(sstr.length > 1)
                    {
                        paramMap.put(URLDecoder.decode(sstr[0], "UTF-8"), URLDecoder.decode(sstr[1], "UTF-8"));
                    }
                    else
                    {
                        paramMap.put(URLDecoder.decode(sstr[0], "UTF-8"), "");
                    }
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println("getMapFromString Error: " + ex.toString());
        }

        return paramMap;
    }

    public String getPluginConfigParam(String pluginID, String param) {
        StringBuilder sb = new StringBuilder();
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString(param);
    }

    public String getPluginName(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString("pluginname");
    }

    public String getPluginJar(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        //using path in the other config file

        return AgentEngine.config.getPluginPath() + sObj.getString("jarfile");
    }

    public String getPluginMainClass(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString("mainclass");
    }

    public String getCPluginClass(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString("cpluginclass");
    }

    public String getPackageName(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString("package");
    }

    public String getCExecutorClass(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj.getString("cexecutorclass");
    }

    public String getPluginStatus(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
        return sObj.getString(pluginID);
    }

    public boolean setPluginStatus(String pluginID, int status) throws ConfigurationException {
        SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
        sObj.setProperty(pluginID, Integer.toString(status));
        iniConfObj.save();
        return true;
    }

    public SubnodeConfiguration getPluginConfig(String pluginID) {
        SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
        return sObj;
    }

}