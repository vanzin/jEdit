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
 * The buffersets manager. A singleton instance of this
 * can be obtained from jEdit.getBufferSetManager()
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
				// Unlink the buffer from this bufferSet.
				BufferSet viewBufferSet = view.getLocalBufferSet();
				viewBufferSet.getAllBuffers(new BufferSetClosed(viewBufferSet));
			}
		}
		else if (message instanceof EditPaneUpdate)
		{
			EditPaneUpdate editPaneUpdate = (EditPaneUpdate) message;
			if (editPaneUpdate.getWhat() == EditPaneUpdate.DESTROYED)
			{
				EditPane editPane = editPaneUpdate.getEditPane();
				// If the editPane has own BufferSet, unlink the buffer from this bufferSet.
				if (editPane.getBufferSetScope() == BufferSet.Scope.editpane)
				{
					BufferSet editPaneBufferSet = editPane.getBufferSet();
					editPaneBufferSet.getAllBuffers(new BufferSetClosed(editPaneBufferSet));
				}
			}
		}
		else if (message instanceof PropertiesChanged)
		{
			// pass on PropertiesChanged message to BufferSets so
			// they can resort themselves as needed.
			visit(new BufferSetVisitor()
			{
				public void visit(BufferSet bufferSet)
				{
					bufferSet.handleMessage();
				}
			});
		}

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
		return getOwners(buffer).size();
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
			addBuffer(jEdit.getGlobalBufferSet(), buffer);
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

	//{{{ moveBuffer() method
	/**
	 * Moves a buffer from a old position to a new position in the
	 * BufferSet used in an EditPane.
	 */
	public void moveBuffer(EditPane editPane,
		int oldPosition, int newPosition)
	{
		editPane.getBufferSet().moveBuffer(oldPosition, newPosition);
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
		Set<BufferSet> owners = getOwners(buffer);
		owners.remove(bufferSet);
		bufferSet.removeBuffer(buffer);

		if (owners.isEmpty())
		{
			Log.log(Log.DEBUG, this, "Buffer:"+buffer+" is in no bufferSet anymore, closing it");
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

	//{{{ removeBuffer() method
	/**
	 * remove a buffer from all bufferSets.
	 *
	 * @param buffer the buffer that must be removed
	 */
	public void removeBuffer(Buffer buffer)
	{
		for (BufferSet bufferSet : getOwners(buffer))
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

	//{{{ visit() method
	/**
	 * This method will visit all buffersets.
	 *
	 * @param visitor the bufferset visitor
	 */
	public void visit(BufferSetVisitor visitor)
	{
		BufferSet global = jEdit.getGlobalBufferSet();
		visitor.visit(jEdit.getGlobalBufferSet());
		for (View view: jEdit.getViews())
		{
			BufferSet viewLocal = view.getLocalBufferSet();
			if (viewLocal != null)
			{
				visitor.visit(viewLocal);
			}
			for (EditPane editPane: view.getEditPanes())
			{
				BufferSet used = editPane.getBufferSet();
				if (used != global && used != viewLocal)
				{
					visitor.visit(used);
				}
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ getOwners() method
	/**
	    @return set of BufferSets that contain buffer
        */
	private Set<BufferSet> getOwners(Buffer buffer)
	{
		final Set<BufferSet> candidates = new HashSet<BufferSet>();
		// Collect all BufferSets.
		visit(new BufferSetVisitor()
		{
			public void visit(BufferSet bufferSet)
			{
				candidates.add(bufferSet);
			}
		});
		// Remove all that doesn't contain the buffer.
		Iterator<BufferSet> i = candidates.iterator();
		while (i.hasNext())
		{
			if (i.next().indexOf(buffer) == -1)
			{
				i.remove();
			}
		}
		// Remaining are the result.
		return candidates;
	} //}}}

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

		//{{{ BufferSetClosed constructors
		private BufferSetClosed(BufferSet closedBufferSet)
		{
			this.closedBufferSet = closedBufferSet;
		} //}}}

		//{{{ bufferAdded() method
		@Override
		public void bufferAdded(Buffer buffer, int index)
		{
			Set<BufferSet> owners = getOwners(buffer);
			owners.remove(closedBufferSet);
			if (owners.isEmpty())
			{
				Log.log(Log.MESSAGE, this, "The buffer " +
					buffer + " was removed from a BufferSet, closing it");
				jEdit._closeBuffer(null, buffer);
			}
		} //}}}

	} //}}}

	//}}}
}
