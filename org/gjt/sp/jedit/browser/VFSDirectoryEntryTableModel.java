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
import java.util.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1
 */
public class VFSDirectoryEntryTableModel extends AbstractTableModel
{
	//{{{ VFSDirectoryEntryTableModel constructor
	public VFSDirectoryEntryTableModel()
	{
		extAttrs = new ArrayList();
	} //}}}

	//{{{ setRoot() method
	public void setRoot(VFS vfs, ArrayList list)
	{
		extAttrs.clear();
		addExtendedAttributes(vfs);

		/* if(files != null && files.length != 0)
			fireTableRowsDeleted(0,files.length - 1); */

		files = new Entry[list.size()];
		for(int i = 0; i < files.length; i++)
		{
			files[i] = new Entry((VFS.DirectoryEntry)list.get(i),0);
		}

		/* if(files.length != 0)
			fireTableRowsInserted(0,files.length - 1); */

		fireTableStructureChanged();
	} //}}}

	//{{{ expand() method
	public int expand(VFS vfs, Entry entry, ArrayList list)
	{
		int startIndex = -1;
		for(int i = 0; i < files.length; i++)
		{
			if(files[i] == entry)
				startIndex = i;
		}

		collapse(vfs,startIndex);

		addExtendedAttributes(vfs);
		entry.expanded = true;

		if(list != null)
		{
			Entry[] newFiles = new Entry[files.length + list.size()];
			System.arraycopy(files,0,newFiles,0,startIndex + 1);
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

			/* fireTableRowsInserted(startIndex + 1,
				startIndex + list.size() + 1); */
		}

		/* fireTableRowsUpdated(startIndex,startIndex); */

		fireTableStructureChanged();

		return startIndex;
	} //}}}

	//{{{ collapse() method
	public void collapse(VFS vfs, int index)
	{
		Entry entry = files[index];
		if(!entry.expanded)
			return;

		entry.expanded = false;

		int lastIndex = index + 1;
		while(lastIndex < files.length)
		{
			Entry e = files[lastIndex];

			if(e.expanded)
			{
				removeExtendedAttributes(VFSManager.getVFSForPath(
					e.dirEntry.path));
			}

			if(e.level <= entry.level)
				break;
			else
				lastIndex++;
		}

		removeExtendedAttributes(vfs);

		Entry[] newFiles = new Entry[files.length - lastIndex + index + 1];
		System.arraycopy(files,0,newFiles,0,index + 1);
		System.arraycopy(files,lastIndex,newFiles,index + 1,
			files.length - lastIndex);

		files = newFiles;

		/* fireTableRowsUpdated(index,index);
		fireTableRowsDeleted(index + 1,lastIndex); */

		fireTableStructureChanged();
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 1 + extAttrs.size();
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
		if(col == 0)
			return jEdit.getProperty("vfs.browser.name");
		else
			return jEdit.getProperty("vfs.browser." + getExtendedAttribute(col - 1));
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int col)
	{
		return Entry.class;
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int row, int col)
	{
		if(files == null)
			return null;
		else
			return files[row];
	} //}}}

	//{{{ getExtendedAttribute() method
	public String getExtendedAttribute(int index)
	{
		return ((ExtendedAttribute)extAttrs.get(index)).name;
	} //}}}

	//{{{ Package-private members
	Entry[] files;
	//}}}

	//{{{ Private members
	private List extAttrs;

	//{{{ addExtendedAttributes() method
	private void addExtendedAttributes(VFS vfs)
	{
		System.err.println("adding " + vfs);
		String[] attrs = vfs.getExtendedAttributes();
vfs_attr_loop:	for(int i = 0; i < attrs.length; i++)
		{
			Iterator iter = extAttrs.iterator();
			while(iter.hasNext())
			{
				ExtendedAttribute attr = (ExtendedAttribute)
					iter.next();
				if(attrs[i].equals(attr.name))
				{
					attr.ref++;
					continue vfs_attr_loop;
				}
			}

			// this vfs has an extended attribute which is not
			// in the list. add it to the end with a ref count
			// of 1
			extAttrs.add(new ExtendedAttribute(attrs[i]));
		}
	} //}}}

	//{{{ removeExtendedAttributes() method
	private void removeExtendedAttributes(VFS vfs)
	{
		System.err.println("removing " + vfs);
		String[] attrs = vfs.getExtendedAttributes();
vfs_attr_loop:	for(int i = 0; i < attrs.length; i++)
		{
			Iterator iter = extAttrs.iterator();
			while(iter.hasNext())
			{
				ExtendedAttribute attr = (ExtendedAttribute)
					iter.next();
				if(attrs[i].equals(attr.name))
				{
					if(--attr.ref == 0)
					{
						// we no longer have any
						// dirs using this extended
						// attribute
						iter.remove();
					}

					continue vfs_attr_loop;
				}
			}

			// this vfs has an extended attribute which is not
			// in the list ???
			Log.log(Log.WARNING,this,"We forgot about " + attrs[i]);
		}
	} //}}}

	//}}}

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

	//{{{ ExtendedAttribute class
	static class ExtendedAttribute
	{
		/* reference counter allows us to remove a column from
		 * the table when no directory using this column is
		 * visible */
		int ref;

		String name;

		ExtendedAttribute(String name)
		{
			this.name = name;
			ref = 1;
		}
	} //}}}
}
