/*
 * ThreadUtilities.java - Utilities for threading
 * :tabSize=8:indentSize=8:noTabs=false:
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
 * encapsulated in Task.
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
	 * @param runnable the runnable to run
	 */
	public static void runInDispatchThread(Runnable runnable)
	{
		if (EventQueue.isDispatchThread())
			runnable.run();
		else
			EventQueue.invokeLater(runnable);
	} //}}}

	//{{{ runInDispatchThreadAndWait() method
	public static void runInDispatchThreadAndWait(Runnable runnable)
	{
		MyRunnable run = new MyRunnable(runnable);
		runInDispatchThread(run);
		while (!run.done)
		{
			synchronized (run)
			{
				try
				{
					run.wait(1000L);
				}
				catch (InterruptedException e)
				{
					Log.log(Log.ERROR, ThreadUtilities.class, e);
				}
			}
		}
	} //}}}

	//{{{ runInBackground() method
	/**
	 * Run the runnable in the threadpool.
	 * The runnable will be encapsulated in a {@link Task}
	 * @see #runInBackground(Task)
	 *
	 * @param runnable the runnable to run
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
	 * The runnable will be encapsulated in a {@link Task}
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
			t.setName("jEdit Worker #" +threadIDs.getAndIncrement());
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
	private static class MyRunnable implements Runnable
	{
		private final Runnable runnable;

		private volatile boolean done;

		private MyRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void run()
		{
			runnable.run();
			done = true;
			synchronized (this)
			{
				notifyAll();
			}
		}
	} //}}}

}
