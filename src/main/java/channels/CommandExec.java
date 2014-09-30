package channels;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import core.AgentEngine;
import plugins.PluginInterface;
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
				sb.append("enable\n");
				sb.append("enable_plugin\n");
				sb.append("disable\n");
				sb.append("disable_plugin");
				
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
			else if(ce.getCmdArg().startsWith("enable_plugin"))
			{
				//String plugin = ce.getCmdArg().substring(ce.getCmdArg().indexOf("enable_plugin") + 1);
				//System.out.println("Plugin Name0=" + plugin);
				System.out.println("Plugin Name0=" + ce.getCmdArg());
				
				
				//ce.setCmdResult(plugins());
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
	
	public static String enablePlugin(String plugin) //loop through known plugins on agent
	{
		StringBuilder sb = new StringBuilder();
		List<String> pluginListDisabled = AgentEngine.pluginsconfig.getEnabledPluginList(0);
		
		if(pluginListDisabled.size() > 0)
		{
			for(String pluginName : pluginListDisabled)
			{
				if(pluginName.equals(plugin))
				{
					sb.append("Plugin: [" + plugin + "] Enabled");
					break;
				}
			}
			sb.append("No Configuration Found for Plugin: [" + plugin + "]");
		}
		else
		{
			sb.append("No Plugins Found!");
		}
		return sb.toString();
    }
}
