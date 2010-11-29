/*
 * WorkThread.java - Background thread that does stuff
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

/**
 * Services work requests in the background.
 * @author Slava Pestov
 * @deprecated
 * @see org.gjt.sp.util.ThreadUtilities
 * @version $Id$
 */
@Deprecated
public class WorkThread extends Thread implements ThreadAbortMonitor
{
	public WorkThread(WorkThreadPool pool, ThreadGroup group, String name)
	{
		super(group, name);
		// so that jEdit doesn't exit with no views open automatically
		//setDaemon(true);
		setPriority(Thread.MIN_PRIORITY);

		this.pool = pool;
	}

	/**
	 * Sets if the current request can be aborted.
	 * If set to true and already aborted, the thread will be stopped
	 *
	 * @param abortable true if the WorkThread is abortable
	 * @since jEdit 2.6pre1
	 */
	public void setAbortable(boolean abortable)
	{
		synchronized(abortLock)
		{
			this.abortable = abortable;
			if(aborted)
				stop(new Abort());
		}
	}

	/**
	 * Returns if the work thread is currently running a request.
	 * @return true if a request is currently running
	 */
	public boolean isRequestRunning()
	{
		return requestRunning;
	}


	public boolean isAborted()
	{
		synchronized (abortLock)
		{
			return aborted;
		}
	}

	/**
	 * Returns the status text.
	 * @return the status label
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * Sets the status text.
	 * @param status the new status of the thread
	 * @since jEdit 2.6pre1
	 */
	public void setStatus(String status)
	{
		this.status = status;
		pool.fireProgressChanged(this);
	}

	/**
	 * Returns the progress value.
	 * @return the progress value
	 */
	public int getProgressValue()
	{
		return progressValue;
	}

	/**
	 * Sets the progress value.
	 * @param progressValue the new progress value
	 * @since jEdit 2.6pre1
	 */
	public void setProgressValue(int progressValue)
	{
		this.progressValue = progressValue;
		pool.fireProgressChanged(this);
	}

	/**
	 * Returns the progress maximum.
	 * @return the maximum value of the progression
	 */
	public int getProgressMaximum()
	{
		return progressMaximum;
	}

	/**
	 * Sets the maximum progress value.
	 * @param progressMaximum the maximum value of the progression
	 * @since jEdit 2.6pre1
	 */
	public void setProgressMaximum(int progressMaximum)
	{
		this.progressMaximum = progressMaximum;
		pool.fireProgressChanged(this);
	}

	/**
	 * Aborts the currently running request, if allowed.
	 * @since jEdit 2.6pre1
	 */
	public void abortCurrentRequest()
	{
		synchronized(abortLock)
		{
			if(abortable && !aborted)
				stop(new Abort());
			aborted = true;
		}
	}

	public void run()
	{
		Log.log(Log.DEBUG,this,"Work request thread starting [" + getName() + "]");

		for(;;)
		{
			doRequests();
		}
	}

	// private members
	private WorkThreadPool pool;
	private final Object abortLock = new Object();
	private boolean requestRunning;
	private boolean abortable;
	private boolean aborted;
	private String status;
	private int progressValue;
	private int progressMaximum;

	private void doRequests()
	{
		WorkThreadPool.Request request;
		for(;;)
		{
			request = pool.getNextRequest();
			if(request == null)
				break;
			else
			{
				requestRunning = true;
				pool.fireStatusChanged(this);
				doRequest(request);
				requestRunning = false;
			}
		}

		pool.fireStatusChanged(this);

		synchronized(pool.waitForAllLock)
		{
			// notify a running waitForRequests() method
			pool.waitForAllLock.notifyAll();
		}

		synchronized(pool.lock)
		{
			// wait for more requests
			try
			{
				pool.lock.wait();
			}
			catch(InterruptedException ie)
			{
				Log.log(Log.ERROR,this,ie);
			}
		}
	}

	private void doRequest(WorkThreadPool.Request request)
	{
		Log.log(Log.DEBUG,WorkThread.class,"Running in work thread: " + request);

		try
		{
			request.run.run();
		}
		catch(Abort a)
		{
			Log.log(Log.ERROR,WorkThread.class,"Unhandled abort", a);
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,WorkThread.class,"Exception in work thread: ", t);
		}
		finally
		{
			synchronized(abortLock)
			{
				aborted = abortable = false;
			}
			status = null;
			progressValue = progressMaximum = 0;
			pool.requestDone();
			pool.fireStatusChanged(this);
		}
	}

	public static class Abort extends Error
	{
		public Abort()
		{
			super("Work request aborted");
		}
	}
}
