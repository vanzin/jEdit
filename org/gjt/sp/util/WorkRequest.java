/*
 * WorkRequest.java - Runnable subclass
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000 Slava Pestov
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

import javax.annotation.concurrent.GuardedBy;
import org.gjt.sp.util.WorkThread.Abort;

/**
 * A subclass of the Runnable interface.
 * @since jEdit 2.6pre1
 * @deprecated
 * @see org.gjt.sp.util.Task
 * @version $Id$
 */
@Deprecated
public abstract class WorkRequest implements Runnable, ProgressObserver
{
	/**
	 * If the max value is greater that <code>Integer.MAX_VALUE</code> this 
	 * will be true and all values will be divided by 1024.
	 * @since jEdit 4.3pre3
	 */
	private volatile boolean largeValues;
	@GuardedBy("this") private String status;
	@GuardedBy("this") private int progressValue;
	@GuardedBy("this") private int progressMaximum;
	@GuardedBy("this") private int runNo;   // final
	@GuardedBy("this") private boolean isRunning;

	public void workRequestStart()
	{
		int runNo = WorkThreadPool.INSTANCE.workRequestStart(this);
		synchronized (this)
		{
			this.runNo = runNo;
			this.isRunning = true;
		}
		WorkThreadPool.INSTANCE.fireStatusChanged(this);
	}

	public void workRequestEnd()
	{
		WorkThreadPool.INSTANCE.workRequestEnd(this);

		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).resetAbortable();

		synchronized (this)
		{
			status = null;
			progressValue = 0;
			progressMaximum = 0;
			isRunning = false;
		}
		WorkThreadPool.INSTANCE.fireProgressChanged(this);
		WorkThreadPool.INSTANCE.fireStatusChanged(this);
	}

	/**
	 * Sets if the request can be aborted.
	 */
	public void setAbortable(boolean abortable)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).setAbortable(abortable);
	}

	/**
	 * Aborts the currently running request, if allowed.
	 * @since jEdit 2.6pre1
	 */
	public void abortRequest()
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).abortCurrentRequest();
	}
	/**
	 * Sets the status text.
	 * @param status The status text
	 */
	public void setStatus(String status)
	{
		synchronized (this)
		{
			this.status = status;
		}
		WorkThreadPool.INSTANCE.fireProgressChanged(this);
	}

	/**
	 * Sets the progress value.
	 * @param value The progress value.
	 * @deprecated use {@link #setValue(long)}
	 */
	public void setProgressValue(int value)
	{
		synchronized (this)
		{
			this.progressValue = value;
		}
		WorkThreadPool.INSTANCE.fireProgressChanged(this);
	}

	/**
	 * Sets the maximum progress value.
	 * @param value The progress value.
	 * @deprecated use {@link #setMaximum(long)}
	 */
	public void setProgressMaximum(int value)
	{
		synchronized (this)
		{
			this.progressMaximum = value;
		}
		WorkThreadPool.INSTANCE.fireProgressChanged(this);
	}

	//{{{ setValue() method
	/**
	 * Update the progress value.
	 *
	 * @param value the new value
	 * @since jEdit 4.3pre3
	 */
	public void setValue(long value)
	{
		if (largeValues)
		{
			setProgressValue((int) (value >> 10));
		}
		else
		{
			setProgressValue((int) value);
		}
	} //}}}

	//{{{ setValue() method
	/**
	 * Update the maximum value.
	 *
	 * @param value the new maximum value
	 * @since jEdit 4.3pre3
	 */
	public void setMaximum(long value)
	{
		if (value > Integer.MAX_VALUE)
		{
			largeValues = true;
			setProgressMaximum((int) (value >> 10));
		}
		else
		{
			largeValues = false;
			setProgressMaximum((int) value);
		}
	} //}}}

	//{{{ run() method
	public void run()
	{
		Log.log(Log.DEBUG,this,"Running in thread: " + Thread.currentThread());

		workRequestStart();
		try
		{
			_run();
		}
		catch(Abort a)
		{
			Log.log(Log.ERROR,this,"Unhandled abort", a);
		}
		catch(Throwable th)
		{
			Log.log(Log.ERROR,this,"Exception in work thread: ", th);
		}
		finally
		{
			workRequestEnd();
		}
	} //}}}

	/**
	 * Returns the status text.
	 * @return the status label
	 */
	public synchronized String getStatus()
	{
		return status;
	}

	/**
	 * Returns the progress value.
	 * @return the progress value
	 */
	public synchronized int getProgressValue()
	{
		return progressValue;
	}

	/**
	 * Returns the progress maximum.
	 * @return the maximum value of the progression
	 */
	public synchronized int getProgressMaximum()
	{
		return progressMaximum;
	}

	abstract public void _run();

	public synchronized int getRunNo()
	{
		return runNo;
	}

	public synchronized boolean isRequestRunning()
	{
		return isRunning;
	}
}
