package channels;

import core.agentEngine;

public class logEvent {

  private String eventType;
  private String eventMsg;
  private String agentName;

  public logEvent(String eventType, String eventMsg)
  {
	  this.eventType = eventType;
	  this.eventMsg = eventMsg;
	  this.agentName = agentEngine.config.getAgentName();
  }
    
  public String getEventType()
  {
	  return eventType;
  }
  public void setEventType(String eventType)
  {
	  this.eventType = eventType;
  }
  
  public String getEventMsg()
  {
	  return eventMsg;
  }
  public void setEventMsg(String eventMsg)
  {
	  this.eventMsg = eventMsg;
  }
  public String getAgentName()
  {
	  return eventMsg;
  }
  @Override
  public String toString()
  {
	  return eventType + "," + agentName + "," + eventMsg;
  }
  
  
}
