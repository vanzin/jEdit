/*
 * InputHandler.java - Manages key bindings and executes actions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import javax.swing.JOptionPane;
import java.awt.event.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
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
public abstract class InputHandler extends KeyAdapter
{
	//{{{ InputHandler constructor
	/**
	 * Creates a new input handler.
	 * @param view The view
	 */
	public InputHandler(View view)
	{
		this.view = view;
		repeatCount = 1;
	} //}}}

	//{{{ processKeyEvent() method
	/**
	 * Utility method, calls one of {@link #keyPressed(KeyEvent)},
	 * {@link #keyReleased(KeyEvent)}, or {@link #keyTyped(KeyEvent)}.
	 * @since jEdit 4.0pre4
	 */
	public void processKeyEvent(KeyEvent evt)
	{
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			keyReleased(evt);
			break;
		}
	} //}}}

	//{{{ addKeyBinding() method
	/**
	 * Adds a key binding to this input handler.
	 * @param keyBinding The key binding (the format of this is
	 * input-handler specific)
	 * @param action The action
	 */
	public abstract void addKeyBinding(String keyBinding, EditAction action);
	//}}}

	//{{{ removeKeyBinding() method
	/**
	 * Removes a key binding from this input handler.
	 * @param keyBinding The key binding
	 */
	public abstract void removeKeyBinding(String keyBinding);
	//}}}

	//{{{ removeAllKeyBindings() method
	/**
	 * Removes all key bindings from this input handler.
	 */
	public abstract void removeAllKeyBindings();
	//}}}

	//{{{ isPrefixActive() method
	/**
	 * Returns if a prefix key has been pressed.
	 */
	public boolean isPrefixActive()
	{
		return false;
	} //}}}

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

	//{{{ getLastActionCount() method
	/**
	 * Returns the number of times the last action was executed.
	 * @since jEdit 2.5pre5
	 */
	public int getLastActionCount()
	{
		return lastActionCount;
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

	//{{{ readNextChar() method
	/**
	 * @deprecated Use the other form of this method instead
	 */
	public void readNextChar(String code)
	{
		readNextChar = code;
	} //}}}

	//{{{ resetLastActionCount() method
	/**
	 * Resets the last action count. This should be called when an
	 * editing operation that is not an action is invoked, for example
	 * a mouse click.
	 * @since jEdit 4.0pre1
	 */
	public void resetLastActionCount()
	{
		lastActionCount = 0;
	} //}}}

	//{{{ invokeAction() method
	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 */
	public void invokeAction(EditAction action)
	{
		Buffer buffer = view.getBuffer();

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

				Object[] pp = { label, new Integer(_repeatCount) };

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

	//{{{ Protected members
	private static final int REPEAT_COUNT_THRESHOLD = 20;

	//{{{ Instance variables
	protected View view;
	protected int repeatCount;

	protected EditAction lastAction;
	protected int lastActionCount;

	protected String readNextChar;
	//}}}

	//{{{ userInput() method
	protected void userInput(char ch)
	{
		lastActionCount = 0;

		if(readNextChar != null)
			invokeReadNextChar(ch);
		else
		{
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
						new Integer(repeatCount) };

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

				Buffer buffer = view.getBuffer();
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
				recorder.record(repeatCount,ch);
		}

		repeatCount = 1;
	} //}}}

	//{{{ invokeReadNextChar() method
	protected void invokeReadNextChar(char ch)
	{
		Buffer buffer = view.getBuffer();

		/* if(buffer.insideCompoundEdit())
			buffer.endCompoundEdit(); */

		String charStr = MiscUtilities.charsToEscapes(String.valueOf(ch));

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

		view.getStatus().setMessage(null);
	} //}}}

	//}}}
}
