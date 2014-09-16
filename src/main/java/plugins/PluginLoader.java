package plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

public class PluginLoader {

	private static URLClassLoader cl;
	private static ServiceLoader<PluginInterface> sl;
	
	public PluginLoader(String pluginPath) throws ClassNotFoundException, IOException
	{
		
		File dir = new File(pluginPath);
		URL loadPath = dir.toURI().toURL();
		URL[] classUrl = new URL[]{loadPath};
		//cl = new URLClassLoader(classUrl);
		
		JarFile jarFile = new JarFile(pluginPath);
		Enumeration e = jarFile.entries();

		URL[] urls = { new URL("jar:file:" + pluginPath+"!/") };
		cl = URLClassLoader.newInstance(urls);
		
		
		sl = ServiceLoader.load(PluginInterface.class, cl);
		
	}
	
	
	public String getPluginName() 
	{
		String str = null;
		Iterator<PluginInterface> apit = sl.iterator();
        while (apit.hasNext())
        {
        	str = apit.next().getName();
        }
        return str;
		    
	}
	
	public String getPluginVersion() 
	{
		String str = null;
		Iterator<PluginInterface> apit = sl.iterator();
        while (apit.hasNext())
        {
        	str = apit.next().getVersion();
        }
        return str;
		    
	}
	
	public String getPluginCommandSet() 
	{
		String str = null;
		Iterator<PluginInterface> apit = sl.iterator();
        while (apit.hasNext())
        {
        	str = apit.next().getCommandSet();
        }
        return str;
		    
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
