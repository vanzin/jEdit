/*
 * WorkThreadPool.java - Background thread pool that does stuff
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

//{{{ Imports
import javax.annotation.concurrent.GuardedBy;
import javax.swing.event.EventListenerList;

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicInteger;
//}}}

/**
 * A pool of work threads.
 * @author Slava Pestov
 * @version $Id$
 * @deprecated
 * @see org.gjt.sp.util.ThreadUtilities
 * @since jEdit 2.6pre1
 */
@Deprecated
public enum WorkThreadPool
{
	INSTANCE("jEdit I/O");

	//{{{ WorkThreadPool constructor
	/**
	 * Creates a new work thread pool with the specified number of
	 * work threads.
	 * @param name The thread name prefix
	 * @param count The number of work threads
	 */
	private WorkThreadPool(String name)
	{
		listenerList = new EventListenerList();
//		jEdit.getIntegerProperty("ioThreadCount", 4)
	} //}}}

	//{{{ start() method
	/**
	 * Queue the AWT runner for the first time.
	 */
	public void start()
	{
		synchronized(lock)
		{
			started = true;
			queueAWTRunner();
		}
	} //}}}

	//{{{ addWorkRequest() method
	/**
	 * Adds a work request to the queue. Only in one case the
	 * <code>Runnable</code> is executed directly: if <code>inAWT</code>
	 * is <code>true</code> and the queue is empty
	 * at the moment of call.
	 * @param run The runnable
	 * @param inAWT If true, will be executed in AWT thread. Otherwise,
	 * will be executed in work thread.
	 */
	public void addWorkRequest(Runnable run, boolean inAWT)
	{

		if(inAWT)
		{
			synchronized(lock)
			{
				//{{{ if there are no requests, execute AWT requests immediately
				if(started && TaskManager.INSTANCE.countIoTasks() == 0 && awtRequestCount == 0)
				{
//					Log.log(Log.DEBUG,this,"AWT immediate: " + run);

					ThreadUtilities.runInDispatchThread(run);
					return;
				} //}}}

				Request request = new Request(run);
	
				if(firstAWTRequest == null && lastAWTRequest == null)
					firstAWTRequest = lastAWTRequest = request;
				else
				{
					lastAWTRequest.next = request;
					lastAWTRequest = request;
				}
	
				awtRequestCount++;
			}

			// queue AWT request
			queueAWTRunner();
		} else
			throw new IllegalArgumentException();
	} //}}}

	//{{{ getRequestCount() method
	/**
	 * Returns the number of pending requests.
	 * @return the pending request count
	 */
	public int getRequestCount()
	{
		return TaskManager.INSTANCE.countIoTasks();
	} //}}}

	//{{{ getThreadCount() method
	/**
	 * Returns the number of threads in this pool.
	 * @return the thread count
	 */
	public int getThreadCount()
	{
		return 0;
	} //}}}

	public WorkRequest getWorkRequestWithRunNo(int index)
	{
		throw new UnsupportedOperationException();
	}

	//{{{ addProgressListener() method
	/**
	 * Adds a progress listener to this thread pool.
	 * @param listener The listener
	 */
	public void addProgressListener(WorkThreadProgressListener listener)
	{
		listenerList.add(WorkThreadProgressListener.class,listener);
	} //}}}

	//{{{ removeProgressListener() method
	/**
	 * Removes a progress listener from this thread pool.
	 * @param listener The listener
	 */
	public void removeProgressListener(WorkThreadProgressListener listener)
	{
		listenerList.remove(WorkThreadProgressListener.class,listener);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final EventListenerList listenerList;
	private final Object lock = new Object();

	@GuardedBy("lock") private boolean started;

	// AWT thread queue
	@GuardedBy("lock") private boolean awtRunnerQueued;
	@GuardedBy("lock") private Request firstAWTRequest;
	@GuardedBy("lock") private Request lastAWTRequest;
	@GuardedBy("lock") private int awtRequestCount;

	//}}}

	//{{{ doAWTRequests() method
	private void doAWTRequests()
	{
		Request req = null;
		while(TaskManager.INSTANCE.countIoTasks() == 0)
		{
			synchronized (lock)
			{
				if(firstAWTRequest != null)
				{
					req = getNextAWTRequest();
				} else
					break;
			}
			doAWTRequest(req);
		}
	} //}}}

	//{{{ doAWTRequest() method
	/**
	 * @param request the request to run
	 */
	private void doAWTRequest(Request request)
	{
//		Log.log(Log.DEBUG,this,"Running in AWT thread: " + request);

		try
		{
			request.run.run();
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,this,"Exception "
				+ "in AWT thread:");
			Log.log(Log.ERROR,this,t);
		}

		synchronized(lock)
		{
			awtRequestCount--;
		}
	} //}}}

	//{{{ queueAWTRunner() method
	public void queueAWTRunner()
	{
		boolean queueAwtRunner = false;
		synchronized (lock)
		{
			if(started && !awtRunnerQueued && firstAWTRequest != null)
			{
				queueAwtRunner = this.awtRunnerQueued = true;
			}
		}

		if(queueAwtRunner)
		{
			EventQueue.invokeLater(new RunRequestsInAWTThread());
//			Log.log(Log.DEBUG,this,"AWT runner queued");
		}
	} //}}}

	//{{{ queueAWTRunnerNowandWait() method
	public void queueAWTRunnerAndWait()
	{
		ThreadUtilities.runInDispatchThreadAndWait(new RunRequestsInAWTThread());
	} //}}}

	//{{{ getNextAWTRequest() method
	/** Must always be called with the lock held. */
	private Request getNextAWTRequest()
	{
		Request request = firstAWTRequest;
		firstAWTRequest = firstAWTRequest.next;
		if(firstAWTRequest == null)
			lastAWTRequest = null;

		if(request.alreadyRun)
			throw new InternalError("AIEE!!! Request run twice!!! " + request.run);
		request.alreadyRun = true;

		/* StringBuffer buf = new StringBuffer("AWT request queue is now: ");
		Request _request = request.next;
		while(_request != null)
		{
			buf.append(_request.id);
			if(_request.next != null)
				buf.append(",");
			_request = _request.next;
		}
		Log.log(Log.DEBUG,this,buf.toString()); */

		return request;
	} //}}}

	//}}}

	private static AtomicInteger requstCounter = new AtomicInteger();

	//{{{ Request class
	private static class Request
	{
		int id = requstCounter.incrementAndGet();

		Runnable run;

		boolean alreadyRun;

		Request next;

		Request(Runnable run)
		{
			this.run = run;
		}

		public String toString()
		{
			return "[id=" + id + ",run=" + run + ']';
		}
	} //}}}

	//{{{ RunRequestsInAWTThread class
	private class RunRequestsInAWTThread implements Runnable
	{
		public void run()
		{
			synchronized(lock)
			{
				awtRunnerQueued = false;
			}
			doAWTRequests();
		}
	} //}}}
}
