/*
 * KeyEventWorkaround.java - Works around bugs in Java event handling
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000 Slava Pestov
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
//}}}

public class KeyEventWorkaround
{
	// from JDK 1.2 InputEvent.java
	public static final int ALT_GRAPH_MASK = 1 << 5;

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
			if(keyCode == KeyEvent.VK_CONTROL ||
				keyCode == KeyEvent.VK_SHIFT ||
				keyCode == KeyEvent.VK_ALT ||
				keyCode == KeyEvent.VK_META ||
				keyCode == '\0')
				return null;

			if(!mac)
				handleBrokenKeys(evt,keyCode);

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
			if(mac)
			{
				if(evt.isControlDown() || evt.isMetaDown())
					return null;
			}
			else
			{
				if((evt.isControlDown() ^ evt.isAltDown())
					|| evt.isMetaDown())
					return null;

				// if the last key was a broken key, filter
				// out all except 'a'-'z' that occur 750 ms after.
				if(last == LAST_BROKEN && System.currentTimeMillis()
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
			}

			return evt;
		//}}}
		default:
			return evt;
		}
	} //}}}

	//{{{ Private members

	//{{{ Static variables
	private static boolean mac;
	private static long lastKeyTime;

	private static int last;
	private static final int LAST_NOTHING = 0;
	private static final int LAST_ALTGR = 1;
	private static final int LAST_ALT = 2;
	private static final int LAST_BROKEN = 3;
	//}}}

	//{{{ Class initializer
	static
	{
		mac = (System.getProperty("os.name").indexOf("Mac OS") != -1);
	} //}}}

	//{{{ handleBrokenKeys() method
	private static void handleBrokenKeys(KeyEvent evt, int keyCode)
	{
		if(evt.isAltDown() && evt.isControlDown()
			&& !evt.isMetaDown())
		{
			last = LAST_ALTGR;
			return;
		}
		else if(!(evt.isAltDown() || evt.isControlDown() || evt.isMetaDown()))
		{
			last = LAST_NOTHING;
			return;
		}

		if(evt.isAltDown())
			last = LAST_ALT;
		else if((keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z)
			&& keyCode != KeyEvent.VK_LEFT && keyCode != KeyEvent.VK_RIGHT
			&& keyCode != KeyEvent.VK_UP && keyCode != KeyEvent.VK_DOWN
			&& keyCode != KeyEvent.VK_DELETE && keyCode != KeyEvent.VK_BACK_SPACE
			 && keyCode != KeyEvent.VK_TAB && keyCode != KeyEvent.VK_ENTER)
			last = LAST_BROKEN;
		else
			last = LAST_NOTHING;

		lastKeyTime = System.currentTimeMillis();
	} //}}}

	//}}}
}
