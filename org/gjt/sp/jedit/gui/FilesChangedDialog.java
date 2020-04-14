/*
 * FilesChangedDialog.java - Files changed on disk
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.manager.BufferManager;
import org.gjt.sp.util.EnhancedTreeCellRenderer;
import org.gjt.sp.util.GenericGUIUtilities;
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

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
		setContentPane(content);

		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(UIManager.getIcon("OptionPane.warningIcon")));
		iconBox.add(Box.createGlue());
		content.add(BorderLayout.WEST,iconBox);

		JPanel centerPanel = new JPanel(new BorderLayout());

		JLabel label = new JLabel(jEdit.getProperty("files-changed.caption"));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		centerPanel.add(BorderLayout.NORTH, label);

		DefaultMutableTreeNode deleted = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.deleted"),true);
		DefaultMutableTreeNode changed = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.changed"
			+ (alreadyReloaded ? "-auto" : "")),true);
		DefaultMutableTreeNode changedDirty = new DefaultMutableTreeNode(
			jEdit.getProperty("files-changed.changed-dirty"
			+ (alreadyReloaded ? "-auto" : "")),true);
		java.util.List<Buffer> buffers = jEdit.getBufferManager().getBuffers();
		for(int i = 0; i < states.length; i++)
		{
			Buffer buffer = buffers.get(i);
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
		bufferTree.setRowHeight(0);
		bufferTree.setRootVisible(false);
		bufferTree.setVisibleRowCount(10);
		bufferTree.setCellRenderer(new Renderer());
		bufferTree.getSelectionModel().addTreeSelectionListener(new TreeHandler());
		bufferTree.getSelectionModel().setSelectionMode(
			TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		centerPanel.add(BorderLayout.CENTER, new JScrollPane(bufferTree));

		content.add(BorderLayout.CENTER, centerPanel);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.setBorder(BorderFactory.createEmptyBorder(17, 0, 0, 0));
		buttons.add(Box.createGlue());

		if(!alreadyReloaded)
		{
			JButton selectAll = new JButton(jEdit.getProperty(
				"files-changed.select-all"));
			selectAll.setMnemonic(jEdit.getProperty(
				"files-changed.select-all.mnemonic").charAt(0));
			buttons.add(selectAll);
			selectAll.addActionListener(e -> selectAll());

			buttons.add(Box.createHorizontalStrut(6));

			reload = new JButton(jEdit.getProperty(
				"files-changed.reload"));
			reload.setMnemonic(jEdit.getProperty(
				"files-changed.reload.mnemonic").charAt(0));
			buttons.add(reload);
			reload.addActionListener(e -> action("RELOAD"));

			buttons.add(Box.createHorizontalStrut(6));

			ignore = new JButton(jEdit.getProperty("files-changed.ignore"));
			ignore.setMnemonic(jEdit.getProperty(
				"files-changed.ignore.mnemonic").charAt(0));
			buttons.add(ignore);
			ignore.addActionListener(e -> action("IGNORE"));

			buttons.add(Box.createHorizontalStrut(6));
		}

		JButton close = new JButton(jEdit.getProperty("common.close"));
		getRootPane().setDefaultButton(close);
		buttons.add(close);
		close.addActionListener(e -> dispose());

		content.add(BorderLayout.SOUTH, buttons);

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

		GenericGUIUtilities.requestFocus(this,bufferTree);

		updateEnabled();

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	@Override
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members
	private final View view;
	private final JTree bufferTree;
	private final DefaultTreeModel bufferTreeModel;
	private final DefaultMutableTreeNode root;

	// hack so that 'select all' does not change current buffer
	private boolean selectAllInProgress;

	private JButton reload;
	private JButton ignore;

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		TreePath[] paths = bufferTree
			.getSelectionPaths();
		boolean enabled = false;
		if(paths != null)
		{
			for (TreePath tp : paths)
			{
				Object[] path = tp.getPath();
				if (path.length == 3)
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

		BufferManager bufferManager = jEdit.getBufferManager();
		for (TreePath path : paths)
		{
			// is it a header?
			if (path.getPathCount() == 2)
				continue;

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (!(node.getUserObject() instanceof String))
				return;

			bufferManager.getBuffer((String) node.getUserObject())
				.ifPresent(buffer ->
				{
					if ("RELOAD".equals(action))
						buffer.reload(view);
					else
					{
						buffer.setAutoReload(false);
						buffer.setAutoReloadDialog(false);
					}

					MutableTreeNode parent = (MutableTreeNode) node.getParent();
					parent.remove(node);
				});
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

	//{{{ TreeHandler class
	class TreeHandler implements TreeSelectionListener
	{
		@Override
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
				jEdit.getBufferManager()
					.getBuffer((String)node.getUserObject())
					.ifPresent(buffer -> view.showBuffer(buffer));
			}
		}
	} //}}}

	//{{{ Renderer class
	static class Renderer extends EnhancedTreeCellRenderer
	{
		Renderer()
		{
			entryFont = UIManager.getFont("Tree.font");
			if(entryFont == null)
				entryFont = jEdit.getFontProperty("metal.secondary.font");
			groupFont = entryFont.deriveFont(Font.BOLD);
		}

		@Override
		protected TreeCellRenderer newInstance()
		{
			return new Renderer();
		}

		@Override
		protected void configureTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;

			if(node.getParent() == tree.getModel().getRoot())
				setFont(groupFont);
			else
				setFont(entryFont);

			setIcon(null);
		}

		private Font entryFont;
		private final Font groupFont;
	} //}}}
}
