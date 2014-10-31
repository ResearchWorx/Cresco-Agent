package shared;

import java.util.concurrent.Callable;

import shared.MsgEvent;


public class MsgOut implements Callable 
{

	private MsgEvent me;
	public MsgOut(MsgEvent me) 
	{
		this.me = me;
	}
	
	public Boolean call() 
	{
		//return Integer.valueOf(word.length());
		System.out.println("hot damn");
		return true;
	}
}
