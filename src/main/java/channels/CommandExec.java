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
				
				List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList();
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
				ce.setCmdType(AgentEngine.config.getAgentName());
				ce.setCmdResult(sb.toString());
			}
			return ce;		
		}
		else if(ce.getCmdType().equals("execute")) //Execute and respond to execute commands
		{
			if(ce.getCmdArg().toLowerCase().startsWith("plugin")) //pass plugin commands directly to plugin
			{
				
				List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList();
				
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
				sb.append("show agent\t\t\t\t Shows Agent Info\n");
				sb.append("show plugins\t\t\t\t Shows Plugins Info\n");
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
        	return sb.toString().substring(0,sb.toString().length()-1);
		}
		else
		{
			sb.append("No Plugins Found!");
			return sb.toString();
		}
    }
}
