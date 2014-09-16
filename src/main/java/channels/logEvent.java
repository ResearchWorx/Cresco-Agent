package channels;

public class logEvent {

  private String eventType;
  private String eventMsg;
  private String eventSource;
  
  public logEvent(String eventType, String eventMsg)
  {
	  this.eventType = eventType;
	  this.eventMsg = eventMsg;
  }
    
  public String getEventType()
  {
	  return eventType;
  }
  public void setEventSource(String eventSource)
  {
	  this.eventSource = eventSource;
  }
  public String getEventSource()
  {
	  return eventSource;
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
	  return eventType + "," + eventSource + "," + eventMsg;
  }
  
  
}