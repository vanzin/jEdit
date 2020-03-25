/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011-2013 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui.statusbar;

//{{{ Imports
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;

import javax.swing.*;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.FieldPosition;
import java.text.MessageFormat;
//}}}

/**
 * A Statusbar widget that monitor the task manager.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.5pre1
 */
public class TaskMonitorWidgetFactory implements StatusWidgetFactory
{
	//{{{ getWidget() method
	@Override
	public Widget getWidget(View view)
	{
		Widget widget = new TaskMonitorWidget(view);
		widget.getComponent().setToolTipText(jEdit.getProperty("statusbar.task-monitor.tooltip"));
		return widget;
	} //}}}

	//{{{ TaskMonitorWidget class
	private static class TaskMonitorWidget extends JLabel implements Widget, TaskListener
	{
		private final MessageFormat messageFormat;
		private final Object[] args;
		private final StringBuffer stringBuffer;
		private final FieldPosition fieldPosition;

		private TaskMonitorWidget(View view)
		{
			setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			setFont(getFont().deriveFont(Font.BOLD));
			String property = jEdit.getProperty("statusbar.task-monitor.template");
			args = new Object[1];
			messageFormat = new MessageFormat(property);
			fieldPosition = new FieldPosition(0);
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						view.getDockableWindowManager().showDockableWindow("task-monitor");
					}
				}
			});
			stringBuffer = new StringBuffer();
		}

		@Override
		public void addNotify()
		{
			super.addNotify();
			TaskManager.instance.addTaskListener(this);
			update();
		}

		@Override
		public void removeNotify()
		{
			super.removeNotify();
			TaskManager.instance.removeTaskListener(this);
		}

		@Override
		public JComponent getComponent()
		{
			return this;
		}

		@Override
		public void propertiesChanged()
		{
		}

		@Override
		public void update()
		{
			int count = TaskManager.instance.countTasks();
			if (count == 0)
			{
				setIcon(null);
				setText(null);
			}
			else
			{
				synchronized (messageFormat)
				{
					setIcon(GUIUtilities.loadIcon("loader.gif"));
					args[0] = count;
					setText(messageFormat.format(args, stringBuffer, fieldPosition).toString());
					stringBuffer.setLength(0);
				}
			}
		}

		@Override
		public void waiting(Task task)
		{
			update();
		}

		@Override
		public void running(Task task)
		{
			update();
		}

		@Override
		public void done(Task task)
		{
			update();
		}

		@Override
		public void statusUpdated(Task task)
		{
			update();
		}

		@Override
		public void maximumUpdated(Task task)
		{
			update();
		}

		@Override
		public void valueUpdated(Task task)
		{
			update();
		}
	} //}}}
}