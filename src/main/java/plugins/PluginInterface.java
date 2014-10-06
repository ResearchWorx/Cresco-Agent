package plugins;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.SubnodeConfiguration;

import shared.CmdEvent;
import shared.LogEvent;

public interface PluginInterface {

	   public boolean initialize(ConcurrentLinkedQueue<LogEvent> logQueue, SubnodeConfiguration configObj,String pluginSlot, String agentName);
	   public String getName();
	   public String getVersion();
	   public CmdEvent incomingCommand(CmdEvent command);
	   public LogEvent incomingLog(LogEvent log);
	   
	   public void shutdown();
	   
	}


