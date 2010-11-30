/*
 * KeyEventWorkaround.java - Works around bugs in Java event handling
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
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
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.input.AbstractInputHandler;
import org.gjt.sp.util.Log;
//}}}

/**
 * Various hacks to get keyboard event handling to behave in a consistent manner
 * across Java implementations. This type of stuff should not be necessary, but
 * Java's keyboard handling is crap, to put it mildly.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class KeyEventWorkaround
{
	//{{{ isBindable() method
	public static boolean isBindable(int keyCode)
	{
		switch(keyCode)
		{
		case KeyEvent.VK_ALT:
		case KeyEvent.VK_ALT_GRAPH:
		case KeyEvent.VK_CONTROL:
		case KeyEvent.VK_SHIFT:
		case KeyEvent.VK_META:
		case KeyEvent.VK_DEAD_GRAVE:
		case KeyEvent.VK_DEAD_ACUTE:
		case KeyEvent.VK_DEAD_CIRCUMFLEX:
		case KeyEvent.VK_DEAD_TILDE:
		case KeyEvent.VK_DEAD_MACRON:
		case KeyEvent.VK_DEAD_BREVE:
		case KeyEvent.VK_DEAD_ABOVEDOT:
		case KeyEvent.VK_DEAD_DIAERESIS:
		case KeyEvent.VK_DEAD_ABOVERING:
		case KeyEvent.VK_DEAD_DOUBLEACUTE:
		case KeyEvent.VK_DEAD_CARON:
		case KeyEvent.VK_DEAD_CEDILLA:
		case KeyEvent.VK_DEAD_OGONEK:
		case KeyEvent.VK_DEAD_IOTA:
		case KeyEvent.VK_DEAD_VOICED_SOUND:
		case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
			return false;
		default:
			return true;
		}
	} //}}}

	//{{{ isPrintable() method
	/**
	 * We need to know if a keycode can potentially result in a
	 * keytyped.
	 * @since jEdit 4.3pre2
	 */
	public static boolean isPrintable(int keyCode)
	{
		switch(keyCode)
		{
		/* case KeyEvent.VK_ENTER:
		case KeyEvent.VK_TAB: */
		case KeyEvent.VK_SPACE:
		case KeyEvent.VK_COMMA:
		case KeyEvent.VK_MINUS:
		case KeyEvent.VK_PERIOD:
		case KeyEvent.VK_SLASH:
		case KeyEvent.VK_0:
		case KeyEvent.VK_1:
		case KeyEvent.VK_2:
		case KeyEvent.VK_3:
		case KeyEvent.VK_4:
		case KeyEvent.VK_5:
		case KeyEvent.VK_6:
		case KeyEvent.VK_7:
		case KeyEvent.VK_8:
		case KeyEvent.VK_9:
		case KeyEvent.VK_SEMICOLON:
		case KeyEvent.VK_EQUALS   :
		case KeyEvent.VK_A:
		case KeyEvent.VK_B:
		case KeyEvent.VK_C:
		case KeyEvent.VK_D:
		case KeyEvent.VK_E:
		case KeyEvent.VK_F:
		case KeyEvent.VK_G:
		case KeyEvent.VK_H:
		case KeyEvent.VK_I:
		case KeyEvent.VK_J:
		case KeyEvent.VK_K:
		case KeyEvent.VK_L:
		case KeyEvent.VK_M:
		case KeyEvent.VK_N:
		case KeyEvent.VK_O:
		case KeyEvent.VK_P:
		case KeyEvent.VK_Q:
		case KeyEvent.VK_R:
		case KeyEvent.VK_S:
		case KeyEvent.VK_T:
		case KeyEvent.VK_U:
		case KeyEvent.VK_V:
		case KeyEvent.VK_W:
		case KeyEvent.VK_X:
		case KeyEvent.VK_Y:
		case KeyEvent.VK_Z:
		case KeyEvent.VK_OPEN_BRACKET :
		case KeyEvent.VK_BACK_SLASH   :
		case KeyEvent.VK_CLOSE_BRACKET:
	/*	case KeyEvent.VK_NUMPAD0 :
		case KeyEvent.VK_NUMPAD1 :
		case KeyEvent.VK_NUMPAD2 :
		case KeyEvent.VK_NUMPAD3 :
		case KeyEvent.VK_NUMPAD4 :
		case KeyEvent.VK_NUMPAD5 :
		case KeyEvent.VK_NUMPAD6 :
		case KeyEvent.VK_NUMPAD7 :
		case KeyEvent.VK_NUMPAD8 :
		case KeyEvent.VK_NUMPAD9 :
		case KeyEvent.VK_MULTIPLY:
		case KeyEvent.VK_ADD     :
		case KeyEvent.VK_SEPARATOR:
		case KeyEvent.VK_SUBTRACT   :
		case KeyEvent.VK_DECIMAL    :
		case KeyEvent.VK_DIVIDE     :*/
		case KeyEvent.VK_BACK_QUOTE:
		case KeyEvent.VK_QUOTE:
		case KeyEvent.VK_DEAD_GRAVE:
		case KeyEvent.VK_DEAD_ACUTE:
		case KeyEvent.VK_DEAD_CIRCUMFLEX:
		case KeyEvent.VK_DEAD_TILDE:
		case KeyEvent.VK_DEAD_MACRON:
		case KeyEvent.VK_DEAD_BREVE:
		case KeyEvent.VK_DEAD_ABOVEDOT:
		case KeyEvent.VK_DEAD_DIAERESIS:
		case KeyEvent.VK_DEAD_ABOVERING:
		case KeyEvent.VK_DEAD_DOUBLEACUTE:
		case KeyEvent.VK_DEAD_CARON:
		case KeyEvent.VK_DEAD_CEDILLA:
		case KeyEvent.VK_DEAD_OGONEK:
		case KeyEvent.VK_DEAD_IOTA:
		case KeyEvent.VK_DEAD_VOICED_SOUND:
		case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
		case KeyEvent.VK_AMPERSAND:
		case KeyEvent.VK_ASTERISK:
		case KeyEvent.VK_QUOTEDBL:
		case KeyEvent.VK_LESS:
		case KeyEvent.VK_GREATER:
		case KeyEvent.VK_BRACELEFT:
		case KeyEvent.VK_BRACERIGHT:
		case KeyEvent.VK_AT:
		case KeyEvent.VK_COLON:
		case KeyEvent.VK_CIRCUMFLEX:
		case KeyEvent.VK_DOLLAR:
		case KeyEvent.VK_EURO_SIGN:
		case KeyEvent.VK_EXCLAMATION_MARK:
		case KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
		case KeyEvent.VK_LEFT_PARENTHESIS:
		case KeyEvent.VK_NUMBER_SIGN:
		case KeyEvent.VK_PLUS:
		case KeyEvent.VK_RIGHT_PARENTHESIS:
		case KeyEvent.VK_UNDERSCORE:
			return true;
		default:
			return false;
		}
	} //}}}

	//{{{ isMacControl() method
	/**
	 * Apple sucks.
	 */
	public static boolean isMacControl(KeyEvent evt)
	{
		return (OperatingSystem.isMacOS() &&
			(evt.getModifiers() & InputEvent.CTRL_MASK) != 0
			&& evt.getKeyChar() <= 0x2B);
	} //}}}

	//{{{ isNumericKeypad() method
	public static boolean isNumericKeypad(int keyCode)
	{
		switch(keyCode)
		{
		case KeyEvent.VK_NUMPAD0:
		case KeyEvent.VK_NUMPAD1:
		case KeyEvent.VK_NUMPAD2:
		case KeyEvent.VK_NUMPAD3:
		case KeyEvent.VK_NUMPAD4:
		case KeyEvent.VK_NUMPAD5:
		case KeyEvent.VK_NUMPAD6:
		case KeyEvent.VK_NUMPAD7:
		case KeyEvent.VK_NUMPAD8:
		case KeyEvent.VK_NUMPAD9:
		case KeyEvent.VK_MULTIPLY:
		case KeyEvent.VK_ADD:
		/* case KeyEvent.VK_SEPARATOR: */
		case KeyEvent.VK_SUBTRACT:
		case KeyEvent.VK_DECIMAL:
		case KeyEvent.VK_DIVIDE:
			return true;
		default:
			return false;
		}
	} //}}}

	//{{{ processKeyEvent() method
	public static KeyEvent processKeyEvent(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		char ch = evt.getKeyChar();
		int modifiers = evt.getModifiers();

		switch(evt.getID())
		{
		//{{{ KEY_PRESSED...
		case KeyEvent.KEY_PRESSED:
			// get rid of keys we never need to handle
			switch(keyCode)
			{
			case '\0':
				return null;
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_META:
				break;
			default:
				if(!evt.isMetaDown())
				{
					if(!evt.isControlDown()
						&& !evt.isAltDown())
					{
						if(isPrintable(keyCode))
						{
							return null;
						}
					}
				}

				if(Debug.ALT_KEY_PRESSED_DISABLED)
				{
					/* we don't handle key pressed A+ */
					/* they're too troublesome */
					if((modifiers & InputEvent.ALT_MASK) != 0)
						return null;
				}

				if(isNumericKeypad(keyCode))
					last = LAST_NUMKEYPAD;
				else
					last = LAST_NOTHING;

				break;
			}
			break;
		//}}}
		//{{{ KEY_TYPED...
		case KeyEvent.KEY_TYPED:
			// need to let \b through so that backspace will work
			// in HistoryTextFields
			if(!isMacControl(evt)
				&& (ch < 0x20 || ch == 0x7f || ch == 0xff)
				&& ch != '\b' && ch != '\t' && ch != '\n')
			{
				return null;
			}

			if(Debug.DUMP_KEY_EVENTS)
			{
				Log.log(Log.DEBUG,"KEWa","Key event (working around): "
					+ AbstractInputHandler.toString(evt)+": last="+last+".");
			}

			if(!Debug.ALTERNATIVE_DISPATCHER)
			{
				if(((modifiers & InputEvent.CTRL_MASK) != 0
					^ (modifiers & InputEvent.ALT_MASK) != 0)
					|| (modifiers & InputEvent.META_MASK) != 0)
				{
					return null;
				}
			}

			// if the last key was a numeric keypad key
			// and NumLock is off, filter it out
			if(last == LAST_NUMKEYPAD)
			{
				last = LAST_NOTHING;
				if((ch >= '0' && ch <= '9') || ch == '.'
					|| ch == '/' || ch == '*'
					|| ch == '-' || ch == '+')
				{
					return null;
				}
			}
			// Windows JDK workaround
			else if(last == LAST_ALT)
			{
				last = LAST_NOTHING;
				switch(ch)
				{
				case 'B':
				case 'M':
				case 'X':
				case 'c':
				case '!':
				case ',':
				case '?':
					return null;
				}
			}
			break;
		//}}}
		//{{{ KEY_RELEASED...
		case KeyEvent.KEY_RELEASED:
			switch(keyCode)
			{
			case KeyEvent.VK_ALT:
				// we consume this to work around the bug
				// where A+TAB window switching activates
				// the menu bar on Windows.
				// http://bugs.sun.com/view_bug.do?bug_id=6458497
				//
				// This should be removed if the fix for the
				// above problem became widely available, to
				// allow the menu bar activation.
				evt.consume();
				break;
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_META:
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_END:
			case KeyEvent.VK_HOME:
				/* workaround for A+keys producing
				 * garbage on Windows */
				if(modifiers == InputEvent.ALT_MASK)
					last = LAST_ALT;
				break;
			}
			break;
		//}}}
		}
		return evt;
	} //}}}

	//{{{ numericKeypadKey() method
	/**
	 * A workaround for non-working NumLock status in some Java versions.
	 * @since jEdit 4.0pre8
	 */
	public static void numericKeypadKey()
	{
		last = LAST_NOTHING;
	} //}}}

	//{{{ Private members
	private static int last;
	private static final int LAST_NOTHING = 0;
	private static final int LAST_NUMKEYPAD = 1;
	private static final int LAST_ALT = 2;
	//}}}
}
