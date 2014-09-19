package channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
			//System.out.println(ce.getCmdType() + " " + ce.getCmdArg() + " " + ce.getCmdResult());
			StringBuilder sb = new StringBuilder();
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
		else if(ce.getCmdType().toLowerCase().startsWith("plugin"))
		{
			List<String> pluginList = AgentEngine.pluginsconfig.getEnabledPluginList();
			for(String pluginName : pluginList)
			{
				if((AgentEngine.pluginMap.containsKey(pluginName)) && (pluginName.equals(ce.getCmdType())))
				{
					PluginInterface pi = AgentEngine.pluginMap.get(pluginName);
					String tmpstr = ce.getCmdArg();
					tmpstr = tmpstr.substring(tmpstr.indexOf("_") + 1);
				    ce.setCmdArg(tmpstr);
				    ce = pi.executeCommand(ce);				
				}
			}
		
		}
		else if(ce.getCmdArg().equals("show"))
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Help Show\n");
			sb.append("show agent\t\t Shows Agent Info\n");
			sb.append("show plugins\t\t Shows Plugins Info");
			sb.append("plugin/[number of plugin]\t\t Enter Plugin Mode");
			
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
		else if(ce.getCmdType().startsWith("plugin_")) 
		{
			//go to specific plugin
			if(AgentEngine.pluginMap.containsKey(ce.getCmdType()))
			{
				System.out.println(ce.getCmdType() + " " + ce.getCmdArg() + " " + ce.getCmdResult());
				ce = AgentEngine.pluginMap.get(ce.getCmdType()).executeCommand(ce);
			}
			else
			{
				ce.setCmdResult("Plugin Not found");	
			}
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
