/*
 * AbstractInputHandler.java - Manages key bindings and executes actions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.gjt.sp.jedit.input;

import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.util.Log;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * @author Matthieu Casanova
 * @version $Id: FoldHandler.java 5568 2006-07-10 20:52:23Z kpouer $
 */
public abstract class AbstractInputHandler
{
	protected int lastActionCount;
	/** This listener will receive keyboard events if it is not null. */
	protected KeyListener keyEventInterceptor;
	protected String readNextChar;
	protected int repeatCount;

	protected static final int REPEAT_COUNT_THRESHOLD = 20;

	//{{{ AbstractInputHandler constructor
	public AbstractInputHandler()
	{
		repeatCount = 1;
	} //}}}

	//{{{ getLastActionCount() method
	/**
	 * Returns the number of times the last action was executed.
	 * @since jEdit 2.5pre5
	 */
	public int getLastActionCount()
	{
		return lastActionCount;
	} //}}}

	//{{{ resetLastActionCount() method
	/**
	 * Resets the last action count. This should be called when an
	 * editing operation that is not an action is invoked, for example
	 * a mouse click.
	 * @since jEdit 4.0pre1
	 */
	public void resetLastActionCount()
	{
		lastActionCount = 0;
	} //}}}

	//{{{ getKeyEventInterceptor() method
	public KeyListener getKeyEventInterceptor()
	{
		return keyEventInterceptor;
	} //}}}

	//{{{ setKeyEventInterceptor() method
	/**
	 * Sets the listener that will handle all key events in this
	 * view. For example, the complete word command uses this so
	 * that all key events are passed to the word list popup while
	 * it is visible.
	 * @param keyEventInterceptor the KeyListener that will receive the events
	 */
	public void setKeyEventInterceptor(KeyListener keyEventInterceptor)
	{
		this.keyEventInterceptor = keyEventInterceptor;
	} //}}}

	//{{{ isPrefixActive() method
	/**
	 * Returns if a prefix key has been pressed.
	 */
	public boolean isPrefixActive()
	{
		return readNextChar != null;
	} //}}}

	//{{{ handleKey() method
	/**
	 * Handles a keystroke.
	 * @param keyStroke The key stroke.
	 * @param dryRun only calculate the return value, do not have any other effect
	 * @return true if the input could be handled.
	 * @since jEdit 4.3pre7
	 */
	public abstract boolean handleKey(KeyEventTranslator.Key keyStroke,boolean dryRun);
	//}}}

	//{{{ processKeyEvent() method
	public abstract void processKeyEvent(KeyEvent evt, int from, boolean global); 
	//}}}

	//{{{ processKeyEventKeyStrokeHandling() method
	protected void processKeyEventKeyStrokeHandling(KeyEvent evt,int from,String mode,boolean global)
	{
		KeyEventTranslator.Key keyStroke = KeyEventTranslator.translateKeyEvent2(evt);

		if(keyStroke != null)
		{
			keyStroke.setIsFromGlobalContext(global);
			if(Debug.DUMP_KEY_EVENTS)
			{
				Log.log(Log.DEBUG,this,"Translated (key "+mode+"): "+keyStroke+" from "+from);
			}
			boolean consumed = false;
			if(handleKey(keyStroke,keyStroke.isPhantom())) {
				evt.consume();

				consumed = true;
			}
			if(Debug.DUMP_KEY_EVENTS)
			{
				Log.log(Log.DEBUG,this,"Translated (key "+mode+"): "+keyStroke+" from "+from+": consumed="+consumed+'.');
			}
		}
	} //}}}
}
