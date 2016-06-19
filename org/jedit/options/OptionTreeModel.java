/*
 * OptionTreeModel.java - OptionGroup-backed TreeModel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Portions Copyright (C) 2003 Slava Pestov
 * Copyright (C) 2012 Alan Ezust
 * Copyright (C) 2016 Eric Le Lay (extracted from PluginOptionGroup)
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
package org.jedit.options;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.gjt.sp.jedit.OptionGroup;

/**
 * TreeModel implementation backed by an OptionGroup 
 **/
public class OptionTreeModel implements TreeModel
{
	private OptionGroup root;
	private EventListenerList listenerList;

	public OptionTreeModel()
	{
		this(new OptionGroup(null));
	}

	public OptionTreeModel(OptionGroup root)
	{
		this.root = root;
		listenerList = new EventListenerList();
	}
	
	
	public void addTreeModelListener(TreeModelListener l)
	{
		listenerList.add(TreeModelListener.class, l);
	}

	public void removeTreeModelListener(TreeModelListener l)
	{
		listenerList.remove(TreeModelListener.class, l);
	}

	public Object getChild(Object parent, int index)
	{
		if (parent instanceof OptionGroup)
		{
			return ((OptionGroup)parent).getMember(index);
		}
		else
		{
			return null;
		}
	}

	public int getChildCount(Object parent)
	{
		if (parent instanceof OptionGroup)
		{
			return ((OptionGroup)parent).getMemberCount();
		}
		else
		{
			return 0;
		}
	}

	public int getIndexOfChild(Object parent, Object child)
	{
		if (parent instanceof OptionGroup)
		{
			return ((OptionGroup)parent)
				.getMemberIndex(child);
		}
		else
		{
			return -1;
		}
	}

	public Object getRoot()
	{
		return root;
	}

	public boolean isLeaf(Object node)
	{
		return !(node instanceof OptionGroup);
	}

	public void valueForPathChanged(TreePath path, Object newValue)
	{
		// this model may not be changed by the TableCellEditor
	}

	protected void fireNodesChanged(Object source, Object[] path,
		int[] childIndices, Object[] children)
	{
		Object[] listeners = listenerList.getListenerList();

		TreeModelEvent modelEvent = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] != TreeModelListener.class)
				continue;

			if (modelEvent == null)
			{
				modelEvent = new TreeModelEvent(source,
					path, childIndices, children);
			}

			((TreeModelListener)listeners[i + 1])
				.treeNodesChanged(modelEvent);
		}
	}

	protected void fireNodesInserted(Object source, Object[] path,
		int[] childIndices, Object[] children)
	{
		Object[] listeners = listenerList.getListenerList();

		TreeModelEvent modelEvent = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] != TreeModelListener.class)
				continue;

			if (modelEvent == null)
			{
				modelEvent = new TreeModelEvent(source,
					path, childIndices, children);
			}

			((TreeModelListener)listeners[i + 1])
				.treeNodesInserted(modelEvent);
		}
	}

	protected void fireNodesRemoved(Object source, Object[] path,
		int[] childIndices, Object[] children)
	{
		Object[] listeners = listenerList.getListenerList();

		TreeModelEvent modelEvent = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] != TreeModelListener.class)
				continue;

			if (modelEvent == null)
			{
				modelEvent = new TreeModelEvent(source,
					path, childIndices, children);
			}

			((TreeModelListener)listeners[i + 1])
				.treeNodesRemoved(modelEvent);
		}
	}

	protected void fireTreeStructureChanged(Object source,
		Object[] path, int[] childIndices, Object[] children)
	{
		Object[] listeners = listenerList.getListenerList();

		TreeModelEvent modelEvent = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] != TreeModelListener.class)
				continue;

			if (modelEvent == null)
			{
				modelEvent = new TreeModelEvent(source,
					path, childIndices, children);
			}

			((TreeModelListener)listeners[i + 1])
				.treeStructureChanged(modelEvent);
		}
	}
}