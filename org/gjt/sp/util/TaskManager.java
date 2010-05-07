/*
 * TaskManager.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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
package org.gjt.sp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Matthieu Casanova
 */
public class TaskManager
{
	public static final TaskManager instance = new TaskManager();

	private List<TaskListener> listeners;

	private TaskManager()
	{
		listeners = new CopyOnWriteArrayList<TaskListener>();
	}

	public void addTaskListener(TaskListener listener)
	{
		if (!listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}

	public void removeTaskListener(TaskListener listener)
	{
		if (listeners.contains(listener))
			listeners.remove(listener);
	}

	void fireWaiting(Task task)
	{
		List<TaskListener> listeners = this.listeners;
		for (TaskListener listener : listeners)
		{
			listener.waiting(task);
		}
	}

	void fireRunning(Task task)
	{
		List<TaskListener> listeners = this.listeners;
		for (TaskListener listener : listeners)
		{
			listener.running(task);
		}
	}

	void fireDone(Task task)
	{
		List<TaskListener> listeners = this.listeners;
		for (TaskListener listener : listeners)
		{
			listener.done(task);
		}
	}

	public static Task decorate(Runnable runnable)
	{
		return new MyTask(runnable);
	}

	private static class MyTask extends Task
	{
		private Runnable runnable;

		private MyTask(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void _run()
		{
			runnable.run();
		}
	}
}
