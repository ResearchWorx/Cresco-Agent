package shared;

public class CmdEvent {

  private String cmdType;
  private String cmdArg;
  private String cmdResult;
  
  public CmdEvent(String cmdType, String cmdArg)
  {
	  this.cmdType = cmdType;
	  this.cmdArg = cmdArg;
  }
    
  public String getCmdType()
  {
	  return cmdType;
  }
  public void setCmdType(String cmdType)
  {
	  this.cmdType = cmdType;
  }
  public String getCmdArg()
  {
	  return cmdArg;
  }
  public void setCmdArg(String cmdArg)
  {
	  this.cmdArg = cmdArg;
  }
  public String getCmdResult()
  {
	  return cmdResult;
  }
  public void setCmdResult(String cmdResult)
  {
	  this.cmdResult = cmdResult;
  }
  
}