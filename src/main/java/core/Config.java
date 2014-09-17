package core;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class Config {

	private HierarchicalINIConfiguration iniConfObj;
	
	public Config(String configFile) throws ConfigurationException
	{
	    iniConfObj = new HierarchicalINIConfiguration(configFile);
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
	    iniConfObj.save();
	    return true;
	}
	public int getWatchDogTimer()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getInt("watchdogtimer");
	}
	public String getPluginConfigFile()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("plugin_config_file");
	}
	
	
	public String getAMPQLogExchange()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_log_exchange");
	}
	public boolean setAMPQLogExchange(String logExchange) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_log_exchange", logExchange);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQLogHost()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_log_host");
	}
	public boolean setAMPQLogHost(String host) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_log_host", host);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQLogUser()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_log_username");	    
	}
	public boolean setAMPQLogUser(String userName) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_log_username", userName);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQLogPassword()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_log_password");	    
	}
	public boolean setAMPQLogPassword(String password) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_log_password", password);
	    iniConfObj.save();
	    return true;
	}
	
	
	
	public String getAMPQControlExchange()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_control_exchange");
	}
	public boolean setAMPQControlExchange(String controlExchange) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_control_exchange", controlExchange);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQControlHost(String type)
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