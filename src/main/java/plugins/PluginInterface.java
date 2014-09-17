package plugins;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.SubnodeConfiguration;

import channels.LogEvent;

public interface PluginInterface {

	   public boolean initialize(ConcurrentLinkedQueue<LogEvent> logQueue, SubnodeConfiguration config);
	   public String getName();
	   public String getVersion();
	   public String getCommandSet();
	}


