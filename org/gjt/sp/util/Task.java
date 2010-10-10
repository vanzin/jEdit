/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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

package org.gjt.sp.util;

import javax.swing.*;

/**
 * A Task is basically a Runnable but with some information about it's progression.
 * @since jEdit 4.4pre1
 * @author Matthieu Casanova
 */
public abstract class Task implements Runnable, ProgressObserver
{
	private long value;
	private String status;
	private long maximum;

	private String label;

	/**
	 * The thread in which the task is running.
	 * It is set automatically when the task starts.
	 */
	private Thread thread;

	private SwingWorker.StateValue state;

	private volatile boolean cancellable = true;

	//{{{ Task Constructor
	protected Task()
	{
		state = SwingWorker.StateValue.PENDING;
	} //}}}

	//{{{ run() method
	public final void run()
	{
		state = SwingWorker.StateValue.STARTED;
		TaskManager.instance.fireRunning(this);
		try
		{
			thread = Thread.currentThread();
			_run();
			thread = null;
		}
		catch (Throwable t)
		{
			Log.log(Log.ERROR, this, t);
		}
		state = SwingWorker.StateValue.DONE;
		TaskManager.instance.fireDone(this);
	} //}}}

	/**
	 * This is the method you have to implement and that will be executed
	 * in the thread.
	 */
	public abstract void _run();

	public final void setValue(long value)
	{
		this.value = value;
		TaskManager.instance.fireValueUpdated(this);
	}

	public final void setMaximum(long maximum)
	{
		this.maximum = maximum;
		TaskManager.instance.fireMaximumUpdated(this);
	}

	public void setStatus(String status)
	{
		this.status = status;
		TaskManager.instance.fireStatusUpdated(this);
	}

	public long getValue()
	{
		return value;
	}

	public String getStatus()
	{
		return status;
	}

	public long getMaximum()
	{
		return maximum;
	}

	public SwingWorker.StateValue getState()
	{
		return state;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public boolean isCancellable()
	{
		return cancellable;
	}

	public void setCancellable(boolean cancellable)
	{
		this.cancellable = cancellable;
	}

	//{{{ cancel() method
	/**
	 * Cancel the task
	 */
	public void cancel()
	{
		if (cancellable && thread != null)
			thread.interrupt();
	} //}}}


	@Override
	public String toString()
	{
		return "Task[" + state + ',' + status + ',' + value + '/' + maximum + ']';
	}
}
