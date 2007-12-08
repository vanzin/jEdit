/*
 * KeyEventTranslator.java - Hides some warts of AWT event API
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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
import java.util.HashMap;
import java.util.Map;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.Options;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
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
	
	protected static KeyEvent lastKeyPressEvent;
	
	protected static boolean lastKeyPressAccepted;

	/**
	 * Pass this an event from {@link
	 * KeyEventWorkaround#processKeyEvent(java.awt.event.KeyEvent)}.
	 * @param evt the KeyEvent to translate
	 * @since jEdit 4.2pre3
	 */
	public static Key translateKeyEvent(KeyEvent evt)
	{
		Key key = translateKeyEvent2(evt);
		
		if (key!=null) {
			if (key.isPhantom()) {
				key = null;
			}
		}
		
		return key;
	}
	
	/**
	 * Pass this an event from {@link
	 * KeyEventWorkaround#processKeyEvent(java.awt.event.KeyEvent)}.
	 * @param evt the KeyEvent to translate
	 * @since jEdit 4.2pre3
	 */
	public static Key translateKeyEvent2(KeyEvent evt)
	{
		if (Options.SIMPLIFIED_KEY_HANDLING)
		{	// This is still experimental code.

			/**
				A summary of Java key handling intricacies:
				(1) No "key pressed" events are generated for umlaut keys and for "combined characters" (key for diacritic mark + key for base character), only "key typed" and "key released" events are generated for them
				(2) The "key typed" event for Ctrl+J is indistinguishable from the "key typed" event for Ctrl+Return (in both cases: keycode=0, keychar=0xa) (in Java 1.5 under linux, but not in Java 1.6)
				(3) If a key is pressed longer, not only additional "key typed" events but also additional "key released", "key pressed" events are generated.
				(4) There are no proper key events generated for dead key + space (like '^' + ' ' resulting in '^') in Java 1.5 under linux. In Java 1.6, this bug is fixed. 
					
				For (2), we could simply ignore "key typed" events (as (3) allows us to do so). But then we would loose umlaut keys and combined characters (due to (1)).
				For (1), we could simply ignore "key pressed" events. But then we would suffer from (2).
				
				Thus, we need to distinguish for (2) at the "key pressed" event state, however fire the internal key events only at the "key typed" stage.
				This makes it necessary to store information about the last "key pressed" event (to be able to distinguish). 
					
			*/
			char	keyChar		= evt.getKeyChar();
			int	keyCode		= evt.getKeyCode();
			int	modifiers	= evt.getModifiers();
			boolean usecooked	= !evt.isActionKey();

//			Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): 1: keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+", usecooked="+usecooked+", event="+evt+".");
			
			/*
				Workaround against the bug of jdk1.5, that Ctrl+A has keyChar 0x1 instead of keyChar 0x41:
				
				This bug may be related to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6320676
			*/
			if ((modifiers&InputEvent.CTRL_MASK)!=0) {
//				Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+": 1.");
				if (keyChar<0x20) {
//					Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+": 1.1.");
					if (keyChar!=keyCode) { // specifically: if the real Escape, Backspace, Delete, Tab, Enter key was pressed, then this is false
//						Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+": 1.1.1");
						keyChar+=0x40;
						
						if ((keyChar>='A')&&(keyChar<='Z')) {	// if they are uppercase letters
								keyChar+=0x20; 		// make them lowercase letters
						}
//						usecooked	= false;

					}
				}
				
				if (keyChar=='\\') { // for compatibility with traditional jEdit installations (Shortcuts are called "C+BACK_SLASH" instead of "C+\")
//					Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+": 1.1.1: backslash.");
					keyChar		= 0;
					keyCode		= KeyEvent.VK_BACK_SLASH;
				}
				
//				Log.log(Log.DEBUG,"KeyEventTranslator","translateKeyEvent(): 2: keyChar="+((int) keyChar)+",keyCode="+keyCode+",modifiers="+modifiers+", event="+evt+".");
			}
			
			
			
			/**
				These keys are hidden "control keys". That is, they are used as special function key (instead of representing a character to be input), but
				Java delivers them with a valid keyChar. We intentionally ignore this keyChar.
				(However, not ignoring the keyChar would be an easy way to enter "escape" or "delete" characters into the edited text document, but this is not what we want.) 
			*/
			switch (keyChar) {
				case 0x1b:	// case KeyEvent.VK_ESCAPE:
				case 0x08:	// case KeyEvent.VK_BACK_SPACE:
				case 0x7f:	// case KeyEvent.VK_DELETE:
				case 0x09:	// case KeyEvent.VK_TAB:
				case 0x0a:	// case KeyEvent.VK_ENTER:
				case KeyEvent.CHAR_UNDEFINED:
					usecooked	= false;
					keyChar		= 0;
			}

			boolean accept = false;
			boolean acceptAsPhantom = false;
			if (true) {
				switch(evt.getID()) {
					case KeyEvent.KEY_PRESSED:
						accept			= !usecooked;
						acceptAsPhantom		= !accept;
						lastKeyPressAccepted	= accept;
						lastKeyPressEvent	= evt;
					break;
					case KeyEvent.KEY_TYPED:
						if (lastKeyPressAccepted&&(lastKeyPressEvent!=null)&&(lastKeyPressEvent.getKeyChar()==evt.getKeyChar())) {
							// Do not emit internal key event twice.
							// This works around the case where "Ctrl+J" and "Ctrl+Return" are indistinguishable in that "Ctrl+Return" is handled at the "key pressed" stage where "Ctrl+J" is handled at the "key typed" stage.
						} else {
							accept		= usecooked;
							acceptAsPhantom	= !accept;
						}
					break;
					default:
				}
			} else { 
				/*
					This attempt does work for the "Ctrl+Enter"-Problem, but this does work neither for umlauts nor for combined synthetic keys (like characters with diacritic marks).
					The reason is that java 1.5.0_06 (on Linux) does not deliver "key pressed" events for those keys, only "key typed" events.
				*/
				/*
					We ignore all the "key typed" events, as key repeat is already synthetically generated by synthetic "key pressed" "key released" events.
					"key typed" events have less information.
					
					This is highly experimental, as this relies on the JVM to generate these synthetic "key released", "key pressed" events.
				*/
				switch(evt.getID()) {
					case KeyEvent.KEY_PRESSED:
						accept	= true;
						if (usecooked) { 		// This destroys information, but this is what the rest of jEdit is used to :-(
							keyCode = 0;
						}
					break;
					default:
				}
			}
				
			Key returnValue = null;
			
			if (accept||acceptAsPhantom) {
				if (!accept && acceptAsPhantom) {
					if (keyChar!=0) {
						keyCode = 0;
					}
				}
				
				returnValue = new Key(modifiersToString(modifiers),keyCode,keyChar);
				
				if (!accept && acceptAsPhantom) {
					if (keyChar!=0) {
					}
					returnValue.setIsPhantom(true);
				}
			}

			return returnValue;
		}
		else
		{
			int modifiers = evt.getModifiers();
			Key returnValue;
	
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
							modifiersToString(modifiers),
							'\0',Character.toLowerCase(
							(char)keyCode));
					}
				}
				else
				{
					if(keyCode == KeyEvent.VK_TAB)
					{
						evt.consume();
						returnValue = new Key(
							modifiersToString(modifiers),
							keyCode,'\0');
					}
					else if(keyCode == KeyEvent.VK_SPACE)
					{
						// for SPACE or S+SPACE we pass the
						// key typed since international
						// keyboards sometimes produce a
						// KEY_PRESSED SPACE but not a
						// KEY_TYPED SPACE, eg if you have to
						// do a "<space> to insert ".
						if((modifiers & ~InputEvent.SHIFT_MASK) == 0)
							returnValue = null;
						else
						{
							returnValue = new Key(
								modifiersToString(modifiers),
								0,' ');
						}
					}
					else
					{
						returnValue = new Key(
							modifiersToString(modifiers),
							keyCode,'\0');
					}
				}
				break;
			case KeyEvent.KEY_TYPED:
				char ch = evt.getKeyChar();
	
				if(KeyEventWorkaround.isMacControl(evt))
					ch |= 0x60;
	
				switch(ch)
				{
				case '\n':
				case '\t':
				case '\b':
					return null;
				case ' ':
					if((modifiers & ~InputEvent.SHIFT_MASK) != 0)
						return null;
				}
	
				int ignoreMods;
				if(Debug.ALT_KEY_PRESSED_DISABLED)
				{
					/* on MacOS, A+ can be user input */
					ignoreMods = InputEvent.SHIFT_MASK
						| InputEvent.ALT_GRAPH_MASK
						| InputEvent.ALT_MASK;
				}
				else
				{
					/* on MacOS, A+ can be user input */
					ignoreMods = InputEvent.SHIFT_MASK
						| InputEvent.ALT_GRAPH_MASK;
				}
	
				if((modifiers & InputEvent.ALT_GRAPH_MASK) == 0
					&& evt.getWhen()
					- KeyEventWorkaround.lastKeyTime < 750L
					&& (KeyEventWorkaround.modifiers & ~ignoreMods)
					!= 0)
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
				{
					if(ch == ' ')
					{
						returnValue = new Key(
							modifiersToString(modifiers),
							0,ch);
					}
					else
						returnValue = new Key(null,0,ch);
				}
				break;
			default:
				return null;
			}
	
			/* I guess translated events do not have the 'evt' field set
			so consuming won't work. I don't think this is a problem as
			nothing uses translation anyway */
			Key trans = transMap.get(returnValue);
			if(trans == null)
				return returnValue;
			else
				return trans;
		}
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
		int modifiers = 0;
		String key;
		int endOfModifiers = keyStroke.indexOf('+');
		if(endOfModifiers <= 0)	// not found or found at first
		{
			key = keyStroke;
		}
		else
		{
			for(int i = 0; i < endOfModifiers; i++)
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
			key = keyStroke.substring(endOfModifiers + 1);
		}
		if(key.length() == 1)
		{
			return new Key(modifiersToString(modifiers),0,key.charAt(0));
		}
		else if(key.length() == 0)
		{
			Log.log(Log.ERROR,KeyEventTranslator.class,
				"Invalid key stroke: " + keyStroke);
			return null;
		}
		else if(key.equals("SPACE"))
		{
			return new Key(modifiersToString(modifiers),0,' ');
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
				Log.log(Log.ERROR,KeyEventTranslator.class,
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
	 * You can map more than one Java modifier to a symobolic modifier, for 
	 * example :
	 * <p><code><pre>
	 *	setModifierMapping(
	 *		InputEvent.CTRL_MASK,
	 *		InputEvent.ALT_MASK | InputEvent.META_MASK,
	 *		0,
	 *		InputEvent.SHIFT_MASK);
	 *<pre></code></p>
	 *
	 * You cannot map a Java modifer to more than one symbolic modifier.
	 *
	 * @param c The modifier(s) to map the <code>C</code> modifier to
	 * @param a The modifier(s) to map the <code>A</code> modifier to
	 * @param m The modifier(s) to map the <code>M</code> modifier to
	 * @param s The modifier(s) to map the <code>S</code> modifier to
	 *
	 * @since jEdit 4.2pre3
	 */
	public static void setModifierMapping(int c, int a, int m, int s)
	{
	
		int duplicateMapping = 
			(c & a) | (c & m) | (c & s) | (a & m) | (a & s) | (m & s);
		
		if((duplicateMapping & InputEvent.CTRL_MASK) != 0) {
			throw new IllegalArgumentException(
				"CTRL is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.ALT_MASK) != 0) {
			throw new IllegalArgumentException(
				"ALT is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.META_MASK) != 0) {
			throw new IllegalArgumentException(
				"META is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.SHIFT_MASK) != 0) {
			throw new IllegalArgumentException(
				"SHIFT is mapped to more than one modifier");
		}
			
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
		if((mod & c) != 0)
			return 'C';
		else if((mod & a) != 0)
			return 'A';
		else if((mod & m) != 0)
			return 'M';
		else if((mod & s) != 0)
			return 'S';
		else
			return '\0';
	} //}}}

	//{{{ modifiersToString() method
	private static final int[] MODS = {
		InputEvent.CTRL_MASK,
		InputEvent.ALT_MASK,
		InputEvent.META_MASK,
		InputEvent.SHIFT_MASK
	};

	public static String modifiersToString(int mods)
	{
		StringBuffer buf = null;

		for(int i = 0; i < MODS.length; i++)
		{
			if((mods & MODS[i]) != 0)
				buf = lazyAppend(buf,getSymbolicModifierName(MODS[i]));
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
		StringBuilder buf = new StringBuilder();
		if(evt.isControlDown())
			buf.append(getSymbolicModifierName(InputEvent.CTRL_MASK));
		if(evt.isAltDown())
			buf.append(getSymbolicModifierName(InputEvent.ALT_MASK));
		if(evt.isMetaDown())
			buf.append(getSymbolicModifierName(InputEvent.META_MASK));
		if(evt.isShiftDown())
			buf.append(getSymbolicModifierName(InputEvent.SHIFT_MASK));
		return buf.length() == 0 ? null : buf.toString();
	} //}}}

	static int c, a, m, s;

	//{{{ Private members
	/** This map is a pool of Key. */
	private static final Map<Key, Key> transMap = new HashMap<Key, Key>();

	private static StringBuffer lazyAppend(StringBuffer buf, char ch)
	{
		if(buf == null)
			buf = new StringBuffer();
		if(buf.indexOf(String.valueOf(ch)) == -1)
			buf.append(ch);
		return buf;
	} //}}}

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
		public final String modifiers;
		public final int key;
		public final char input;

		private final int hashCode;
		/**
			Wether this Key event applies to all jEdit windows (and not only a specific jEdit GUI component). 
		*/
		protected boolean isFromGlobalContext;
		
		/**
			Wether this Key event is a phantom key event. A phantom key event is a kind of duplicate key event which
			should not - due to its nature of being a duplicate - generate any action on data.
			However, phantom key events may be necessary to notify the rest of the GUI that the key event, if it was not a phantom key event but a real key event,
			would generate any action and thus would be consumed.
		*/
		protected boolean isPhantom;

		public Key(String modifiers, int key, char input)
		{
			this.modifiers = modifiers;
			this.key = key;
			this.input = input;
			hashCode = key + input;
		}

		@Override
		public int hashCode()
		{
			return hashCode;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o instanceof Key)
			{
				Key k = (Key)o;
				if(StandardUtilities.objectsEqual(modifiers,
					k.modifiers) && key == k.key
					&& input == k.input)
				{
					return true;
				}
			}

			return false;
		}

		@Override
		public String toString()
		{
			return (modifiers == null ? "" : modifiers)
				+ '<'
				+ Integer.toString(key,16)
				+ ','
				+ Integer.toString(input,16)
				+ '>';
		}
		
		public void setIsFromGlobalContext(boolean to) {
			isFromGlobalContext = to;
		}
		
		public boolean isFromGlobalContext() {
			return isFromGlobalContext;
		}
		
		public void setIsPhantom(boolean to) {
			isPhantom = to;
		}
		
		public boolean isPhantom() {
			return isPhantom;
		}
	} //}}}
}
