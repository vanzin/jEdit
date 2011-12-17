/*
 * OptionGroupPane.java - A Pane (view) for displaying/selecting OptionGroups.
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:
 *
 * Copyright (C) 2005 Slava Pestov
 * Copyright (C) 2005 Alan Ezust 
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
package org.gjt.sp.jedit.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.gjt.sp.util.StringModel;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.OptionGroup;
import org.gjt.sp.jedit.OptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.OptionsDialog.PaneNameRenderer;
import org.gjt.sp.util.Log;


/**
 * An option pane for displaying groups of options. There is a lot of code here
 * which was taken from OptionDialog, but this class is a component which can
 * be embedded in other Dialogs.
 * 
 * @see OptionDialog
 * 
 * 
 * @author ezust
 * 
 */
public class OptionGroupPane extends AbstractOptionPane implements TreeSelectionListener
{
	// {{{ Members
	OptionGroup optionGroup;

	JSplitPane splitter;

	JTree paneTree;

	OptionPane currentPane;

	OptionTreeModel optionTreeModel;

	HashMap<Object, OptionPane> deferredOptionPanes;

	JPanel stage;

	StringModel title = new StringModel();
	// }}}

	public OptionGroupPane(OptionGroup group)
	{
		super(group.getName());
		optionGroup = group;

		init();
	}

	void addTextListener(TextListener l)
	{
		title.addTextListener(l);
	}

	public String getTitle() 
	{
		return title.toString();
	}
	void setTitle(String newTitle)
	{
		title.setText(newTitle);
	}

	// {{{ valueChanged() method
	public void valueChanged(TreeSelectionEvent evt)
	{
		TreePath path = evt.getPath();

		if (path == null)
			return;

		Object lastPathComponent = path.getLastPathComponent();
		if (!(lastPathComponent instanceof String || lastPathComponent instanceof OptionPane))
		{
			return;
		}

		Object[] nodes = path.getPath();

		StringBuffer buf = new StringBuffer();

		OptionPane optionPane = null;

		int lastIdx = nodes.length - 1;

		for (int i = paneTree.isRootVisible() ? 0 : 1; i <= lastIdx; i++)
		{
			String label;
			Object node = nodes[i];
			if (node instanceof OptionPane)
			{
				optionPane = (OptionPane) node;
				label = jEdit.getProperty("options." + optionPane.getName()
					+ ".label");
			}
			else if (node instanceof OptionGroup)
			{
				label = ((OptionGroup) node).getLabel();
			}
			else if (node instanceof String)
			{
				label = jEdit.getProperty("options." + node + ".label");
				optionPane = (OptionPane) deferredOptionPanes.get((String) node);
				if (optionPane == null)
				{
					String propName = "options." + node + ".code";
					String code = jEdit.getProperty(propName);
					if (code != null)
					{
						optionPane = (OptionPane) BeanShell.eval(jEdit
							.getActiveView(), BeanShell.getNameSpace(),
							code);

						if (optionPane != null)
						{
							deferredOptionPanes.put(node, optionPane);
						}
						else
							continue;
					}
					else
					{
						Log.log(Log.ERROR, this, propName + " not defined");
						continue;
					}
				}
			}
			else
			{
				continue;
			}
			if (label != null)
				buf.append(label);

			if (i > 0 && i < lastIdx)
				buf.append(": ");
		}

		if (optionPane == null)
			return;
		
		String ttext = jEdit.getProperty("optional.title-template", new Object[] {
			optionGroup.getName(), buf.toString() });
		setTitle(ttext);

		try
		{
			optionPane.init();
		}
		catch (Throwable t)
		{
			Log.log(Log.ERROR, this, "Error initializing option pane:");
			Log.log(Log.ERROR, this, t);
		}

		if (currentPane != null)
			stage.remove(currentPane.getComponent());
		currentPane = optionPane;
		stage.add(BorderLayout.CENTER, currentPane.getComponent());
		stage.revalidate();
		stage.repaint();

		if (!isShowing())
			addNotify();

		currentPane = optionPane;
	} // }}}

        // {{{ selectPane() methods
	private boolean selectPane(OptionGroup node, String name)
	{
		return selectPane(node, name, new ArrayList());
	} 

