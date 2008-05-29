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

import javax.swing.event.EventListenerList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A BufferSet is a sorted list of buffers.
 * 
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSet
{
	public static final String SCOPE[] = {"global", "view", "editpane" };
	private final List<Buffer> buffers;
	private EventListenerList listeners;

	private Buffer untitledCleanBuffer;
	private boolean checkForCleanBuffer;
	
	
	public BufferSet()
	{
		buffers = Collections.synchronizedList(new ArrayList<Buffer>());
		listeners = new EventListenerList();
	}

	public BufferSet(BufferSet copy)
	{
		this();
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
				if (buf.isUntitled() && !buffer.isDirty())
				{
					untitledBuffer = buf;
				}
			}

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

	public Buffer[] getAllBuffers()
	{
		Buffer[] buffers = new Buffer[this.buffers.size()];
		return this.buffers.toArray(buffers);
	}

	public void addBufferSetListener(BufferSetListener listener)
	{
		listeners.add(BufferSetListener.class, listener);
	}

	public void removeBufferSetListener(BufferSetListener listener)
	{
		listeners.remove(BufferSetListener.class, listener);
	}
}
