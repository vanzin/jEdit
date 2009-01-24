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

//{{{ Imports
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;

import java.util.*;
//}}}

/**
 * The buffersets manager.
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSetManager implements EBComponent
{
	//{{{ NewBufferSetAction enum
	public enum NewBufferSetAction
	{
		empty, copy, currentbuffer;

		public static NewBufferSetAction fromString(String s)
		{
			NewBufferSetAction[] newBufferSetActions = values();
			for (NewBufferSetAction newBufferSetAction : newBufferSetActions)
			{
				if (newBufferSetAction.getName().equals(s))
					return newBufferSetAction;
			}

			return currentbuffer;
		}

		public String getName()
		{
			return super.toString();
		}

		@Override
		public String toString()
		{
			return jEdit.getProperty("options.editpane.bufferset.newbufferset." + getName());
		}
	} //}}}

	//{{{ BufferSetManager constructor
	public BufferSetManager()
	{
		global = new BufferSet();
		viewBufferSetMap = Collections.synchronizedMap(new HashMap<View, BufferSet>());
		editPaneBufferSetMap = Collections.synchronizedMap(new HashMap<EditPane, BufferSet>());
		bufferBufferSetMap = Collections.synchronizedMap(new HashMap<Buffer, Set<BufferSet>>());
		EditBus.addToBus(this);
	} //}}}

	//{{{ handleMessage() method
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
					viewBufferSet.getAllBuffers(new BufferSetClosed(view, viewBufferSet));
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
					editPaneBufferSet.getAllBuffers(new BufferSetClosed(editPane, editPaneBufferSet));
				}
			}
		}
		else if (message instanceof PropertiesChanged)
		{
			// pass on PropertiesChanged message to BufferSets so
			// they can resort themselves as needed.
			global.handleMessage();
			for (BufferSet bufferSet : editPaneBufferSetMap.values())
			{
				bufferSet.handleMessage();
			}
			for (BufferSet bufferSet : viewBufferSetMap.values())
			{
				bufferSet.handleMessage();
			}
		}

	} //}}}

	//{{{ getGlobalBufferSet() method
	/**
	 * Returns the global bufferSet.
	 *
	 * @return the global bufferSet
	 */
	public BufferSet getGlobalBufferSet()
	{
		return global;
	} //}}}

	//{{{ getViewBufferSet() methods
	/**
	 * Returns a view bufferSet for the given view.
	 * If it doesn't exist it is created
	 *
	 * @param view a view
	 * @return the view's bufferSet
	 */
	public BufferSet getViewBufferSet(View view)
	{
		BufferSet bufferSet = viewBufferSetMap.get(view);
		if (bufferSet == null)
		{
			bufferSet = new BufferSet();
			viewBufferSetMap.put(view, bufferSet);
		}
		return bufferSet;
	} //}}}

	//{{{ getEditPaneBufferSet() method
	/**
	 * Returns a EditPane bufferSet for the given EditPane.
	 * If it doesn't exist it is created
	 *
	 * @param editPane the editPAne
	 * @return the EditPane's bufferSet
	 */
	public BufferSet getEditPaneBufferSet(EditPane editPane)
	{
		BufferSet bufferSet = editPaneBufferSetMap.get(editPane);
		if (bufferSet == null)
		{
			bufferSet = new BufferSet();
			editPaneBufferSetMap.put(editPane, bufferSet);
		}

		return bufferSet;
	} //}}}

	//{{{ mergeBufferSet() method
	/**
	 * Merge the content of the source bufferSet into the target bufferSet
	 * @param target the target bufferSet
	 * @param source the source bufferSet
	 * @see org.gjt.sp.jedit.EditPane#setBuffer(org.gjt.sp.jedit.Buffer)
	 */
	public void mergeBufferSet(BufferSet target, BufferSet source)
	{
		Buffer[] buffers = source.getAllBuffers();
		for (Buffer buffer : buffers)
		{
			addBuffer(target, buffer);
		}
	} //}}}

	//{{{ countBufferSets() method
	/**
	 * Count the bufferSets in which the buffer is.
	 * @param buffer the buffer
	 * @return the number of buffersets in which buffer is
	 * @see org.gjt.sp.jedit.jEdit#closeBuffer(org.gjt.sp.jedit.EditPane, org.gjt.sp.jedit.Buffer)
	 */
	public int countBufferSets(Buffer buffer)
	{
		Set<BufferSet> sets = bufferBufferSetMap.get(buffer);
		if (sets == null)
			return 0;
		return sets.size();
	} //}}}

	//{{{ addBuffer() methods
	/**
	 * Add a buffer into the current editPane of the given view.
	 * If the view is null, it will be added to the global bufferSet
	 * @param view a view (or null)
	 * @param buffer the buffer to add
	 */
	public void addBuffer(View view, Buffer buffer)
	{
		EditPane editPane = view == null ? null : view.getEditPane();
		addBuffer(editPane, buffer);
	}

	/**
	 * Add a buffer into the current editPane of the given editPane.
	 * If the editPane is null, it will be added to the global bufferSet
	 * @param editPane an EditPane (or null)
	 * @param buffer the buffer to add
	 */
	public void addBuffer(EditPane editPane, Buffer buffer)
	{
		if (editPane == null)
		{
			addBuffer(global, buffer);
		}
		else
		{
			BufferSet bufferSet = editPane.getBufferSet();
			addBuffer(bufferSet, buffer);
		}
	}

	/**
	 * Add a buffer in the given bufferSet.
	 *
	 * @param bufferSet the bufferSet
	 * @param buffer the buffer to add
	 */
	public void addBuffer(BufferSet bufferSet, Buffer buffer)
	{
		Set<BufferSet> bufferSets = bufferBufferSetMap.get(buffer);
		if (bufferSets == null)
		{
			bufferSets = new HashSet<BufferSet>();
			bufferBufferSetMap.put(buffer, bufferSets);
		}
		bufferSets.add(bufferSet);
		bufferSet.addBuffer(buffer);
	} //}}}

	//{{{ addAllBuffers() method
	/**
	 * Add all buffers to the bufferSet.
	 *
	 * @param bufferSet the bufferSet
	 */
	public void addAllBuffers(BufferSet bufferSet)
	{
		Buffer[] buffers = jEdit.getBuffers();
		for (Buffer buffer : buffers)
		{
			if (!buffer.isClosed())
				addBuffer(bufferSet, buffer);
		}
	} //}}}

	//{{{ removeBuffer() methods
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
	 */
	void removeBuffer(BufferSet bufferSet, Buffer buffer)
	{
		Log.log(Log.DEBUG, this, "removeBuffer("+bufferSet+','+buffer+')');
		Set<BufferSet> bufferSets = bufferBufferSetMap.get(buffer);
		bufferSets.remove(bufferSet);
		bufferSet.removeBuffer(buffer);

		if (bufferSets.isEmpty())
		{
			Log.log(Log.DEBUG, this, "Buffer:"+buffer+" is in no bufferSet anymore, closing it");
			jEdit._closeBuffer(null, buffer);
		}
		else if (!hasListeners(buffer))
		{
			Log.log(Log.DEBUG, this, "Buffer:" + buffer + " is only in bufferSets that have no listeners, closing it");
			jEdit._closeBuffer(null, buffer);
		}
		if (bufferSet.size() == 0 && bufferSet.hasListeners())
		{
			int untitledCount = jEdit.getNextUntitledBufferId();
			Buffer newEmptyBuffer = jEdit.openTemporary(jEdit.getActiveView(), null,
								    "Untitled-" + untitledCount,true, null);
			jEdit.commitTemporary(newEmptyBuffer);
			jEdit.getBufferSetManager().addBuffer(bufferSet, newEmptyBuffer);
		}
	} //}}}

	//{{{ hasListeners() method
	/**
	 * Check if a buffer is in at least one bufferSet that as some listeners.
	 * Otherwise nobody can see it.
	 *
	 * @param buffer the buffer
	 * @return true if the buffer is in a bufferSet that has listeners
	 */
	public boolean hasListeners(Buffer buffer)
	{
		Set<BufferSet> bufferSets = bufferBufferSetMap.get(buffer);
		if (bufferSets == null)
			return false;
		return !bufferSets.isEmpty();
		/*for (BufferSet bs: bufferSets)
		{
			if (bs.hasListeners())
			{
				return true;
			}
		}
		return false;*/
	} //}}}

	//{{{ removeBuffer() method
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
			if (bufferSet.size() == 0 && bufferSet.hasListeners())
			{
				int untitledCount = jEdit.getNextUntitledBufferId();
				Buffer newEmptyBuffer = jEdit.openTemporary(jEdit.getActiveView(), null,
									    "Untitled-" + untitledCount,true, null);
				jEdit.commitTemporary(newEmptyBuffer);
				jEdit.getBufferSetManager().addBuffer(bufferSet, newEmptyBuffer);
			}
		}

	} //}}}

	//{{{ clear() method
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
			}
		});
	} //}}}

	//{{{ visit() method
	/**
	 * This method will visit all buffersets.
	 *
	 * @param visitor the bufferset visitor
	 */
	public void visit(BufferSetVisitor visitor)
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
	} //}}}

	//{{{ Private members

	//{{{ Fields
	/** The global bufferSet. */
	private final BufferSet global;
	/** The BufferSets that will be associated to a View. */
	private final Map<View, BufferSet> viewBufferSetMap;
	private final Map<EditPane, BufferSet> editPaneBufferSetMap;

	/** The BufferSets that contains the Buffer. */
	private final Map<Buffer, Set<BufferSet>> bufferBufferSetMap;
	//}}}

	//{{{ BufferSetVisitor interface
	public static interface BufferSetVisitor
	{
		void visit(BufferSet bufferSet);
	} //}}}

	//{{{ BufferSetClosed class
	private class BufferSetClosed extends BufferSetAdapter
	{
		/** The closed bufferSet. */
		private final BufferSet closedBufferSet;

		/**
		 * The closed view.
		 * If there is a closed view, there is no closed edit pane
		 */
		private View closedView;

		/**
		 * The closed EditPane.
 		 * If there is a closed edit pane, there is no closed view
		 */
		private EditPane closedEditPane;

		/** The previous editPane where to put dirty buffers if necessary. */
		private EditPane prevEditPane;

		//{{{ BufferSetClosed constructors
		private BufferSetClosed(View closedView,
					BufferSet closedBufferSet)
		{
			this.closedView = closedView;
			this.closedBufferSet = closedBufferSet;
			init();
		}

		private BufferSetClosed(EditPane closedEditPane,
					BufferSet closedBufferSet)
		{
			this.closedEditPane = closedEditPane;
			this.closedBufferSet = closedBufferSet;
			init();
		} //}}}


		//{{{ init() method
		private void init()
		{
			if (closedView != null)
			{
					View prev = closedView.getPrev();
					if (prev != null)
					{
						prevEditPane = prev.getEditPane();
					}
				}
			else
			{
				View view = closedEditPane.getView();
				EditPane[] editPanes = view.getEditPanes();
				for (EditPane editPane : editPanes)
				{
					if (editPane != closedEditPane)
					{
						prevEditPane = editPane;
						break;
					}
				}
				if (prevEditPane == null)
				{
					View prev = view.getPrev();
					if (prev != null)
					{
						prevEditPane = prev.getEditPane();
					}
				}
			}
		} //}}}

		//{{{ bufferAdded() method
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
					if (prevEditPane != null)
					{
						Log.log(Log.MESSAGE, this,
							"The buffer " +  buffer +
							" was removed from a BufferSet, it is dirty, adding it to " +
							prevEditPane);
						prevEditPane.setBuffer(buffer);
					}
					else
					{
						Log.log(Log.ERROR, this,
							"The buffer " + buffer +
							" was removed from a BufferSet, it is dirty, but there is no other edit pane");
					}
				}
				else
				{
					// the buffer is not dirty I close it
					Log.log(Log.MESSAGE, this, "The buffer " +
						buffer + " was removed from a BufferSet, it is clean, closing it");
					jEdit._closeBuffer(null, buffer);
				}
			}
		} //}}}

	} //}}}

	//}}}
}
