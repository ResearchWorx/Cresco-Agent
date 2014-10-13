package core;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class Config {

	private HierarchicalINIConfiguration iniConfObj;
	
	public Config(String configFile) throws ConfigurationException
	{
	    iniConfObj = new HierarchicalINIConfiguration(configFile);
	    iniConfObj.setAutoSave(true);
	}
	
	public String getAgentName()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("agentname");
	}
	public boolean setAgentName(String agentname) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("agentname", agentname);
	    return true;
	}
	public boolean getGenerateName()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		int value = Integer.parseInt(sObj.getString("generatename"));
		if(value == 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public boolean setGenerateName(boolean genname) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    if(genname)
	    {
	    	sObj.setProperty("generatename", 1);
	    }
	    else
	    {
	    	sObj.setProperty("generatename", 0);
	    }
	    return true;
	}
	public int getWatchDogTimer()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getInt("watchdogtimer");
	}
	public int getControllerDiscoveryTimeout()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getInt("controllerdiscoverytimeout");
	}
	public String getPluginConfigFile()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("plugin_config_file");
	}
	
	
	public String getRegion()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("region");
	}
	public boolean setRegion(String region) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("region", region);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQControlHost()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_control_host");
	}
	public boolean setAMPQControlHost(String host) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_control_host", host);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQControlUser()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_control_username");	    
	}
	public boolean setAMPQControlUser(String userName) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_control_username", userName);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQControlPassword()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_control_password");	    
	}
	public boolean setAMPQControlPassword(String password) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_control_password", password);
	    iniConfObj.save();
	    return true;
	}
	
}