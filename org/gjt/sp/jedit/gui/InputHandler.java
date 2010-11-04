/*
 * InputHandler.java - Manages key bindings and executes actions
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
import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.input.AbstractInputHandler;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import java.awt.event.KeyEvent;
import java.awt.*;
//}}}

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.<p>
 *
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov
 * @version $Id$
 * @see org.gjt.sp.jedit.gui.DefaultInputHandler
 */
public abstract class InputHandler extends AbstractInputHandler<EditAction>
{
	//{{{ InputHandler constructor
	/**
	 * Creates a new input handler.
	 * @param view The view
	 */
	protected InputHandler(View view)
	{
		this.view = view;
	} //}}}

	//{{{ handleKey() method
	/**
	 * Handles a keystroke.
	 * @param keyStroke The key stroke.
	 * @return true if the input could be handled.
	 * @since jEdit 4.2pre5
	 */
	public final boolean handleKey(KeyEventTranslator.Key keyStroke)
	{
		return handleKey(keyStroke, false);
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 * @since 4.3pre7
	 */
	@Override
	public void processKeyEvent(KeyEvent evt, int from, boolean global)
	{
		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event                 : "
				+ AbstractInputHandler.toString(evt) + " from " + from);
			Log.log(Log.DEBUG,this,view+".isFocused()="+view.isFocused()+'.',new Exception());
		}

		if(view.getTextArea().hasFocus() && from == View.VIEW)
			return;

