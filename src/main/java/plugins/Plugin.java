package plugins;

import com.researchworx.cresco.library.plugin.core.CPlugin;
import org.apache.commons.configuration.SubnodeConfiguration;
import com.researchworx.cresco.library.messaging.MsgEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.*;

public class Plugin {
    private String name;
    private String version;
    private CPlugin plugin;
    private URLClassLoader loader;

    public Plugin(String jarPath) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Manifest manifest = new JarInputStream(new FileInputStream(new File(jarPath))).getManifest();
        Attributes mainAttributess = manifest.getMainAttributes();
        name = mainAttributess.getValue("artifactId");
        version = mainAttributess.getValue("Implementation-Version");
        loader = URLClassLoader.newInstance(new URL[] { new URL("jar:file:" + jarPath + "!/") });
        ServiceLoader<CPlugin> serviceLoader = ServiceLoader.load(CPlugin.class, loader);
        plugin = serviceLoader.iterator().next();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean Start(ConcurrentLinkedQueue<MsgEvent> msgQueue, SubnodeConfiguration config, String region, String agent, String pluginID) {
        try {
            return plugin.initialize(msgQueue, config, region, agent, pluginID);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void Message(MsgEvent msg) {
        plugin.msgIn(msg);
    }

    public void Stop() {
        plugin.shutdown();
    }

    public void Dispose() {
        Stop();
        plugin = null;
        try {
            loader.close();
        } catch (IOException e) { }
    }
}
