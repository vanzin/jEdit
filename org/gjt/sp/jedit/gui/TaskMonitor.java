/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010 jEdit contributors
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

package org.gjt.sp.jedit.gui;

import org.gjt.sp.util.Task;
import org.gjt.sp.util.TaskListener;
import org.gjt.sp.util.TaskManager;

import javax.swing.*;
import java.awt.*;

/**
 * @author Matthieu Casanova
 */
public class TaskMonitor  extends JPanel implements TaskListener
{
	private DefaultListModel model;

	public TaskMonitor()
	{
		super(new BorderLayout());
		model = new DefaultListModel();
		JList taskList = new JList(model);
		taskList.setCellRenderer(new TaskCellRenderer());
		JScrollPane scroll = new JScrollPane(taskList);
		add(scroll);
	}

	@Override
	public void addNotify()
	{
		TaskManager.instance.addTaskListener(this);
		super.addNotify();
	}

	@Override
	public void removeNotify()
	{
		TaskManager.instance.removeTaskListener(this);
		super.removeNotify();    
	}

	public void waiting(Task task)
	{
		model.addElement(task);
	}

	public void running(Task task)
	{
		repaint();
	}

	public void done(Task task)
	{
		model.removeElement(task);
	}

	public void statusUpdated(Task task)
	{
		repaint();
	}

	public void maximumUpdated(Task task)
	{
		repaint();
	}

	public void valueUpdated(Task task)
	{
		repaint();
	}

	private static class TaskCellRenderer extends JProgressBar implements ListCellRenderer
	{
		private TaskCellRenderer()
		{
			setStringPainted(true);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
							      boolean cellHasFocus)
		{
			Task task = (Task) value;
			task.getMaximum();
			if (task.getMaximum() == 0L)
			{
				setIndeterminate(true);
			}
			else
			{
				setIndeterminate(false);
				long max = task.getMaximum();
				long val = task.getValue();
				if (max > Integer.MAX_VALUE)
				{
					max >>= 10L;
					val >>= 10L;

				}
				setMaximum((int) max);
				setValue((int) val);
			}
			setToolTipText(task.getLabel());
			setString(task.getStatus());
			return this; 
		}
	}
}
