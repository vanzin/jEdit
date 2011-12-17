package org.gjt.sp.util;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A model for a single String, which supports TextListeners. Simpler to use
 * than Document when all you need is to store a single string.
 * 
 * @author ezust
 * 
 */
public class StringModel
{

	String theText = null;

	LinkedList<TextListener> listeners = new LinkedList<TextListener>();

	public void addTextListener(TextListener tl)
	{
		listeners.add(tl);
	}

	void removeTextListener(TextListener tl)
	{
		listeners.remove(tl);
	}

	void fireTextChanged()
	{
		TextEvent te = new TextEvent(this, TextEvent.TEXT_VALUE_CHANGED);
		Iterator<TextListener> itr = listeners.iterator();
		while (itr.hasNext())
		{
			itr.next().textValueChanged(te);
		}
	}

	public String toString()
	{
		return theText;
	}

	public void setText(String newText)
	{
		this.theText = newText;
		fireTextChanged();
	}
}
