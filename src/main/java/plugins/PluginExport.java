package plugins;

import com.google.gson.Gson;
import core.AgentEngine;

import javax.xml.bind.DatatypeConverter;
import java.util.*;

public class PluginExport {

    private Gson gson;

    public PluginExport() {
        gson = new Gson();
    }

    public String getPluginExport() {
        //boolean isExported = false;
        String exportString = null;
        try {
            List<Map<String,String>> configMapList = new ArrayList<>();
            Map<String, Plugin> map = AgentEngine.pluginMap;
            for (Map.Entry<String, Plugin> entry : map.entrySet())
            {
                    String pluginId = entry.getKey();
                    int status = entry.getValue().getStatus();
                    boolean isActive = entry.getValue().getActive();

                    Map<String,String> configMap = new HashMap<>();

                    Map<String,String> paramMap = AgentEngine.pluginsconfig.getPluginConfigMap(pluginId);
                    configMap.put("status", String.valueOf(status));
                    configMap.put("isactive", String.valueOf(isActive));
                    configMap.put("pluginid", pluginId);
                    configMap.put("configparams", gson.toJson(paramMap));
                    configMapList.add(configMap);
            }

            exportString = DatatypeConverter.printBase64Binary(AgentEngine.stringCompress(gson.toJson(configMapList)));

        } catch(Exception ex) {
            System.out.println("PluginExport.pluginExport() Error " + ex.getMessage());
        }

        return exportString;
    }

}