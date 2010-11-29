/*
 * WorkThreadPool.java - Background thread pool that does stuff
 * :tabSize=8:indentSize=8:noTabs=false:
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
import javax.swing.event.EventListenerList;
import java.awt.EventQueue;
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
public class WorkThreadPool
{
	//{{{ WorkThreadPool constructor
	/**
	 * Creates a new work thread pool with the specified number of
	 * work threads.
	 * @param name The thread name prefix
	 * @param count The number of work threads
	 */
	public WorkThreadPool(String name, int count)
	{
		listenerList = new EventListenerList();

		if(count != 0)
		{
			threadGroup = new ThreadGroup(name);
			threads = new WorkThread[count];
			for(int i = 0; i < threads.length; i++)
			{
				threads[i] = new WorkThread(this,threadGroup,name + " #" + (i+1));
			}
		}
		else
			Log.log(Log.WARNING,this,"Async I/O disabled");
	} //}}}

	//{{{ start() method
	/**
	 * Starts all the threads in this thread pool.
	 */
	public void start()
	{
		/* not really needed since threads don't start until after */
		synchronized(lock)
		{
			started = true;

			if(awtRequestCount != 0 && requestCount == 0)
				queueAWTRunner();
		}

		if(threads != null)
		{
			for(int i = 0; i < threads.length; i++)
			{
				threads[i].start();
			}
		}
	} //}}}

	//{{{ addWorkRequest() method
	/**
	 * Adds a work request to the queue.
	 * @param run The runnable
	 * @param inAWT If true, will be executed in AWT thread. Otherwise,
	 * will be executed in work thread
	 */
	public void addWorkRequest(Runnable run, boolean inAWT)
	{
		if(threads == null)
		{
			run.run();
			return;
		}

		synchronized(lock)
		{
			//{{{ if there are no requests, execute AWT requests immediately
			if(started && inAWT && requestCount == 0 && awtRequestCount == 0)
			{
//				Log.log(Log.DEBUG,this,"AWT immediate: " + run);

				ThreadUtilities.runInDispatchThread(run);
				return;
			} //}}}

			Request request = new Request(run);

			//{{{ Add to AWT queue...
			if(inAWT)
			{
				if(firstAWTRequest == null && lastAWTRequest == null)
					firstAWTRequest = lastAWTRequest = request;
				else
				{
					lastAWTRequest.next = request;
					lastAWTRequest = request;
				}

				awtRequestCount++;

				// if no requests are running, requestDone()
				// will not be called, so we must queue the
				// AWT runner ourselves.
				if(started && requestCount == 0)
					queueAWTRunner();
			} //}}}
			//{{{ Add to work thread queue...
			else
			{
				if(firstRequest == null && lastRequest == null)
					firstRequest = lastRequest = request;
				else
				{
					lastRequest.next = request;
					lastRequest = request;
				}

				requestCount++;
			} //}}}

			lock.notifyAll();
		}
	} //}}}

	//{{{ waitForRequests() method
	/**
	 * Waits until all requests are complete.
	 */
	public void waitForRequests()
	{
		if(threads == null)
			return;

		synchronized(waitForAllLock)
		{
			while(requestCount != 0)
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

		if(EventQueue.isDispatchThread())
		{
			// do any queued AWT runnables
			doAWTRequests();
		}
		else
		{
			try
			{
				EventQueue.invokeAndWait(new RunRequestsInAWTThread());
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}
	} //}}}

	//{{{ getRequestCount() method
	/**
	 * Returns the number of pending requests.
	 * @return the pending request count
	 */
	public int getRequestCount()
	{
		return requestCount;
	} //}}}

	//{{{ getThreadCount() method
	/**
	 * Returns the number of threads in this pool.
	 * @return the thread count
	 */
	public int getThreadCount()
	{
		if(threads == null)
			return 0;
		else
			return threads.length;
	} //}}}

	//{{{ getThread() method
	/**
	 * Returns the specified thread.
	 * @param index The index of the thread
	 * @return a WorkThread
	 */
	public WorkThread getThread(int index)
	{
		return threads[index];
	} //}}}

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
	final Object lock = new Object();
	final Object waitForAllLock = new Object();

	//{{{ fireStatusChanged() method
	void fireStatusChanged(WorkThread thread)
	{
		final Object[] listeners = listenerList.getListenerList();
		if(listeners.length != 0)
		{
			int index = 0;
			for(int i = 0; i < threads.length; i++)
			{
				if(threads[i] == thread)
				{
					index = i;
					break;
				}
			}

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
	void fireProgressChanged(WorkThread thread)
	{
		final Object[] listeners = listenerList.getListenerList();
		if(listeners.length != 0)
		{
			int index = 0;
			for(int i = 0; i < threads.length; i++)
			{
				if(threads[i] == thread)
				{
					index = i;
					break;
				}
			}

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

	//{{{ requestDone() method
	void requestDone()
	{
		synchronized(lock)
		{
			requestCount--;

			if(requestCount == 0 && firstAWTRequest != null)
				queueAWTRunner();
		}
	} //}}}

	//{{{ getNextRequest() method
	Request getNextRequest()
	{
		synchronized(lock)
		{
			Request request = firstRequest;
			if(request == null)
				return null;

			firstRequest = firstRequest.next;
			if(firstRequest == null)
				lastRequest = null;

			if(request.alreadyRun)
				throw new InternalError("AIEE!!! Request run twice!!! " + request.run);
			request.alreadyRun = true;

			/* StringBuffer buf = new StringBuffer("request queue is now: ");
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
		}
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private boolean started;
	private ThreadGroup threadGroup;
	private WorkThread[] threads;

	// Request queue
	private Request firstRequest;
	private Request lastRequest;
	private int requestCount;

	// AWT thread magic
	private boolean awtRunnerQueued;
	private Request firstAWTRequest;
	private Request lastAWTRequest;
	private int awtRequestCount;

	private final EventListenerList listenerList;
	//}}}

	//{{{ doAWTRequests() method
	/** Must always be called with the lock held. */
	private void doAWTRequests()
	{
		while(requestCount == 0 && firstAWTRequest != null)
		{
			doAWTRequest(getNextAWTRequest());
		}
	} //}}}

	//{{{ doAWTRequest() method
	/**
	 * Must always be called with the lock held.
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

		awtRequestCount--;
	} //}}}

	//{{{ queueAWTRunner() method
	/** Must always be called with the lock held. */
	private void queueAWTRunner()
	{
		if(!awtRunnerQueued)
		{
			awtRunnerQueued = true;
			EventQueue.invokeLater(new RunRequestsInAWTThread());
//			Log.log(Log.DEBUG,this,"AWT runner queued");
		}
	} //}}}

	//{{{ getNextAWTRequest() method
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

	static int ID;

	//{{{ Request class
	static class Request
	{
		int id = ++ID;

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
	class RunRequestsInAWTThread implements Runnable
	{
		public void run()
		{
			synchronized(lock)
			{
				awtRunnerQueued = false;
				if(requestCount == 0)
					doAWTRequests();
			}
		}
	} //}}}
}
