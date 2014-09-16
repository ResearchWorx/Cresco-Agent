package plugins;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class ConfigModules {

	private HierarchicalINIConfiguration iniConfObj;
	
	public ConfigModules(String configFile) throws ConfigurationException
	{
	    //String iniFile = "Cresco-Agent-Netflow.ini";
	    
		iniConfObj = new HierarchicalINIConfiguration(configFile);
	}
	
	public int getModuleInstances()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugins");
		return sObj.getInt("moduleinstances");
	}
	public int getWatchDogTimer(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
		return sObj.getInt("watchdogtimer");
	}
	public String getAMPQDataExchange(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
		return sObj.getString("ampq_data_exchange");
	}
	public boolean setAMPQLogExchange(String pluginID,String dataExchange) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
	    sObj.setProperty("ampq_data_exchange", dataExchange);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQHost(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
		return sObj.getString("ampq_host");
	}
	public boolean setAMPQHost(String pluginID,String host) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
	    sObj.setProperty("ampq_host", host);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQUser(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
		return sObj.getString("ampq_username");	    
	}
	public boolean setAMPQUser(String pluginID,String userName) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
	    sObj.setProperty("ampq_username", userName);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQPassword(String pluginID)
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
		return sObj.getString("ampq_password");	    
	}
	public boolean setAMPQPassword(String pluginID,String password) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("plugin" + pluginID);
	    sObj.setProperty("ampq_password", password);
	    iniConfObj.save();
	    return true;
	}
	
}