package plugins;

import core.AgentEngine;

import java.util.*;

public class PluginHealthWatcher {

    public Timer timer;
    private int wdTimer = 5000;

    public PluginHealthWatcher() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new HealthWatcherTaskTask(), wdTimer, wdTimer);
    }

    private class HealthWatcherTaskTask extends TimerTask {
        public void run() {

            List<String> pluginList = new ArrayList();

            for (String key : AgentEngine.pluginMap.keySet()) {
                pluginList.add(key);
            }

            for(String key : pluginList) {
                Plugin checkPlugin = AgentEngine.pluginMap.get(key);
                if(checkPlugin.getActive()) {
                    long ts = checkPlugin.getWatchdogTS();
                    long timer = checkPlugin.getWatchdogTimer();
                    long maxtime = ts + (timer * 3);
                    boolean isHealthy = false;

                    if (maxtime > System.currentTimeMillis()) {
                        isHealthy = true;
                    }

                    if(!isHealthy) {
                        //plugin has failed
                        AgentEngine.pluginMap.get(key).setStatus_code(40);
                        AgentEngine.getCoreLogger().error("Plugin {} has failed WATCHDOG check! Status Code {}", key, AgentEngine.pluginMap.get(key).getStatus_code());
                    } else {
                        //recover from timeout
                        if(AgentEngine.pluginMap.get(key).getStatus_code() == 40) {
                            AgentEngine.pluginMap.get(key).setStatus_code(10);
                        }
                    }

                    /*else {
                        AgentEngine.getCoreLogger().info("Agent Status: " + AgentEngine.isActive + " comminit:" + AgentEngine.isCommInit);
                        AgentEngine.getCoreLogger().info("Plugin {} passed WATCHDOG check ", key);
                    }
                    */
                }

            }

        }
    }

}