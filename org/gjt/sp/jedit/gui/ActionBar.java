/*
 * ActionBar.java - For invoking actions directly
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
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Action invocation bar.
 */
public class ActionBar extends JPanel
{
	//{{{ ActionBar constructor
	public ActionBar(final View view, boolean temp)
	{
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

		actions = jEdit.getActions();
		Arrays.sort(actions,new MiscUtilities.StringICaseCompare());
		this.view = view;
		this.temp = temp;

		add(Box.createHorizontalStrut(2));

		JLabel label = new JLabel(jEdit.getProperty("view.action.prompt"));
		add(label);
		add(Box.createHorizontalStrut(12));
		add(action = new ActionTextField());
		Dimension max = action.getPreferredSize();
		max.width = Integer.MAX_VALUE;
		action.setMaximumSize(max);
		action.addActionListener(new ActionHandler());
		action.getDocument().addDocumentListener(new DocumentHandler());

		if(temp)
		{
			close = new RolloverButton(GUIUtilities.loadIcon("closebox.gif"));
			close.addActionListener(new ActionHandler());
			close.setToolTipText(jEdit.getProperty(
				"view.action.close-tooltip"));
			add(close);
		}

		//{{{ Create the timer used by the completion popup
		timer = new Timer(0,new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				String text = action.getText();
				if(text == null)
					return;
				text = text.trim();
				if(text.length() == 0)
					return;
				EditAction[] completions = getCompletions(text);
				for(int i = 0; i < completions.length; i++)
				{
					System.err.println(completions[i]);
				}
			}
		}); //}}}

		timerComplete();

		// if 'temp' is true, hide search bar after user is done with it
		this.temp = temp;
	} //}}}

	//{{{ getField() method
	public HistoryTextField getField()
	{
		return action;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private View view;
	private boolean temp;
	private HistoryTextField action;
	private Timer timer;
	private RolloverButton close;
	private EditAction[] actions;
	//}}}

	//{{{ timerComplete() method
	private void timerComplete()
	{
		timer.stop();
		timer.setRepeats(false);
		timer.setInitialDelay(300);
		timer.start();
	} //}}}

	//{{{ invoke() method
	private void invoke()
	{
		if(temp)
			view.removeToolBar(ActionBar.this);

		String cmd = action.getText();
		if(cmd != null)
		{
			cmd = cmd.trim();
			if(cmd.length() != 0)
			{
				int index = cmd.indexOf('=');
				if(index == -1)
				{
					EditAction[] completions = getCompletions(cmd);
					if(completions.length != 0)
					{
						EditAction act = completions[0];
						String label = act.getLabel();
						if(label == null)
							label = act.getName();
						else
							label = GUIUtilities.prettifyMenuLabel(label);
						view.getStatus().setMessageAndClear(label);
						view.getInputHandler().invokeAction(act);
					}
					else
					{
						view.getStatus().setMessageAndClear(
							jEdit.getProperty(
							"view.action.no-completions"));
					}
				}
				else
				{
					String propName = cmd.substring(0,index).trim();
					String propValue = cmd.substring(index + 1);
					if(propName.startsWith("buffer."))
					{
						Buffer buffer = view.getBuffer();
						buffer.setStringProperty(
							propName.substring("buffer.".length()),
							propValue);
						buffer.propertiesChanged();
					}
					else if(propName.startsWith("!buffer."))
					{
						jEdit.setProperty(propName.substring(1),propValue);
						jEdit.propertiesChanged();
					}
					else
					{
						jEdit.setProperty(propName,propValue);
						jEdit.propertiesChanged();
					}
				}
			}
		}

		if(hasFocus())
		{
			// if action didn't move focus out of action bar
			view.getEditPane().focusOnTextArea();
		}
	} //}}}

	//{{{ getCompletions() method
	private EditAction[] getCompletions(String str)
	{
		ArrayList returnValue = new ArrayList(actions.length);
		for(int i = 0; i < actions.length; i++)
		{
			if(actions[i].getName().indexOf(str) != -1)
				returnValue.add(actions[i]);
		}

		return (EditAction[])returnValue.toArray(new EditAction[returnValue.size()]);
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == close)
				view.removeToolBar(ActionBar.this);
			else
				invoke();
		}
	} //}}}

	//{{{ DocumentHandler class
	class DocumentHandler implements DocumentListener
	{
		//{{{ insertUpdate() method
		public void insertUpdate(DocumentEvent evt)
		{
			timerComplete();
		} //}}}

		//{{{ removeUpdate() method
		public void removeUpdate(DocumentEvent evt)
		{
			timerComplete();
		} //}}}

		//{{{ changedUpdate() method
		public void changedUpdate(DocumentEvent evt) {}
		//}}}
	} //}}}

	//{{{ ActionTextField class
	class ActionTextField extends HistoryTextField
	{
		boolean repeat;
		boolean nonDigit;

		ActionTextField()
		{
			super("action");
			setSelectAllOnFocus(true);
		}

		public void processKeyEvent(KeyEvent evt)
		{
			evt = KeyEventWorkaround.processKeyEvent(evt);
			if(evt == null)
				return;

			switch(evt.getID())
			{
			case KeyEvent.KEY_TYPED:
				char ch = evt.getKeyChar();
				if(!nonDigit && Character.isDigit(ch))
				{
					super.processKeyEvent(evt);
					repeat = true;
					timer.stop();
					view.getInputHandler().setRepeatCount(
						Integer.parseInt(action.getText()));
				}
				else
				{
					nonDigit = true;
					if(repeat)
						passToView(evt);
					else
						super.processKeyEvent(evt);
				}
				break;
			case KeyEvent.KEY_PRESSED:
				int keyCode = evt.getKeyCode();
				if(evt.isActionKey()
					|| evt.isControlDown()
					|| evt.isAltDown()
					|| evt.isMetaDown()
					|| keyCode == KeyEvent.VK_ENTER
					|| keyCode == KeyEvent.VK_TAB
					|| keyCode == KeyEvent.VK_ESCAPE)
				{
					nonDigit = true;
					if(repeat)
					{
						passToView(evt);
						break;
					}
					else if(keyCode == KeyEvent.VK_ESCAPE)
					{
						evt.consume();
						if(temp)
							view.removeToolBar(ActionBar.this);
						view.getEditPane().focusOnTextArea();
						break;
					}
				}
				super.processKeyEvent(evt);
				break;
			}
		}

		private void passToView(KeyEvent evt)
		{
			if(temp)
				view.removeToolBar(ActionBar.this);
			view.getTextArea().processKeyEvent(evt);
			if(hasFocus())
			{
				// if action didn't move focus out of action bar
				view.getEditPane().focusOnTextArea();
			}
		}

		public void addNotify()
		{
			super.addNotify();
			repeat = nonDigit = false;
		}
	} //}}}

	//}}}
}
