/*
 * TextAreaInputHandler.java - Manages key bindings and executes actions
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

//{{{ Imports
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.gui.KeyEventWorkaround;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import org.gjt.sp.jedit.JEditBeanShellAction;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.ShortcutPrefixActiveEvent;
//}}}

/**
 * This class manage the key bindings and execute the actions binded on the
 * keyboard events for the standalone textarea.
 *
 * @author Matthieu Casanova
 * @version $Id: FoldHandler.java 5568 2006-07-10 20:52:23Z kpouer $
 */
public abstract class TextAreaInputHandler extends AbstractInputHandler<JEditBeanShellAction>
{
	private final TextArea textArea;

	//{{{ TextAreaInputHandler constructor
	protected TextAreaInputHandler(TextArea textArea)
	{
		this.textArea = textArea;
		bindings = currentBindings = new Hashtable();
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 * @param evt the keyboard event
	 * @param from the source of the event. Since this is the input handler of the textarea, it should always be 1
	 * @param global it is only true if the event comes from the DefaultKeyboardFocusManager
	 * @since 4.3pre7
	 */
	@Override
	public void processKeyEvent(KeyEvent evt, int from, boolean global)
	{
		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event                 : "
				+ toString(evt) + " from " + from);
		//	Log.log(Log.DEBUG,this,view+".isFocused()="+view.isFocused()+'.',new Exception());
		}

