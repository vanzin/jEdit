/*
 * KeyEventTranslator.java - Hides some warts of AWT event API
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import java.awt.event.*;
import java.awt.Toolkit;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * In conjunction with the <code>KeyEventWorkaround</code>, hides some 
 * warts in the AWT key event API.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class KeyEventTranslator
{
	//{{{ addTranslation() method
	/**
	 * Adds a keyboard translation.
	 * @param key1 Translate this key
	 * @param key2 Into this key
	 * @since jEdit 4.2pre3
	 */
	public static void addTranslation(Key key1, Key key2)
	{
		transMap.put(key1,key2);
	} //}}}

	//{{{ translateKeyEvent() method
	/**
	 * Pass this an event from {@link
	 * KeyEventWorkaround#processKeyEvent(java.awt.event.KeyEvent)}.
	 * @since jEdit 4.2pre3
	 */
	public static Key translateKeyEvent(KeyEvent evt)
	{
		int modifiers = evt.getModifiers();
		Key returnValue = null;

		switch(evt.getID())
		{
		case KeyEvent.KEY_PRESSED:
			int keyCode = evt.getKeyCode();
			if((keyCode >= KeyEvent.VK_0
				&& keyCode <= KeyEvent.VK_9)
				|| (keyCode >= KeyEvent.VK_A
				&& keyCode <= KeyEvent.VK_Z))
			{
				if(Debug.ALTERNATIVE_DISPATCHER)
					return null;
				else
				{
					returnValue = new Key(
						getModifierString(evt),
						'\0',Character.toLowerCase(
						(char)keyCode));
				}
			}
			else
			{
				if(keyCode == KeyEvent.VK_TAB)
					evt.consume();
				returnValue = new Key(getModifierString(evt),keyCode,'\0');
			}
			break;
		case KeyEvent.KEY_TYPED:
			char ch = evt.getKeyChar();

			switch(ch)
			{
			case '\n':
			case '\t':
			case '\b':
			case ' ':
				return null;
			}

			boolean mod;
			if(System.currentTimeMillis()
				-  KeyEventWorkaround.lastKeyTime < 750)
			{
				if(modifiers == InputEvent.ALT_GRAPH_MASK)
				{
					mod = false;
				}
				else if((KeyEventWorkaround.modifiers
					& ~(InputEvent.SHIFT_MASK | InputEvent.ALT_GRAPH_MASK)) != 0)
				{
					mod = true;
				}
				else
				{
					mod = false;
				}
			}
			else
			{
				mod = false;
			}

			if(mod)
			{
				if(Debug.ALTERNATIVE_DISPATCHER)
				{
					returnValue = new Key(
						modifiersToString(modifiers),
						0,ch);
				}
				else
					return null;
			}
			else
				returnValue = new Key(null,0,ch);
			break;
		default:
			return null;
		}

		Key trans = (Key)transMap.get(returnValue);
		if(trans == null)
			return returnValue;
		else
			return trans;
	} //}}}

	//{{{ parseKey() method
	/**
	 * Converts a string to a keystroke. The string should be of the
	 * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
	 * is any combination of A for Alt, C for Control, S for Shift
	 * or M for Meta, and <i>shortcut</i> is either a single character,
	 * or a keycode name from the <code>KeyEvent</code> class, without
	 * the <code>VK_</code> prefix.
	 * @param keyStroke A string description of the key stroke
	 * @since jEdit 4.2pre3
	 */
	public static Key parseKey(String keyStroke)
	{
		if(keyStroke == null)
			return null;
		int index = keyStroke.indexOf('+');
		int modifiers = 0;
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
			return new Key(modifiersToString(modifiers),0,key.charAt(0));
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

			return new Key(modifiersToString(modifiers),ch,'\0');
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
	 * @since jEdit 4.2pre3
	 */
	public static void setModifierMapping(int c, int a, int m, int s)
	{
		KeyEventTranslator.c = c;
		KeyEventTranslator.a = a;
		KeyEventTranslator.m = m;
		KeyEventTranslator.s = s;
	} //}}}

	//{{{ getSymbolicModifierName() method
	/**
	 * Returns a the symbolic modifier name for the specified Java modifier
	 * flag.
	 *
	 * @param mod A modifier constant from <code>InputEvent</code>
	 *
	 * @since jEdit 4.2pre3
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

	//{{{ modifiersToString() method
	public static String modifiersToString(int mods)
	{
		StringBuffer buf = null;

		if((mods & InputEvent.CTRL_MASK) != 0)
		{
			if(buf == null)
				buf = new StringBuffer();
			buf.append(getSymbolicModifierName(InputEvent.CTRL_MASK));
		}
		if((mods & InputEvent.ALT_MASK) != 0)
		{
			if(buf == null)
				buf = new StringBuffer();
			buf.append(getSymbolicModifierName(InputEvent.ALT_MASK));
		}
		if((mods & InputEvent.META_MASK) != 0)
		{
			if(buf == null)
				buf = new StringBuffer();
			buf.append(getSymbolicModifierName(InputEvent.META_MASK));
		}
		if((mods & InputEvent.SHIFT_MASK) != 0)
		{
			if(buf == null)
				buf = new StringBuffer();
			buf.append(getSymbolicModifierName(InputEvent.SHIFT_MASK));
		}

		if(buf == null)
			return null;
		else
			return buf.toString();
	} //}}}

	//{{{ getModifierString() method
	/**
	 * Returns a string containing symbolic modifier names set in the
	 * specified event.
	 *
	 * @param evt The event
	 *
	 * @since jEdit 4.2pre3
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
		return (buf.length() == 0 ? null : buf.toString());
	} //}}}

	static int c, a, m, s;

	//{{{ Private members
	private static Map transMap = new HashMap();

	static
	{
		if(OperatingSystem.isMacOS())
		{
			setModifierMapping(
				InputEvent.META_MASK,  /* == C+ */
				InputEvent.CTRL_MASK,  /* == A+ */
				/* M+ discarded by key event workaround! */
				InputEvent.ALT_MASK,   /* == M+ */
				InputEvent.SHIFT_MASK  /* == S+ */);
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

	//{{{ Key class
	public static class Key
	{
		public String modifiers;
		public int key;
		public char input;

		public Key(String modifiers, int key, char input)
		{
			this.modifiers = modifiers;
			this.key = key;
			this.input = input;
		}

		public int hashCode()
		{
			return key + input;
		}

		public boolean equals(Object o)
		{
			if(o instanceof Key)
			{
				Key k = (Key)o;
				if(MiscUtilities.objectsEqual(modifiers,
					k.modifiers) && key == k.key
					&& input == k.input)
				{
					return true;
				}
			}

			return false;
		}

		public String toString()
		{
			return (modifiers == null ? "" : modifiers)
				+ "<"
				+ Integer.toString(key,16)
				+ ","
				+ Integer.toString(input,16)
				+ ">";
		}
	} //}}}
}
