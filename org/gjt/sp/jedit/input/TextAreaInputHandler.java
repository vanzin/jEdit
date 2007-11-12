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

import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.gui.GrabKeyDialog;
import org.gjt.sp.jedit.gui.KeyEventWorkaround;
import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.*;

/**
 * This class manage the key bindings and execute the actions binded on th
 * keyboard events.
 *
 * @author Matthieu Casanova
 * @version $Id: FoldHandler.java 5568 2006-07-10 20:52:23Z kpouer $
 */
public class TextAreaInputHandler extends AbstractInputHandler
{
	private final TextArea textArea;

	//{{{ TextAreaInputHandler constructor
	public TextAreaInputHandler(TextArea textArea)
	{
		this.textArea = textArea;
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 * @since 4.3pre7
	 */
	public void processKeyEvent(KeyEvent evt, int from, boolean global)
	{
		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event                 : "
				+ GrabKeyDialog.toString(evt) + " from " + from);
		//	Log.log(Log.DEBUG,this,view+".isFocused()="+view.isFocused()+'.',new Exception());
		}

		evt = _preprocessKeyEvent(evt);
		if(evt == null)
			return;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event after workaround: "
				+ GrabKeyDialog.toString(evt) + " from " + from);
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
	private KeyEvent _preprocessKeyEvent(KeyEvent evt)
	{
		Component focusOwner = textArea;
		if (true /*Options.SIMPLIFIED_KEY_HANDLING*/)
		{
			/*
				It seems that the "else" path below does
				not work. Apparently, is is there to prevent
				some keyboard events to be "swallowed" by
				jEdit when the keyboard event in fact should
				be scheduled to swing for further handling.

				On some "key typed" events, the "return null;"
				is triggered. However, these key events
				actually do not seem to be handled elseewhere,
				so they are not handled at all.

				This behaviour exists with old keyboard handling
				as well as with new keyboard handling. However,
				the new keyboard handling is more sensitive
				about what kinds of key events it receives. It
				expects to see all "key typed" events,
				which is incompatible with the "return null;"
				below.

				This bug triggers jEdit bug 1493185 ( https://sourceforge.net/tracker/?func=detail&aid=1493185&group_id=588&atid=100588 ).

				Thus, we disable the possibility of
				key event swallowing for the new key event
				handling.

			*/
		}
		else
		{
			JComponent comp = (JComponent)focusOwner;
			InputMap map = comp.getInputMap();
			ActionMap am = comp.getActionMap();

			if(map != null && am != null && comp.isEnabled())
			{
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(evt);
				Object binding = map.get(keyStroke);
				if(binding != null && am.get(binding) != null)
				{
					return null;
				}
			}
		}

		if(evt.isConsumed())
			return null;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event (preprocessing) : "
					+ GrabKeyDialog.toString(evt));
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
		if (!dryRun)
		{
			if(input != '\0')
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
				else
				{
					switch (keyStroke.key)
					{
						case KeyEvent.VK_LEFT:
							textArea.goToPrevCharacter("S".equals(keyStroke.modifiers));
							break;
						case KeyEvent.VK_RIGHT:
							textArea.goToNextCharacter("S".equals(keyStroke.modifiers));
							break;
						case KeyEvent.VK_UP:
							textArea.goToPrevLine("S".equals(keyStroke.modifiers));
							break;
						case KeyEvent.VK_DOWN:
							textArea.goToNextLine("S".equals(keyStroke.modifiers));
							break;
						case KeyEvent.VK_BACK_SPACE:
							textArea.backspace();
							break;
						case KeyEvent.VK_DELETE:
							textArea.delete();
							break;
					}
				}

			}
		}
		return false;
	} //}}}

	//{{{ userInput() method
	protected void userInput(char ch)
	{
		lastActionCount = 0;


		if(repeatCount == 1)
			textArea.userInput(ch);
		else
		{
			// stop people doing dumb stuff like C+ENTER 100 C+n
			/*if(repeatCount > REPEAT_COUNT_THRESHOLD)
			{
				Object[] pp = { String.valueOf(ch),
					repeatCount };

				if(GUIUtilities.confirm(view,
					"large-repeat-count.user-input",pp,
					JOptionPane.WARNING_MESSAGE,
					JOptionPane.YES_NO_OPTION)
					!= JOptionPane.YES_OPTION)
				{
					repeatCount = 1;
					view.getStatus().setMessage(null);
					return;
				}
			}

			JEditBuffer buffer = textArea.getBuffer();
			try
			{
				if(repeatCount != 1)
					buffer.beginCompoundEdit();
				for(int i = 0; i < repeatCount; i++)
					textArea.userInput(ch);
			}
			finally
			{
				if(repeatCount != 1)
					buffer.endCompoundEdit();
			}   */
		}

		repeatCount = 1;
	} //}}}

	//{{{ invokeReadNextChar() method
	protected void invokeReadNextChar(char ch)
	{
		String charStr = MiscUtilities.charsToEscapes(String.valueOf(ch));

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
