/*
 * OptionsDialog.java - Global options dialog
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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.options.*;
import org.gjt.sp.util.Log;

/**
 * An abstract tabbed options dialog box.
 * @author Slava Pestov
 * @version $Id$
 */
public class OptionsDialog extends EnhancedDialog
	implements ActionListener, TreeSelectionListener
{
	public OptionsDialog(View view)
	{
		super(view, jEdit.getProperty("options.title"), true);

		view.showWaitCursor();

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.setLayout(new BorderLayout());

		JPanel stage = new JPanel(new BorderLayout());
		stage.setBorder(new EmptyBorder(0,6,0,0));
		content.add(stage, BorderLayout.CENTER);

		// currentLabel displays the path of the currently selected
		// OptionPane at the top of the stage area
		currentLabel = new JLabel();
		currentLabel.setHorizontalAlignment(JLabel.LEFT);
		currentLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1,
			0, Color.black));
		stage.add(currentLabel, BorderLayout.NORTH);

		cardPanel = new JPanel(new CardLayout());
		cardPanel.setBorder(new EmptyBorder(5,0,0,0));
		stage.add(cardPanel, BorderLayout.CENTER);

		paneTree = new JTree(createOptionTreeModel());

		paneTree.setCellRenderer(new PaneNameRenderer());
		paneTree.putClientProperty("JTree.lineStyle", "Angled");
		paneTree.setShowsRootHandles(true);
		paneTree.setRootVisible(false);
		content.add(new JScrollPane(paneTree,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
			BorderLayout.WEST);

		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
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

		// compute the jEdit branch
		TreePath jEditPath = new TreePath(new Object[]{ paneTree
			.getModel().getRoot(), jEditGroup });

		// register the Options dialog as a TreeSelectionListener.
		// this is done before the initial selection to ensure that the
		// first selected OptionPane is displayed on startup.
		paneTree.getSelectionModel().addTreeSelectionListener(this);

		// select the first member of the jEdit group
		paneTree.setSelectionPath(jEditPath.pathByAddingChild(
			jEditGroup.getMember(0)));

		view.hideWaitCursor();

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public void addOptionGroup(OptionGroup group)
	{
		addOptionGroup(group, pluginsGroup);
	}

	public void addOptionPane(OptionPane pane)
	{
		addOptionPane(pane, pluginsGroup);
	}

	// EnhancedDialog implementation
	public void ok()
	{
		ok(true);
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

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
	}

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
	}

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
	}

	// private members
	private Hashtable panes;
	private JTree paneTree;
	private JPanel cardPanel;
	private JLabel currentLabel;
	private JButton ok;
	private JButton cancel;
	private JButton apply;
	private OptionGroup jEditGroup;
	private OptionGroup pluginsGroup;

	private OptionTreeModel createOptionTreeModel()
	{
		OptionTreeModel paneTreeModel = new OptionTreeModel();
		OptionGroup rootGroup = (OptionGroup) paneTreeModel.getRoot();

		// initialize the jEdit branch of the options tree
		jEditGroup = new OptionGroup("jedit");

		addOptionPane(new GeneralOptionPane(), jEditGroup);
		addOptionPane(new AppearanceOptionPane(), jEditGroup);
		addOptionPane(new TextAreaOptionPane(), jEditGroup);
		addOptionPane(new GutterOptionPane(), jEditGroup);
		addOptionPane(new ColorOptionPane(), jEditGroup);
		addOptionPane(new StyleOptionPane(), jEditGroup);
		addOptionPane(new LoadSaveOptionPane(), jEditGroup);
		addOptionPane(new EditingOptionPane(), jEditGroup);
		addOptionPane(new ModeOptionPane(), jEditGroup);
		addOptionPane(new ShortcutsOptionPane(), jEditGroup);
		addOptionPane(new DockingOptionPane(), jEditGroup);
		addOptionPane(new ContextOptionPane(), jEditGroup);
		addOptionPane(new ToolBarOptionPane(), jEditGroup);
		addOptionPane(new AbbrevsOptionPane(), jEditGroup);
		addOptionPane(new PrintOptionPane(), jEditGroup);

		OptionGroup browserGroup = new OptionGroup("browser");
		addOptionPane(new BrowserOptionPane(), browserGroup);
		addOptionPane(new BrowserColorsOptionPane(), browserGroup);
		addOptionGroup(browserGroup, jEditGroup);

		addOptionGroup(jEditGroup, rootGroup);

		// initialize the Plugins branch of the options tree
		pluginsGroup = new OptionGroup("plugins");

		// Query plugins for option panes
		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin ep = plugins[i];
			try
			{
				ep.createOptionPanes(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR, ep,
					"Error creating option pane");
				Log.log(Log.ERROR, ep, t);
			}
		}

		// only add the Plugins branch if there are OptionPanes
		if (pluginsGroup.getMemberCount() > 0)
		{
			addOptionGroup(pluginsGroup, rootGroup);
		}

		return paneTreeModel;
	}

	private void addOptionGroup(OptionGroup child, OptionGroup parent)
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
	}

	private void addOptionPane(OptionPane pane, OptionGroup parent)
	{
		String name = pane.getName();

		cardPanel.add(pane.getComponent(), name);

		parent.addOptionPane(pane);
	}

	class PaneNameRenderer extends JLabel implements TreeCellRenderer
	{
		public PaneNameRenderer()
		{
			setOpaque(true);

			paneFont = UIManager.getFont("Tree.font");
			groupFont = new Font(paneFont.getName(),
				paneFont.getStyle() | Font.BOLD,
				paneFont.getSize());
		}

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			if (selected)
			{
				this.setBackground(UIManager.getColor(
					"Tree.selectionBackground"));
				this.setForeground(UIManager.getColor(
					"Tree.selectionForeground"));
			}
			else
			{
				this.setBackground(UIManager.getColor(
					"Tree.background"));
				this.setForeground(UIManager.getColor(
					"Tree.foreground"));
			}

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
					setText(name);
				}
				else
				{
					setText(label);
				}
			}

			setIcon(null);

			return this;
		}

		private Border noFocusBorder = BorderFactory.createEmptyBorder(
			1, 1, 1, 1);
		private Border focusBorder = BorderFactory.createLineBorder(
			UIManager.getColor("Tree.selectionBorderColor"));

		private Font paneFont;
		private Font groupFont;
	}

	class OptionTreeModel implements TreeModel
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

		private OptionGroup root = new OptionGroup("root");
		private EventListenerList listenerList = new EventListenerList();
	}
}