		evt = _preprocessKeyEvent(evt);
		if(evt == null)
			return;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event after workaround: "
				+ AbstractInputHandler.toString(evt) + " from " + from);
		}

		Component prefixFocusOwner = view.getPrefixFocusOwner();
		boolean focusOnTextArea = false;
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			// if the user pressed eg C+e n n in the
			// search bar we want focus to go back there
			// after the prefix is done
			if(prefixFocusOwner != null)
			{
				if(prefixFocusOwner.isShowing())
				{
					prefixFocusOwner.requestFocus();
					focusOnTextArea = true;
				}
			}

			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else if(from == View.ACTION_BAR
				|| isPrefixActive()
				|| view.getTextArea().hasFocus())
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
				if(prefixFocusOwner != null)
				{
					if(prefixFocusOwner.isShowing())
					{
						prefixFocusOwner.requestFocus();
						focusOnTextArea = true;
					}
					view.setPrefixFocusOwner(null);
				}

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
		if(view.isClosed())
			return null;
		Component focusOwner = view.getFocusOwner();
		if(focusOwner instanceof JComponent)
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
		
		if(focusOwner instanceof JTextComponent)
		{
			// fix for the bug where key events in JTextComponents
			// inside views are also handled by the input handler
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_TAB:
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_SPACE:
					return null;
				}
			}
		}

		if(evt.isConsumed())
			return null;

		if(Debug.DUMP_KEY_EVENTS)
		{
			Log.log(Log.DEBUG,this,"Key event (preprocessing) : "
					+ AbstractInputHandler.toString(evt));
		}

		return KeyEventWorkaround.processKeyEvent(evt);
	} //}}}

	//{{{ processKeyEventSub() method
	private void processKeyEventSub(boolean focusOnTextArea)
	{
		// we might have been closed as a result of
		// the above
		if(view.isClosed())
			return;

		// this is a weird hack.
		// we don't want C+e a to insert 'a' in the
		// search bar if the search bar has focus...
		if(isPrefixActive())
		{
			Component focusOwner = view.getFocusOwner();
			if(focusOwner instanceof JTextComponent)
			{
				view.setPrefixFocusOwner(focusOwner);
				view.getTextArea().requestFocus();
			}
			else if(focusOnTextArea)
			{
				view.getTextArea().requestFocus();
			}
			else
			{
				view.setPrefixFocusOwner(null);
			}
		}
		else
		{
			view.setPrefixFocusOwner(null);
		}
	}
	//}}}

	//{{{ getRepeatCount() method
	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount()
	{
		return repeatCount;
	} //}}}

	//{{{ setRepeatCount() method
	/**
	 * Sets the number of times the next action will be repeated.
	 * @param repeatCount The repeat count
	 */
	public void setRepeatCount(int repeatCount)
	{
		int oldRepeatCount = this.repeatCount;
		this.repeatCount = repeatCount;
		if(oldRepeatCount != repeatCount)
			view.getStatus().setMessage(null);
	} //}}}

	//{{{ getLastAction() method
	/**
	 * Returns the last executed action.
	 * @since jEdit 2.5pre5
	 */
	public EditAction getLastAction()
	{
		return lastAction;
	} //}}}

	//{{{ readNextChar() method
	/**
	 * Invokes the specified BeanShell code, replacing __char__ in the
	 * code with the next input character.
	 * @param msg The prompt to display in the status bar
	 * @param code The code
	 * @since jEdit 3.2pre2
	 */
	public void readNextChar(String msg, String code)
	{
		view.getStatus().setMessage(msg);
		readNextChar = code;
	} //}}}

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
		invokeAction(jEdit.getAction(action));
	} //}}}

	//{{{ invokeAction() method
	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 */
	@Override
	public void invokeAction(EditAction action)
	{
		JEditBuffer buffer = view.getBuffer();

		/* if(buffer.insideCompoundEdit())
			buffer.endCompoundEdit(); */

		// remember the last executed action
		if(!action.noRememberLast())
		{
			HistoryModel.getModel("action").addItem(action.getName());
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
			action.invoke(view);
		else
		{
			// stop people doing dumb stuff like C+ENTER 100 C+n
			if(_repeatCount > REPEAT_COUNT_THRESHOLD)
			{
				String label = action.getLabel();
				if(label == null)
					label = action.getName();
				else
					label = GUIUtilities.prettifyMenuLabel(label);

				Object[] pp = { label, _repeatCount };

				if(GUIUtilities.confirm(view,"large-repeat-count",pp,
					JOptionPane.WARNING_MESSAGE,
					JOptionPane.YES_NO_OPTION)
					!= JOptionPane.YES_OPTION)
				{
					repeatCount = 1;
					view.getStatus().setMessage(null);
					return;
				}
			}

			try
			{
				buffer.beginCompoundEdit();

				for(int i = 0; i < _repeatCount; i++)
					action.invoke(view);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}

		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null && !action.noRecord())
			recorder.record(_repeatCount,action.getCode());

		// If repeat was true originally, clear it
		// Otherwise it might have been set by the action, etc
		if(_repeatCount != 1)
		{
			// first of all, if this action set a
			// readNextChar, do not clear the repeat
			if(readNextChar != null)
				return;

			repeatCount = 1;
			view.getStatus().setMessage(null);
		}
	} //}}}

	//{{{ invokeLastAction() method
	public void invokeLastAction()
	{
		if(lastAction == null)
			view.getToolkit().beep();
		else
			invokeAction(lastAction);
	} //}}}

	//{{{ Instance variables
	protected final View view;

	//}}}

	//{{{ userInput() method
	protected void userInput(char ch)
	{
		lastActionCount = 0;

		JEditTextArea textArea = view.getTextArea();

		/* Buffer buffer = view.getBuffer();
		if(!buffer.insideCompoundEdit())
			buffer.beginCompoundEdit(); */

		if(repeatCount == 1)
			textArea.userInput(ch);
		else
		{
			// stop people doing dumb stuff like C+ENTER 100 C+n
			if(repeatCount > REPEAT_COUNT_THRESHOLD)
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

			JEditBuffer buffer = view.getBuffer();
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
			}
		}

		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null)
		{
			recorder.recordInput(repeatCount,ch,
				textArea.isOverwriteEnabled());
		}

		repeatCount = 1;
	} //}}}

	//{{{ invokeReadNextChar() method
	protected void invokeReadNextChar(char ch)
	{
		JEditBuffer buffer = view.getBuffer();

		/* if(buffer.insideCompoundEdit())
			buffer.endCompoundEdit(); */

		String charStr = StandardUtilities.charsToEscapes(String.valueOf(ch));

		// this might be a bit slow if __char__ occurs a lot
		int index;
		while((index = readNextChar.indexOf("__char__")) != -1)
		{
			readNextChar = readNextChar.substring(0,index)
				+ '\'' + charStr + '\''
				+ readNextChar.substring(index + 8);
		}

		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record(getRepeatCount(),readNextChar);

		view.getStatus().setMessage(null);

		if(getRepeatCount() != 1)
		{
			try
			{
				buffer.beginCompoundEdit();

				BeanShell.eval(view,BeanShell.getNameSpace(),
					"for(int i = 1; i < "
					+ getRepeatCount() + "; i++)\n{\n"
					+ readNextChar + "\n}");
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
		else
			BeanShell.eval(view,BeanShell.getNameSpace(),readNextChar);

		readNextChar = null;
	} //}}}
}
