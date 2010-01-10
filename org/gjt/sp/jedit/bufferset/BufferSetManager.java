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
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
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
	//{{{ BufferSetManager constructor
	public BufferSetManager()
	{
		EditBus.addToBus(this);
		scope = BufferSet.Scope.fromString(jEdit.getProperty("bufferset.scope", "global"));
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage message)
	{
		if (message instanceof EditPaneUpdate)
		{
			EditPaneUpdate editPaneUpdate = (EditPaneUpdate) message;
			if (editPaneUpdate.getWhat() == EditPaneUpdate.DESTROYED)
			{
				EditPane editPane = editPaneUpdate.getEditPane();
				BufferSet bufferSet = editPane.getBufferSet();
				Buffer[] allBuffers = bufferSet.getAllBuffers();
				for (Buffer buffer : allBuffers)
				{
					removeBuffer(bufferSet, buffer);
				}
			}
		}
		else if (message instanceof PropertiesChanged)
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
			editPane = jEdit.getActiveView().getEditPane();
		}
		BufferSet bufferSet = editPane.getBufferSet();
		addBuffer(bufferSet, buffer);
	}

	/**
	 * Add a buffer in the given bufferSet.
	 *
	 * @param bufferSet the bufferSet
	 * @param buffer the buffer to add
	 */
	public void addBuffer(BufferSet bufferSet, final Buffer buffer)
	{
		switch (scope)
		{
			case editpane:
				bufferSet.addBuffer(buffer);
				break;
			case view:
				EditPane owner = bufferSet.getOwner();
				EditPane[] editPanes = owner.getView().getEditPanes();
				for (EditPane editPane:editPanes)
				{
					if (editPane == null)
						continue;
					BufferSet bfs = editPane.getBufferSet();
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
			Buffer newEmptyBuffer = createUntitledBuffer();
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
				Buffer newEmptyBuffer = createUntitledBuffer();
				jEdit.getBufferSetManager().addBuffer(bufferSet, newEmptyBuffer);
			}
		}

	} //}}}

	public static Buffer createUntitledBuffer()
	{
		int untitledCount = jEdit.getNextUntitledBufferId();
		Buffer newEmptyBuffer = jEdit.openTemporary(jEdit.getActiveView(), null,
							    "Untitled-" + untitledCount,true, null);
		jEdit.commitTemporary(newEmptyBuffer);
		return newEmptyBuffer;
	}

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
						while (set.iterator().hasNext())
						{
							Buffer buffer = set.iterator().next();
							bufferSet.addBuffer(buffer);
						}
					}
				});
			}
		}
		this.scope = scope;
		EditBus.send(new EditPaneUpdate(null, EditPaneUpdate.BUFFERSET_CHANGED));
	}

	public BufferSet.Scope getScope()
	{
		return scope;
	}

	/** The scope of the bufferSets. */
	private BufferSet.Scope scope;
}
