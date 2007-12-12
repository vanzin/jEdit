/*
 * DefaultInputHandler.java - Default implementation of an input handler
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.event.InputEvent;
import java.awt.Toolkit;
import java.util.Hashtable;
import org.gjt.sp.jedit.*;
//}}}

/**
 * The default input handler. It maps sequences of keystrokes into actions
 * and inserts key typed events into the text area.
 * @author Slava Pestov
 * @version $Id$
 */
public class DefaultInputHandler extends InputHandler
{
	//{{{ DefaultInputHandler constructor
	/**
	 * Creates a new input handler with no key bindings defined.
	 * @param view The view
	 * @param bindings An explicitly-specified set of key bindings,
	 * must not be null.
	 * @since jEdit 4.3pre1
	 */
	public DefaultInputHandler(View view, Hashtable bindings)
	{
		super(view);

		if(bindings == null)
			throw new NullPointerException();
		this.bindings = this.currentBindings = bindings;
	} //}}}

	//{{{ DefaultInputHandler constructor
	/**
	 * Creates a new input handler with no key bindings defined.
	 * @param view The view
	 */
	public DefaultInputHandler(View view)
	{
		this(view,new Hashtable());
	} //}}}

	//{{{ DefaultInputHandler constructor
	/**
	 * Creates a new input handler with the same set of key bindings
	 * as the one specified. Note that both input handlers share
	 * a pointer to exactly the same key binding table; so adding
	 * a key binding in one will also add it to the other.
	 * @param copy The input handler to copy key bindings from
	 * @param view The view
	 */
	public DefaultInputHandler(View view, DefaultInputHandler copy)
	{
		this(view,copy.bindings);
	} //}}}

	//{{{ isPrefixActive() method
	/**
	 * Returns if a prefix key has been pressed.
	 */
	@Override
	public boolean isPrefixActive()
	{
		return bindings != currentBindings
			|| super.isPrefixActive();
	} //}}}

	//{{{ setCurrentBindings() method
	@Override
	public void setCurrentBindings(Hashtable bindings)
	{
		view.getStatus().setMessage((String)bindings.get(PREFIX_STR));
		currentBindings = bindings;
	} //}}}

	//{{{ handleKey() method
	/**
	 * Handles the given keystroke.
	 * @param keyStroke The key stroke
	 * @param dryRun only calculate the return value, do not have any other effect
	 * @since jEdit 4.2pre5
	 */
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
					view.getStatus().setMessage(null);
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
		else if(o instanceof EditAction)
		{
			if (!dryRun)
			{
				setCurrentBindings(bindings);
				sendShortcutPrefixOff();
				invokeAction((EditAction)o);
			}
			return true;
		}
		if (!dryRun)
		{
			sendShortcutPrefixOff();
		}
		return false;
	} //}}}
	
	//{{{ getSymbolicModifierName() method
	/**
	 * Returns a the symbolic modifier name for the specified Java modifier
	 * flag.
	 *
	 * @param mod A modifier constant from <code>InputEvent</code>
	 *
	 * @since jEdit 4.1pre3
	 */
	public static char getSymbolicModifierName(int mod)
	{
		return KeyEventTranslator.getSymbolicModifierName(mod);
	} //}}}

	//{{{ getModifierString() method
	/**
	 * Returns a string containing symbolic modifier names set in the
	 * specified event.
	 *
	 * @param evt The event
	 *
	 * @since jEdit 4.1pre3
	 */
	public static String getModifierString(InputEvent evt)
	{
		return KeyEventTranslator.getModifierString(evt);
	} //}}}
}
