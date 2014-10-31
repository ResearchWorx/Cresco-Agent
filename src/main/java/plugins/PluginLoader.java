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
		
		File dir = new File(pluginPath);
		URL loadPath = dir.toURI().toURL();
		URL[] classUrl = new URL[]{loadPath};
		
		JarFile jarFile = new JarFile(pluginPath);
		Enumeration e = jarFile.entries();

		URL[] urls = { new URL("jar:file:" + pluginPath+"!/") };
		cl = URLClassLoader.newInstance(urls);
		
		sl = ServiceLoader.load(PluginInterface.class, cl);
			
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
