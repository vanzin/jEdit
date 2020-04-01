/*
 * ThreadUtilities.java - Utilities for threading
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
 * Portions Copyright (C) 2010 Marcelo Vanzin
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
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
//}}}

/**
 * The threadpool of jEdit.
 * It uses a ExecutorService from the java.util.concurrent package.
 * You can run {@link Task} or {@link Runnable} in it, Runnables will be
 * encapsulated in Task and displayed in the Task Monitor. 
 *
 * @author Matthieu Casanova
 * @author Marcelo Vanzin
 * @since jEdit 4.4pre1
 */
public class ThreadUtilities
{
	//{{{ runInDispatchThread() method
	/**
	 * Run the runnable in EventDispatch Thread.
	 * If the current thread is EventDispatch, it will run
	 * immediately otherwise it will be queued in the DispatchThread
	 * The difference with VFSManager.runInAWTThread() method is that
	 * this one will not wait for IO Request before being executed
	 *
	 * @param runnable the runnable to run - it should return something meaningful from 
	 *    toString() so that we can display it in the Task Monitor.
	 */
	public static void runInDispatchThread(Runnable runnable)
	{
		if (EventQueue.isDispatchThread())
			runnable.run();
		else
			EventQueue.invokeLater(runnable);
	} //}}}

	//{{{ runInDispatchThreadAndWait() method
	/** Runs the runnable in EDT through <code>invokeLater</code>,
	 *  but returns only after the runnable is executed.
	 *  This method is uninterruptible.
	 *  <p>Note the difference from <code>invokeAndWait</code>.
	 *  If current thread is not EDT and there are runnables
	 *  queued in EDT:
	 *  <ul><li>this method runs the runnable after them</li>
	 *  <li><code>invokeAndWait</code> runs the runnable before them
	 *  </li></ul>
	 * @param runnable the runnable to run - it should return something meaningful from 
	 *    toString() so that we can display it in the Task Monitor.
	 */
	public static void runInDispatchThreadAndWait(Runnable runnable)
	{
		boolean interrupted = false;
		CountDownLatchRunnable run = new CountDownLatchRunnable(runnable);
		runInDispatchThread(run);
		while (run.done.getCount() > 0)
		{
			try
			{
				run.done.await();
			}
			catch (InterruptedException e)
			{
				interrupted = true;
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	} //}}}

	//{{{ runInDispatchThreadNow() method
	/**
	 * Runs the runnable in EDT through <code>invokeAndWait</code>.
	 * Even if the thread gets interrupted, the call does not return
	 * until the runnable finishes (uninterruptible method).
	 * <p>
	 * This method uses <code>EventQueue.invokeAndWait</code>, so
	 * the following remark applies:
	 * <p>If you use invokeAndWait(), make sure that the thread that calls
	 * invokeAndWait() does not hold any locks that other threads might
	 * need while the call is occurring.
	 * From the article:
	 * <a href="http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html#event_dispatching">
	 * Threads and Swing</a>
	 * @param runnable the runnable to run - it should return something meaningful from 
	 *    toString() so that we can display it in the Task Monitor.
	 */
	public static void runInDispatchThreadNow(Runnable runnable)
	{
		boolean interrupted = false;
		CountDownLatchRunnable run = new CountDownLatchRunnable(runnable);
		try
		{
			EventQueue.invokeAndWait(run);
		}
		catch (InterruptedException e)
		{
			interrupted = true;
		}
		catch (InvocationTargetException ite)
		{
			Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException)cause;
			else
			{
				Log.log(Log.ERROR, ThreadUtilities.class,
					"Invocation Target Exception:");
				Log.log(Log.ERROR, runnable.getClass(),
					cause);
			}
		}
		while (run.done.getCount() > 0)
		{
			try
			{
				run.done.await();
			}
			catch (InterruptedException e)
			{
				interrupted = true;
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	} //}}}

	//{{{ runInBackground() method
	/**
	 * Run the runnable in the threadpool.
	 * The runnable will be encapsulated in a {@link Task}
	 * @see #runInBackground(Task)
	 *
	 * @param runnable the runnable to run - it should return something meaningful from 
	     toString() so that we can display it in the Task Monitor.
	 */
	public static void runInBackground(Runnable runnable)
	{
		Task task;
		if (runnable instanceof Task)
		{
			task = (Task) runnable;
		}
		else
		{
			task = TaskManager.decorate(runnable);
		}
		TaskManager.instance.fireWaiting(task);
		threadPool.execute(task);
	}

	/**
	 * Run the task in the threadpool.
	 *
	 * @param task the task to run
	 */
	public static void runInBackground(Task task)
	{
		TaskManager.instance.fireWaiting(task);
		threadPool.execute(task);
	} //}}}

	private ThreadUtilities()
	{
	}

	//{{{ JEditThreadFactory class
	private static class JEditThreadFactory implements ThreadFactory
	{
		private JEditThreadFactory()
		{
			threadIDs = new AtomicInteger(0);
			threadGroup = new ThreadGroup("jEdit Workers");
		}

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(threadGroup, r);
			t.setName("jEdit Worker #" + threadIDs.getAndIncrement());
			return t;
		}

		private final AtomicInteger threadIDs;
		private final ThreadGroup threadGroup;
	} //}}}


	private static final ExecutorService threadPool;

	private static final int CORE_POOL_SIZE = 4;

	static
	{
		threadPool = Executors.newCachedThreadPool(new JEditThreadFactory());
		((ThreadPoolExecutor) threadPool).setCorePoolSize(CORE_POOL_SIZE);
	}

	//{{{ MyRunnable class
	private static class CountDownLatchRunnable implements Runnable
	{
		private final Runnable runnable;

		private final CountDownLatch done = new CountDownLatch(1);

		private CountDownLatchRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void run()
		{
			runnable.run();
			done.countDown();
		}
	} //}}}

}
