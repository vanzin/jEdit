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

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements consumer/producer locking scemantics.
 * @author Peter Graves
 * @version $Id$
 * The lock tries to be re-entrant when called from the same thread in some
 * cases.
 * 
 * The following is ok:
 * read lock
 * read lock
 * read unlock
 * read unlock
 * 
 * write lock
 * read lock
 * read unlock
 * write unlock
 * 
 * The following is not ok:
 * 
 * read lock
 * write lock
 * write unlock
 * read unlock
 * 
 * write lock
 * write lock
 * write unlock
 * write unlock
 *
 * @deprecated Use java.util.concurrent.locks.ReentrantReadWriteLock which
 * is available since J2SE 5.0 (1.5). This class was written for J2SE 1.4,
 * and is still here only for compatibility.
 */
public class ReadWriteLock
{
	//{{{ readLock() method
	public void readLock()
	{
		body.readLock().lock();
	} //}}}

	//{{{ readUnlock() method
	public void readUnlock()
	{
		body.readLock().unlock();
	} //}}}

	//{{{ writeLock() method
	public void writeLock()
	{
		body.writeLock().lock();
	} //}}}

	//{{{ writeUnlock() method
	public void writeUnlock()
	{
		body.writeLock().unlock();
	} //}}}

	//{{{ isWriteLocked() method
	public boolean isWriteLocked()
	{
		return body.isWriteLocked();
	} //}}}

	//{{{ Private members
	private final ReentrantReadWriteLock body = new ReentrantReadWriteLock();
	//}}}
}
