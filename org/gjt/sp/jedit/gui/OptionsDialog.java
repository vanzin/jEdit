/*
 * OptionsDialog.java - Tree options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * An abstract tabbed options dialog box.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class OptionsDialog extends EnhancedDialog
	implements ActionListener, TreeSelectionListener
{
	//{{{ OptionsDialog constructor
	public OptionsDialog(View view, String name, String pane)
	{
		super(view, jEdit.getProperty(name + ".title"), true);

		view.showWaitCursor();

		this.name = name;

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel stage = new JPanel(new BorderLayout(6,6));

		// currentLabel displays the path of the currently selected
		// OptionPane at the top of the stage area
		currentLabel = new JLabel();
		currentLabel.setHorizontalAlignment(JLabel.LEFT);
		stage.add(currentLabel, BorderLayout.NORTH);

		cardPanel = new JPanel(new CardLayout());
		stage.add(cardPanel, BorderLayout.CENTER);

		paneTree = new JTree(createOptionTreeModel());
		paneTree.setVisibleRowCount(1);
		paneTree.setCellRenderer(new PaneNameRenderer());

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			paneTree.putClientProperty("JTree.lineStyle", "Angled");

		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);

		JScrollPane scroller = new JScrollPane(paneTree,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			scroller,stage);
		content.add(splitter, BorderLayout.CENTER);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(6));
		getRootPane().setDefaultButton(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		buttons.add(Box.createHorizontalStrut(6));
		apply = new JButton(jEdit.getProperty("common.apply"));
		apply.addActionListener(this);
		buttons.add(apply);

		buttons.add(Box.createGlue());

		content.add(buttons, BorderLayout.SOUTH);

		// register the Options dialog as a TreeSelectionListener.
		// this is done before the initial selection to ensure that the
		// first selected OptionPane is displayed on startup.
		paneTree.getSelectionModel().addTreeSelectionListener(this);

		OptionGroup rootNode = (OptionGroup)paneTree.getModel().getRoot();
		for(int i = 0; i < rootNode.getMemberCount(); i++)
		{
			paneTree.expandPath(new TreePath(
			new Object[] { rootNode, rootNode.getMember(i) }));
		}

		if(pane == null || !selectPane(rootNode,pane))
			selectPane(rootNode,firstPane);

		view.hideWaitCursor();

		pack();
		setLocationRelativeTo(view);
		show();
	} //}}}

	//{{{ addOptionGroup() method
	public void addOptionGroup(OptionGroup group)
	{
		addOptionGroup(group, getDefaultGroup());
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(OptionPane pane)
	{
		addOptionPane(pane, getDefaultGroup());
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		jEdit.setProperty(name + ".last",currentPane);
		ok(true);
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		jEdit.setProperty(name + ".last",currentPane);
		dispose();
	} //}}}

	//{{{ ok() method
	public void ok(boolean dispose)
	{
		OptionTreeModel m = (OptionTreeModel) paneTree
			.getModel();
		((OptionGroup) m.getRoot()).save();

		/* This will fire the PROPERTIES_CHANGED event */
		jEdit.propertiesChanged();

		// Save settings to disk
		jEdit.saveSettings();

		// get rid of this dialog if necessary
		if(dispose)
			dispose();
	} //}}}

	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();

		if(source == ok)
		{
			ok();
		}
		else if(source == cancel)
		{
			cancel();
		}
		else if(source == apply)
		{
			ok(false);
		}
	} //}}}

	//{{{ valueChanged() method
	public void valueChanged(TreeSelectionEvent evt)
	{
		TreePath path = evt.getPath();

		if (path == null || !(path.getLastPathComponent() instanceof
			OptionPane)) return;

		Object[] nodes = path.getPath();

		StringBuffer buf = new StringBuffer();

		OptionPane optionPane = null;
		String name = null;

		int lastIdx = nodes.length - 1;

		for (int i = paneTree.isRootVisible() ? 0 : 1;
			i <= lastIdx; i++)
		{
			if (nodes[i] instanceof OptionPane)
			{
				optionPane = (OptionPane)nodes[i];
				name = optionPane.getName();
			}
			else if (nodes[i] instanceof OptionGroup)
			{
				name = ((OptionGroup)nodes[i]).getName();
			}
			else
			{
				continue;
			}

			if (name != null)
			{
				String label = jEdit.getProperty("options." +
					name + ".label");

				if (label == null)
				{
					buf.append(name);
				}
				else
				{
					buf.append(label);
				}
			}

			if (i != lastIdx) buf.append(": ");
		}

		currentLabel.setText(buf.toString());

		optionPane.init();

		pack();
		((CardLayout)cardPanel.getLayout()).show(cardPanel, name);
		currentPane = name;
	} //}}}

	//{{{ Protected members
	protected abstract OptionTreeModel createOptionTreeModel();
	protected abstract OptionGroup getDefaultGroup();

	//{{{ addOptionGroup() method
	protected void addOptionGroup(OptionGroup child, OptionGroup parent)
	{
		Enumeration enum = child.getMembers();

		while (enum.hasMoreElements())
		{
			Object elem = enum.nextElement();

			if (elem instanceof OptionPane)
			{
				addOptionPane((OptionPane) elem, child);
			}
			else if (elem instanceof OptionGroup)
			{
				addOptionGroup((OptionGroup) elem, child);
			}
		}

		parent.addOptionGroup(child);
	} //}}}

	//{{{ addOptionPane() method
	protected void addOptionPane(OptionPane pane, OptionGroup parent)
	{
		String name = pane.getName();
		if(firstPane == null)
			firstPane = name;

		cardPanel.add(pane.getComponent(), name);

		parent.addOptionPane(pane);
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private String name;
	private JTree paneTree;
	private JPanel cardPanel;
	private JLabel currentLabel;
	private JButton ok;
	private JButton cancel;
	private JButton apply;
	private String currentPane;
	private String firstPane;
	//}}}

	//{{{ selectPane() method
	private boolean selectPane(OptionGroup node, String name)
	{
		return selectPane(node,name,new ArrayList());
	} //}}}

	//{{{ selectPane() method
	private boolean selectPane(OptionGroup node, String name, ArrayList path)
	{
		path.add(node);

		Enumeration enum = node.getMembers();
		while(enum.hasMoreElements())
		{
			Object obj = enum.nextElement();
			if(obj instanceof OptionGroup)
			{
				OptionGroup grp = (OptionGroup)obj;
				if(grp.getName().equals(name))
				{
					path.add(grp);
					path.add(grp.getMember(0));
					TreePath treePath = new TreePath(
						path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);
					return true;
				}
				else if(selectPane((OptionGroup)obj,name,path))
					return true;
			}
			else
			{
				OptionPane pane = (OptionPane)obj;
				if(pane.getName().equals(name))
				{
					path.add(pane);
					TreePath treePath = new TreePath(
						path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);
					return true;
				}
			}
		}

		path.remove(node);

		return false;
	} //}}}

	//}}}

	//{{{ PaneNameRenderer class
	class PaneNameRenderer extends DefaultTreeCellRenderer
	{
		public PaneNameRenderer()
		{
			paneFont = UIManager.getFont("Tree.font");
			groupFont = paneFont.deriveFont(Font.BOLD);
		}

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,
				selected,expanded,leaf,row,hasFocus);

			String name = null;

			if (value instanceof OptionGroup)
			{
				name = ((OptionGroup)value).getName();
				this.setFont(groupFont);
			}
			else if (value instanceof OptionPane)
			{
				name = ((OptionPane)value).getName();
				this.setFont(paneFont);
			}

			if (name == null)
			{
				setText(null);
			}
			else
			{
				String label = jEdit.getProperty("options." +
					name + ".label");

				if (label == null)
				{
					setText("NO LABEL PROPERTY: " + name);
				}
				else
				{
					setText(label);
				}
			}

			setIcon(null);

			return this;
		}

		private Font paneFont;
		private Font groupFont;
	} //}}}

	//{{{ OptionTreeModel class
	public class OptionTreeModel implements TreeModel
	{
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
			return node instanceof OptionPane;
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

		private OptionGroup root = new OptionGroup(null);
		private EventListenerList listenerList = new EventListenerList();
	} //}}}
}
