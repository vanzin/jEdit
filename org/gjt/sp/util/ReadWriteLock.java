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

public class ReadWriteLock
{
	//{{{ readLock() method
	public synchronized void readLock()
	{
		while(!obtainReadLock())
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				break;
			}
		}
	} //}}}

	//{{{ readUnlock() method
	public synchronized void readUnlock()
	{
		releaseReadLock();

		if(readLockCount == 0)
			notifyAll();
	} //}}}

	//{{{ writeLock() method
	public synchronized void writeLock()
	{
		waitingWriters++;

		while(!obtainWriteLock())
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
				break;
			}
		}

		waitingWriters--;
	} //}}}

	//{{{ writeUnlock() method
	public synchronized void writeUnlock()
	{
		releaseWriteLock();

		if(writeLockCount == 0)
			notifyAll();
	} //}}}

	//{{{ isWriteLocked() method
	public synchronized boolean isWriteLocked()
	{
		return writeLockCount > 0;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	/*
	    readLockCount   writeLockCount  theOnlyThread   Description
	    -------------   --------------  -------------   -----------------------
		0               0               null        no locks
		0               0               <value>     N/A, no locks, theOnlyThread must be null
	
		1               0               null        N/A, one thread must be recorded by theOnlyThread
		> 1             0               null        multiple threads hold multiple read locks
		> 0             0               <value>     one thread hold multiple read locks
	
		>= 0            > 0             null        N/A, one thread must be recorded by theOnlyThread
		>= 0            > 0             <value>     one thread holds multiple read and write locks
	*/
	private Thread  theOnlyThread;

	private int     readLockCount;

	private int     writeLockCount;
	private int     waitingWriters;
	//}}}

	//{{{ obtainReadLock() method
	private boolean obtainReadLock()
	{
		boolean canLock = true;

		if(writeLockCount != 0 || waitingWriters != 0)
			canLock = theOnlyThread == Thread.currentThread();

		if(canLock)
		{
			if(readLockCount == 0 && theOnlyThread == null)
				theOnlyThread = Thread.currentThread();
			else if(readLockCount > 0 && theOnlyThread != Thread.currentThread())
				theOnlyThread = null;

			readLockCount++;
		}

		return canLock;
	} //}}}

	//{{{ releaseReadLock() method
	private void releaseReadLock()
	{
		if(writeLockCount == 0)
		{
			if(readLockCount == 1)
				theOnlyThread = null;
			else if(readLockCount == 2)
				theOnlyThread = Thread.currentThread();
		}

		readLockCount--;
	} //}}}

	//{{{ obtainWriteLock() method
	private boolean obtainWriteLock()
	{
		boolean canLock = false;

		if(theOnlyThread == null)
			canLock = readLockCount == 0;
		else
			canLock = theOnlyThread == Thread.currentThread();

		if(canLock)
		{
			theOnlyThread = Thread.currentThread();

			writeLockCount++;
		}

		return canLock;
	} //}}}

	//{{{ releaseWriteLock() method
	private void releaseWriteLock()
	{
		if(readLockCount == 0 && writeLockCount == 1)
			theOnlyThread = null;

		writeLockCount--;
	} //}}}

	//}}}
}
