/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
 * Portions copyright (C) 2001 Mike Dillon
 * Portions copyright (C) 2008-2021 Matthieu Casanova
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
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.statusbar.StatsBarWidgetPanel;
import org.gjt.sp.jedit.gui.statusbar.StatusBarEventType;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskAdapter;
import org.gjt.sp.util.TaskManager;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
//}}}

/** The status bar used to display various information to the user.
 *
 * Currently, it is used for the following:
 * <ul>
 * <li>Displaying caret position information
 * <li>Displaying {@link InputHandler#readNextChar(String,String)} prompts
 * <li>Displaying {@link #setMessage(String)} messages
 * <li>Displaying I/O progress
 * <li>Displaying various editor settings
 * <li>Displaying memory status
 * </ul>
 *
 * @version $Id$
 * @author Slava Pestov
 * @since jEdit 3.2pre2
 */
public class StatusBar extends JPanel
{
	//{{{ StatusBar constructor
	public StatusBar(View view)
	{
		super(new BorderLayout());
		setName("StatusBar");
		setBorder(new CompoundBorder(new EmptyBorder(4,0,0,
			OperatingSystem.isMacOS() ? 18 : 0),
			UIManager.getBorder("TextField.border")));

		this.view = view;

		leadingWidgetsBox = new StatsBarWidgetPanel("view.status-leading", view);
		trailingWidgetsBox = new StatsBarWidgetPanel("view.status", view);
		panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.WEST, leadingWidgetsBox);
		panel.add(BorderLayout.EAST, trailingWidgetsBox);
		add(BorderLayout.CENTER,panel);

		message = new JLabel(" ");
		setMessageComponent(message);

		taskHandler = new TaskHandler();
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		Color fg = jEdit.getColorProperty("view.status.foreground");
		Color bg = jEdit.getColorProperty("view.status.background");

		panel.setBackground(bg);
		panel.setForeground(fg);
		message.setBackground(bg);
		message.setForeground(fg);

		leadingWidgetsBox.propertiesChanged();
		trailingWidgetsBox.propertiesChanged();

		updateBufferStatus();
		updateMiscStatus();
	} //}}}

	//{{{ addNotify() method
	@Override
	public void addNotify()
	{
		super.addNotify();
		TaskManager.instance.addTaskListener(taskHandler);
	} //}}}

	//{{{ removeNotify() method
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		TaskManager.instance.removeTaskListener(taskHandler);
	} //}}}

	//{{{ TaskListener implementation
	private class TaskHandler extends TaskAdapter
	{
		private final Runnable statusLineIo = new Runnable()
		{
			@Override
			public void run()
			{
				// don't obscure existing message
				if(!currentMessageIsIO && message != null && !message.getText().trim().isEmpty())
					return;

				int requestCount = TaskManager.instance.countIoTasks();
				if(requestCount == 0)
				{
					setMessageAndClear(jEdit.getProperty(
						"view.status.io.done"));
					currentMessageIsIO = true;
				}
				else if(requestCount == 1)
				{
					setMessage(jEdit.getProperty(
						"view.status.io-1"));
					currentMessageIsIO = true;
				}
				else
				{
					Object[] args = {requestCount};
					setMessage(jEdit.getProperty(
						"view.status.io",args));
					currentMessageIsIO = true;
				}
			}
		};

		//{{{ waiting() method
		@Override
		public void waiting(Task task)
		{
			SwingUtilities.invokeLater(statusLineIo);
		} //}}}

		//{{{ done() method
		@Override
		public void done(Task task)
		{
			SwingUtilities.invokeLater(statusLineIo);
		} //}}}
	} //}}}

	//}}}

	//{{{ getMessage() method
	/**
	 * Returns the current message.
	 *
	 * @return the current message
	 * @since jEdit 4.4pre1
	 */
	public String getMessage()
	{
		return message.getText();
	} //}}}

	//{{{ setMessageAndClear() method
	/**
	 * Show a message for a short period of time.
	 * @param message The message
	 * @since jEdit 3.2pre5
	 */
	public void setMessageAndClear(String message)
	{
		setMessage(message);

		tempTimer = new Timer(0, evt ->
		{
			// so if view is closed in the meantime...
			if(isShowing())
				setMessage(null);
		});

		tempTimer.setInitialDelay(10000);
		tempTimer.setRepeats(false);
		tempTimer.start();
	} //}}}

	//{{{ setMessage() method
	/**
	 * Displays a status message.
	 * @param message the message to display, it can be null
	 */
	public void setMessage(String message)
	{
		if(tempTimer != null)
		{
			tempTimer.stop();
			tempTimer = null;
		}

		setMessageComponent(this.message);

		if(message == null)
		{
			if(view.getMacroRecorder() != null)
				this.message.setText(jEdit.getProperty("view.status.recording"));
			else
				this.message.setText(" ");
		}
		else
			this.message.setText(message);
	} //}}}

	//{{{ setMessageComponent() method
	public void setMessageComponent(Component comp)
	{
		currentMessageIsIO = false;

		if (comp == null || messageComp == comp)
		{
			return;
		}

		messageComp = comp;
		panel.add(BorderLayout.CENTER, messageComp);
	} //}}}

	//{{{ updateCaretStatus() method
	/** Updates the status bar with information about the caret position, line number, etc */
	public void updateCaretStatus()
	{
		updateEvent(StatusBarEventType.Caret);
	} //}}}

	//{{{ updateBufferStatus() method
	public void updateBufferStatus()
	{
		updateEvent(StatusBarEventType.Buffer);
	} //}}}

	//{{{ updateMiscStatus() method
	public void updateMiscStatus()
	{
		updateEvent(StatusBarEventType.Misc);
	} //}}}

	//{{{ updateEvent() method
	/**
	 * Update the widgets that are interested in the given event type
	 * @param statusBarEventType the event type
	 * @since jEdit 5.7pre1
	 */
	public void updateEvent(StatusBarEventType statusBarEventType)
	{
		leadingWidgetsBox.updateEvent(statusBarEventType);
		trailingWidgetsBox.updateEvent(statusBarEventType);
	} //}}}

	//{{{ Private members
	private final TaskHandler taskHandler;
	private final View view;
	private final JPanel panel;
	private final StatsBarWidgetPanel leadingWidgetsBox;
	private final StatsBarWidgetPanel trailingWidgetsBox;
	private Component messageComp;
	private final JLabel message;

	@Nullable
	private Timer tempTimer;
	private boolean currentMessageIsIO;
	//}}}
}
