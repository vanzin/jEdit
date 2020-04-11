/*
 * AwtRunnableQueue.java - Queue for task to run in the Event Dispatch Thread
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2012 Thomas Meyer
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

import java.awt.EventQueue;
import java.util.LinkedList;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A queue for runnables that should run in the EDT after all pending 
 * IO tasks are finished
 * @author Thomas Meyer
 * @since jEdit 5.1pre1
 */
@ThreadSafe
public enum AwtRunnableQueue
{
	INSTANCE;

	@GuardedBy("this") private boolean awtQueueStarted;
	@GuardedBy("this") private boolean awtRunnerQueued;
	@GuardedBy("this") private final LinkedList<Runnable> awtQueue;

	//{{{ Constructor method
	private AwtRunnableQueue()
	{
		awtQueue = new LinkedList<>();
	} //}}}

	//{{{ start() method
	/**
	 * Queue the AWT runner for the first time.
	 */
	public void start()
	{
		synchronized (this)
		{
			awtQueueStarted = true;
		}
		queueAWTRunner(false);
	} //}}}


	//{{{ runAfterIoTasks() method
	/**
	 * Adds a runnable to the AWT queue to run in the EDT 
	 * after all pending IO tasks are finished
	 * @param run The runnable to queue for execution in the EDT after all IO tasks
	 */
	public void runAfterIoTasks(Runnable run)
	{
		boolean runDirectly = false;

		//{{{ if there are no requests, execute AWT requests immediately
		synchronized (this)
		{
			if(awtQueueStarted && TaskManager.instance.countIoTasks() == 0 && awtQueue.isEmpty())
				runDirectly = true;
		}
		if(runDirectly)
		{
//			Log.log(Log.DEBUG,this,"AWT immediate: " + run);
			ThreadUtilities.runInDispatchThread(run);
			return;
		} //}}}

		synchronized (this)
		{
			awtQueue.offer(run);
		}

		// queue AWT request
		queueAWTRunner(false);
	} //}}}

	//{{{ queueAWTRunner() method
	public void queueAWTRunner(boolean wait)
	{
		if(wait)
			ThreadUtilities.runInDispatchThreadAndWait(new RunRequestsInAWTThread());
		else
		{
			synchronized (this)
			{
				if(awtQueue.isEmpty())
					return;

				if(!awtQueueStarted || awtRunnerQueued)
					return;

				awtRunnerQueued = true;
			}

			EventQueue.invokeLater(new RunRequestsInAWTThread());
//			Log.log(Log.DEBUG,this,"AWT runner queued");
		}
	} //}}}

	//{{{ RunRequestsInAWTThread class
	private class RunRequestsInAWTThread implements Runnable
	{
		@Override
		public void run()
		{
			Runnable nextRunnable;
			// enable queuing of AWT runner again
			synchronized (AwtRunnableQueue.this)
			{
				awtRunnerQueued = false;
				nextRunnable = awtQueue.peek();
			}
			while(TaskManager.instance.countIoTasks() == 0 && nextRunnable != null)
			{
				doAWTRequest(nextRunnable);
				synchronized (AwtRunnableQueue.this)
				{
					// consume current entry
					awtQueue.poll();
					nextRunnable = awtQueue.peek();
				}
			}
		}

		//{{{ doAWTRequest() method
		/**
		 * Actually run the Runnable
		 * @param request the request to run
		 */
		private void doAWTRequest(Runnable request)
		{
//			Log.log(Log.DEBUG,this,"Running in AWT thread: " + request);

			try
			{
				request.run();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Exception "
					+ "in AWT thread:");
				Log.log(Log.ERROR,this,t);
			}
		} //}}}
	} //}}}
}
