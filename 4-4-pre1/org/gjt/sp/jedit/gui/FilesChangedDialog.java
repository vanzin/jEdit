/*
 * FilesChangedDialog.java - Files changed on disk
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Files changed on disk dialog.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class FilesChangedDialog extends EnhancedDialog
{
	//{{{ FilesChangedDialog constructor
	public FilesChangedDialog(View view, int[] states,
		boolean alreadyReloaded)
	{
		super(view,jEdit.getProperty("files-changed.title"),false);

		this.view = view;

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(UIManager.getIcon("OptionPane.warningIcon")));
		iconBox.add(Box.createGlue());
		content.add(BorderLayout.WEST,iconBox);

		JPanel centerPanel = new JPanel(new BorderLayout());

		JLabel label = new JLabel(jEdit.getProperty("files-changed.caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		centerPanel.add(BorderLayout.NORTH,label);

		DefaultMutableTreeNode deleted = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.deleted"),true);
		DefaultMutableTreeNode changed = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.changed"
			+ (alreadyReloaded ? "-auto" : "")),true);
		DefaultMutableTreeNode changedDirty = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.changed-dirty"
			+ (alreadyReloaded ? "-auto" : "")),true);
		Buffer[] buffers = jEdit.getBuffers();
		for(int i = 0; i < states.length; i++)
		{
			Buffer buffer = buffers[i];
			DefaultMutableTreeNode addTo;
			switch(states[i])
			{
			case Buffer.FILE_DELETED:
				addTo = deleted;
				break;
			case Buffer.FILE_CHANGED:
				addTo = buffer.isDirty() ? changedDirty : changed;
				break;
			default:
				addTo = null;
				break;
			}

			if(addTo != null)
			{
				addTo.add(new DefaultMutableTreeNode(
					buffer.getPath()));
			}
		}

		root = new DefaultMutableTreeNode("",true);
		if(deleted.getChildCount() != 0)
		{
			root.add(deleted);
		}
		if(changed.getChildCount() != 0)
		{
			root.add(changed);
		}
		if(changedDirty.getChildCount() != 0)
		{
			root.add(changedDirty);
		}

		bufferTreeModel = new DefaultTreeModel(root);
		bufferTree = new JTree(bufferTreeModel);
		bufferTree.setRootVisible(false);
		bufferTree.setVisibleRowCount(10);
		bufferTree.setCellRenderer(new Renderer());
		bufferTree.getSelectionModel().addTreeSelectionListener(new TreeHandler());
		bufferTree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		centerPanel.add(BorderLayout.CENTER,new JScrollPane(bufferTree));

		content.add(BorderLayout.CENTER,centerPanel);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());

		if(!alreadyReloaded)
		{
			selectAll = new JButton(jEdit.getProperty(
				"files-changed.select-all"));
			selectAll.setMnemonic(jEdit.getProperty(
				"files-changed.select-all.mnemonic").charAt(0));
			buttons.add(selectAll);
			selectAll.addActionListener(new ActionHandler());

			buttons.add(Box.createHorizontalStrut(12));

			reload = new JButton(jEdit.getProperty(
				"files-changed.reload"));
			reload.setMnemonic(jEdit.getProperty(
				"files-changed.reload.mnemonic").charAt(0));
			buttons.add(reload);
			reload.addActionListener(new ActionHandler());

			buttons.add(Box.createHorizontalStrut(12));

			ignore = new JButton(jEdit.getProperty("files-changed.ignore"));
			ignore.setMnemonic(jEdit.getProperty(
				"files-changed.ignore.mnemonic").charAt(0));
			buttons.add(ignore);
			ignore.addActionListener(new ActionHandler());

			buttons.add(Box.createHorizontalStrut(12));
		}

		close = new JButton(jEdit.getProperty("common.close"));
		getRootPane().setDefaultButton(close);
		buttons.add(close);
		close.addActionListener(new ActionHandler());

		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		bufferTree.expandPath(new TreePath(
			new Object[] {
				root,
				deleted
			}));
		bufferTree.expandPath(new TreePath(
			new Object[] {
				root,
				changed
			}));
		bufferTree.expandPath(new TreePath(
			new Object[] {
				root,
				changedDirty
			}));

		GUIUtilities.requestFocus(this,bufferTree);

		updateEnabled();

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members
	private View view;
	private JTree bufferTree;
	private DefaultTreeModel bufferTreeModel;
	private DefaultMutableTreeNode root;
	private JButton selectAll;

	// hack so that 'select all' does not change current buffer
	private boolean selectAllInProgress;

	private JButton reload;
	private JButton ignore;
	private JButton close;

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		TreePath[] paths = bufferTree
			.getSelectionPaths();
		boolean enabled = false;
		if(paths != null)
		{
			for(int i = 0; i < paths.length; i++)
			{
				Object[] path = paths[i].getPath();
				if(path.length == 3)
					enabled = true;
			}
		}

		if(reload != null)
			reload.setEnabled(enabled);

		if (ignore != null)
			ignore.setEnabled(enabled);
	} //}}}

	//{{{ selectAll() method
	private void selectAll()
	{
		selectAllInProgress = true;

		TreeNode[] path = new TreeNode[3];
		path[0] = root;
		for(int i = 0; i < root.getChildCount(); i++)
		{
			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode)
				root.getChildAt(i);
			path[1] = node;
			for(int j = 0; j < node.getChildCount(); j++)
			{
				DefaultMutableTreeNode node2 =
					(DefaultMutableTreeNode)
					node.getChildAt(j);
				path[2] = node2;
				bufferTree.getSelectionModel()
					.addSelectionPath(
					new TreePath(path));
			}
		}

		selectAllInProgress = false;

		updateEnabled();
	} //}}}

	//{{{ reload() method
	private void action(String action)
	{
		TreePath[] paths = bufferTree
			.getSelectionPaths();
		if(paths == null || paths.length == 0)
			return;

		int row = bufferTree.getRowForPath(paths[0]);

		for(int i = 0; i < paths.length; i++)
		{
			TreePath path = paths[i];

			// is it a header?
			if(path.getPathCount() == 2)
				continue;

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			if(!(node.getUserObject() instanceof String))
			{
				return;
			}

			Buffer buffer = jEdit.getBuffer(
				(String)node.getUserObject());
			if(buffer == null)
				return;

			if ("RELOAD".equals(action))
				buffer.reload(view);
			else
			{
				buffer.setAutoReload(false);
				buffer.setAutoReloadDialog(false);
			}

			DefaultMutableTreeNode parent =
				(DefaultMutableTreeNode)
				node.getParent();
			parent.remove(node);
		}

		bufferTreeModel.reload(root);

		// we expand those that are non-empty, and
		// remove those that are empty
		TreeNode[] nodes = { root, null };

		// remove empty category branches
		for(int j = 0; j < root.getChildCount(); j++)
		{
			DefaultMutableTreeNode node
				= (DefaultMutableTreeNode)
				root.getChildAt(j);
			if(root.getChildAt(j)
				.getChildCount() == 0)
			{
				root.remove(j);
				j--;
			}
			else
			{
				nodes[1] = node;
				bufferTree.expandPath(
					new TreePath(nodes));
			}
		}

		if(root.getChildCount() == 0)
			dispose();
		else
		{
			if(row >= bufferTree.getRowCount())
				row = bufferTree.getRowCount() - 1;
			TreePath path = bufferTree.getPathForRow(row);
			if(path.getPathCount() == 2)
			{
				// selected a header; skip to the next row
				bufferTree.setSelectionRow(row + 1);
			}
			else
				bufferTree.setSelectionPath(path);
		}
	} //}}}

	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == selectAll)
				selectAll();
			else if(source == reload)
				action("RELOAD");
			else if(source == close)
				dispose();
			else if (source == ignore)
				action("IGNORE");
		}
	} //}}}

	//{{{ TreeHandler class
	class TreeHandler implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent evt)
		{
			if(selectAllInProgress)
				return;

			updateEnabled();

			TreePath[] paths = bufferTree
				.getSelectionPaths();
			if(paths == null || paths.length == 0)
				return;
			TreePath path = paths[paths.length - 1];
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			if(node.getUserObject() instanceof String)
			{
				Buffer buffer = jEdit.getBuffer(
					(String)node.getUserObject());
				if(buffer != null)
					view.showBuffer(buffer);
			}
		}
	} //}}}

	//{{{ Renderer class
	static class Renderer extends DefaultTreeCellRenderer
	{
		Renderer()
		{
			entryFont = UIManager.getFont("Tree.font");
			if(entryFont == null)
				entryFont = jEdit.getFontProperty("metal.secondary.font");
			groupFont = entryFont.deriveFont(Font.BOLD);
		}

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,
				selected,expanded,leaf,row,hasFocus);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

			if(node.getParent() == tree.getModel().getRoot())
				setFont(groupFont);
			else
				setFont(entryFont);

			setIcon(null);

			return this;
		}

		private Font entryFont;
		private Font groupFont;
	} //}}}
}
