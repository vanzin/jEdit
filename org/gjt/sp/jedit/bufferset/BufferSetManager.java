/*
 * BufferSetManager.java - Manages the buffersets.
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

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.util.Log;

import java.util.*;

/**
 * The buffersets manager.
 * 
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSetManager implements EBComponent
{
	public BufferSetManager()
	{
		global = new BufferSet(BufferSet.SCOPE[0]);
		viewBufferSetMap = Collections.synchronizedMap(new HashMap<View, BufferSet>());
		editPaneBufferSetMap = Collections.synchronizedMap(new HashMap<EditPane, BufferSet>());
		bufferBufferSetMap = Collections.synchronizedMap(new HashMap<Buffer, Set<BufferSet>>());
		emptyBufferSets = Collections.synchronizedSet(new HashSet<BufferSet>());
		EditBus.addToBus(this);
	}

	public void handleMessage(EBMessage message)
	{
		if (message instanceof ViewUpdate)
		{
			ViewUpdate viewUpdate = (ViewUpdate) message;
			if (viewUpdate.getWhat() == ViewUpdate.CLOSED)
			{
				View view = viewUpdate.getView();
				// If the view has a BufferSet, unlink the buffer from this bufferSet.
				BufferSet viewBufferSet = viewBufferSetMap.remove(view);
				if (viewBufferSet != null)
				{
					viewBufferSet.getAllBuffers(new BufferSetClosed(viewBufferSet));
				}
			}
		}
		else if (message instanceof EditPaneUpdate)
		{
			EditPaneUpdate editPaneUpdate = (EditPaneUpdate) message;
			if (editPaneUpdate.getWhat() == EditPaneUpdate.DESTROYED)
			{
				EditPane editPane = editPaneUpdate.getEditPane();
				// If the editPane has a BufferSet, unlink the buffer from this bufferSet.
				BufferSet editPaneBufferSet = editPaneBufferSetMap.remove(editPane);
				if (editPaneBufferSet != null)
				{
					editPaneBufferSet.getAllBuffers(new BufferSetClosed(editPaneBufferSet));
				}
			}
		}
	}

	/**
	 * Retourne le bufferSet global.
	 *
	 * @return le bufferSet global
	 */
	public BufferSet getGlobalBufferSet()
	{
		return global;
	}

	public BufferSet getViewBufferSet(View view, BufferSet source)
	{
		BufferSet bufferSet = viewBufferSetMap.get(view);
		if (bufferSet == null)
		{
			bufferSet = createBufferSet(BufferSet.SCOPE[1],source);
			viewBufferSetMap.put(view, bufferSet);
		}
		return bufferSet;
	}

	public BufferSet getViewBufferSet(View view)
	{
		return getViewBufferSet(view, null);
	}

	public BufferSet getEditPaneBufferSet(EditPane editPane, BufferSet source)
	{
		BufferSet bufferSet = editPaneBufferSetMap.get(editPane);
		if (bufferSet == null)
		{
			bufferSet = createBufferSet(BufferSet.SCOPE[2],source);
			editPaneBufferSetMap.put(editPane, bufferSet);
		}
		return bufferSet;
	}

	public BufferSet getEditPaneBufferSet(EditPane editPane)
	{
		return getEditPaneBufferSet(editPane, null);
	}

	/**
	 * Count the bufferSets in which the buffer is.
	 * @param buffer the buffer
	 * @return the number of buffersets in which buffer is
	 */
	public int countBufferSets(Buffer buffer)
	{
		Set<BufferSet> sets = bufferBufferSetMap.get(buffer);
		return sets.size();
	}

	public void addBuffer(View view, Buffer buffer)
	{
		Set<BufferSet> bufferSets = bufferBufferSetMap.get(buffer);
		if (bufferSets == null)
		{
			bufferSets = new HashSet<BufferSet>();
			bufferBufferSetMap.put(buffer, bufferSets);
		}
		if (view == null)
		{
			global.addBuffer(buffer);
			bufferSets.add(global);
		}
		else
		{
			EditPane editPane = view.getEditPane();
			BufferSet bufferSet = editPane.getBufferSet();
			bufferSet.addBuffer(buffer);
			bufferSets.add(bufferSet);
		}
	}

	/**
	 * Remove a buffer from the EditPane's bufferSet.
	 * 
	 * @param editPane the editPane It cannot be null
	 * @param buffer the buffer
	 */
	public void removeBuffer(EditPane editPane, Buffer buffer)
	{
		BufferSet bufferSet = editPane.getBufferSet();
		removeBuffer(bufferSet, buffer);
	}

	/**
	 * Remove a buffer from a View's BufferSet.
	 *
	 * @param bufferSet the bufferSet
	 * @param buffer the buffer that will be removed
	 * @return true if the buffer is not in a BufferSet anymore
	 */
	void removeBuffer(BufferSet bufferSet, Buffer buffer)
	{
		Log.log(Log.DEBUG, this, "removeBuffer("+bufferSet+','+buffer+')');
		Set<BufferSet> bufferSets = bufferBufferSetMap.get(buffer);
		bufferSets.remove(bufferSet);
		bufferSet.removeBuffer(buffer);
		if (bufferSet.size() == 0)
		{
			emptyBufferSets.add(bufferSet);
		}
		if (bufferSets.isEmpty())
		{
			Log.log(Log.DEBUG, this, "Buffer:"+buffer+" is in no bufferSet anymore, closing it");
			jEdit._closeBuffer(null, buffer);
		}
		else
		{
			boolean shouldClose = true;
			for (BufferSet bs: bufferSets)
			{
				if (bs.hasListeners())
				{
					shouldClose = false;
					break;
				}
			}
			if (shouldClose)
			{
				Log.log(Log.DEBUG, this, "Buffer:"+buffer+" is only in bufferSets that have no listeners, closing it");
				jEdit._closeBuffer(null, buffer);
			}
		}
	}

	/**
	 * remove a buffer from all bufferSets.
	 *
	 * @param buffer the buffer that must be removed
	 */
	public void removeBuffer(Buffer buffer)
	{
		Set<BufferSet> sets = bufferBufferSetMap.remove(buffer);
		for (BufferSet bufferSet : sets)
		{
			bufferSet.removeBuffer(buffer);
			if (bufferSet.size() == 0)
			{
				emptyBufferSets.add(bufferSet);
			}
		}
	}

	/**
	 * Close all buffers.
	 */
	public void clear()
	{
		bufferBufferSetMap.clear();
		visit(new BufferSetVisitor()
		{
			public void visit(BufferSet bufferSet)
			{
				bufferSet.clear();
				emptyBufferSets.add(bufferSet);
			}
		});
	}

	public boolean hasEmptyBufferSets()
	{
		return !emptyBufferSets.isEmpty();
	}

	/**
	 * Add this buffer to all empty buffersets.
	 * Usually it will be a new untitled bufferSet
	 *
	 * @param buffer the buffer to add
	 */
	public void addBufferToEmptyBufferSets(Buffer buffer)
	{
		Set<BufferSet> sets = bufferBufferSetMap.get(buffer);
		if (sets == null)
		{
			sets = new HashSet<BufferSet>();
			bufferBufferSetMap.put(buffer, sets);
		}
		synchronized (emptyBufferSets)
		{
			for (BufferSet bufferSet : emptyBufferSets)
			{
				if (bufferSet.size() == 0)
				{
					sets.add(bufferSet);
					bufferSet.addBuffer(buffer);
				}
			}
			emptyBufferSets.clear();
		}
	}

	/**
	 * This method will visit all buffersets.
	 *
	 * @param visitor the bufferset visitor
	 */
	private void visit(BufferSetVisitor visitor)
	{
		visitor.visit(global);
		Collection<BufferSet> bufferSetCollection = viewBufferSetMap.values();
		for (BufferSet bufferSet : bufferSetCollection)
		{
			visitor.visit(bufferSet);
		}
		Collection<BufferSet> sets = editPaneBufferSetMap.values();
		for (BufferSet bufferSet : sets)
		{
			visitor.visit(bufferSet);
		}
	}

	/** The global bufferSet. */
	private final BufferSet global;
	/** The BufferSets that will be associated to a View. */
	private final Map<View, BufferSet> viewBufferSetMap;
	private final Map<EditPane, BufferSet> editPaneBufferSetMap;

	/** The BufferSets that contains the Buffer. */
	private final Map<Buffer, Set<BufferSet>> bufferBufferSetMap;

	/**
	 * This set contains the buffersets that are currently empty.
	 * They will need to get a new buffer quickly 
	 */
	private final Set<BufferSet> emptyBufferSets;

	/**
	 * Add all buffers to the bufferSet.
	 *
	 * @param bufferSet the bufferSet
	 */
	private void addAllBuffers(BufferSet bufferSet)
	{
		Buffer[] buffers = jEdit.getBuffers();
		for (int i = 0; i < buffers.length; i++)
		{
			Buffer buffer = buffers[i];
			bufferBufferSetMap.get(buffer).add(bufferSet);
			bufferSet.addBuffer(buffer);
		}
	}

	/**
	 * Create a bufferSet
	 * @param source the source bufferSet. If it exists the buffers will be copied otherwise all open buffers
	 * are added
	 * @return the new bufferSet
	 */
	private BufferSet createBufferSet(String scope, BufferSet source)
	{
		BufferSet bufferSet;
		if (source == null)
		{
			bufferSet = new BufferSet(scope);
			addAllBuffers(bufferSet);
		}
		else
		{
			bufferSet = new BufferSet(scope, source);
		}
		Buffer[] allBuffers = bufferSet.getAllBuffers();
		for (Buffer buffer : allBuffers)
		{
			bufferBufferSetMap.get(buffer).add(bufferSet);
		}
		return bufferSet;
	}

	private static interface BufferSetVisitor
	{
		void visit(BufferSet bufferSet);
	}

	private class BufferSetClosed extends BufferSetAdapter
	{
		private BufferSet closedBufferSet;

		private BufferSetClosed(BufferSet closedBufferSet)
		{
			this.closedBufferSet = closedBufferSet;
		}

		@Override
		public void bufferAdded(Buffer buffer, int index)
		{
			Set<BufferSet> sets = bufferBufferSetMap.get(buffer);
			sets.remove(closedBufferSet);
			if (sets.isEmpty())
			{
				// the buffer do not belong to any other BufferSet
				if (buffer.isDirty())
				{
					EditPane editPane = jEdit.getActiveView().getEditPane();
					Log.log(Log.MESSAGE, this, "The buffer " + buffer + " was removed from a BufferSet, it is dirty, adding it to " + editPane);
					// the buffer is dirty, I open it in the first edit pane (is it a good choice ?)
					editPane.setBuffer(buffer);
				}
				else
				{
					// the buffer is not dirty I close it
					Log.log(Log.MESSAGE, this, "The buffer " + buffer + " was removed from a BufferSet, it is clean, closing it");
					jEdit._closeBuffer(null, buffer);
				}
			}
		}
	}
}
