/*
 * OptionsDialog.java - Tree options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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
import java.util.List;


import org.gjt.sp.jedit.*;
import org.gjt.sp.util.EnhancedTreeCellRenderer;
import org.gjt.sp.util.Log;
//}}}

/**
 * An abstract options dialog box.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class OptionsDialog extends EnhancedDialog
	implements ActionListener, TreeSelectionListener
{
	//{{{ Instance variables
	private String name;
	private JSplitPane splitter;
	protected JTree paneTree;
	private JScrollPane stage;
	private JButton ok;
	private JButton cancel;
	private JButton apply;
	protected OptionPane currentPane;
	private Map<Object, OptionPane> deferredOptionPanes;
	//}}}

	//{{{ OptionsDialog constructor
	/**
	 * @param frame - the parent frame for dialogs created
	 * @param name the name of an option pane - it must have a .title and .code
	 *  		property defined in order to instantiate.
	 * @param pane the initial pane to show when this is created.
	 */
	protected OptionsDialog(Frame frame, String name, String pane)
	{
		super(frame, jEdit.getProperty(name + ".title"), true);
		init(name,pane);
	} //}}}

	//{{{ OptionsDialog constructor
	protected OptionsDialog(Dialog dialog, String name, String pane)
	{
		super(dialog, jEdit.getProperty(name + ".title"), true);
		init(name,pane);
	} //}}}

	//{{{ addOptionGroup() method
	public void addOptionGroup(OptionGroup group)
	{
		getDefaultGroup().addOptionGroup(group);
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(OptionPane pane)
	{
		getDefaultGroup().addOptionPane(pane);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		if(currentPane != null)
			jEdit.setProperty(name + ".last",currentPane.getName());
		ok(true);
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		if(currentPane != null)
			jEdit.setProperty(name + ".last",currentPane.getName());
		dispose();
	} //}}}

	//{{{ ok() method
	public void ok(boolean dispose)
	{
		OptionTreeModel m = (OptionTreeModel) paneTree
			.getModel();
		save(m.getRoot());

		/* This will fire the PROPERTIES_CHANGED event */
		jEdit.propertiesChanged();

		// Save settings to disk
		jEdit.saveSettings();

		// get rid of this dialog if necessary
		if(dispose)
			dispose();
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		GUIUtilities.saveGeometry(this,name);
		jEdit.setIntegerProperty(name + ".splitter",splitter.getDividerLocation());
		super.dispose();
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

		if(path == null)
			return;

		Object lastPathComponent = path.getLastPathComponent();
		if(!(lastPathComponent instanceof String
			|| lastPathComponent instanceof OptionPane))
		{
			return;
		}

		Object[] nodes = path.getPath();

		StringBuilder buf = new StringBuilder();

		OptionPane optionPane = null;

		int lastIdx = nodes.length - 1;

		for (int i = paneTree.isRootVisible() ? 0 : 1;
			i <= lastIdx; i++)
		{
			String label;
			Object node = nodes[i];
			if (node instanceof OptionPane)
			{
				optionPane = (OptionPane)node;
				label = jEdit.getProperty("options."
					+ optionPane.getName()
					+ ".label");
			}
			else if (node instanceof OptionGroup)
			{
				label = ((OptionGroup)node).getLabel();
			}
			else if (node instanceof String)
			{
				label = jEdit.getProperty("options."
					+ node + ".label");
				optionPane = deferredOptionPanes.get(node);
				if(optionPane == null)
				{
					String propName = "options." + node + ".code";
					String code = jEdit.getProperty(propName);
					if(code != null)
					{
						optionPane = (OptionPane)
							BeanShell.eval(
							jEdit.getActiveView(),
							BeanShell.getNameSpace(),
							code
						);

						if(optionPane != null)
						{
							deferredOptionPanes.put(
								node,optionPane);
						}
						else
							continue;
					}
					else
					{
						Log.log(Log.ERROR,this,propName
							+ " not defined");
						continue;
					}
				}
			}
			else
			{
				continue;
			}

			buf.append(label);

			if (i != lastIdx)
				buf.append(": ");
		}

		if(optionPane == null)
			return;

		setTitle(jEdit.getProperty("options.title-template",
			new Object[] { jEdit.getProperty(name + ".title"),
			buf.toString() }));

		try
		{
			optionPane.init();
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,this,"Error initializing options:");
			Log.log(Log.ERROR,this,t);
		}

		currentPane = optionPane;
		stage.setViewportView(currentPane.getComponent());
		stage.revalidate();
		stage.repaint();

		if(!isShowing())
			addNotify();

		updateSize();

		currentPane = optionPane;
	} //}}}

	//{{{ Protected members
	// {{{ createOptionTreeModel
	/**
	 * Creates the tree model that goes on the left of the option pane,
	 * loading all the items that are needed.
	 */
	protected abstract OptionTreeModel createOptionTreeModel();
	// }}}

	protected abstract OptionGroup getDefaultGroup();
	//}}}

	//{{{ init() method
	/**
	 * @param name the name of this pane
	 * @param pane - a sub-pane name to select (?)
	 * Could someone please write better docs for this function?
	 * Creates buttons, adds listeners, and makes the pane visible.
	 * This method is called automatically from the constructor,
	 *
	 * and also calls init on each of the optionPanes?
	 *
	 * @since jEdit 4.3pre9 (was private before)
	 */
	protected void init(String name, String pane)
	{
		this.name = name;

		deferredOptionPanes = new HashMap<Object, OptionPane>();

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);
		stage = new JScrollPane();

		paneTree = new JTree(createOptionTreeModel());
		paneTree.setVisibleRowCount(1);
		paneTree.setCellRenderer(new PaneNameRenderer());

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			paneTree.putClientProperty("JTree.lineStyle", "Angled");

		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);

		JScrollPane scroller = new JScrollPane(paneTree,
						       ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
						       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setMinimumSize(new Dimension(100, 0));
		splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					  jEdit.getBooleanProperty("appearance.continuousLayout"),
					  scroller,
					  stage);
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

		// returns false if no such pane exists; calling with null
		// param selects first option pane found
		if(!selectPane(rootNode, pane))
			selectPane(rootNode,null);

		splitter.setDividerLocation(paneTree.getPreferredSize().width
			+ scroller.getVerticalScrollBar().getPreferredSize()
			.width);

		GUIUtilities.loadGeometry(this,name);
		int dividerLocation = jEdit.getIntegerProperty(name + ".splitter",-1);
		if(dividerLocation != -1)
			splitter.setDividerLocation(dividerLocation);

		// in case saved geometry is too small
		updateSize();

		setVisible(true);
	} //}}}

	//{{{ Private members

	//{{{ selectPane() method
	private boolean selectPane(OptionGroup node, String name)
	{
		return selectPane(node,name,new ArrayList<Object>());
	} //}}}

	//{{{ selectPane() method
	private boolean selectPane(OptionGroup node, String name, List<Object> path)
	{
		path.add(node);

		Enumeration<Object> e = node.getMembers();
		while(e.hasMoreElements())
		{
			Object obj = e.nextElement();
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
			else if(obj instanceof OptionPane)
			{
				OptionPane pane = (OptionPane)obj;
				if(pane.getName().equals(name)
					|| name == null)
				{
					path.add(pane);
					TreePath treePath = new TreePath(
						path.toArray());
					paneTree.scrollPathToVisible(treePath);
					paneTree.setSelectionPath(treePath);
					return true;
				}
			}
			else if(obj instanceof String)
			{
				String pane = (String)obj;
				if(pane.equals(name)
					|| name == null)
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

	//{{{ save() method
	private void save(Object obj)
	{
		if(obj instanceof OptionGroup)
		{
			OptionGroup grp = (OptionGroup)obj;
			Enumeration<Object> members = grp.getMembers();
			while(members.hasMoreElements())
			{
				save(members.nextElement());
			}
		}
		else if(obj instanceof OptionPane)
		{
			try
			{
				((OptionPane)obj).save();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error saving options:");
				Log.log(Log.ERROR,this,t);
			}
		}
		else if(obj instanceof String)
		{
			save(deferredOptionPanes.get(obj));
		}
	} //}}}

	//{{{ updateSize() method
	private void updateSize()
	{
		Dimension currentSize = getSize();
		Dimension requestedSize = getPreferredSize();
		Dimension newSize = new Dimension(
			Math.max(currentSize.width,requestedSize.width),
			Math.max(currentSize.height,requestedSize.height)
		);
		if(newSize.width < 300)
			newSize.width = 300;
		if(newSize.height < 200)
			newSize.height = 200;
		setSize(newSize);
		validate();
	} //}}}

	//}}}

	//{{{ PaneNameRenderer class
	public static class PaneNameRenderer extends EnhancedTreeCellRenderer
	{
		public PaneNameRenderer()
		{
			paneFont = UIManager.getFont("Tree.font");
			if(paneFont == null)
				paneFont = jEdit.getFontProperty("metal.secondary.font");
			groupFont = paneFont.deriveFont(Font.BOLD);
		}

		@Override
		protected TreeCellRenderer newInstance()
		{
			return new PaneNameRenderer();
		}

		@Override
		protected void configureTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			String name = null;

			if (value instanceof OptionGroup)
			{
				setText(((OptionGroup)value).getLabel());
				setFont(groupFont);
			}
			else if (value instanceof OptionPane)
			{
				name = ((OptionPane)value).getName();
				setFont(paneFont);
			}
			else if (value instanceof String)
			{
				name = (String) value;
				setFont(paneFont);
			}

			if (name != null)
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
		}

		private Font paneFont;
		private final Font groupFont;
	} //}}}

	//{{{ OptionTreeModel class
	public class OptionTreeModel implements TreeModel
	{
		public OptionTreeModel()
		{
			this(new OptionGroup(null));
		}

		public OptionTreeModel(OptionGroup root)
		{
			this.root = root;
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

		private final OptionGroup root;
		private final EventListenerList listenerList = new EventListenerList();
	} //}}}
}
