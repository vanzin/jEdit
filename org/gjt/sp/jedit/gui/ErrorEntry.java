package org.gjt.sp.jedit.gui;

import java.util.Vector;

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

//{{{ ErrorEntry class
public class ErrorEntry
{
	String path;
	String[] messages;

	public ErrorEntry(String path, String messageProp, Object[] args)
	{
		this.path = path;

		String message = jEdit.getProperty(messageProp,args);
		if(message == null)
			message = "Undefined property: " + messageProp;

		Log.log(Log.ERROR,this,path + ":");
		Log.log(Log.ERROR,this,message);

		Vector<String> tokenizedMessage = new Vector<String>();
		int lastIndex = -1;
		for(int i = 0; i < message.length(); i++)
		{
			if(message.charAt(i) == '\n')
			{
				tokenizedMessage.addElement(message.substring(
					lastIndex + 1,i));
				lastIndex = i;
			}
		}

		if(lastIndex != message.length())
		{
			tokenizedMessage.addElement(message.substring(
				lastIndex + 1));
		}

		messages = new String[tokenizedMessage.size()];
		tokenizedMessage.copyInto(messages);
	}

	public boolean equals(Object o)
	{
		if(o instanceof ErrorEntry)
		{
			ErrorEntry e = (ErrorEntry)o;
			return e.path.equals(path);
		}
		else
			return false;
	}

	public String path()
	{
		return path;
	}

	public String[] messages()
	{
		return messages;
	}

	// This enables users to copy the error messages to
	// clipboard with Ctrl+C on Windows. But it works only
	// if the entry is selected by a mouse click.
	public String toString()
	{
		return path + ":\n" +
			TextUtilities.join(java.util.Arrays.asList(messages), "\n");
	}
} //}}}