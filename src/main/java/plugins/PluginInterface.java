package plugins;

import org.apache.commons.configuration.SubnodeConfiguration;

public interface PluginInterface {

	   public boolean initialize(SubnodeConfiguration config);
	   public String getName();
	   public String getVersion();
	   public String getCommandSet();
	}


