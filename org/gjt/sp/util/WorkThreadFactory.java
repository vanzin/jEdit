/*
 * WorkThreadFactory.java - Factory for WorkThreads
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

import java.util.concurrent.ThreadFactory;
import javax.annotation.concurrent.GuardedBy;

import org.gjt.sp.jedit.JARClassLoader;

public class WorkThreadFactory implements ThreadFactory
{
	private final ThreadGroup threadGroup;
	private final String name;
	private final ClassLoader classLoader;
	@GuardedBy("this") private int i;

	public WorkThreadFactory(String name)
	{
		this.name = name;
		this.classLoader = new JARClassLoader();
		this.threadGroup = new ThreadGroup(name);
	}

	@Override
	public Thread newThread(Runnable r)
	{
		int threadCounter;
		synchronized (this)
		{
			threadCounter = i;
			i++;
		}

		Thread t = new WorkThread(threadGroup, r, name + " " + threadCounter);
		t.setContextClassLoader(classLoader);
		// so that jEdit doesn't exit with no views open automatically
		//setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY); // FIXME: good idea?
		return t;
	}

}
