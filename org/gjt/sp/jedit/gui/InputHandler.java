/*
 * InputHandler.java - Manages key bindings and executes actions
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

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.event.*;
import java.awt.Component;
import java.util.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

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
	/**
	 * Creates a new input handler.
	 * @param view The view
	 */
	public InputHandler(View view)
	{
		this.view = view;
	}

	/**
	 * Adds a key binding to this input handler.
	 * @param keyBinding The key binding (the format of this is
	 * input-handler specific)
	 * @param action The action
	 */
	public abstract void addKeyBinding(String keyBinding, EditAction action);

	/**
	 * Removes a key binding from this input handler.
	 * @param keyBinding The key binding
	 */
	public abstract void removeKeyBinding(String keyBinding);

	/**
	 * Removes all key bindings from this input handler.
	 */
	public abstract void removeAllKeyBindings();

	/**
	 * Returns if a prefix key has been pressed.
	 */
	public boolean isPrefixActive()
	{
		return false;
	}

	/**
	 * Returns if repeating is enabled. When repeating is enabled,
	 * actions will be executed multiple times. This is usually
	 * invoked with a special key stroke in the input handler.
	 */
	public boolean isRepeatEnabled()
	{
		return repeat;
	}

	/**
	 * Enables repeating. When repeating is enabled, actions will be
	 * executed multiple times. Once repeating is enabled, the input
	 * handler should read a number from the keyboard.
	 */
	public void setRepeatEnabled(boolean repeat)
	{
		this.repeat = repeat;
		repeatCount = 0;
		view.getStatus().setMessage(null);
	}

	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount()
	{
		return (repeat && repeatCount > 0 ? repeatCount : 1);
	}

	/**
	 * Sets the number of times the next action will be repeated.
	 * @param repeatCount The repeat count
	 */
	public void setRepeatCount(int repeatCount)
	{
		repeat = true;
		this.repeatCount = repeatCount;
		view.getStatus().setMessage(null);
	}

	/**
	 * Returns the last executed action.
	 * @since jEdit 2.5pre5
	 */
	public EditAction getLastAction()
	{
		return lastAction;
	}

	/**
	 * Returns the number of times the last action was executed.
	 * @since jEdit 2.5pre5
	 */
	public int getLastActionCount()
	{
		return lastActionCount;
	}

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
		readNextChar(code);
	}

	/**
	 * @deprecated Use the other form of this method instead
	 */
	public void readNextChar(String code)
	{
		readNextChar = code;
	}

	/**
	 * Invokes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 * @param source The event source
	 */
	public void invokeAction(EditAction action)
	{
		Buffer buffer = view.getBuffer();

		buffer.endCompoundEdit();

		// remember the last executed action
		if(lastAction == action)
			lastActionCount++;
		else
		{
			lastAction = action;
			lastActionCount = 1;
		}

		// remember old values, in case action changes them
		boolean _repeat = repeat;
		int _repeatCount = getRepeatCount();

		// execute the action
		if(action.noRepeat() || _repeatCount == 1)
			action.invoke(view);
		else
		{
			// stop people doing dumb stuff like C+ENTER 100 C+n
			if(_repeatCount > REPEAT_COUNT_THRESHOLD)
			{
				String label = jEdit.getProperty(action.getLabel());
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
					repeat = false;
					repeatCount = 0;
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
		if(_repeat)
		{
			// first of all, if this action set a
			// readNextChar, do not clear the repeat
			if(readNextChar != null)
				return;

			repeat = false;
			repeatCount = 0;
			view.getStatus().setMessage(null);
		}
	}

	// protected members
	private static final int REPEAT_COUNT_THRESHOLD = 20;

	protected View view;
	protected boolean repeat;
	protected int repeatCount;

	protected EditAction lastAction;
	protected int lastActionCount;

	protected String readNextChar;

	protected void userInput(char ch)
	{
		lastAction = null;

		if(readNextChar != null)
			invokeReadNextChar(ch);
		else
		{
			Buffer buffer = view.getBuffer();
			if(!buffer.insideCompoundEdit())
				buffer.beginCompoundEdit();

			JEditTextArea textArea = view.getTextArea();
			int _repeatCount = getRepeatCount();
			if(_repeatCount == 1)
				textArea.userInput(ch);
			else
			{
				// stop people doing dumb stuff like C+ENTER 100 C+n
				if(_repeatCount > REPEAT_COUNT_THRESHOLD)
				{
					Object[] pp = { String.valueOf(ch),
						new Integer(_repeatCount) };

					if(GUIUtilities.confirm(view,
						"large-repeat-count.user-input",pp,
						JOptionPane.WARNING_MESSAGE,
						JOptionPane.YES_NO_OPTION)
						!= JOptionPane.YES_OPTION)
					{
						repeat = false;
						repeatCount = 0;
						view.getStatus().setMessage(null);
						return;
					}
				}

				for(int i = 0; i < _repeatCount; i++)
					textArea.userInput(ch);
			}

			Macros.Recorder recorder = view.getMacroRecorder();

			if(recorder != null)
				recorder.record(_repeatCount,ch);
		}

		setRepeatEnabled(false);
	}

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

		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record(getRepeatCount(),readNextChar);

		if(getRepeatCount() != 1)
		{
			Buffer buffer = view.getBuffer();

			try
			{
				buffer.beginCompoundEdit();

				BeanShell.eval(view,"for(int i = 1; i < "
					+ getRepeatCount() + "; i++)\n{\n"
					+ readNextChar + "\n}",false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
		else
			BeanShell.eval(view,readNextChar,false);

		readNextChar = null;

		view.getStatus().setMessage(null);
	}
}
