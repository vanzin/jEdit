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

import javax.annotation.concurrent.GuardedBy;

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
	public WorkThread(ThreadGroup threadGroup, Runnable run, String name)
	{
		super(threadGroup,run,name);
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

	public boolean isAborted()
	{
		synchronized (abortLock)
		{
			return aborted;
		}
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

	// private members
	private final Object abortLock = new Object();
	@GuardedBy("abortLock") private boolean abortable;
	@GuardedBy("abortLock") private boolean aborted;

	public static class Abort extends Error
	{
		public Abort()
		{
			super("Work request aborted");
		}
	}

	public void resetAbortable()
	{
		synchronized(abortLock)
		{
			aborted = abortable = false;
		}
	}
}
