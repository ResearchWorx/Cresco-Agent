package plugins;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.SubnodeConfiguration;

import shared.CmdEvent;
import shared.LogEvent;

public interface PluginInterface {

	   public boolean initialize(ConcurrentLinkedQueue<LogEvent> logQueue, SubnodeConfiguration configObj,String pluginSlot);
	   public String getName();
	   public String getVersion();
	   public CmdEvent executeCommand(CmdEvent command);
	   public void shutdown();
	   
	}


