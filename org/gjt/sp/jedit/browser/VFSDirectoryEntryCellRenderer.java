/*
 * VFSDirectoryEntryCellRenderer.java - renders table cells for the VFS browser
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2001, 2003 Slava Pestov
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
import javax.swing.border.*;
import javax.swing.table.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;
//}}}

public class VFSDirectoryEntryCellRenderer extends DefaultTableCellRenderer
{
	public static Icon fileIcon = GUIUtilities.loadIcon("File.png");
	public static Icon openFileIcon = GUIUtilities.loadIcon("OpenFile.png");
	public static Icon dirIcon = GUIUtilities.loadIcon("Folder.png");
	public static Icon openDirIcon = GUIUtilities.loadIcon("OpenFolder.png");
	public static Icon filesystemIcon = GUIUtilities.loadIcon("DriveSmall.png");
	public static Icon loadingIcon = GUIUtilities.loadIcon("ReloadSmall.png");

	//{{{ VFSDirectoryEntryCellRenderer constructor
	public VFSDirectoryEntryCellRenderer()
	{
		plainFont = UIManager.getFont("Tree.font");
		if(plainFont == null)
			plainFont = jEdit.getFontProperty("metal.secondary.font");
		boldFont = plainFont.deriveFont(Font.BOLD);
		setBorder(new EmptyBorder(1,0,1,0));
	} //}}}

	//{{{ getTableCellRendererComponent() method
	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, 
		int row, int column)
	{
		super.getTableCellRendererComponent(table,value,isSelected,
			hasFocus,row,column);

		if(value instanceof VFSDirectoryEntryTableModel.Entry)
		{
			VFSDirectoryEntryTableModel.Entry entry =
				(VFSDirectoryEntryTableModel.Entry)value;
			VFS.DirectoryEntry file = entry.dirEntry;

			underlined = (jEdit.getBuffer(file.path) != null);

			setIcon(showIcons
				? getIconForFile(file,entry.expanded)
				: null);
			setFont(file.type == VFS.DirectoryEntry.FILE
				? plainFont : boldFont);
			setText(file.name);
			setBorder(new EmptyBorder(0,entry.level * 5,0,0));

			if(!isSelected)
			{
				Color color = file.getColor();

				setForeground(color == null
					? UIManager.getColor("Tree.foreground")
					: color);
			}
		}

		return this;
	} //}}}

	//{{{ paintComponent() method
	public void paintComponent(Graphics g)
	{
		if(underlined)
		{
			Font font = getFont();

			FontMetrics fm = getFontMetrics(font);
			int x, y;
			if(getIcon() == null)
			{
				x = 0;
				y = fm.getAscent() + 2;
			}
			else
			{
				x = getIcon().getIconWidth() + getIconTextGap();
				y = Math.max(fm.getAscent() + 2,16);
			}
			g.setColor(getForeground());
			g.drawLine(x,y,x + fm.stringWidth(getText()),y);
		}

		super.paintComponent(g);
	} //}}}

	//{{{ getIconForFile() method
	public static Icon getIconForFile(VFS.DirectoryEntry file, boolean expanded)
	{
		if(file.type == VFS.DirectoryEntry.DIRECTORY)
			return (expanded ? openDirIcon : dirIcon);
		else if(file.type == VFS.DirectoryEntry.FILESYSTEM)
			return filesystemIcon;
		else if(jEdit.getBuffer(file.path) != null)
			return openFileIcon;
		else
			return fileIcon;
	} //}}}

	//{{{ Package-private members
	boolean showIcons;

	//{{{ propertiesChanged() method
	void propertiesChanged()
	{
		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
	} //}}}

	//}}}

	//{{{ Private members
	private Font plainFont;
	private Font boldFont;

	private boolean underlined;
	//}}}
}
