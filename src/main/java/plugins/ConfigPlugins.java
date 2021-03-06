package plugins;

import core.AgentEngine;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

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