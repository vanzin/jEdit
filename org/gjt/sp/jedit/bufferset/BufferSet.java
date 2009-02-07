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

//{{{ Imports
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import javax.swing.event.EventListenerList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//}}}

/**
 * A BufferSet is an ordered list of buffers.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSet
{
	//{{{ Scope enum
	public enum Scope
	{
		global, view, editpane;

		public static Scope fromString(String s)
		{
			Scope[] scopes = values();
			for (Scope scope: scopes)
			{
				if (scope.toString().equals(s))
					return scope;
			}

			return global;
		}
	} //}}}

	//{{{ BufferSet constructor
	/**
	 * Create a new BufferSet.
	 */
	public BufferSet()
	{
		buffers = Collections.synchronizedList(new ArrayList<Buffer>());
		listeners = new EventListenerList();

		if (jEdit.getBooleanProperty("sortBuffers"))
		{
			if (jEdit.getBooleanProperty("sortByName"))
				sorter = nameSorter;
			else
				sorter = pathSorter;
		}
	} //}}}

	//{{{ addBufferAt() method
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

			if (sorter != null)
			{
				if (buffers.contains(buffer))
					return;
				buffers.add(buffer);
				Collections.sort(buffers, sorter);
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
	} //}}}

	//{{{ getBuffer() method
	/**
	 * Returns the Buffer at the given index.
	 * @param index the index. The index must exists
	 * @return the buffer at the index.
	 */
	public Buffer getBuffer(int index)
	{
		return buffers.get(index);
	} //}}}

	//{{{ getPreviousBuffer() method
	public Buffer getPreviousBuffer(int index)
	{
		if (buffers.isEmpty())
			return null;
		if (buffers.size() < 2)
			return buffers.get(0);
		if (index <= 0)
			return buffers.get(buffers.size() - 1);
		return buffers.get(index - 1);
	} //}}}

	//{{{ getNextBuffer() method
	public Buffer getNextBuffer(int index)
	{
		if (buffers.isEmpty())
			return null;
		if (buffers.size() < 2)
			return buffers.get(buffers.size()-1);
		if (index >= buffers.size() - 1)
			return buffers.get(0);
		return buffers.get(index + 1);
	} //}}}

	//{{{ indexOf() method
	public int indexOf(Buffer buffer)
	{
		return buffers.indexOf(buffer);
	} //}}}

	//{{{ size() method
	public int size()
	{
		return buffers.size();
	} //}}}

	//{{{ getAllBuffers() methods
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
	} //}}}

	//{{{ addBufferSetListener() method
	/**
	 * Add a BufferSetListener.
	 * @param listener the new BufferSetListener
	 */
	public void addBufferSetListener(BufferSetListener listener)
	{
		Log.log(Log.DEBUG, this, hashCode() + ": addBufferSetListener " + listener);
		listeners.add(BufferSetListener.class, listener);
	} //}}}

	//{{{ removeBufferSetListener() method
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
	} //}}}

	//{{{ hasListeners() method
	/**
	 * Check if the BufferSet has listeners.
	 *
	 * @return true if the bufferSet has listeners
	 */
	public boolean hasListeners()
	{
		return listeners.getListenerCount() != 0;
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return "BufferSet[nbBuffers="+size()+']';
	} //}}}

	//{{{ Package-private members

	//{{{ addBuffer() method
	void addBuffer(Buffer buffer)
	{
		addBufferAt(buffer,  -1);
	} //}}}

	//{{{ handleMessage
	/**
	 * This method is called by BufferSetManager to signal that this
	 * BufferSet needs to react to a change in the sorting properties.
	 */
	void handleMessage()
	{
		if (jEdit.getBooleanProperty("sortBuffers"))
		{
			// set the appropriate sorter
			if (jEdit.getBooleanProperty("sortByName"))
				sorter = nameSorter;
			else
				sorter = pathSorter;

			// do the sort
			Collections.sort(buffers, sorter);

			// notify the listeners so they can repaint themselves
			BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
			for (BufferSetListener listener : listeners)
			{
				listener.bufferSetSorted();
			}
		}
		else
		{
			// user has elected not to sort BufferSets
			sorter = null;
		}
	} //}}}

	//{{{ moveBuffer() method
	void moveBuffer(int oldPosition, int newPosition)
	{
		if (sorter != null)
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
	} //}}}

	//{{{ removeBuffer() method
	void removeBuffer(Buffer buffer)
	{
		int index;
		synchronized (buffers)
		{
			index = buffers.indexOf(buffer);
			if (index == -1)
				return;

			buffers.remove(index);
		}
		BufferSetListener[] listeners = this.listeners.getListeners(BufferSetListener.class);
		Log.log(Log.DEBUG, this, hashCode() + ": Buffer removed " + buffer);
		for (BufferSetListener listener : listeners)
		{
			listener.bufferRemoved(buffer, index);
		}
	} //}}}

	//}}}

	//{{{ Private members
	private final List<Buffer> buffers;
	private EventListenerList listeners;

	private static final Comparator<Buffer> nameSorter = new NameSorter();
	private static final Comparator<Buffer> pathSorter = new PathSorter();
	private Comparator<Buffer> sorter;
	//}}}


	//{{{ NameSorter class
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
	} //}}}

	//{{{ PathSorter class
	private static class PathSorter implements Comparator<Buffer>
	{
		public int compare(Buffer o1, Buffer o2)
		{
			return StandardUtilities.compareStrings(o1.getPath(), o2.getPath(), true);
		}
	} //}}}
}
