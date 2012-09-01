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

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.WorkThreadFactory;

import java.awt.EventQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
	INSTANCE("jEdit I/O", jEdit.getIntegerProperty("ioThreadCount", 4));

	//{{{ WorkThreadPool constructor
	/**
	 * Creates a new work thread pool with the specified number of
	 * work threads.
	 * @param name The thread name prefix
	 * @param count The number of work threads
	 */
	private WorkThreadPool(String name, int count)
	{
		if(count <= 0)
			throw new IllegalArgumentException();

		listenerList = new EventListenerList();

		ThreadFactory threadFactory = new WorkThreadFactory(name);
		threadPool = Executors.newFixedThreadPool(count, threadFactory);
		workRequests = new WorkRequest[count];
		nopWorkRequest = new WorkRequest() {

			@Override
			public void _run() {
			}

			@Override
			public boolean isRequestRunning() {
				return false;
			}
		};
	} //}}}

	//{{{ start() method
	/**
	 * Starts all the threads in this thread pool.
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
				if(started && requestCount.get() == 0 && awtRequestCount == 0)
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
		{
			WorkRequest wr;
			if (run instanceof WorkRequest)
			{
				wr = (WorkRequest) run;
			}
			else
			{
//				Log.log(Log.DEBUG,this,"Runnable " + run + " in VFS background queue is not a WorkRequest!");
				wr = decorate(run);
			}
			requestCount.incrementAndGet();
			threadPool.execute(wr);
		}
	} //}}}

	private WorkRequest decorate(Runnable run)
	{
		return new AbstractWorkRequest(run);
	}

	private static class AbstractWorkRequest extends WorkRequest
	{
		private final Runnable runnable;

		private AbstractWorkRequest(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void _run()
		{
			runnable.run();
		}
	}

	//{{{ waitForRequests() method
	/**
	 * Waits until all requests are complete.
	 */
	public void waitForRequests()
	{
		synchronized(waitForAllLock)
		{
			while(requestCount.get() != 0)
			{
				try
				{
					waitForAllLock.wait();
				}
				catch(InterruptedException ie)
				{
					Log.log(Log.ERROR,this,ie);
				}
			}
		}

		// do any queued AWT runnables
		doAWTRequests();
	} //}}}

	//{{{ getRequestCount() method
	/**
	 * Returns the number of pending requests.
	 * @return the pending request count
	 */
	public int getRequestCount()
	{
		return requestCount.get();
	} //}}}

	//{{{ getThreadCount() method
	/**
	 * Returns the number of threads in this pool.
	 * @return the thread count
	 */
	public int getThreadCount()
	{
		return workRequests.length;
	} //}}}

	public int workRequestStart(WorkRequest workRequest)
	{
		int i = 0;
		synchronized(lock)
		{
			for(int n = workRequests.length; i < n ; i++)
			{
				if(workRequests[i] == null)
					break;
			}
			workRequests[i] = workRequest;
		}

		return i;
	}

	public void workRequestEnd(WorkRequest workRequest)
	{
		if(Thread.currentThread() instanceof WorkThread)
			requestCount.decrementAndGet();
		else
			Log.log(Log.DEBUG,this,"WorkRequest run in non-WorkThread thread!");

		int runNo = workRequest.getRunNo();
		synchronized(lock)
		{
			workRequests[runNo] = null;
		}

		synchronized(waitForAllLock)
		{
			// notify a running waitForRequests() method
			waitForAllLock.notifyAll();
		}

		queueAWTRunner();
	}

	public WorkRequest getWorkRequestWithRunNo(int index)
	{
		synchronized(lock)
		{
			WorkRequest workRequest = workRequests[index];
			if(workRequest == null) {
				return nopWorkRequest;
			} else
				return workRequest;
		}
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

	//{{{ Package-private members

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private final ExecutorService threadPool;
	private final EventListenerList listenerList;
	// map requst to old static thread id for IOProgressMonitor
	@GuardedBy("lock") private final WorkRequest[] workRequests;
	private final WorkRequest nopWorkRequest;

	@GuardedBy("lock") private boolean started;

	// Request queue
	private final AtomicInteger requestCount = new AtomicInteger();

	// AWT thread queue
	@GuardedBy("lock") private boolean awtRunnerQueued;
	@GuardedBy("lock") private Request firstAWTRequest;
	@GuardedBy("lock") private Request lastAWTRequest;
	@GuardedBy("lock") private int awtRequestCount;

	private static AtomicInteger requstCounter = new AtomicInteger();

	//}}}

	private final Object lock = new Object();
	private final Object waitForAllLock = new Object();

	//{{{ fireStatusChanged() method
	void fireStatusChanged(WorkRequest workRequest)
	{
		final Object[] listeners = listenerList.getListenerList();
		if(listeners.length != 0)
		{
			int index = workRequest.getRunNo();

			for(int i = listeners.length - 2; i >= 0; i--)
			{
				if(listeners[i] == WorkThreadProgressListener.class)
				{
					((WorkThreadProgressListener)listeners[i+1])
						.statusUpdate(WorkThreadPool.this,index);
				}
			}
		}
	} //}}}

	//{{{ fireProgressChanged() method
	void fireProgressChanged(WorkRequest workRequest)
	{
		final Object[] listeners = listenerList.getListenerList();
		if(listeners.length != 0)
		{
			int index = workRequest.getRunNo();

			for(int i = listeners.length - 2; i >= 0; i--)
			{
				if(listeners[i] == WorkThreadProgressListener.class)
				{
					((WorkThreadProgressListener)listeners[i+1])
						.progressUpdate(WorkThreadPool.this,index);
				}
			}
		}
	} //}}}

	//{{{ doAWTRequests() method
	private void doAWTRequests()
	{
		Request req = null;
		while(requestCount.get() == 0)
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
			Log.log(Log.ERROR,WorkThread.class,"Exception "
				+ "in AWT thread:");
			Log.log(Log.ERROR,WorkThread.class,t);
		}

		synchronized(lock)
		{
			awtRequestCount--;
		}
	} //}}}

	//{{{ queueAWTRunner() method
	/** Must always be called with the lock held. */
	private void queueAWTRunner()
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
