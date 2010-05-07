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
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
//}}}

/**
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
		runInBackground(run);
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

	//{{{ execute() method
	/**
	 * Run the runnable in the threadpool.
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
	} //}}}

	private ThreadUtilities()
	{
	}

	private static class JEditThreadFactory implements ThreadFactory
	{
		private JEditThreadFactory()
		{
			threadIDs = new AtomicInteger(0);
			threadGroup = new ThreadGroup("jEdit Workers");
		}

		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(threadGroup, r);
			t.setName("jEdit Worker #" +
				threadIDs.getAndIncrement());
			return t;
		}

		private final AtomicInteger threadIDs;
		private final ThreadGroup threadGroup;
	}

	private static final ExecutorService threadPool;

	static
	{
		threadPool = Executors.newCachedThreadPool(new JEditThreadFactory());
		((ThreadPoolExecutor) threadPool).setCorePoolSize(2);
		((ThreadPoolExecutor) threadPool).setMaximumPoolSize(10);
	}

	private static class MyRunnable implements Runnable
	{
		private final Runnable runnable;

		private volatile boolean done;

		private MyRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}

		public void run()
		{
			runnable.run();
			done = true;
			synchronized (this)
			{
				notifyAll();
			}
		}
	}
}
