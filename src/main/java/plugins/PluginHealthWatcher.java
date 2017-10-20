package plugins;

import core.AgentEngine;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PluginHealthWatcher {

    public Timer timer;
    private int wdTimer = 5000;

    public PluginHealthWatcher() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new HealthWatcherTaskTask(), 0, wdTimer);
    }

    private class HealthWatcherTaskTask extends TimerTask {
        public void run() {

            Map<String, Plugin> map = AgentEngine.pluginMap;
            for (Map.Entry<String, Plugin> entry : map.entrySet())
            {
                if(entry.getValue().getActive()) {
                    long ts = entry.getValue().getWatchdogTS();
                    long timer = entry.getValue().getWatchdogTimer();
                    long maxtime = ts + (timer * 3);
                    boolean isHealthy = false;

                    if (maxtime > System.currentTimeMillis()) {
                        isHealthy = true;
                    }

                    if(!isHealthy) {
                        //plugin has failed
                        entry.getValue().setStatus_code(40);
                    }
                }

            }

        }
    }

}