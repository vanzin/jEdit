/*
 * Task.java
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

import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;

/**
 * @author Matthieu Casanova
 */
public abstract class Task implements Runnable, ProgressObserver
{
	private long value;
	private String status;
	private long maximum;

	public enum State
	{
		Waiting, Running, Done
	}

	private State state;

	protected Task()
	{
		state = State.Waiting;
	}

	public final void run()
	{
		state = State.Running;
		TaskManager.instance.fireRunning(this);
		try
		{
			_run();
		}
		catch (Throwable t)
		{
			Log.log(Log.ERROR, this, t);
		}
		state = State.Done;
		TaskManager.instance.fireDone(this);
	}

	public abstract void _run();

	public void setValue(long value)
	{
		this.value = value;
	}

	public void setMaximum(long maximum)
	{
		this.maximum = maximum;
	}

	public void setStatus(String status)
	{
		this.status = status;
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

	public State getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "Task[" + state + ',' + status + ',' + value + '/' + maximum + ']';
	}
}