		evt = _preprocessKeyEvent(evt);
		if(evt == null)
			return;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event after workaround: "
				+ toString(evt) + " from " + from);
		}

		boolean focusOnTextArea = false;
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			// if the user pressed eg C+e n n in the
			// search bar we want focus to go back there
			// after the prefix is done


			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else if(isPrefixActive() || textArea.hasFocus())
			{
				processKeyEventKeyStrokeHandling(evt,from,"type ",global);
			}


			processKeyEventSub(focusOnTextArea);

			break;
		case KeyEvent.KEY_PRESSED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyPressed(evt);
			else if(KeyEventWorkaround.isBindable(evt.getKeyCode()))
			{
				processKeyEventKeyStrokeHandling(evt,from,"press",global);

				processKeyEventSub(focusOnTextArea);

			}
			break;
		case KeyEvent.KEY_RELEASED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyReleased(evt);
			break;
		}
	} //}}}

	//{{{ _preprocessKeyEvent() method
	/**
	 * This method returns if the keyboard event can be handled or not.
	 *
	 * @param evt the keyboard event
	 * @return null if the keyboard event cannot be handled, or the keyboard event itself
	 * otherwise
	 */
	private KeyEvent _preprocessKeyEvent(KeyEvent evt)
	{
		if(evt.isConsumed())
			return null;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event (preprocessing) : "
					+ toString(evt));
		}

		return KeyEventWorkaround.processKeyEvent(evt);
	} //}}}

	//{{{ processKeyEventSub() method
	private void processKeyEventSub(boolean focusOnTextArea)
	{
		// this is a weird hack.
		// we don't want C+e a to insert 'a' in the
		// search bar if the search bar has focus...
		if (isPrefixActive() && focusOnTextArea)
		{
			textArea.requestFocus();
		}
	} //}}}

	//{{{ getAction() method
	protected abstract JEditBeanShellAction getAction(String action);
	//}}}

	//{{{ invokeAction() method
	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 * @since jEdit 4.2pre1
	 */
	@Override
	public void invokeAction(String action)
	{
		invokeAction(getAction(action));
	} //}}}

	//{{{ invokeAction() method
	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 */
	@Override
	public void invokeAction(JEditBeanShellAction action)
	{
		JEditBuffer buffer = textArea.getBuffer();

		/* if(buffer.insideCompoundEdit())
			buffer.endCompoundEdit(); */

		// remember the last executed action
		if(!action.noRememberLast())
		{
			if(lastAction == action)
				lastActionCount++;
			else
			{
				lastAction = action;
				lastActionCount = 1;
			}
		}

		// remember old values, in case action changes them
		int _repeatCount = repeatCount;

		// execute the action
		if(action.noRepeat() || _repeatCount == 1)
			action.invoke(textArea);
		else
		{
			try
			{
				buffer.beginCompoundEdit();

				for(int i = 0; i < _repeatCount; i++)
					action.invoke(textArea);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}

		// If repeat was true originally, clear it
		// Otherwise it might have been set by the action, etc
		if(_repeatCount != 1)
		{
			// first of all, if this action set a
			// readNextChar, do not clear the repeat
			if(readNextChar != null)
				return;

			repeatCount = 1;
		}
	} //}}}

	//{{{ handleKey() method
	/**
	 * Handles the given keystroke.
	 * @param keyStroke The key stroke
	 * @param dryRun only calculate the return value, do not have any other effect
	 * @since jEdit 4.2pre5
	 */
	@Override
	public boolean handleKey(KeyEventTranslator.Key keyStroke,boolean dryRun)
	{
		char input = '\0';
		if(keyStroke.modifiers == null
			|| keyStroke.modifiers.equals("S"))
		{
			switch(keyStroke.key)
			{
			case '\n':
			case '\t':
				input = (char)keyStroke.key;
				break;
			default:
				input = keyStroke.input;
				break;
			}
		}

		if(readNextChar != null)
		{
			if(input != '\0')
			{
				if (!dryRun)
				{
					setCurrentBindings(bindings);
					invokeReadNextChar(input);
					repeatCount = 1;
				}
				return true;
			}
			else
			{
				if (!dryRun)
				{
					readNextChar = null;
				}
			}
		}

		Object o = currentBindings.get(keyStroke);
		if(o == null)
		{
			if (!dryRun)
			{
				// Don't beep if the user presses some
				// key we don't know about unless a
				// prefix is active. Otherwise it will
				// beep when caps lock is pressed, etc.
				if(currentBindings != bindings)
				{
					Toolkit.getDefaultToolkit().beep();
					// F10 should be passed on, but C+e F10
					// shouldn't
					repeatCount = 1;
					setCurrentBindings(bindings);
				}
				else if(input != '\0')
				{
					if (!keyStroke.isFromGlobalContext())
					{ // let user input be only local
						userInput(input);
					}
				}
				else
				{
					// this is retarded. excuse me while I drool
					// and make stupid noises
					if(KeyEventWorkaround.isNumericKeypad(keyStroke.key))
						KeyEventWorkaround.numericKeypadKey();
				}
				sendShortcutPrefixOff();
			}
		}
		else if(o instanceof Hashtable)
		{
			if (!dryRun)
			{
				setCurrentBindings((Hashtable)o);
				ShortcutPrefixActiveEvent.firePrefixStateChange(currentBindings, true);
				shortcutOn = true;
			}
			return true;
		}
		else if(o instanceof String)
		{
			if (!dryRun)
			{
				setCurrentBindings(bindings);
				sendShortcutPrefixOff();
				invokeAction((String)o);
			}
			return true;
		}
		else if(o instanceof JEditBeanShellAction)
		{
			if (!dryRun)
			{
				setCurrentBindings(bindings);
				sendShortcutPrefixOff();
				invokeAction((JEditBeanShellAction)o);
			}
			return true;
		}
		if (!dryRun)
		{
			sendShortcutPrefixOff();
		}
		return false;
	} //}}}

	//{{{ userInput() method
	protected void userInput(char ch)
	{
		lastActionCount = 0;


		if(repeatCount == 1)
			textArea.userInput(ch);

		repeatCount = 1;
	} //}}}

	//{{{ invokeReadNextChar() method
	protected void invokeReadNextChar(char ch)
	{
		String charStr = StandardUtilities.charsToEscapes(String.valueOf(ch));

		// this might be a bit slow if __char__ occurs a lot
		int index;
		while((index = readNextChar.indexOf("__char__")) != -1)
		{
			readNextChar = readNextChar.substring(0,index)
				+ '\'' + charStr + '\''
				+ readNextChar.substring(index + 8);
		}
		readNextChar = null;
	} //}}}
}
