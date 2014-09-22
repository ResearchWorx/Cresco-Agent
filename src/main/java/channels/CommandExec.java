package channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
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
		if(ce.getCmdType().equals("discover"))
		{
			StringBuilder sb = new StringBuilder();
			sb.append("help\n");
			sb.append("show\n");
			sb.append("show_agent\n");
			sb.append("show_plugins\n");
			
			List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList();
			
			for(String pluginName : pluginList)
			{
				if(AgentEngine.pluginMap.containsKey(pluginName))
				{
					PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
					CmdEvent tmpCe = pi.executeCommand(ce);
					BufferedReader bufReader = new BufferedReader(new StringReader(tmpCe.getCmdResult()));
					
					String line=null;
					while( (line=bufReader.readLine()) != null )
					{
						sb.append(pluginName + "_" + line + "\n");
					}
				}
			}
			
			ce.setCmdType(AgentEngine.config.getAgentName());
			ce.setCmdResult(sb.toString());
			
			return ce;		
		}
		else if(ce.getCmdArg().toLowerCase().startsWith("plugin"))
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
			
		
		}
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
		
		else
		{
			ce.setCmdResult("Agent Command [" + ce.getCmdType() + "] unknown");
		}
		return ce;
	}
	
	private String agent()
	{
		StringBuilder str = new StringBuilder();
		//str.append(logProducerActive 
		String settings = "logProducerActive=" + AgentEngine.logProducerActive + "\n";
		settings = settings + "watchDogActive=" + AgentEngine.watchDogActive;
		return settings;
	}
	public static String plugins() 
	{
		Map mp = AgentEngine.pluginMap;
		StringBuilder sb = new StringBuilder();
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            //System.out.println(pairs.getKey() + " = " + pairs.getValue());
            String pluginName = pairs.getKey().toString();
            PluginInterface pi = (PluginInterface)pairs.getValue();
            sb.append("Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion() + "\n");
            		
            //System.out.println("Plugin Configuration: [" + pluginName + "] Initialized: " + pi.getVersion());
    		//it.remove(); // avoids a ConcurrentModificationException
        }
        return sb.toString().substring(0,sb.toString().length()-1);
    }
}
