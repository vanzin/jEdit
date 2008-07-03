/*
 * BufferSet.java - A Set of Buffer.
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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
package org.gjt.sp.jedit.bufferset;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import javax.swing.event.EventListenerList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A BufferSet is a sorted list of buffers.
 * 
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSet
{
	public static final String[] SCOPE = {"global", "view", "editpane" };
	private final List<Buffer> buffers;
	private EventListenerList listeners;

	private Buffer untitledCleanBuffer;
	private boolean checkForCleanBuffer;

	private final String scope;
	private static final Comparator<Buffer> nameSorter = new NameSorter();
	private static final Comparator<Buffer> pathSorter = new PathSorter();

	private boolean sorted;

	private Comparator<Buffer> comparator;

	public BufferSet(String scope)
	{
		buffers = Collections.synchronizedList(new ArrayList<Buffer>());
		listeners = new EventListenerList();
		this.scope = scope;


		sorted = jEdit.getBooleanProperty("sortBuffers");
		if (sorted)
		{
			if (jEdit.getBooleanProperty("sortByName"))
				comparator = nameSorter;
			else
				comparator = pathSorter;
		}
	}

	public BufferSet(String scope, BufferSet copy)
	{
		this(scope);
		buffers.addAll(copy.buffers);
		untitledCleanBuffer = copy.untitledCleanBuffer;
		checkForCleanBuffer = copy.checkForCleanBuffer;
	}

	void addBuffer(Buffer buffer)
	{
		addBufferAt(buffer,  -1);
	}

	public void addBufferAt(Buffer buffer, int position)
	{
		Log.log(Log.DEBUG, this, hashCode() + " addBufferAt("+buffer+','+position+')');
		Buffer untitledBuffer = null;
		synchronized (buffers)
		{
			if (buffers.size() == 1)
			{
				Buffer buf = buffers.get(0);
				if (buf.isUntitled() && !buf.isDirty())
				{
					untitledBuffer = buf;
				}
			}

			if (sorted)
			{
				buffers.add(buffer);
				Collections.sort(buffers, comparator);
				position = buffers.indexOf(buffer);
			}
			else
			{
				int oldPos = buffers.indexOf(buffer);
				if (oldPos != -1)
				{
					if (position == -1)
					{
						return;
					}
					moveBuffer(oldPos, position);
					return;
				}
				int size = buffers.size();
				if (position == -1 || position > size)
				{
					position = size;
				}
				buffers.add(position, buffer);
			}
		}
		BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
		Log.log(Log.DEBUG, this, hashCode() + ": Buffer added " + buffer + " at " + position);
		for (BufferSetListener listener : listeners)
		{
			listener.bufferAdded(buffer, position);
		}

		// I don't like this reverse control
		if (untitledBuffer != null)
		{
			jEdit.getBufferSetManager().removeBuffer(this, untitledBuffer);
		}
	}

	void moveBuffer(int oldPosition, int newPosition)
	{
		if (sorted)
		{
			// Buffers are sorted, do nothing
			return;
		}
		Buffer buffer;
		synchronized (buffers)
		{
			buffer = buffers.remove(oldPosition);
			int size = buffers.size();
			if (newPosition == -1 || newPosition > size)
			{
				newPosition = size;
			}
			buffers.add(newPosition, buffer);
		}
		BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
		Log.log(Log.DEBUG, this, hashCode() + ": Buffer moved " + buffer + " from " + oldPosition + " to " + newPosition);
		for (BufferSetListener listener : listeners)
		{
			listener.bufferMoved(buffer, oldPosition, newPosition);
		}
	}

	void removeBuffer(Buffer buffer)
	{
		int index;
		synchronized (buffers)
		{
			index = buffers.indexOf(buffer);
			if (index == -1)
				return;
			if (untitledCleanBuffer == buffer)
			{
				untitledCleanBuffer = null;
				checkForCleanBuffer = true;
			}
			buffers.remove(index);
		}
		BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
		Log.log(Log.DEBUG, this, hashCode() + ": Buffer removed " + buffer);
		for (BufferSetListener listener : listeners)
		{
			listener.bufferRemoved(buffer, index);
		}
	}

	/**
	 * Returns the untitled clean buffer contained in this BufferSet.
	 *
	 * @return an untitled clean buffer, it can be null
	 */
	public Buffer getUntitledCleanBuffer()
	{
		synchronized (buffers)
		{
			if (checkForCleanBuffer && untitledCleanBuffer == null)
			{
				for (Buffer buffer : buffers)
				{
					if (buffer.isUntitled() && !buffer.isDirty())
					{
						untitledCleanBuffer = buffer;
						break;
					}
				}
				checkForCleanBuffer = false;
			}
			return untitledCleanBuffer;
		}
	}

	void clear()
	{
		buffers.clear();
		BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
		Log.log(Log.DEBUG, this, hashCode() + ": Buffer clear");
		for (BufferSetListener listener : listeners)
		{
			listener.bufferCleared();
		}
	}

	public Buffer getBuffer(int index)
	{
		return buffers.get(index);
	}

	public String getScope()
	{
		return scope;
	}

	public Buffer getPreviousBuffer(int index)
	{
		if (buffers.isEmpty())
			return null;
		if (buffers.size() < 2)
			return buffers.get(0);
		if (index <= 0)
			return buffers.get(buffers.size() - 1);
		return buffers.get(index - 1);
	}


	public Buffer getNextBuffer(int index)
	{
		if (buffers.isEmpty())
			return null;
		if (buffers.size() < 2)
			return buffers.get(buffers.size()-1);
		if (index >= buffers.size() - 1)
			return buffers.get(0);
		return buffers.get(index + 1);
	}

	public int indexOf(Buffer buffer)
	{
		return buffers.indexOf(buffer);
	}

	public int size()
	{
		return buffers.size();
	}

	public void getAllBuffers(BufferSetListener listener)
	{
		synchronized (buffers)
		{
			for (int i = 0;i<buffers.size();i++)
			{
				Buffer buffer = buffers.get(i);
				Log.log(Log.DEBUG, this, hashCode() + ": Buffer added " + buffer + " at " + i);
				listener.bufferAdded(buffer, i);
			}
		}
	}

	/**
	 * Returns an array of all buffers in this bufferSet.
	 *
	 * @return an array of all Buffers
	 */
	public Buffer[] getAllBuffers()
	{
		Buffer[] buffers = new Buffer[this.buffers.size()];
		return this.buffers.toArray(buffers);
	}

	/**
	 * Add a BufferSetListener.
	 * @param listener the new BufferSetListener
	 */
	public void addBufferSetListener(BufferSetListener listener)
	{
		Log.log(Log.DEBUG, this, hashCode() + ": addBufferSetListener " + listener);
		listeners.add(BufferSetListener.class, listener);
	}

	/**
	 * Remove a BufferSetListener.
	 * If there are no listeners anymore, remove all buffers from the bufferSet.
	 * @param listener the removed BufferSetListener
	 */
	public void removeBufferSetListener(BufferSetListener listener)
	{
		Log.log(Log.DEBUG, this, hashCode() + ": removeBufferSetListener " + listener);
		listeners.remove(BufferSetListener.class, listener);
		if (!hasListeners())
		{
			// must empty the bufferSet
			Buffer[] buffers = getAllBuffers();
			BufferSetManager bufferSetManager = jEdit.getBufferSetManager();
			for (Buffer buffer : buffers)
			{
				bufferSetManager.removeBuffer(this, buffer);
			}
		}
	}

	/**
	 * Check if the BufferSet has listeners.
	 *
	 * @return true if the bufferSet has listeners
	 */
	public boolean hasListeners()
	{
		return listeners.getListenerCount() != 0;
	}

	@Override
	public String toString()
	{
		return "BufferSet["+scope+",nbBuffers="+size()+']';
	}

	private static class NameSorter implements Comparator<Buffer>
	{
		public int compare(Buffer o1, Buffer o2)
		{

			int ret = StandardUtilities.compareStrings(o1.getName(), o2.getName(), true);
			if (ret == 0)
			{
				ret = StandardUtilities.compareStrings(o1.getPath(), o2.getPath(), true);
			}
			return ret;
		}
	}

	private static class PathSorter implements Comparator<Buffer>
	{
		public int compare(Buffer o1, Buffer o2)
		{
			return StandardUtilities.compareStrings(o1.getPath(), o2.getPath(), true);
		}
	}
}
