/*
 * VFSDirectoryEntryTableModel.java - VFS directory entry table model
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

package org.gjt.sp.jedit.browser;

import javax.swing.table.*;
import javax.swing.*;
import java.util.ArrayList;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;

public class VFSDirectoryEntryTableModel extends AbstractTableModel
{
	//{{{ VFSDirectoryEntryTableModel constructor
	public VFSDirectoryEntryTableModel()
	{
		columns = new Column[3];
		columns[0] = new Column("status");
		columns[1] = new Column("size");
		columns[2] = new Column("modified");
	} //}}}

	//{{{ setRoot() method
	public void setRoot(ArrayList files)
	{
		this.files = new Entry[files.size()];
		for(int i = 0; i < files.size(); i++)
		{
			this.files[i] = new Entry((VFS.DirectoryEntry)files.get(i),0);
		}
		fireTableStructureChanged();
	} //}}}

	//{{{ expand() method
	public void expand(Entry entry, ArrayList list)
	{
		entry.expanded = true;

		int startIndex = -1;
		for(int i = 0; i < files.length; i++)
		{
			if(files[i] == entry)
				startIndex = i;
		}

		collapse(startIndex);

		if(list != null)
		{
			Entry[] newFiles = new Entry[files.length + list.size()];
			System.arraycopy(files,0,newFiles,0,startIndex);
			for(int i = 0; i < list.size(); i++)
			{
				newFiles[startIndex + i + 1] = new Entry(
					(VFS.DirectoryEntry)list.get(i),
					entry.level + 1);
			}
			System.arraycopy(files,startIndex + 1,
				newFiles,startIndex + list.size() + 1,
				files.length - startIndex - 1);
			this.files = newFiles;

			fireTableRowsInserted(startIndex + 1,
				startIndex + list.size() + 1);
		}
	} //}}}

	//{{{ collapse() method
	public void collapse(int index)
	{
		Entry entry = files[index];
		if(!entry.expanded)
			return;

		int lastIndex = index + 1;
		for(;;)
		{
			Entry e = files[lastIndex];
			if(e.level <= entry.level)
				break;
			else
				lastIndex++;
		}

		Entry[] newFiles = new Entry[files.length - (lastIndex - index)];
		System.arraycopy(files,0,newFiles,0,index + 1);
		System.arraycopy(files,lastIndex,newFiles,index + 1,
			lastIndex - index);

		fireTableRowsDeleted(index + 1,lastIndex);
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 3 + columns.length;
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		if(files == null)
			return 0;
		else
			return files.length;
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int col)
	{
		switch(col)
		{
		case 0:
			return null;
		case 1:
			return jEdit.getProperty("vfs.browser.name");
		case 2:
			return jEdit.getProperty("vfs.browser.type");
		default:
			return jEdit.getProperty("vfs.browser."
				+ columns[col - 3].name);
		}
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int col)
	{
		switch(col)
		{
		case 0:
			return Icon.class;
		case 1:
			return Entry.class;
		default:
			return String.class;
		}
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int row, int col)
	{
		if(files == null)
			return null;

		Entry entry = files[row];
		VFS.DirectoryEntry dirEntry = entry.dirEntry;
		switch(col)
		{
		case 0:
			if(entry.dirEntry.type == VFS.DirectoryEntry.FILE)
				return null;
			else if(entry.expanded)
				return UIManager.getIcon("Tree.expandedIcon");
			else
				return UIManager.getIcon("Tree.collapsedIcon");
		case 1:
			return entry;
		case 2:
			switch(entry.dirEntry.type)
			{
			case VFS.DirectoryEntry.FILE:
				return jEdit.getProperty("vfs.browser.type.file");
			case VFS.DirectoryEntry.DIRECTORY:
				return jEdit.getProperty("vfs.browser.type.directory");
			case VFS.DirectoryEntry.FILESYSTEM:
				return jEdit.getProperty("vfs.browser.type.filesystem");
			}
			throw new InternalError();
		default:
			return columns[col - 3].get(entry.dirEntry);
		}
	} //}}}

	//{{{ Package-private members
	Entry[] files;
	//}}}

	//{{{ Private members
	private Column[] columns;
	//}}}

	//{{{ Inner classes

	//{{{ Entry class
	static class Entry
	{
		VFS.DirectoryEntry dirEntry;
		boolean expanded;
		// how deeply we are nested
		int level;

		Entry(VFS.DirectoryEntry dirEntry, int level)
		{
			this.dirEntry = dirEntry;
			this.level = level;
		}
	} //}}}

	//{{{ Column class
	class Column
	{
		String name;

		Column(String name)
		{
			this.name = name;
		}

		String get(VFS.DirectoryEntry entry)
		{
			return "foo bar";
		}
	} //}}}

	//}}}
}
