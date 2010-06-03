/*
 * BufferSetManager.java - Manages the buffersets.
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008, 2010 Matthieu Casanova
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
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.EditBus.EBHandler;
import java.util.*;
//}}}

/**
 * The buffersets manager. A singleton instance of this
 * can be obtained from jEdit.getBufferSetManager()
 *
 * @author Matthieu Casanova
 * @since jEdit 4.3pre15
 */
public class BufferSetManager
{
	//{{{ BufferSetManager constructor
	public BufferSetManager()
	{
		EditBus.addToBus(this);
		try
		{
			scope = BufferSet.Scope.valueOf(jEdit.getProperty("bufferset.scope", "global"));
		}
		catch (IllegalArgumentException e)
		{
			Log.log(Log.ERROR, this, e);
			scope = BufferSet.Scope.global;
		}
	} //}}}

	//{{{ handleEditPaneUpdate() method
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate message)
	{
		if (message.getWhat() == EditPaneUpdate.DESTROYED)
		{
			EditPane editPane = message.getEditPane();
			BufferSet bufferSet = editPane.getBufferSet();
			Buffer[] allBuffers = bufferSet.getAllBuffers();
			for (Buffer buffer : allBuffers)
			{
				_removeBuffer(bufferSet, buffer);
			}
		}
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		// pass on PropertiesChanged message to BufferSets so
		// they can resort themselves as needed.
		jEdit.visit(new JEditVisitorAdapter()
		{
			@Override
			public void visit(EditPane editPane)
			{
				editPane.getBufferSet().propertiesChanged();
			}
		});
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

	//{{{ setScope() method
	public void setScope(BufferSet.Scope scope)
	{
		if (scope == this.scope)
			return;
		if (scope.compareTo(this.scope) > 0)
		{
			// The new scope is wider
			if (scope == BufferSet.Scope.global)
			{
				final Buffer[] buffers = jEdit.getBuffers();
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane)
					{
						BufferSet bufferSet = editPane.getBufferSet();
						for (Buffer buffer : buffers)
						{
							bufferSet.addBuffer(buffer);
						}
					}
				});
			}
			else
			{
				final Map<View,Set<Buffer>> buffersMap = new HashMap<View, Set<Buffer>>();
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane)
					{
						BufferSet bufferSet = editPane.getBufferSet();
						Buffer[] buffers = bufferSet.getAllBuffers();
						Set<Buffer> set = buffersMap.get(editPane.getView());
						if (set == null)
						{
							set = new HashSet<Buffer>();
							buffersMap.put(editPane.getView(), set);
						}
						set.addAll(Arrays.asList(buffers));
					}
				});
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane)
					{
						BufferSet bufferSet = editPane.getBufferSet();
						Set<Buffer> set = buffersMap.get(editPane.getView());
						for (Buffer buffer : set)
						{
							bufferSet.addBuffer(buffer);
						}
					}
				});
			}
		}
		this.scope = scope;
		EditBus.send(new PropertiesChanged(this));
	} //}}}

	//{{{ getScope() method
	public BufferSet.Scope getScope()
	{
		return scope;
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
	public void addBuffer(EditPane editPane, final Buffer buffer)
	{
		if (editPane == null)
		{
			editPane = jEdit.getActiveView().getEditPane();
		}
		BufferSet bufferSet = editPane.getBufferSet();
		switch (scope)
		{
			case editpane:
				bufferSet.addBuffer(buffer);
				break;
			case view:
				EditPane[] editPanes = editPane.getView().getEditPanes();
				for (EditPane pane:editPanes)
				{
					if (pane == null)
						continue;
					BufferSet bfs = pane.getBufferSet();
					bfs.addBuffer(buffer);
				}
				break;
			case global:
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane)
					{
						BufferSet bfs = editPane.getBufferSet();
						bfs.addBuffer(buffer);
					}
				});
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
		switch (scope)
		{
			case editpane:
				BufferSet bufferSet = editPane.getBufferSet();
				removeBuffer(bufferSet, buffer);
				break;
			case view:
				EditPane[] editPanes = editPane.getView().getEditPanes();
				for (EditPane pane : editPanes)
				{
					removeBuffer(pane.getBufferSet(), buffer);
				}
				break;
			case global:
				jEdit._closeBuffer(null, buffer);
				break;
		}
	}

	/**
	 * Remove a buffer from a bufferSet.
	 * And make sure that the bufferSet is not empty after that
	 *
	 * @param bufferSet the bufferSet
	 * @param buffer the buffer that will be removed
	 */
	void removeBuffer(BufferSet bufferSet, Buffer buffer)
	{
		Log.log(Log.DEBUG, this, "removeBuffer("+bufferSet+','+buffer+')');
		_removeBuffer(bufferSet, buffer);
		bufferRemoved(bufferSet);
	}

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
			bufferRemoved(bufferSet);
		}
	} //}}}

	//{{{ bufferRemoved() method
	/**
	 * This method is called when a buffer has been removed from a bufferSet.
	 * If it is empty, an untitled buffer is created and added to the bufferSet
	 * @param bufferSet the bufferSet from which the buffer was removed
	 */
	private void bufferRemoved(BufferSet bufferSet)
	{
		if (bufferSet.size() == 0)
		{
			Buffer newEmptyBuffer = createUntitledBuffer();
			EditPane editPaneOwner = getOwner(bufferSet);
			addBuffer(editPaneOwner, newEmptyBuffer);
		}
	} //}}}

	//{{{ _removeBuffer() method
	/**
	 * Remove a buffer from a bufferSet.
	 * Used when closing an EditPane
	 *
	 * @param bufferSet the bufferSet
	 * @param buffer the buffer that will be removed
	 */
	private void _removeBuffer(BufferSet bufferSet, Buffer buffer)
	{
		Set<BufferSet> owners = getOwners(buffer);
		owners.remove(bufferSet);
		bufferSet.removeBuffer(buffer);

		if (owners.isEmpty())
		{
			Log.log(Log.DEBUG, this, "Buffer:"+buffer+" is in no bufferSet anymore, closing it");
			jEdit._closeBuffer(null, buffer);
		}
	} //}}}

	//{{{ createUntitledBuffer() method
	/**
	 * Create an untitled buffer
	 * @return the new untitled buffer
	 */
	public static Buffer createUntitledBuffer()
	{
		int untitledCount = jEdit.getNextUntitledBufferId();
		Buffer newEmptyBuffer = jEdit.openTemporary(jEdit.getActiveView(), null,
							    "Untitled-" + untitledCount,true, null);
		jEdit.commitTemporary(newEmptyBuffer);
		return newEmptyBuffer;
	} //}}}

	//{{{ Private members

	//{{{ getOwners() method
	/**
	 * @return set of BufferSets that contain buffer
	 * @since 4.4pre1
	 */
	public Set<BufferSet> getOwners(Buffer buffer)
	{
		final Set<BufferSet> candidates = new HashSet<BufferSet>();
		// Collect all BufferSets.
		jEdit.visit(new JEditVisitorAdapter()
		{
			@Override
			public void visit(EditPane editPane)
			{
				candidates.add(editPane.getBufferSet());
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

	//{{{ getOwner() method
	/**
	 * Return the editpane that owns the BufferSet
	 * @param bufferSet the bufferSet
	 * @return the owner of the given bufferSet
	 */
	private static EditPane getOwner(BufferSet bufferSet)
	{
		View[] views = jEdit.getViews();
		for (View view : views)
		{
			EditPane[] editPanes = view.getEditPanes();
			for (EditPane editPane : editPanes)
			{
				if (editPane.getBufferSet() == bufferSet)
				{
					return editPane;
				}
			}
		}
		return null;
	} //}}}

	/** The scope of the bufferSets. */
	private BufferSet.Scope scope;
	//}}}
}
