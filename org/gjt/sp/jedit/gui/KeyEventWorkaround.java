/*
 * KeyEventWorkaround.java - Works around bugs in Java event handling
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001, 2002 Slava Pestov
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
import java.awt.*;
import org.gjt.sp.jedit.OperatingSystem;
//}}}

/**
 * This class contains various hacks to get keyboard event handling to behave in
 * a consistent manner across Java implementations, many of which are
 * hopelessly broken in this regard.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class KeyEventWorkaround
{
	//{{{ processKeyEvent() method
	public static KeyEvent processKeyEvent(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		char ch = evt.getKeyChar();

		switch(evt.getID())
		{
		//{{{ KEY_PRESSED...
		case KeyEvent.KEY_PRESSED:
			// get rid of keys we never need to handle
			switch(keyCode)
			{
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_META:
			case '\0':
				return null;
			default:
				switch(keyCode)
				{
					case KeyEvent.VK_NUMPAD0:   case KeyEvent.VK_NUMPAD1:
					case KeyEvent.VK_NUMPAD2:   case KeyEvent.VK_NUMPAD3:
					case KeyEvent.VK_NUMPAD4:   case KeyEvent.VK_NUMPAD5:
					case KeyEvent.VK_NUMPAD6:   case KeyEvent.VK_NUMPAD7:
					case KeyEvent.VK_NUMPAD8:   case KeyEvent.VK_NUMPAD9:
					case KeyEvent.VK_MULTIPLY:  case KeyEvent.VK_ADD:
					/* case KeyEvent.VK_SEPARATOR: */ case KeyEvent.VK_SUBTRACT:
					case KeyEvent.VK_DECIMAL:   case KeyEvent.VK_DIVIDE:
						last = LAST_NUMKEYPAD;
						lastKeyTime = System.currentTimeMillis();
						return evt;
				}

				if(!OperatingSystem.isMacOS()
					&& !OperatingSystem.hasJava14())
				{
					handleBrokenKeys(evt,keyCode);
				}
				else
					last = LAST_NOTHING;
				break;
			}

			return evt;
		//}}}
		//{{{ KEY_TYPED...
		case KeyEvent.KEY_TYPED:
			// need to let \b through so that backspace will work
			// in HistoryTextFields
			if((ch < 0x20 || ch == 0x7f || ch == 0xff) && ch != '\b')
				return null;

			// "Alt" is the option key on MacOS, and it can generate
			// user input
			if(OperatingSystem.isMacOS())
			{
				if(evt.isControlDown() || evt.isMetaDown())
					return null;
			}
			else
			{
				if((evt.isControlDown() ^ evt.isAltDown())
					|| evt.isMetaDown())
					return null;
			}

			// On JDK 1.4 with Windows, some Alt-key sequences send
			// bullshit in a KEY_TYPED afterwards. We filter it out
			// here
			if(last == LAST_MOD)
			{
				switch(ch)
				{
				case 'B':
				case 'X':
				case 'c':
				case '!':
				case ',':
				case '?':
					last = LAST_NOTHING;
					return null;
				}
			}

			// if the last key was a numeric keypad key
			// and NumLock is off, filter it out
			if(last == LAST_NUMKEYPAD && System.currentTimeMillis()
				- lastKeyTime < 750)
			{
				last = LAST_NOTHING;
				if((ch >= '0' && ch <= '9') || ch == '.'
					|| ch == '/' || ch == '*'
					|| ch == '-' || ch == '+')
				{
					return null;
				}
			}
			// if the last key was a broken key, filter
			// out all except 'a'-'z' that occur 750 ms after.
			else if(last == LAST_BROKEN && System.currentTimeMillis()
				- lastKeyTime < 750 && !Character.isLetter(ch))
			{
				last = LAST_NOTHING;
				return null;
			}
			// otherwise, if it was ALT, filter out everything.
			else if(last == LAST_ALT && System.currentTimeMillis()
				- lastKeyTime < 750)
			{
				last = LAST_NOTHING;
				return null;
			}

			return evt;
		//}}}
		//{{{ KEY_RELEASED...
		case KeyEvent.KEY_RELEASED:
			if(keyCode == KeyEvent.VK_ALT)
			{
				// bad workaround... on Windows JDK 1.4, some
				// Alt-sequences generate random crap afterwards
				if(OperatingSystem.isWindows()
					&& OperatingSystem.hasJava14())
					last = LAST_MOD;
			}
			return evt;
		default:
			return evt;
		}
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

	//{{{ Static variables
	private static long lastKeyTime;

	private static int last;
	private static final int LAST_NOTHING = 0;
	private static final int LAST_ALT = 1;
	private static final int LAST_BROKEN = 2;
	private static final int LAST_NUMKEYPAD = 3;
	private static final int LAST_MOD = 4;
	//}}}

	//{{{ handleBrokenKeys() method
	private static void handleBrokenKeys(KeyEvent evt, int keyCode)
	{
		if(evt.isAltDown() && evt.isControlDown()
			&& !evt.isMetaDown())
		{
			last = LAST_NOTHING;
			return;
		}
		else if(!(evt.isAltDown() || evt.isControlDown() || evt.isMetaDown()))
		{
			last = LAST_NOTHING;
			return;
		}

		if(evt.isAltDown())
			last = LAST_ALT;

		switch(keyCode)
		{
			case KeyEvent.VK_LEFT:      case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_UP:        case KeyEvent.VK_DOWN:
			case KeyEvent.VK_DELETE:    case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_TAB:       case KeyEvent.VK_ENTER:
				last = LAST_NOTHING;
				break;
			default:
				if(keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z)
					last = LAST_BROKEN;
				else
					last = LAST_NOTHING;
				break;
		}

		lastKeyTime = System.currentTimeMillis();
	} //}}}

	//}}}
}
