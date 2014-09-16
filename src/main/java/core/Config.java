package core;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class Config {

	private HierarchicalINIConfiguration iniConfObj;
	
	public Config(String configFile) throws ConfigurationException
	{
	    //String iniFile = "Cresco-Agent-Netflow.ini";
	    
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
	public int getInstances()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getInt("instances");
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
	public String getAMPQHost()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_host");
	}
	public boolean setAMPQHost(String host) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_host", host);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQUser()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_username");	    
	}
	public boolean setAMPQUser(String userName) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_username", userName);
	    iniConfObj.save();
	    return true;
	}
	public String getAMPQPassword()
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
		return sObj.getString("ampq_password");	    
	}
	public boolean setAMPQPassword(String password) throws ConfigurationException
	{
		SubnodeConfiguration sObj = iniConfObj.getSection("general");
	    sObj.setProperty("ampq_password", password);
	    iniConfObj.save();
	    return true;
	}
	
}