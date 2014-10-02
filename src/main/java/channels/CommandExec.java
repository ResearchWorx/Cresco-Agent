package channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

import core.AgentEngine;
import plugins.PluginInterface;
import plugins.PluginLoader;
import shared.CmdEvent;

public class CommandExec {

	public CommandExec()
	{
		
	}
	
	public CmdEvent cmdExec(CmdEvent ce) throws IOException
	{
		//Respond with command-set from discover messages
		if(ce.getCmdType().equals("discover"))
		{
			StringBuilder sb = new StringBuilder();
			//send discover message to plugin if addressed to plugin
			if(ce.getCmdArg().toLowerCase().startsWith("plugin"))
			{
				
				List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList(1);
				for(String pluginName : pluginList)
				{
					
		        	if((AgentEngine.pluginMap.containsKey(pluginName)) && (pluginName.equals(ce.getCmdArg())))
					{
						PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
					    ce = pi.executeCommand(ce);
					    break;
					}
				}				
			
			}
			else //if not to plugin return agent discover message
			{
				sb.append("help\n");
				sb.append("show\n");
				sb.append("show_agent\n");
				sb.append("show_plugins\n");
				sb.append("show_address\n");
				sb.append("enable\n");
				
				List<String> pluginListDisabled = AgentEngine.pluginsconfig.getEnabledPluginList(0);
				if(pluginListDisabled.size() > 0)
				{
					sb.append("enable_plugin\n");
					for(String pluginName : pluginListDisabled)
					{
						sb.append("enable_plugin_" + pluginName + "\n");
					}
				}
				
				sb.append("disable\n");
				List<String> pluginListEnabled = AgentEngine.pluginsconfig.getEnabledPluginList(1);
				if(pluginListEnabled.size() > 0)
				{
					sb.append("disable_plugin\n");
					for(String pluginName : pluginListEnabled)
					{
						sb.append("disable_plugin_" + pluginName + "\n");
					}
				}
				ce.setCmdType(AgentEngine.config.getAgentName());
				ce.setCmdResult(sb.toString());
				
			}
			return ce;		
		}
		else if(ce.getCmdType().equals("execute")) //Execute and respond to execute commands
		{
			if(ce.getCmdArg().toLowerCase().startsWith("plugin")) //pass plugin commands directly to plugin
			{
				
				@SuppressWarnings("unchecked")
				List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList(1);
				
				String arg = ce.getCmdArg().substring(ce.getCmdArg().indexOf("_") + 1);
				String plugin = ce.getCmdArg().substring(0,ce.getCmdArg().indexOf("_"));
	        	
				for(String pluginName : pluginList)
				{
					
		        	if((AgentEngine.pluginMap.containsKey(pluginName)) && (pluginName.equals(plugin)))
					{
						PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
						ce.setCmdArg(arg);
					    ce = pi.executeCommand(ce);
					    break;
					}
				}				
			
			} //process agent commands here
			else if(ce.getCmdArg().equals("show") || ce.getCmdArg().equals("?") || ce.getCmdArg().equals("help"))
			{
				
				StringBuilder sb = new StringBuilder();
				sb.append("\nAgent " + AgentEngine.config.getAgentName() + " Help\n");
				sb.append("-\n");
				sb.append("help\t\t\t\t Shows This Message\n");
				sb.append("show agent\t\t\t\t Shows Agent Info\n");
				sb.append("show plugins\t\t\t\t Shows Plugins Info\n");
				sb.append("show address\t\t\t\t Shows IP address of local host\n");
				sb.append("enable  plugin [plugin/(id)]\t\t\t\t Enables a Plugin\n");
				sb.append("disable plugin [plugin/(id}]\t\t\t\t Disables a Plugin\n");
				sb.append("---");
				sb.append("plugin/[number of plugin]\t\t To access Plugin Info");
				
				ce.setCmdResult(sb.toString());
			}
			else if(ce.getCmdArg().equals("show_agent"))
			{
				ce.setCmdResult(agent());				
			}
			else if(ce.getCmdArg().equals("show_plugins"))
			{
				ce.setCmdResult(plugins());
			}
			else if(ce.getCmdArg().equals("show_address"))
			{
				
				StringBuilder sb = new StringBuilder();
				try {
					  InetAddress localhost = InetAddress.getLocalHost();
					  sb.append(" IP Addr: " + localhost.getHostAddress());
					  // Just in case this host has multiple IP addresses....
					  InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
					  if (allMyIps != null && allMyIps.length > 1) {
						  sb.append(" Full list of IP addresses:");
					    for (int i = 0; i < allMyIps.length; i++) {
					    	sb.append("    " + allMyIps[i]);
					    }
					  }
					} 
				catch (UnknownHostException e) 
				{
					sb.append(" (error retrieving server host name)");
				}

					try {
						sb.append("Full list of Network Interfaces:");
					  for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					    NetworkInterface intf = en.nextElement();
					    sb.append("    " + intf.getName() + " " + intf.getDisplayName());
					    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
					    	sb.append("        " + enumIpAddr.nextElement().toString());
					    }
					  }
					} catch (SocketException e) {
						sb.append(" (error retrieving network interface list)");
					}
					ce.setCmdResult(sb.toString());
					
			}
			else if(ce.getCmdArg().startsWith("enable"))
			{
				//enable_plugin_plugin/1
				String arg = ce.getCmdArg(); 
				//plugin_plugin/1
				String enable_arg = ce.getCmdArg().substring(ce.getCmdArg().indexOf("_") + 1);
				
				if(enable_arg.startsWith("plugin"))
				{
					String enable_plugin = enable_arg.substring(ce.getCmdArg().indexOf("_") + 1);
					ce.setCmdResult(AgentEngine.enablePlugin(enable_plugin,true));
				}
				else
				{
					ce.setCmdResult("Agent Enable Command [" + ce.getCmdArg() + "] unknown");
				}
				
			}
			else if(ce.getCmdArg().startsWith("disable"))
			{
				//disable_plugin_plugin/1
				String arg = ce.getCmdArg(); 
				//plugin_plugin/1
				String disable_arg = ce.getCmdArg().substring(ce.getCmdArg().indexOf("_") + 1);
				
				if(disable_arg.startsWith("plugin"))
				{
					String disable_plugin = disable_arg.substring(ce.getCmdArg().indexOf("_"));
					
					String result = AgentEngine.disablePlugin(disable_plugin,true);
					//System.out.println("Enable Result:" + result);
					ce.setCmdResult(result);							
					
				}
				else
				{
					ce.setCmdResult("Agent Disable Command [" + ce.getCmdArg() + "] unknown");
				}
				
			}
			
		}
		else //if command unknown report that is it unknown
		{
			ce.setCmdResult("Agent Command [" + ce.getCmdType() + "] unknown");
		}
		return ce;
	}
	
	private String agent()
	{
		StringBuilder str = new StringBuilder();
		String settings = "logProducerActive=" + AgentEngine.logProducerActive + "\n";
		settings = settings + "watchDogActive=" + AgentEngine.watchDogActive;
		return settings;
	}
	public static String plugins() //loop through known plugins on agent
	{
		StringBuilder sb = new StringBuilder();
        
		List<String> pluginListEnabled = AgentEngine.pluginsconfig.getEnabledPluginList(1);
		List<String> pluginListDisabled = AgentEngine.pluginsconfig.getEnabledPluginList(0);
		if((pluginListEnabled.size() > 0) || (pluginListDisabled.size() > 0))
		{
			if(pluginListEnabled.size() > 0)
			{
				sb.append("Enabled Plugins:\n");
			}
			for(String pluginName : pluginListEnabled)
			{
				if(AgentEngine.pluginMap.containsKey(pluginName))
				{
					PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
					sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName) + " Initialized: " + pi.getVersion() + "\n");
				}
			}
			if(pluginListDisabled.size() > 0)
			{
				sb.append("Disabled Plugins:\n");
			}
			for(String pluginName : pluginListDisabled)
			{
				sb.append("Plugin: [" + pluginName + "] Name: " + AgentEngine.pluginsconfig.getPluginName(pluginName)  + "\n");
			}		
		}
		else
		{
			sb.append("No Plugins Found!\n");
			
		}
		return sb.toString().substring(0,sb.toString().length()-1);
    }
	
	public static String plugins2() //loop through known plugins on agent
	{
		StringBuilder sb = new StringBuilder();
        
		if(AgentEngine.pluginMap.size() > 0)
		{
			Map mp = AgentEngine.pluginMap;
			Iterator it = mp.entrySet().iterator();
        	while (it.hasNext()) 
        	{
            	Map.Entry pairs = (Map.Entry)it.next();
            	String pluginName = pairs.getKey().toString();
            	PluginInterface pi = (PluginInterface)pairs.getValue();
            	sb.append("Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion() + "\n");
        	}
        	
		}
		else
		{
			sb.append("No Plugins Found!\n");
			
		}
		return sb.toString().substring(0,sb.toString().length()-1);
    }
	
	
}
