/*
 * VFSDirectoryEntryTable.java - VFS directory entry table
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

//{{{ Imports
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.MiscUtilities;
//}}}

public class VFSDirectoryEntryTable extends JTable
{
	//{{{ VFSDirectoryEntryTable constructor
	public VFSDirectoryEntryTable(BrowserView browserView)
	{
		super(new VFSDirectoryEntryTableModel());
		this.browserView = browserView;
		setShowGrid(false);

		TableColumn col1 = getColumnModel().getColumn(0);
		col1.setPreferredWidth(20);

		setDefaultRenderer(VFSDirectoryEntryTableModel.Entry.class,
			renderer = new VFSDirectoryEntryCellRenderer());

		JTableHeader header = getTableHeader();
		header.setReorderingAllowed(false);

		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(true);
		setCellSelectionEnabled(false);
	} //}}}

	//{{{ doTypeSelect() method
	public void doTypeSelect(String str, boolean ignoreCase)
	{
		if(str.length() == 0)
			clearSelection();
		else if(getSelectedRow() == -1)
			doTypeSelect(str,0,getRowCount(),ignoreCase);
		else
		{
			int start = getSelectionModel().getMaxSelectionIndex();
			boolean retVal = doTypeSelect(str,start,getRowCount(),
				ignoreCase);

			if(!retVal)
			{
				// scan from selection to end failed, so
				// scan from start to selection
				doTypeSelect(str,0,start,ignoreCase);
			}
		}
	} //}}}

	//{{{ getSelectedFiles() method
	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		VFSDirectoryEntryTableModel model
		= (VFSDirectoryEntryTableModel)getModel();

		LinkedList returnValue = new LinkedList();
		int[] selectedRows = getSelectedRows();
		for(int i = 0; i < selectedRows.length; i++)
		{
			returnValue.add(model.files[selectedRows[i]].dirEntry);
		}
		return (VFS.DirectoryEntry[])returnValue.toArray(new
		VFS.DirectoryEntry[returnValue.size()]);
	} //}}}

	//{{{ getExpandedDirectories() method
	public Set getExpandedDirectories()
	{
		VFSDirectoryEntryTableModel model
		= (VFSDirectoryEntryTableModel)getModel();

		HashSet returnValue = new HashSet();
		for(int i = 0; i < model.files.length; i++)
		{
			if(model.files[i].expanded)
				returnValue.add(model.files[i].dirEntry.path);
		}
		return returnValue;
	} //}}}

	//{{{ setDirectory() method
	public void setDirectory(Object node, ArrayList list)
	{
		timer.stop();
		typeSelectBuffer.setLength(0);

		if(node == null)
			((VFSDirectoryEntryTableModel)getModel()).setRoot(list);
		else
		{
			((VFSDirectoryEntryTableModel)getModel()).expand(
				(VFSDirectoryEntryTableModel.Entry)node,
				list);
		}
	} //}}}

	//{{{ maybeReloadDirectory() method
	public void maybeReloadDirectory(String path)
	{
		VFSDirectoryEntryTableModel model
		= (VFSDirectoryEntryTableModel)getModel();

		for(int i = 0; i < model.files.length; i++)
		{
			VFSDirectoryEntryTableModel.Entry e = model.files[i];
			if(!e.expanded || e.dirEntry.type == VFS.DirectoryEntry.FILE)
				continue;

			if(path.equals(e.dirEntry.path))
				browserView.loadDirectory(e,path);
		}
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		renderer.propertiesChanged();

		VFS.DirectoryEntry template = new VFS.DirectoryEntry(
			"foo","foo","foo",VFS.DirectoryEntry.FILE,0L,false);
		setRowHeight(renderer.getTableCellRendererComponent(
			this,new VFSDirectoryEntryTableModel.Entry(template,0),
			false,false,0,0).getSize().height);
	} //}}}

	//{{{ processKeyEvent() method
	public void processKeyEvent(KeyEvent evt)
	{
		if(evt.getID() == KeyEvent.KEY_TYPED)
		{
			if(evt.isControlDown() || evt.isAltDown()
				|| evt.isMetaDown())
			{
				return;
			}

			// hack...
			if(evt.isShiftDown() && evt.getKeyChar() == '\n')
				return;

			switch(evt.getKeyChar())
			{
			case '~':
			case '/':
			case '-':
				break;
			default:
				typeSelectBuffer.append(evt.getKeyChar());
				doTypeSelect(typeSelectBuffer.toString(),true);

				timer.stop();
				timer.setInitialDelay(750);
				timer.setRepeats(false);
				timer.start();
				return;
			}
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	} //}}}

	//{{{ Private members
	private BrowserView browserView;
	private VFSDirectoryEntryCellRenderer renderer;
	private StringBuffer typeSelectBuffer = new StringBuffer();
	private Timer timer = new Timer(0,new ClearTypeSelect());

	//{{{ doTypeSelect() method
	private boolean doTypeSelect(String str, int start, int end,
		boolean ignoreCase)
	{
		for(int i = start; i < end; i++)
		{
			VFSDirectoryEntryTableModel.Entry entry =
				(VFSDirectoryEntryTableModel.Entry)getValueAt(i,1);
			String matchAgainst = (MiscUtilities.isAbsolutePath(str)
				? entry.dirEntry.path : entry.dirEntry.name);
			if(matchAgainst.regionMatches(ignoreCase,
				0,str,0,str.length()))
			{
				setRowSelectionInterval(i,i);
				//scrollRowToVisible(i);
				return true;
			}
		}

		return false;
	} //}}}

	//}}}

	//{{{ ClearTypeSelect
	class ClearTypeSelect implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			typeSelectBuffer.setLength(0);
		}
	} //}}}
}
