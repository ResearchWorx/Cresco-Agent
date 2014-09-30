package plugins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class ConfigPlugins {

	private HierarchicalINIConfiguration iniConfObj;
	
	public ConfigPlugins(String configFile) throws ConfigurationException
	{
	    //String iniFile = "Cresco-Agent-Netflow.ini";
	    
		iniConfObj = new HierarchicalINIConfiguration(configFile);
	}
	
	public List getEnabledPluginList(int isEnabled)
	{
		//isEnabled = 1 enabled
		List<String> enabledPlugins = new ArrayList<String>();
		SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
		
		Iterator it = sObj.getKeys();
		while (it.hasNext()) {
			Object key = it.next();
			int value = Integer.parseInt(sObj.getString(key.toString()));
			//result.put(key.toString(), value);
			if(value == isEnabled)
			{
				
				enabledPlugins.add(key.toString());
			
			}	
		}
		return enabledPlugins;	
	}
	public String getPluginName(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
		return sObj.getString("pluginname");
	}
	public String getPluginJar(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
		return sObj.getString("jarfile");
	}
	public String getPluginStatus(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
		return sObj.getString(pluginID);
	}
	public boolean setPluginStatus(String pluginID, int status) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
	    sObj.setProperty(pluginID, Integer.toString(status));
	    iniConfObj.save();
	    return true;
	}
	public SubnodeConfiguration getPluginConfig(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection(pluginID);
		return sObj;
	}
		
}