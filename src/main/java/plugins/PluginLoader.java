package plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

import shared.PluginInterface;

public class PluginLoader {

	private static URLClassLoader cl;
	private static ServiceLoader<PluginInterface> sl;
	
	public PluginLoader(String pluginPath) throws ClassNotFoundException, IOException
	{
		System.out.println("pl0");
		
		File dir = new File(pluginPath);
		System.out.println("pl1");
		
		URL loadPath = dir.toURI().toURL();
		System.out.println("pl2");
		
		URL[] classUrl = new URL[]{loadPath};
		System.out.println("pl3");
		
		JarFile jarFile = new JarFile(pluginPath);
		System.out.println("pl4");
		
		Enumeration e = jarFile.entries();
		System.out.println("pl5");
		
		URL[] urls = { new URL("jar:file:" + pluginPath+"!/") };
		System.out.println("pl6");
		
		cl = URLClassLoader.newInstance(urls);
		System.out.println("pl7");
		
		sl = ServiceLoader.load(PluginInterface.class, cl);
		System.out.println("pl8");
		
	}
	
	public PluginInterface getPluginInterface()
	{
		Iterator<PluginInterface> apit = sl.iterator();
        while (apit.hasNext())
        {
        	//this simply returns the first one found
        	return (PluginInterface)apit.next();
        }
        return null;
	}
	
	public static <T> T loadService(Class<T> api) {
		 
        T result = null;
 
        ServiceLoader<T> impl = ServiceLoader.load(api,cl);
        
        
        
        for (T loadedImpl : impl) {
            result = loadedImpl;
            if ( result != null ) break;
        }
 
        if ( result == null ) throw new RuntimeException(
            "Cannot find implementation for: " + api);
 
        return result;
 
    }
}
