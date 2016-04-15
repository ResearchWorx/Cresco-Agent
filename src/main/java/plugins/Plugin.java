package plugins;

import com.researchworx.cresco.library.plugin.core.CPlugin;
import org.apache.commons.configuration.SubnodeConfiguration;
import com.researchworx.cresco.library.messaging.MsgEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class Plugin {
    private String name;
    private String version;
    private CPlugin plugin;

    public Plugin(String jarPath) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Manifest manifest = new JarInputStream(new FileInputStream(new File(jarPath))).getManifest();
        Attributes mainAttributess = manifest.getMainAttributes();
        this.name = mainAttributess.getValue("artifactId");
        this.version = mainAttributess.getValue("Implementation-Version");
        URLClassLoader loader = URLClassLoader.newInstance(new URL[] { new URL("jar:file:" + jarPath + "!/") });
        ServiceLoader<CPlugin> serviceLoader = ServiceLoader.load(CPlugin.class, loader);
        this.plugin = serviceLoader.iterator().next();
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean Start(ConcurrentLinkedQueue<MsgEvent> msgQueue, SubnodeConfiguration config, String region, String agent, String plugin) {
        try {
            return this.plugin.initialize(msgQueue, config, region, agent, plugin);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void Message(MsgEvent msg) {
        this.plugin.msgIn(msg);
    }

    public void Stop() {
        this.plugin.shutdown();
    }

    public void Dispose() {
        Stop();
        this.plugin = null;
    }
}
