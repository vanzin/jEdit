/*
 * ReadWriteLock.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Peter Graves
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
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

import java.util.Vector;

public class ReadWriteLock
{
	//{{{ readLock() method
	public synchronized void readLock()
	{
		// this seems to make nested readLock() calls work okay.
		// but I have no idea if it actually fixes things or not.
		if (activeReaders != 0 || allowRead())
		{
			++activeReaders;
			//readers.addElement(Thread.currentThread());
			return;
		}
		++waitingReaders;
		while (!allowRead())
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
				--waitingReaders; // Roll back state.
				Log.log(Log.ERROR,this,e);
				return;
			}
		}
		--waitingReaders;
		++activeReaders;
		readers.addElement(Thread.currentThread());
	} //}}}

	//{{{ readUnlock() method
	public synchronized void readUnlock()
	{
		if(activeReaders == 0)
			throw new InternalError("Unbalanced readLock()/readUnlock() calls");

		--activeReaders;
		//readers.removeElement(Thread.currentThread());
		notifyAll();
	} //}}}

	//{{{ writeLock() method
	public synchronized void writeLock()
	{
		if (writerThread != null)
		{
			// Write in progress.
			if (Thread.currentThread() == writerThread)
			{
				// Same thread.
				++lockCount;
				return;
			}
		}
		if (allowWrite())
		{
			claimWriteLock();
			return;
		}

		++waitingWriters;
		while (!allowWrite())
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
				--waitingWriters;
				Log.log(Log.ERROR,this,e);
				return;
			}
		}
		--waitingWriters;
		claimWriteLock();
	} //}}}

	//{{{ writeUnlock() method
	public synchronized void writeUnlock()
	{
		if(activeWriters != 1 || lockCount <= 0)
			throw new InternalError("Unbalanced writeLock()/writeUnlock() calls");

		if(Thread.currentThread() != writerThread)
			throw new InternalError("writeUnlock() from wrong thread");

		if (--lockCount == 0)
		{
			--activeWriters;
			writerThread = null;
			notifyAll();
		}
	} //}}}

	//{{{ isWriteLocked() method
	public synchronized boolean isWriteLocked()
	{
		//Debug.assert(activeWriters == 0 || activeWriters == 1);
		return activeWriters == 1;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private int activeReaders;
	private int activeWriters;
	private int waitingReaders;
	private int waitingWriters;
	private Vector readers = new Vector();

	private Thread writerThread;
	private int lockCount;
	//}}}

	//{{{ allowRead() method
	private final boolean allowRead()
	{
		return (Thread.currentThread() == writerThread)
			|| (waitingWriters == 0 && activeWriters == 0);
	} //}}}

	//{{{ allowWrite() method
	private final boolean allowWrite()
	{
		/*Thread current = Thread.currentThread();
		for(int i = 0; i < readers.size(); i++)
		{
			if(readers.elementAt(i) == current)
				throw new InternalError("Cannot nest writeLock() inside readLock()");
		}*/

		return activeReaders == 0 && activeWriters == 0;
	} //}}}

	//{{{ claimWriteLock() method
	private void claimWriteLock()
	{
		++activeWriters;
		//Debug.assert(writerThread == null);
		writerThread = Thread.currentThread();
		//Debug.assert(lockCount == 0);
		lockCount = 1;
	} //}}}

	//}}}
}
