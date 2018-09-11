package plugins;

import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.plugin.core.CPlugin;
import core.AgentEngine;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.BlockingQueue;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Plugin {
    private static final Logger logger = LoggerFactory.getLogger("Plugins");
    private String pluginID;
    private String jarPath;
    private String name;
    private String version;
    private Object instance;
    private boolean active = false;
    private int status_code = 3;
    private String status_desc = "Plugin Configuration Created";
    private long watchdog_ts = 0;
    private long watchdogtimer = 0;
    private long runtime = 0;
    //private String inode_id;
    //private String resource_id;

    /*
    status_code = 3; //plugin init
    status_code = 8; //plugin disabled
    status_code = 10; //started and working
    status_code = 40; //WATCHDOG check failed with agent
    status_code = 80; //failed to start
    status_code = 90; //Exception on timeout shutdown
    status_code = 91; //Exception on timeout verification to confirm down
    status_code = 92; //timeout on disable verification
     */


    public Plugin(String pluginID, String jarPath) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.pluginID = pluginID;
        this.jarPath = jarPath;
        Manifest manifest = new JarInputStream(new FileInputStream(new File(this.jarPath))).getManifest();
        Attributes mainAttributess = manifest.getMainAttributes();
        name = mainAttributess.getValue("artifactId");
        version = mainAttributess.getValue("Implementation-Version");
        URL url = new File(jarPath).toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[] {new File(jarPath).toURI().toURL()}, this.getClass().getClassLoader());
        ResourceFinder finder = new ResourceFinder("META-INF/services", loader, url);
        Class<?> plugin = finder.findImplementation(CPlugin.class);
        instance = plugin.newInstance();
    }

    public void PreStart() {
        String methodName = "preStart";
        try {
            Method method = instance.getClass().getMethod(methodName);
            try {
                method.invoke(instance);
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target [{}]", pluginID, methodName, e.getMessage());
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
        }
    }

    public boolean Start(BlockingQueue<MsgEvent> msgQueue, SubnodeConfiguration config, String region, String agent, String pluginID) {
        String methodName = "initialize";
        try {
            Method method = instance.getClass().getSuperclass().getDeclaredMethod(methodName, BlockingQueue.class, SubnodeConfiguration.class, String.class, String.class, String.class);
            try {
                active = (boolean) method.invoke(instance, msgQueue, config, region, agent, pluginID);
                //status = 4;
                if(!AgentEngine.isCommInit) {
                    status_code = 10;
                } else {
                    try {
                        int count = 0;
                        while ((status_code != 10) && (count < 120)) {
                            logger.info("Waiting on enable for plugin {} current status_code: {}", pluginID, status_code);
                            Thread.sleep(500);
                            count++;
                        }
                    } catch(Exception ex) {
                        status_code = 80; //failed to start
                    }
                }
                return active;
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
                return false;
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
                return false;
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target\n{}", pluginID, methodName, ExceptionUtils.getStackTrace(e));
                return false;
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
            return false;
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
            return false;
        }
    }

    public void PostStart() {
        String methodName = "postStart";
        try {
            Method method = instance.getClass().getMethod(methodName);
            try {
                method.invoke(instance);
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target [{}]", pluginID, methodName, e.getMessage());
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
        }
    }

    public void Message(MsgEvent msg) {
        String methodName = "msgIn";
        try {
            Method method = instance.getClass().getMethod(methodName, MsgEvent.class);
            try {
                method.invoke(instance, msg);
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target [{}]", pluginID, methodName, e.getMessage());
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
        }
    }

    public void PreStop() {
        String methodName = "preShutdown";
        try {
            Method method = instance.getClass().getMethod(methodName);
            try {
                method.invoke(instance);
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target [{}]", pluginID, methodName, e.getMessage());
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
        }
    }

    public void Stop() {
        String methodName = "shutdown";
        boolean isStopped = false;
        try {
            Method method = instance.getClass().getSuperclass().getDeclaredMethod(methodName);
            try {
                method.invoke(instance);
                isStopped = true;
            } catch (IllegalArgumentException e) {
                logger.error("Plugin [{}] Illegal Argument Exception: [{}] method invoked using illegal arguments [{}]", pluginID, methodName, e.getMessage());
            } catch (IllegalAccessException e) {
                logger.error("Plugin [{}] Illegal Access Exception: [{}] method invoked without access [{}]", pluginID, methodName, e.getMessage());
            } catch (InvocationTargetException e) {
                logger.error("Plugin [{}] Invocation Exception: [{}] method invoked on incorrect target [{}]", pluginID, methodName, e.getMessage());
            }
        } catch (SecurityException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method security level exceeded [{}]", pluginID, methodName, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error("Plugin [{}] Method Exception: [{}] method not found [{}]", pluginID, methodName, e.getMessage());
        }

        if(isStopped) {

            try {
                int count = 0;
                while((status_code != 8) && (count < 10)) { //if plugin can't be confirmed down in 5 sec fail
                    logger.debug("Waiting on disable for plugin {} current status_code: {}", pluginID, status_code);
                    logger.info("Waiting on disable for plugin {} current status_code: {}", pluginID, status_code);

                    Thread.sleep(500);
                    count++;
                }
                if(count == 10) {
                    status_code = 92; //timeout on disable verification
                }
            } catch (Exception ex) {
                status_code = 91; //Exception on timeout verification to confirm down
            }
        } else {
            status_code = 90; //Exception on timeout shutdown
        }
    }

    public void Dispose() {
        Stop();
        instance = null;
        version = null;
        name = null;
        jarPath = null;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public long getWatchdogTimer() {
        return watchdogtimer;
    }

    public void setWatchDogTS(long watchdog_ts) {
        this.watchdog_ts = watchdog_ts;
    }

    public long getWatchdogTS() {
        return watchdog_ts;
    }

    public void setWatchDogTimer(long watchdogtimer) {
        this.watchdogtimer = watchdogtimer;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public int getStatus_code() {return status_code;}

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    public void setStatus_desc(String status_desc) {
        this.status_desc = status_desc;
    }

    public String getStatus_desc() {return status_desc;}


    /*
    public String getInodeId() {return inode_id;}

    public void setInodeId(String inode_id) {
        this.inode_id = inode_id;
    }

    public String getResourceId() {return resource_id;}

    public void setResourceId(String resource_id) {
        this.resource_id = resource_id;
    }
    */

    public void setActive(Boolean isActive) {
        active = isActive;
    }

    public boolean getActive() {
        return active;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"id\":\"");
        sb.append(pluginID);
        sb.append("\",");

        sb.append("\"jar\":\"");
        sb.append(jarPath);
        sb.append("\",");

        sb.append("\"name\":\"");
        sb.append(name);
        sb.append("\",");

        sb.append("\"version\":\"");
        sb.append(version);
        sb.append("\",");

        sb.append("\"active\":");
        sb.append(active);

        sb.append("}");
        return sb.toString();
    }
}
