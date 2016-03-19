package core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;

import shared.MsgEvent;
import shared.MsgEventType;
import shared.PluginInterface;

public class CommandExec {

	public CommandExec()
	{
		
	}
	
	public MsgEvent cmdExec(MsgEvent ce) throws IOException, ConfigurationException
	{
		
	 try
	 {
		 
		if(ce.getMsgType() == MsgEventType.CONFIG) //this is only for controller detection
		{
			//create for initial discovery
			if((ce.getMsgBody() != null) && (ce.getParam("set_region") != null) && (ce.getParam("set_agent") != null))
			{
				if(ce.getMsgBody().equals("comminit"))
				{
					AgentEngine.region = ce.getParam("set_region");
					AgentEngine.config.setRegionName(AgentEngine.region);
					AgentEngine.agent = ce.getParam("set_agent");
					AgentEngine.config.setAgentName(AgentEngine.agent);
					AgentEngine.isCommInit = true;
					System.out.println("region: " + AgentEngine.region);
					System.out.println("agent: " + AgentEngine.agent);
					return null;
				}
				
			}
			else if((ce.getMsgBody() != null) && (ce.getParam("dst_region") != null) && (ce.getParam("dst_agent") != null))
			{
				if((ce.getParam("dst_region").equals(AgentEngine.region)) && (ce.getParam("dst_agent").equals(AgentEngine.agent)))
				{
					if(ce.getMsgBody().equals("controllerenabled"))
					{
						AgentEngine.ControllerActive = true; //if we see a discover there is an active controller
						return null;
					}
					else if(ce.getMsgBody().equals("controllerdisabled"))					
					{
						AgentEngine.ControllerActive = false;
						AgentEngine.MsgInQueueEnabled = false;
						AgentEngine.MsgOutQueueEnabled = false;
						AgentEngine.MsgInQueueActive = false;
						AgentEngine.MsgOutQueueActive = false;			    	
					}
					
				}
			}
		
		}
		
		if(ce.getMsgPlugin() != null) //let plugins deal with their own messages
		{
			if(AgentEngine.pluginMap.containsKey(ce.getMsgPlugin()))
			{
				try
				{
					PluginInterface pi = AgentEngine.pluginMap.get(ce.getMsgPlugin());		
					
					pi.msgIn(ce); //send msg to plugin 
					
					return null; //plugin will deal with its own messages
				}
				catch(Exception ex)
				{
					System.out.println("Agent : CommandExec : Exec Plugin Cmd : " + ex.toString());
					MsgEvent ee = new MsgEvent(MsgEventType.ERROR,AgentEngine.region,AgentEngine.agent,null,"Agent : CommandExec : Exec Plugin Cmd : " + ex.toString());
					return ee;
				}
				
			}
			else
			{
				System.out.println("Agent : CommandExec : Why am I getting a message for a plugin that does not exist?");
				System.out.println(ce.getParamsString());
				MsgEvent ee = new MsgEvent(MsgEventType.ERROR,AgentEngine.region,AgentEngine.agent,null,"Agent : CommandExec : Why am I getting a message for a plugin that does not exist?");
				return ee;
			}
		}
		else //messages for the core agent.
		{
			if(ce.getMsgType() == MsgEventType.DISCOVER)
			{
				/*
				StringBuilder sb = new StringBuilder();
				
				sb.append("help\n");
				sb.append("show\n");
				sb.append("show_address\n");
				sb.append("show_agent\n");
				sb.append("show_name\n");
				sb.append("show_plugins\n");
				sb.append("show_version\n");
				sb.append("enable\n");
				
				ce.setMsgBody(sb.toString());
				*/
				ce.setMsgBody(AgentEngine.config.getPluginConfigString());
			}
			else if(ce.getMsgType() == MsgEventType.CONFIG) //Execute and respond to execute commands
			{
				if(ce.getParam("configtype").equals("pluginadd"))
				{
					Map<String,String> hm = AgentEngine.pluginsconfig.buildPluginMap(ce.getParam("configparams"));
					
					hm.remove("configtype");
					String plugin = AgentEngine.pluginsconfig.addPlugin(hm);
					ce.setParam("plugin", plugin);
					boolean isEnabled = AgentEngine.enablePlugin(plugin, false);
					if(!isEnabled)
					{
						ce.setMsgBody("Failed to Add Plugin:" + plugin);
						AgentEngine.pluginsconfig.removePlugin(plugin);
					}
					else
					{
						ce.setMsgBody("Added Plugin:" + plugin);
					}
				}
				else if(ce.getParam("configtype").equals("plugininventory"))
				{
					String pluginList = "";
					File jarLocation = new File(AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					String parentDirName = jarLocation.getParent(); // to get the parent dir name
					
					File folder = new File(parentDirName + "/plugins");
					if(folder.exists())
					{
					File[] listOfFiles = folder.listFiles();

					    for (int i = 0; i < listOfFiles.length; i++) 
					    {
					      if (listOfFiles[i].isFile()) 
					      {
					        System.out.println("Found Plugin: " + listOfFiles[i].getName());
					        pluginList = pluginList + listOfFiles[i].getName() + ",";
					      } 
					      
					    }
					    if(pluginList.length() > 0)
					    {
					    	pluginList = pluginList.substring(0, pluginList.length() - 1);
					    	System.out.println("pluginList=" + pluginList);
						    ce.setParam("pluginlist", pluginList);
						    ce.setMsgBody("There were " + listOfFiles.length + " plugins found.");
					    }
					    
					}
					else
					{
						ce.setMsgBody("No plugin directory exist to inventory");
					}
					    
					 
					
				}
				else if(ce.getParam("configtype").equals("plugindownload"))
				{
					try
					{
					String baseUrl = ce.getParam("pluginurl");
					if(!baseUrl.endsWith("/"))
					{
						baseUrl = baseUrl + "/";
					}
					
					URL website = new URL(baseUrl + ce.getParam("plugin"));
					ReadableByteChannel rbc = Channels.newChannel(website.openStream());
					/*
					File jarLocation = new File(AgentEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
					String parentDirName = jarLocation.getParent(); // to get the parent dir name
					String pluginDir = parentDirName + "/plugins";
					
					
					//check if directory exist, if not create it
					File pluginDirfile = new File(pluginDir);
					if (!pluginDirfile.exists()) {
						if (pluginDirfile.mkdir()) {
							System.out.println("Directory " + pluginDir + " didn't exist and was created.");
						} else {
							System.out.println("Directory " + pluginDir + " didn't exist and we failed to create it!");
						}
					}
					*/
					
					String pluginFile = AgentEngine.config.getPluginPath() + ce.getParam("plugin");
					boolean forceDownload = false;
					if(ce.getParam("forceplugindownload") != null)
					{
						forceDownload = true;
						System.out.println("Forcing Plugin Download");
					}
					
					File pluginFileObject = new File(pluginFile);
					if (!pluginFileObject.exists() || forceDownload) 
					{
						FileOutputStream fos = new FileOutputStream(AgentEngine.config.getPluginPath() + ce.getParam("plugin"));
						
						fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
						fos.close();
						
						ce.setMsgBody("Downloaded Plugin:" + ce.getParam("plugin"));
						System.out.println("Downloaded Plugin:" + ce.getParam("plugin"));
					}
					else
					{
						ce.setMsgBody("Plugin already exists:" + ce.getParam("plugin"));
						System.out.println("Plugin already exists:" + ce.getParam("plugin"));
					}
					
					}
					catch(Exception ex)
					{
						System.out.println("CommandExec : plugindownload " + ex.toString());
					}
				}
				else if(ce.getParam("configtype").equals("pluginremove"))
				{
					//disable if active
					AgentEngine.disablePlugin(ce.getParam("plugin"),true);
					//remove configuration
					AgentEngine.pluginsconfig.removePlugin(ce.getParam("plugin"));
					ce.setMsgBody("Removed Plugin:" + ce.getParam("plugin"));
					
				}
				else if(ce.getParam("configtype").equals("componentstate"))
				{
					if(ce.getMsgBody().equals("disabled"))
					{
						//System.exit(0);//shutdown agent
						AgentEngine.ds = new DelayedShutdown(5000l);
						ce.setMsgBody("Shutting Down");
					}
				}
				
			}
			else if(ce.getMsgType() == MsgEventType.EXEC) //Execute and respond to execute commands
			{
				if(ce.getParam("cmd").equals("show") || ce.getParam("cmd").equals("?") || ce.getParam("cmd").equals("help"))
				{
					
					StringBuilder sb = new StringBuilder();
					sb.append("\nAgent " + AgentEngine.config.getAgentName() + " Help\n");
					sb.append("-\n");
					sb.append("help\t\t\t\t Shows This Message\n");
					sb.append("show address\t\t\t\t Shows IP address of local host\n");
					sb.append("show agent\t\t\t\t Shows Agent Info\n");
					sb.append("show name\t\t\t\t Shows Name of Agent\n");
					sb.append("show plugins\t\t\t\t Shows Plugins Info\n");
					sb.append("show version\t\t\t\t Shows Agent Version\n");
					sb.append("enable  plugin [plugin/(id)]\t\t\t\t Enables a Plugin\n");
					sb.append("disable plugin [plugin/(id}]\t\t\t\t Disables a Plugin\n");
					sb.append("---");
					sb.append("plugin/[number of plugin]\t\t To access Plugin Info");
					
					ce.setMsgBody(sb.toString());
					
				}
				else if(ce.getParam("cmd").equals("show_name"))
				{
					ce.setMsgBody(AgentEngine.agent);
					
				}
				else if(ce.getParam("cmd").equals("show_plugins"))
				{
					ce.setMsgBody(AgentEngine.listPlugins());
				}
				else if(ce.getParam("cmd").equals("show_version"))
				{
					ce.setMsgBody(AgentEngine.agentVersion);			
				}
				else if(ce.getParam("cmd").equals("show_address"))
				{
					
					StringBuilder sb = new StringBuilder();
					try {
						  InetAddress localhost = InetAddress.getLocalHost();
						  sb.append(" IP Addr: " + localhost.getHostAddress() + "\n");
						  // Just in case this host has multiple IP addresses....
						  InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
						  if (allMyIps != null && allMyIps.length > 1) {
							  sb.append(" Full list of IP addresses:\n");
						    for (int i = 0; i < allMyIps.length; i++) {
						    	sb.append("    " + allMyIps[i]);
						    }
						  }
						} 
					catch (UnknownHostException e) 
					{
						sb.append(" (error retrieving server host name)\n");
					}

						try {
							sb.append("Full list of Network Interfaces:\n");
						  for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
						    NetworkInterface intf = en.nextElement();
						    sb.append("    " + intf.getName() + " " + intf.getDisplayName() + "\n");
						    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
						    	sb.append("        " + enumIpAddr.nextElement().toString() + "\n");
						    }
						  }
						} catch (SocketException e) {
							sb.append(" (error retrieving network interface list)\n");
						}
						
						ce.setMsgBody(sb.toString());
							
				}
				else if(ce.getParam("cmd").startsWith("enable"))
				{
					if(ce.getParam("plugin") != null)
					{
						boolean isEnabled = AgentEngine.enablePlugin(ce.getParam("plugin"),true);
						if(isEnabled)
						{
							ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " enabled");
						}
						else
						{
							ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " failed to enable");
						}
						
					}
					else
					{
						ce.setMsgBody("Agent Enable Command [" + ce.getParam("cmd") + "] unknown");
					}
				}
				else if(ce.getParam("cmd").startsWith("disable"))
				{
					if(ce.getParam("plugin") != null)
					{
						//ce.setMsgBody(AgentEngine.disablePlugin(ce.getParam("plugin"),true));
						boolean isDisabled = AgentEngine.enablePlugin(ce.getParam("plugin"),true);
						if(isDisabled)
						{
							ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " disabled");
						}
						else
						{
							ce.setMsgBody("Plugin:" + ce.getParam("plugin") + " failed to disable");
						}					
					}
					else
					{
						ce.setMsgBody("Agent Disable Command [" + ce.getParam("cmd") + "] unknown");
					}
				}
			}
			else //if command unknown report that is it unknown
			{
				String msg = "Agent Command [" + ce.getMsgType().toString() + "] unknown";
				ce.setMsgBody(msg);
			}
			
		}
		
		return ce;
	 }
	 catch(Exception ex)
	 {
		 MsgEvent ee = AgentEngine.clog.getError("Agent : CommandExec : Error" + ex.toString());
		 System.out.println("MsgType=" + ce.getMsgType().toString());
		 System.out.println("Region=" + ce.getMsgRegion() + " Agent=" + ce.getMsgAgent() + " plugin=" + ce.getMsgPlugin());
		 System.out.println("params=" + ce.getParamsString()); 
		 return ee;
	 }
	}

	private String agent()
	{
		StringBuilder str = new StringBuilder();
		String settings = "watchDogActive=" + AgentEngine.watchDogActive;
		return settings;
	}
	
	
}
