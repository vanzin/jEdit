/*
 * TaskMonitor.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
 * Portions Copyright (C) 2010 Marcelo Vanzin
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
		JScrollPane scroll = new JScrollPane(new JList(model));
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
}
