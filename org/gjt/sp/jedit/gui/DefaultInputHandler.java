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
import javax.swing.KeyStroke;
import java.awt.event.*;
import java.awt.Toolkit;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
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
	 */
	public DefaultInputHandler(View view)
	{
		super(view);

		bindings = currentBindings = new Hashtable();
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
		super(view);

		bindings = currentBindings = copy.bindings;
	} //}}}

	//{{{ addKeyBinding() method
	/**
	 * Adds a key binding to this input handler. The key binding is
	 * a list of white space separated key strokes of the form
	 * <i>[modifiers+]key</i> where modifier is C for Control, A for Alt,
	 * or S for Shift, and key is either a character (a-z) or a field
	 * name in the KeyEvent class prefixed with VK_ (e.g., BACK_SPACE)
	 * @param keyBinding The key binding
	 * @param action The action
	 * @since jEdit 4.2pre1
	 */
	public void addKeyBinding(String keyBinding, String action)
	{
		_addKeyBinding(keyBinding,(Object)action);
	} //}}}

	//{{{ addKeyBinding() method
	/**
	 * Adds a key binding to this input handler. The key binding is
	 * a list of white space separated key strokes of the form
	 * <i>[modifiers+]key</i> where modifier is C for Control, A for Alt,
	 * or S for Shift, and key is either a character (a-z) or a field
	 * name in the KeyEvent class prefixed with VK_ (e.g., BACK_SPACE)
	 * @param keyBinding The key binding
	 * @param action The action
	 */
	public void addKeyBinding(String keyBinding, EditAction action)
	{
		_addKeyBinding(keyBinding,(Object)action);
	} //}}}

	//{{{ removeKeyBinding() method
	/**
	 * Removes a key binding from this input handler. This is not yet
	 * implemented.
	 * @param keyBinding The key binding
	 */
	public void removeKeyBinding(String keyBinding)
	{
		Hashtable current = bindings;

		StringTokenizer st = new StringTokenizer(keyBinding);
		while(st.hasMoreTokens())
		{
			String keyCodeStr = st.nextToken();
			KeyStroke keyStroke = parseKeyStroke(keyCodeStr);
			if(keyStroke == null)
				return;

			if(st.hasMoreTokens())
			{
				Object o = current.get(keyStroke);
				if(o instanceof Hashtable)
					current = ((Hashtable)o);
				else if(o != null)
				{
					// we have binding foo
					// but user asks to remove foo bar?
					current.remove(keyStroke);
					return;
				}
				else
				{
					// user asks to remove non-existent
					return;
				}
			}
			else
				current.remove(keyStroke);
		}
	} //}}}

	//{{{ removeAllKeyBindings() method
	/**
	 * Removes all key bindings from this input handler.
	 */
	public void removeAllKeyBindings()
	{
		bindings.clear();
	} //}}}

	//{{{ getKeyBinding() method
	/**
	 * Returns either an edit action, or a hashtable if the specified key
	 * is a prefix.
	 * @param keyBinding The key binding
	 * @since jEdit 3.2pre5
	 */
	public Object getKeyBinding(String keyBinding)
	{
		Hashtable current = bindings;
		StringTokenizer st = new StringTokenizer(keyBinding);

		while(st.hasMoreTokens())
		{
			KeyStroke keyStroke = parseKeyStroke(st.nextToken());
			if(keyStroke == null)
				return null;

			if(st.hasMoreTokens())
			{
				Object o = current.get(keyStroke);
				if(o instanceof Hashtable)
					current = (Hashtable)o;
				else
					return o;
			}
			else
			{
				return current.get(keyStroke);
			}
		}

		return null;
	} //}}}

	//{{{ isPrefixActive() method
	/**
	 * Returns if a prefix key has been pressed.
	 */
	public boolean isPrefixActive()
	{
		return bindings != currentBindings;
	} //}}}

	//{{{ keyPressed() method
	/**
	 * Handle a key pressed event. This will look up the binding for
	 * the key stroke and execute it.
	 */
	public void keyPressed(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiers();

		if(!(evt.isControlDown() || evt.isAltDown() || evt.isMetaDown()))
		{
			// if modifier active, handle all keys, otherwise
			// only some
			if((keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
				|| (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9))
			{
				return;
			}
			else if(keyCode == KeyEvent.VK_SPACE)
			{
				return;
			}
			else if(readNextChar != null)
			{
				if(keyCode == KeyEvent.VK_ESCAPE)
				{
					readNextChar = null;
					view.getStatus().setMessage(null);
				}
				else if(keyCode == KeyEvent.VK_TAB
					|| keyCode == KeyEvent.VK_ENTER)
				{
					setCurrentBindings(bindings);
					invokeReadNextChar((char)keyCode);
					repeatCount = 1;
					return;
				}
			}
			else
			{
				// ok even with no modifiers
			}
		}

		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode,
			modifiers);

		Object o = currentBindings.get(keyStroke);
		if(o == null)
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
				evt.consume();
				setCurrentBindings(bindings);
			}
		}

		if(readNextChar != null)
		{
			readNextChar = null;
			view.getStatus().setMessage(null);
		}

		if(o instanceof String)
		{
			setCurrentBindings(bindings);
			invokeAction((String)o);
			evt.consume();
		}
		else if(o instanceof EditAction)
		{
			setCurrentBindings(bindings);
			invokeAction((EditAction)o);
			evt.consume();
		}
		else if(o instanceof Hashtable)
		{
			setCurrentBindings((Hashtable)o);
			evt.consume();
		}

		if(o == null)
		{
			switch(evt.getKeyCode())
			{
				case KeyEvent.VK_NUMPAD0:   case KeyEvent.VK_NUMPAD1:
				case KeyEvent.VK_NUMPAD2:   case KeyEvent.VK_NUMPAD3:
				case KeyEvent.VK_NUMPAD4:   case KeyEvent.VK_NUMPAD5:
				case KeyEvent.VK_NUMPAD6:   case KeyEvent.VK_NUMPAD7:
				case KeyEvent.VK_NUMPAD8:   case KeyEvent.VK_NUMPAD9:
				case KeyEvent.VK_MULTIPLY:  case KeyEvent.VK_ADD:
				/* case KeyEvent.VK_SEPARATOR: */ case KeyEvent.VK_SUBTRACT:
				case KeyEvent.VK_DECIMAL:   case KeyEvent.VK_DIVIDE:
					KeyEventWorkaround.numericKeypadKey();
					break;
			}
		}
	} //}}}

	//{{{ keyTyped() method
	/**
	 * Handle a key typed event. This inserts the key into the text area.
	 */
	public void keyTyped(KeyEvent evt)
	{
		char c = evt.getKeyChar();

		// ignore
		if(c == '\b')
			return;

		if(readNextChar != null)
		{
			setCurrentBindings(bindings);
			invokeReadNextChar(c);
			repeatCount = 1;
			return;
		}

		KeyStroke keyStroke;

		// this is a hack. a literal space is impossible to
		// insert in a key binding string, but you can write
		// SPACE.
		switch(c)
		{
		case ' ':
			keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
				evt.getModifiers());
			break;
		case '\t':
			keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
				evt.getModifiers());
			break;
		case '\n':
			keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
				evt.getModifiers());
			break;
		default:
			keyStroke = KeyStroke.getKeyStroke(c);
			break;
		}

		Object o = currentBindings.get(keyStroke);

		if(o instanceof Hashtable)
		{
			setCurrentBindings((Hashtable)o);
		}
		else if(o instanceof String)
		{
			setCurrentBindings(bindings);
			invokeAction((String)o);
		}
		else if(o instanceof EditAction)
		{
			setCurrentBindings(bindings);
			invokeAction((EditAction)o);
		}
		else
		{
			setCurrentBindings(bindings);
			userInput(c);
		}
	} //}}}

	//{{{ setModifierMapping() method
	/**
	 * Changes the mapping between symbolic modifier key names
	 * (<code>C</code>, <code>A</code>, <code>M</code>, <code>S</code>) and
	 * Java modifier flags.
	 *
	 * @param c The modifier to map the <code>C</code> modifier to
	 * @param a The modifier to map the <code>A</code> modifier to
	 * @param m The modifier to map the <code>M</code> modifier to
	 * @param s The modifier to map the <code>S</code> modifier to
	 *
	 * @since jEdit 4.1pre3
	 */
	public static void setModifierMapping(int c, int a, int m, int s)
	{
		DefaultInputHandler.c = c;
		DefaultInputHandler.a = a;
		DefaultInputHandler.m = m;
		DefaultInputHandler.s = s;
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
		// this relies on the fact that if C is mapped to M, then
		// M will be mapped to C.
		if(mod == c)
			return 'C';
		else if(mod == a)
			return 'A';
		else if(mod == m)
			return 'M';
		else if(mod == s)
			return 'S';
		else
			return '\0';
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
		StringBuffer buf = new StringBuffer();
		if(evt.isControlDown())
			buf.append(getSymbolicModifierName(InputEvent.CTRL_MASK));
		if(evt.isAltDown())
			buf.append(getSymbolicModifierName(InputEvent.ALT_MASK));
		if(evt.isMetaDown())
			buf.append(getSymbolicModifierName(InputEvent.META_MASK));
		if(evt.isShiftDown())
			buf.append(getSymbolicModifierName(InputEvent.SHIFT_MASK));
		return buf.toString();
	} //}}}

	//{{{ parseKeyStroke() method
	/**
	 * Converts a string to a keystroke. The string should be of the
	 * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
	 * is any combination of A for Alt, C for Control, S for Shift
	 * or M for Meta, and <i>shortcut</i> is either a single character,
	 * or a keycode name from the <code>KeyEvent</code> class, without
	 * the <code>VK_</code> prefix.
	 * @param keyStroke A string description of the key stroke
	 */
	public static KeyStroke parseKeyStroke(String keyStroke)
	{
		if(keyStroke == null)
			return null;
		int modifiers = 0;
		int index = keyStroke.indexOf('+');
		if(index != -1)
		{
			for(int i = 0; i < index; i++)
			{
				switch(Character.toUpperCase(keyStroke
					.charAt(i)))
				{
				case 'A':
					modifiers |= a;
					break;
				case 'C':
					modifiers |= c;
					break;
				case 'M':
					modifiers |= m;
					break;
				case 'S':
					modifiers |= s;
					break;
				}
			}
		}
		String key = keyStroke.substring(index + 1);
		if(key.length() == 1)
		{
			char ch = key.charAt(0);
			if(modifiers == 0)
				return KeyStroke.getKeyStroke(ch);
			else
			{
				return KeyStroke.getKeyStroke(Character.toUpperCase(ch),
					modifiers);
			}
		}
		else if(key.length() == 0)
		{
			Log.log(Log.ERROR,DefaultInputHandler.class,
				"Invalid key stroke: " + keyStroke);
			return null;
		}
		else
		{
			int ch;

			try
			{
				ch = KeyEvent.class.getField("VK_".concat(key))
					.getInt(null);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,DefaultInputHandler.class,
					"Invalid key stroke: "
					+ keyStroke);
				return null;
			}

			return KeyStroke.getKeyStroke(ch,modifiers);
		}
	} //}}}

	//{{{ Private members

	//{{{ Class initializer
	static
	{
		if(OperatingSystem.isMacOS())
		{
			setModifierMapping(
				InputEvent.META_MASK,
				InputEvent.ALT_MASK,
				InputEvent.CTRL_MASK,
				InputEvent.SHIFT_MASK);
		}
		else
		{
			setModifierMapping(
				InputEvent.CTRL_MASK,
				InputEvent.ALT_MASK,
				InputEvent.META_MASK,
				InputEvent.SHIFT_MASK);
		}
	} //}}}

	private static int c, a, m, s;

	// Stores prefix name in bindings hashtable
	private static Object PREFIX_STR = "PREFIX_STR";

	private Hashtable bindings;
	private Hashtable currentBindings;

	//{{{ setCurrentBindings() method
	private void setCurrentBindings(Hashtable bindings)
	{
		String prefixStr = (String)bindings.get(PREFIX_STR);
		if(prefixStr != null)
		{
			if(currentBindings != this.bindings)
			{
				//XXX this won't work past 2 levels of prefixing
				prefixStr = currentBindings.get(PREFIX_STR)
					+ " " + prefixStr;
			}

			view.getStatus().setMessage(prefixStr);
		}
		else
			view.getStatus().setMessage(null);

		currentBindings = bindings;
	} //}}}

	//{{{ _addKeyBinding() method
	/**
	 * Adds a key binding to this input handler. The key binding is
	 * a list of white space separated key strokes of the form
	 * <i>[modifiers+]key</i> where modifier is C for Control, A for Alt,
	 * or S for Shift, and key is either a character (a-z) or a field
	 * name in the KeyEvent class prefixed with VK_ (e.g., BACK_SPACE)
	 * @param keyBinding The key binding
	 * @param action The action
	 */
	public void _addKeyBinding(String keyBinding, Object action)
	{
		Hashtable current = bindings;

		StringTokenizer st = new StringTokenizer(keyBinding);
		while(st.hasMoreTokens())
		{
			String keyCodeStr = st.nextToken();
			KeyStroke keyStroke = parseKeyStroke(keyCodeStr);
			if(keyStroke == null)
				return;

			if(st.hasMoreTokens())
			{
				Object o = current.get(keyStroke);
				if(o instanceof Hashtable)
					current = (Hashtable)o;
				else
				{
					Hashtable hash = new Hashtable();
					hash.put(PREFIX_STR,keyCodeStr);
					o = hash;
					current.put(keyStroke,o);
					current = (Hashtable)o;
				}
			}
			else
				current.put(keyStroke,action);
		}
	} //}}}

	//}}}
}
