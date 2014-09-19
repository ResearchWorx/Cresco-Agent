package core;


public class Version {

	public Version() 
	{
	}
	
	public String getVersion()
    {
		return getClass().getPackage().getImplementationVersion();
    }
	
}
