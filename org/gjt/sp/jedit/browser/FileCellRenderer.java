/*
 * FileCellRenderer.java - renders list and tree cells for the VFS browser
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2001 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;
//}}}

public class FileCellRenderer extends JLabel implements TreeCellRenderer
{
	public static Icon fileIcon = GUIUtilities.loadIcon("file.gif");
	public static Icon dirIcon = GUIUtilities.loadIcon("closed_folder.gif");
	public static Icon openDirIcon = GUIUtilities.loadIcon("open_folder.gif");
	public static Icon filesystemIcon = GUIUtilities.loadIcon("drive.gif");
	public static Icon loadingIcon = GUIUtilities.loadIcon("drive.gif");

	//{{{ FileCellRenderer constructor
	public FileCellRenderer()
	{
		plainFont = UIManager.getFont("Tree.font");
		boldFont = plainFont.deriveFont(Font.BOLD);

		setOpaque(true);
	} //}}}

	//{{{ getTreeCellRendererComponent() method
	public Component getTreeCellRendererComponent(JTree tree, Object value,
		boolean sel, boolean expanded, boolean leaf, int row,
		boolean focus)
	{
		if(sel)
		{
			setBackground(treeSelectionBackground);
			setForeground(treeSelectionForeground);
		}
		else
		{
			setBackground(treeNoSelectionBackground);
			setForeground(treeNoSelectionForeground);
		}

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
		Object userObject = treeNode.getUserObject();
		if(userObject instanceof VFS.DirectoryEntry)
		{
			VFS.DirectoryEntry file = (VFS.DirectoryEntry)userObject;

			boolean opened = (jEdit.getBuffer(file.path) != null);
			setBorder(opened ? openBorder : closedBorder);

			setIcon(showIcons
				? getIconForFile(file,expanded)
				: null);
			setFont(file.type == VFS.DirectoryEntry.FILE
				? plainFont : boldFont);
			setText(file.name);

			if(!sel)
			{
				Color color = file.getColor();

				setForeground(color == null
					? treeNoSelectionForeground
					: color);
			}
		}
		else if(userObject instanceof BrowserView.LoadingPlaceholder)
		{
			setIcon(showIcons ? loadingIcon : null);
			setFont(plainFont);
			setText(jEdit.getProperty("vfs.browser.tree.loading"));
			setBorder(closedBorder);
		}
		else if(userObject instanceof String)
		{
			setIcon(showIcons ? dirIcon : null);
			setFont(boldFont);
			setText((String)userObject);

			setBorder(closedBorder);
		}
		else
		{
			// userObject is null?
			setIcon(null);
			setText(null);
		}

		return this;
	} //}}}

	//{{{ Protected members

	//{{{ getIconForFile() method
	protected Icon getIconForFile(VFS.DirectoryEntry file, boolean expanded)
	{
		if(file.type == VFS.DirectoryEntry.DIRECTORY)
			return (expanded ? openDirIcon : dirIcon);
		else if(file.type == VFS.DirectoryEntry.FILESYSTEM)
			return filesystemIcon;
		else
			return fileIcon;
	} //}}}

	//}}}

	//{{{ Package-private members
	boolean showIcons;

	//{{{ propertiesChanged() method
	void propertiesChanged()
	{
		// bug in DefaultTreeCellRenderer?
		setBackground(UIManager.getColor("Tree.textBackground"));

		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
		closedBorder = new EmptyBorder(1,4,1,1);
		openBorder = new CompoundBorder(new MatteBorder(0,2,0,0,
			UIManager.getColor("Tree.textForeground")),
			new EmptyBorder(1,2,1,1));

		treeSelectionForeground = UIManager.getColor("Tree.selectionForeground");
		treeNoSelectionForeground = UIManager.getColor("Tree.textForeground");
		treeSelectionBackground = UIManager.getColor("Tree.selectionBackground");
		treeNoSelectionBackground = UIManager.getColor("Tree.textBackground");
	} //}}}

	//}}}

	//{{{ Private members
	private Border closedBorder;
	private Border openBorder;

	private Color treeSelectionForeground;
	private Color treeNoSelectionForeground;
	private Color treeSelectionBackground;
	private Color treeNoSelectionBackground;

	private Font plainFont;
	private Font boldFont;
	//}}}
}
