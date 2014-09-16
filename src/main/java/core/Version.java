package core;


public class Version {

	public Version() 
	{
	}
	
	public String getVersion()
    {
		/*
		Package[] localPackages = Package.getPackages();
		for (int i = 0; i < localPackages.length; i++) {
			String p = localPackages[i].getImplementationVersion();
			System.err.println(p);
			
		}
		*/
		String version = getClass().getPackage().getImplementationVersion();
    	//Package p = getClass().getPackage();
    	//Package p = this.getClass().getPackage();
    	//String version = p.getImplementationVersion();
		return "agentEngine" + "." + version;
		
    }
	
}