	private boolean selectPane(OptionGroup node, String name, ArrayList path)
	{
		path.add(node);

		Enumeration e = node.getMembers();
		while (e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if (obj instanceof OptionGroup)
			{
				OptionGroup grp = (OptionGroup) obj;
				if (grp.getName().equals(name))
				{
					path.add(grp);
					path.add(grp.getMember(0));
					TreePath treePath = new TreePath(path.toArray());
					if (treePath != null)
					{
						paneTree.scrollPathToVisible(treePath);
						paneTree.setSelectionPath(treePath);

						return true;
					}
				}
				else if (selectPane((OptionGroup) obj, name, path))
				{
					return true;
				}
			}
			else if (obj instanceof OptionPane)
			{
				OptionPane pane = (OptionPane) obj;
				if (pane.getName().equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);

					return true;
				}
			}
			else if (obj instanceof String)
			{
				String pane = (String) obj;
				if (pane.equals(name) || name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(path.toArray());
					paneTree.scrollPathToVisible(treePath);
					try {
						paneTree.setSelectionPath(treePath);
					}
					catch (NullPointerException npe) {}

					return true;
				}
			}
		}

		path.remove(node);

		return false;
	} // }}}

	// {{{ init() method
	protected void _init()
	{

		setLayout(new BorderLayout());
		deferredOptionPanes = new HashMap<Object, OptionPane>();
		optionTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) optionTreeModel.getRoot();
		rootGroup.addOptionGroup(optionGroup);
		paneTree = new JTree(optionTreeModel);
		paneTree.setVisibleRowCount(1);
		paneTree.setRootVisible(false);
		paneTree.setCellRenderer(new PaneNameRenderer());

		JPanel content = new JPanel(new BorderLayout(12, 12));
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		add(content, BorderLayout.CENTER);

		stage = new JPanel(new BorderLayout());

		// looks bad with the OS X L&F, apparently...
		if (!OperatingSystem.isMacOSLF())
			paneTree.putClientProperty("JTree.lineStyle", "Angled");

		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);

		JScrollPane scroller = new JScrollPane(paneTree,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setMinimumSize(new Dimension(120, 0));
		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroller, new JScrollPane(stage));
		content.add(splitter, BorderLayout.CENTER);

		// register the Options dialog as a TreeSelectionListener.
		// this is done before the initial selection to ensure that the
		// first selected OptionPane is displayed on startup.
		paneTree.getSelectionModel().addTreeSelectionListener(this);

		OptionGroup rootNode = (OptionGroup) paneTree.getModel().getRoot();
		for (int i = 0; i < rootNode.getMemberCount(); i++)
		{
			paneTree.expandPath(new TreePath(new Object[] { rootNode,
				rootNode.getMember(i) }));
		}

		// returns false if no such pane exists; calling with null
		// param selects first option pane found
		String name = optionGroup.getName();
		selectPane(rootNode, null);
		/*
		 * if ((defaultPaneName != null) && (!selectPane(rootNode,
		 * defaultPaneName))) selectPane(rootNode, null);
		 */

		splitter.setDividerLocation(paneTree.getPreferredSize().width
			+ scroller.getVerticalScrollBar().getPreferredSize().width);

		String pane = jEdit.getProperty(name + ".last");
		selectPane(rootNode, pane);

		int dividerLocation = jEdit.getIntegerProperty(name + ".splitter", -1);
		if (dividerLocation != -1)
			splitter.setDividerLocation(dividerLocation);

	} //}}}

	//{{{ save() methods
	protected void _save()
	{
		if (currentPane != null)
			jEdit.setProperty(getName() + ".last", currentPane.getName());

		save(optionGroup);
	}

	private void save(Object obj)
	{

		if (obj instanceof OptionGroup)
		{
			OptionGroup grp = (OptionGroup) obj;
			Enumeration members = grp.getMembers();
			while (members.hasMoreElements())
			{
				save(members.nextElement());
			}
		}
		else if (obj instanceof OptionPane)
		{
			try
			{
				((OptionPane) obj).save();
			}
			catch (Throwable t)
			{
				Log.log(Log.ERROR, this, "Error saving options:");
				Log.log(Log.ERROR, this, t);
			}
		}
		else if (obj instanceof String)
		{
			save(deferredOptionPanes.get(obj));
		}
	} // }}}
	
	// {{{ class OptionTreeModel
	public class OptionTreeModel implements TreeModel
	{
		private OptionGroup root = new OptionGroup(null);
		private EventListenerList listenerList = new EventListenerList();

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
	} //}}}
}
