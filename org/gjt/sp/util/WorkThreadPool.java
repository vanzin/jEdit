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

import javax.swing.event.EventListenerList;
import javax.swing.SwingUtilities;

/**
 * A pool of work threads.
 * @author Slava Pestov
 * @version $Id$
 * @see org.gjt.sp.util.WorkThread
 * @since jEdit 2.6pre1
 */
public class WorkThreadPool
{
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
	}

	/**
	 * Starts all the threads in this thread pool.
	 */
	public void start()
	{
		if(threads != null)
		{
			for(int i = 0; i < threads.length; i++)
			{
				threads[i].start();
			}
		}
	}

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

		// if inAWT is set and there are no requests
		// pending, execute it immediately
		if(inAWT && requestCount == 0 && awtRequestCount == 0)
		{
// 			Log.log(Log.DEBUG,this,"AWT immediate: " + run);

			if(SwingUtilities.isEventDispatchThread())
				run.run();
			else
				SwingUtilities.invokeLater(run);

			return;
		}

		Request request = new Request(run);

		synchronized(lock)
		{
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
				if(requestCount == 0)
					queueAWTRunner();
			}
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
			}

			lock.notify();
		}
	}

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

		if(SwingUtilities.isEventDispatchThread())
		{
			// do any queued AWT runnables
			doAWTRequests();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(new RunRequestsInAWTThread());
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}
	}

	/**
	 * Returns the number of pending requests.
	 */
	public int getRequestCount()
	{
		return requestCount;
	}

	/**
	 * Returns the number of threads in this pool.
	 */
	public int getThreadCount()
	{
		if(threads == null)
			return 0;
		else
			return threads.length;
	}

	/**
	 * Returns the specified thread.
	 * @param index The index of the thread
	 */
	public WorkThread getThread(int index)
	{
		return threads[index];
	}

	/**
	 * Adds a progress listener to this thread pool.
	 * @param listener The listener
	 */
	public final void addProgressListener(WorkThreadProgressListener listener)
	{
		listenerList.add(WorkThreadProgressListener.class,listener);
	}

	/**
	 * Removes a progress listener from this thread pool.
	 * @param listener The listener
	 */
	public final void removeProgressListener(WorkThreadProgressListener listener)
	{
		listenerList.remove(WorkThreadProgressListener.class,listener);
	}

	// package-private members
	Object lock = new String("Work thread pool request queue lock");
	Object waitForAllLock = new String("Work thread pool waitForAll() notifier");

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

			final int _index = index;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					for(int i = listeners.length - 2; i >= 0; i--)
					{
						if(listeners[i] == WorkThreadProgressListener.class)
						{
							((WorkThreadProgressListener)listeners[i+1])
								.progressUpdate(WorkThreadPool.this,_index);
						}
					}
				}
			});
		}
	}

	void requestDone()
	{
		synchronized(lock)
		{
			requestCount--;

			if(requestCount == 0 && firstAWTRequest != null)
				queueAWTRunner();
		}
	}

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
	}

	// private members
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

	private EventListenerList listenerList;

	private void doAWTRequests()
	{
		while(firstAWTRequest != null)
		{
			doAWTRequest(getNextAWTRequest());
		}
	}

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
	}

	private void queueAWTRunner()
	{
		if(!awtRunnerQueued)
		{
			awtRunnerQueued = true;
			SwingUtilities.invokeLater(new RunRequestsInAWTThread());
			//Log.log(Log.DEBUG,this,"AWT runner queued");
		}
	}

	private Request getNextAWTRequest()
	{
		synchronized(lock)
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
		}
	}

	static int ID;

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
			return "[id=" + id + ",run=" + run + "]";
		}
	}

	class RunRequestsInAWTThread implements Runnable
	{
		public void run()
		{
			awtRunnerQueued = false;
			doAWTRequests();
		}
	}
}
