package shared;

public class LogEvent {

  private String eventType;
  private String eventMsg;
  private String eventSource;
  private String eventAgent;
  
  public LogEvent() //created for XML
  {
	  
  }
  public LogEvent(String eventType, String eventSource, String eventMsg)
  {
	  this.eventType = eventType;
	  this.eventSource = eventSource;
	  this.eventMsg = eventMsg;
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
  public String getEventSource()
  {
	  return eventSource;
  }
  public void setEventSource(String eventSource)
  {
	  this.eventSource = eventSource;
  }
  public String getEventAgent()
  {
	  return eventAgent;
  }
  public void setEventAgent(String eventAgent)
  {
	  this.eventAgent = eventAgent;
  }
 
}