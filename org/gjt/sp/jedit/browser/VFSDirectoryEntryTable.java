/*
 * VFSDirectoryEntryTable.java - VFS directory entry table
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.*;
import static java.awt.event.InputEvent.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import org.gjt.sp.jedit.browser.VFSDirectoryEntryTableModel.Entry;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.VFSPathSelected;
import org.gjt.sp.jedit.ActionContext;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ThreadUtilities;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1
 */
public class VFSDirectoryEntryTable extends JTable
{
	//{{{ VFSDirectoryEntryTable constructor
	VFSDirectoryEntryTable(BrowserView browserView)
	{
		super(new VFSDirectoryEntryTableModel());
		this.browserView = browserView;
		setShowGrid(false);

		setIntercellSpacing(new Dimension(0,0));

		setDefaultRenderer(Entry.class,
			renderer = new FileCellRenderer());

		header = getTableHeader();
		header.setReorderingAllowed(true);
		addMouseListener(new MainMouseHandler());
		header.addMouseListener(new MouseHandler());
		header.setDefaultRenderer(new HeaderRenderer(
			(DefaultTableCellRenderer)header.getDefaultRenderer()));

		setRowSelectionAllowed(true);

		getColumnModel().addColumnModelListener(new ColumnHandler());

		setAutoResizeMode(AUTO_RESIZE_OFF);
	} //}}}

	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		/*
		 * Set up column hook in a way that it survives switching of LAF
		 */
		JTableHeader header = new JTableHeader(getColumnModel())
		{
			ColumnDragHook hook;

			@Override
			public void updateUI()
			{
				if (hook != null)
				{
					hook.uninstallHook();
					hook = null;
				}
				super.updateUI();
				hook = new ColumnDragHook(this);
			}

		};
		return header;
	}

	//{{{ selectFile() method
	public boolean selectFile(String path)
	{
		for(int i = 0; i < getRowCount(); i++)
		{
			Entry entry = (Entry) getValueAt(i,1);
			if(entry.dirEntry.getPath().equals(path))
			{
				setSelectedRow(i);
				return true;
			}
		}

		return false;
	} //}}}

	//{{{ doTypeSelect() method
	public void doTypeSelect(String str, boolean dirsOnly)
	{
		if(str.isEmpty())
			clearSelection();
		else if(getSelectedRow() == -1)
			doTypeSelect(str,0,getRowCount(),dirsOnly);
		else
		{
			int start = getSelectionModel().getMaxSelectionIndex();
			boolean retVal = doTypeSelect(str,start,getRowCount(),
				dirsOnly);

			if(!retVal)
			{
				// scan from selection to end failed, so
				// scan from start to selection
				doTypeSelect(str,0,start,dirsOnly);
			}
		}
	} //}}}

	//{{{ getSelectedFiles() method
	public VFSFile[] getSelectedFiles()
	{
		VFSDirectoryEntryTableModel model
			= (VFSDirectoryEntryTableModel)getModel();

		java.util.List<VFSFile> returnValue = new LinkedList<>();
		int[] selectedRows = getSelectedRows();
		for (int selectedRow : selectedRows)
			returnValue.add(model.files[selectedRow].dirEntry);
		return returnValue.toArray(new VFSFile[0]);
	} //}}}

	//{{{ getExpandedDirectories() method
	public void getExpandedDirectories(Set<String> set)
	{
		VFSDirectoryEntryTableModel model
			= (VFSDirectoryEntryTableModel)getModel();

		if(model.files != null)
		{
			for(int i = 0; i < model.files.length; i++)
			{
				if(model.files[i].expanded)
					set.add(model.files[i].dirEntry.getPath());
			}
		}
	} //}}}

	//{{{ toggleExpanded() method
	public void toggleExpanded(final int row)
	{
		VFSDirectoryEntryTableModel model
		= (VFSDirectoryEntryTableModel)getModel();

		Entry entry = model.files[row];
		if(entry.dirEntry.getType() == VFSFile.FILE)
			return;

		if(entry.expanded)
		{
			model.collapse(VFSManager.getVFSForPath(
				entry.dirEntry.getPath()),row);
			resizeColumns();
			ThreadUtilities.runInDispatchThread(() -> setSelectedRow(row));
		}
		else
		{
			browserView.clearExpansionState();
			browserView.loadDirectory(entry,entry.dirEntry.getPath(),
				false, () -> setSelectedRow(row));
		}
	} //}}}

	//{{{ setDirectory() method
	public void setDirectory(VFS vfs, Object node, java.util.List<VFSFile> list,
		Set<String> tmpExpanded)
	{
		timer.stop();
		typeSelectBuffer.setLength(0);

		VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel)getModel();
		int startIndex;
		if(node == null)
		{
			startIndex = 0;
			model.setRoot(vfs,list);
		}
		else
		{
			startIndex =
				model.expand(
				vfs,
				(Entry)node,
				list);
			startIndex++;
		}

		for(int i = 0; i < list.size(); i++)
		{
			Entry e = model.files[startIndex + i];
			String path = e.dirEntry.getPath();
			if(tmpExpanded.contains(path))
			{
				browserView.loadDirectory(e,path,false);
				tmpExpanded.remove(path);
			}
		}

		resizeColumns();
	} //}}}

	//{{{ maybeReloadDirectory() method
	public void maybeReloadDirectory(String path)
	{
		VFSDirectoryEntryTableModel model
		= (VFSDirectoryEntryTableModel)getModel();

		for(int i = 0; i < model.files.length; i++)
		{
			Entry e = model.files[i];
			if(!e.expanded || e.dirEntry.getType() == VFSFile.FILE)
				continue;

			VFSFile dirEntry = e.dirEntry;
			// work around for broken FTP plugin!
			String otherPath;
			if(dirEntry.getSymlinkPath() == null)
				otherPath = dirEntry.getPath();
			else
				otherPath = dirEntry.getSymlinkPath();
			if(MiscUtilities.pathsEqual(path,otherPath))
			{
				browserView.saveExpansionState();
				browserView.loadDirectory(e,path,false);
				return;
			}
		}
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		renderer.propertiesChanged();

		VFSFile template = new VFSFile(
			"foo","foo","foo",VFSFile.FILE,0L,false);
		setRowHeight(renderer.getTableCellRendererComponent(
			this,new Entry(template,0),
			false,false,0,0).getPreferredSize().height);
		Dimension prefSize = getPreferredSize();
		setPreferredScrollableViewportSize(new Dimension(prefSize.width,
			getRowHeight() * 12));
	} //}}}

	//{{{ scrollRectToVisible() method
	@Override
	public void scrollRectToVisible(Rectangle rect)
	{
		// avoid scrolling to the right
		rect.width = 0;
		super.scrollRectToVisible(rect);
	} //}}}

	//{{{ processKeyEvent() method
	@Override
	public void processKeyEvent(KeyEvent evt)
	{
		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			VFSDirectoryEntryTableModel model =
				(VFSDirectoryEntryTableModel)getModel();
			int row = getSelectedRow();
			ActionContext ac = VFSBrowser.getActionContext();
			ActionContext jac = jEdit.getActionContext();
			EditAction browserUp = ac.getAction("vfs.browser.up");			
			VFSBrowser browser = browserView.getBrowser();
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_LEFT:
				evt.consume();
				if ((evt.getModifiersEx() & ALT_DOWN_MASK) == ALT_DOWN_MASK)
				{
					browser.previousDirectory();
				}
				else 
				{
					if(row != -1)
					{
						if(model.files[row].expanded)
						{
							toggleExpanded(row);
							return;
						}

						for(int i = row - 1; i >= 0; i--)
						{
							if(model.files[i].expanded &&
							   model.files[i].level < model.files[row].level)
							{
								setSelectedRow(i);
								return;
							}
						}
					}

					String dir = browserView.getBrowser()
						.getDirectory();
					dir = MiscUtilities.getParentOfPath(dir);
					browserView.getBrowser().setDirectory(dir);
				}
				break;
			case KeyEvent.VK_TAB:
				evt.consume();
				if ((evt.getModifiersEx() & SHIFT_DOWN_MASK) == SHIFT_DOWN_MASK)
				{
					browserView.getParentDirectoryList().requestFocus();
				}
				else
				{
					browser.focusOnDefaultComponent();	
				}
				break;
			case KeyEvent.VK_BACK_SPACE:
				evt.consume();
				ac.invokeAction(evt, browserUp);
				break;
			case KeyEvent.VK_UP:
				if ((evt.getModifiersEx() & ALT_DOWN_MASK) == ALT_DOWN_MASK)
				{
					evt.consume();
					ac.invokeAction(evt, browserUp);
				}
				break;
			case KeyEvent.VK_DELETE:
				evt.consume();
				EditAction deleteAct = ac.getAction("vfs.browser.delete");
				ac.invokeAction(evt, deleteAct);
				break;
			case KeyEvent.VK_N:
				if ((evt.getModifiersEx() & CTRL_DOWN_MASK) == CTRL_DOWN_MASK)
				{
					evt.consume();
					EditAction ea = ac.getAction("vfs.browser.new-file");
					ac.invokeAction(evt, ea);
				}
				break;
			case KeyEvent.VK_INSERT:
				evt.consume();
				EditAction newDir = ac.getAction("vfs.browser.new-directory");
				ac.invokeAction(evt, newDir);
				break;
			case KeyEvent.VK_ESCAPE:
				EditAction cda = ac.getAction("vfs.browser.closedialog");
				ac.invokeAction(evt, cda);
				break;
			case KeyEvent.VK_F2:
				EditAction ren = ac.getAction("vfs.browser.rename");
				evt.consume();
				ac.invokeAction(evt, ren);
				break;
			case KeyEvent.VK_F5:
				evt.consume();
				EditAction reload= ac.getAction("vfs.browser.reload");
				ac.invokeAction(evt, reload);
				break;
			case KeyEvent.VK_F6:
			case KeyEvent.VK_RIGHT:
				evt.consume();
				if ((evt.getModifiersEx() & ALT_DOWN_MASK) == ALT_DOWN_MASK)
				{
					browser.nextDirectory();
				}
				else if(row != -1)
				{
					if(!model.files[row].expanded)
						toggleExpanded(row);
				}
				break;
			case KeyEvent.VK_ENTER:
				evt.consume();
				browserView.getBrowser().filesActivated(
					evt.isShiftDown()
					? VFSBrowser.M_OPEN_NEW_VIEW
					: VFSBrowser.M_OPEN,false);

				break;
			}
		}
		else if(evt.getID() == KeyEvent.KEY_TYPED)
		{

			if(evt.isControlDown() || evt.isAltDown()
				|| evt.isMetaDown())
			{
				evt.consume();
				return;
			}

			// hack...
			if(evt.isShiftDown() && evt.getKeyChar() == '\n')
			{
				evt.consume();
				return;
			}


			VFSBrowser browser = browserView.getBrowser();

			switch(evt.getKeyChar())
			{
			case '~':
				evt.consume();
				if(browser.getMode() == VFSBrowser.BROWSER)
					browser.setDirectory(System.getProperty(
						"user.home"));
				break;
			case '/':
				evt.consume();
				if(browser.getMode() == VFSBrowser.BROWSER)
					browser.rootDirectory();
				break;
			case '-':
				evt.consume();
				if(browser.getMode() == VFSBrowser.BROWSER)
				{
					browser.setDirectory(
						browser.getView().getBuffer()
						.getDirectory());
				}
				break;
			default:
				evt.consume();
				typeSelectBuffer.append(evt.getKeyChar());
				doTypeSelect(typeSelectBuffer.toString(),
					browser.getMode() == VFSBrowser
					.CHOOSE_DIRECTORY_DIALOG);

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

	//{{{ setSelectedRow() method
	public void setSelectedRow(int row)
	{
		getSelectionModel().setSelectionInterval(row,row);
		scrollRectToVisible(getCellRect(row,0,true));
	} //}}}

	//{{{ Private members
	private final BrowserView browserView;
	private final JTableHeader header;
	private final FileCellRenderer renderer;
	private final StringBuffer typeSelectBuffer = new StringBuffer();
	private final Timer timer = new Timer(0, e -> typeSelectBuffer.setLength(0));
	private boolean resizingColumns;

	//{{{ doTypeSelect() method
	private boolean doTypeSelect(String str, int start, int end,
		boolean dirsOnly)
	{
		VFSFile[] files = ((VFSDirectoryEntryTableModel)
			getModel()).getFiles();

		int index = VFSFile.findCompletion(files,start,end,str,dirsOnly);
		if(index != -1)
		{
			setSelectedRow(index);
			return true;
		}
		else
			return false;
	} //}}}

	//{{{ resizeColumns() method
	private void resizeColumns()
	{
		VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel)getModel();

		FontRenderContext fontRenderContext = new FontRenderContext(null,true,false);
		int[] widths = new int[model.getColumnCount()];
		for(int i = 0; i < widths.length; i++)
		{
			String columnName = model.getColumnName(i);
			if(columnName != null)
			{
				widths[i] = (int)renderer.plainFont
					.getStringBounds(columnName,
					fontRenderContext).getWidth();
			}
		}

		for(int i = 1; i < widths.length; i++)
		{
			//String extAttr = model.getExtendedAttribute(i);
			widths[i] = Math.max(widths[i],model.getColumnWidth(i));
		}

		for(int i = 0; i < model.files.length; i++)
		{
			Entry entry = model.files[i];
			Font font = entry.dirEntry.getType()
				== VFSFile.FILE
				? renderer.plainFont : renderer.boldFont;

			widths[0] = Math.max(widths[0],renderer.getEntryWidth(
				entry,font,fontRenderContext));
		}

		widths[0] += 10;

		TableColumnModel columns = getColumnModel();

		try
		{
			resizingColumns = true;
			for(int i = 0; i < widths.length; i++)
			{
				columns.getColumn(i).setPreferredWidth(widths[i]);
				columns.getColumn(i).setWidth(widths[i]);
			}
		}
		finally
		{
			resizingColumns = false;
		}

		doLayout();
	} //}}}

	//{{{ saveWidths() method
	private void saveWidths()
	{
		if(resizingColumns)
			return;

		VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel)getModel();
		TableColumnModel columns = getColumnModel();

		for(int i = 1; i < model.getColumnCount(); i++)
			model.saveColumnWidth(i,columns.getColumn(i).getWidth());
	} //}}}

	//}}}

	//{{{ ColumnHandler class
	private class ColumnHandler implements TableColumnModelListener
	{
		@Override
		public void columnAdded(TableColumnModelEvent e) {}
		@Override
		public void columnRemoved(TableColumnModelEvent e) {}
		@Override
		public void columnMoved(TableColumnModelEvent e) {
			((VFSDirectoryEntryTableModel)getModel()).columnMoved(e.getFromIndex(), e.getToIndex()); // view indexes
		}
		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {}

		@Override
		public void columnMarginChanged(ChangeEvent e)
		{
			saveWidths();
		}
	} //}}}

	//{{{ class MainMouseHandler
	private class MainMouseHandler extends MouseInputAdapter
	{
		@Override
		public void mouseClicked(MouseEvent e)
		{
			super.mouseClicked(e);
			if (browserView.getBrowser().getMode() != VFSBrowser.BROWSER) return;
			int ind = getSelectionModel().getMinSelectionIndex();
			if (ind == -1)
				return;
			Entry node = (Entry) getModel().getValueAt(ind, 0);
			boolean isDir = node.dirEntry.getType() == VFSFile.DIRECTORY;
			EditBus.send(new VFSPathSelected(browserView.getBrowser().getView(),
							 node.dirEntry.getPath(), isDir));
		}
	} //}}}

	//{{{ MouseHandler class
	private class MouseHandler extends MouseInputAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt)
		{
			// double click on columns header
			if (evt.getSource() == header && evt.getClickCount() == 2)
			{
				VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel) header.getTable().getModel();
				TableColumnModel columnModel = header.getColumnModel();
				int viewColumnIndex = columnModel.getColumnIndexAtX(evt.getX());

				// View index must be used here instead of model index because model order is rearranged by custom code
				// on column move according to view order so that both are equal, but no "structurechanged" is triggered,
				// so view order is the same as model order (maintained by custom code, while JTable's automatic
				// indexing is wrong here) before sortByColumn call finishes, where sortColumnIndex is saved
				// and structureChanged is triggered.
				//
				// If use modelIndex, the bug will arise (sort by column, move that column, sort again, result: sorting
				// mark is at wrong column)
				int modelColumnIndex = viewColumnIndex;
				saveWidths();
				if(model.sortByColumn(modelColumnIndex))
				{
					resizeColumns();
					Log.log(Log.DEBUG,this,"VFSDirectoryEntryTable sorted by "
					+ model.getColumnName(modelColumnIndex)
					+ (model.getAscending() ? " ascending" : " descending") );
				}
			}
		}
	} //}}}

	//{{{ HeaderRenderer
	private static class HeaderRenderer extends DefaultTableCellRenderer
	{
		private final DefaultTableCellRenderer tcr;

		HeaderRenderer(DefaultTableCellRenderer tcr)
		{
			this.tcr = tcr;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel l = (JLabel)tcr.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel)table.getModel();
			Icon icon = column == model.getSortColumnIndex()
				? model.getAscending() ? ASC_ICON : DESC_ICON
				: null;
			l.setIcon(icon);
			// l.setHorizontalTextPosition(l.LEADING);
			return l;
		}
	} //}}}

	//{{{ SortOrder Icons

	static final Icon ASC_ICON  = GUIUtilities.loadIcon("arrow-asc.png");
	static final Icon DESC_ICON = GUIUtilities.loadIcon("arrow-desc.png");

	//}}}


	/**
	 * Original code:
	 *   https://stackoverflow.com/questions/1155137/how-to-keep-a-single-column-from-being-reordered-in-a-jtable/14480948
	 *
	 * A delegating MouseInputListener to be installed instead of
	 * the one registered by the ui-delegate.
	 *
	 * It's implemented to prevent dragging the first column or any other
	 * column over the first.
	 *
	 */
	private static class ColumnDragHook implements MouseInputListener
	{
		private final JTableHeader header;
		private MouseListener mouseDelegate;
		private MouseMotionListener mouseMotionDelegate;
		private int minMouseX;

		ColumnDragHook(JTableHeader header)
		{
			this.header = header;
			installHook();
		}

		/**
		 * Implemented to do some tweaks/bookkeeping before/after
		 * passing the event to the original
		 *
		 * - temporarily disallow reordering if hit on first column
		 * - calculate the max mouseX that's allowable in dragging to the left
		 *
		 */
		@Override
		public void mousePressed(MouseEvent e)
		{
			int index = header.columnAtPoint(e.getPoint());

			boolean reorderingAllowed = header.getReorderingAllowed();

			if (index == 0)
			{
				// temporarily disable re-ordering
				header.setReorderingAllowed(false);
			}
			mouseDelegate.mousePressed(e);
			if (index == 0)
			{
				// re-enable re-ordering
				header.setReorderingAllowed(reorderingAllowed);
			}

			// Calculate minimum X for a column (all except the first one) when dragging
			if (header.getDraggedColumn() != null)
			{
				int draggedColumnX = header.getHeaderRect(index).x;
				int firstColumnWidth = header.getColumnModel().getColumn(0).getWidth();
				minMouseX = firstColumnWidth + (e.getX() - draggedColumnX)- 1;
			}
		}

		/**
		 * Implemented to pass the event to the original only if the
		 * mouseX doesn't lead to dragging the column over the first.
		 */
		@Override
		public void mouseDragged(MouseEvent e)
		{
			TableColumn dragged = header.getDraggedColumn();

			if (dragged != null)
			{
				int index = header.getTable().convertColumnIndexToView(dragged.getModelIndex());

				if (index == 1)
				{
					// dragged column is at second position...
					if (e.getX() < minMouseX) return; // allow only drags to the right
				}
			}
			mouseMotionDelegate.mouseDragged(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			mouseDelegate.mouseReleased(e);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			mouseDelegate.mouseClicked(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			mouseDelegate.mouseEntered(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			mouseDelegate.mouseExited(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			mouseMotionDelegate.mouseMoved(e);
		}


		protected void installHook()
		{
			installMouseHook();
			installMouseMotionHook();
		}

		protected void installMouseMotionHook()
		{
			MouseMotionListener[] listeners = header.getMouseMotionListeners();
			for (int i = 0; i < listeners.length; i++) {
				MouseMotionListener l = listeners[i];
				if (l.getClass().getName().contains("TableHeaderUI"))
				{
					mouseMotionDelegate = l;
					listeners[i] = this;
				}
				header.removeMouseMotionListener(l);
			}
			Arrays.stream(listeners).forEach(header::addMouseMotionListener);
		}

		protected void installMouseHook()
		{
			MouseListener[] listeners = header.getMouseListeners();
			for (int i = 0; i < listeners.length; i++)
			{
				MouseListener l = listeners[i];
				if (l.getClass().getName().contains("TableHeaderUI"))
				{
					mouseDelegate = l;
					listeners[i] = this;
				}
				header.removeMouseListener(l);
			}
			Arrays.stream(listeners).forEach(header::addMouseListener);
		}

		public void uninstallHook()
		{
			uninstallMouseHook();
			uninstallMouseMotionHook();
		}

		protected void uninstallMouseMotionHook()
		{
			MouseMotionListener[] listeners = header.getMouseMotionListeners();
			for (int i = 0; i < listeners.length; i++)
			{
				MouseMotionListener l = listeners[i];
				if (l == this)
					listeners[i] = mouseMotionDelegate;
				header.removeMouseMotionListener(l);
			}
			Arrays.stream(listeners).forEach(header::addMouseMotionListener);
		}

		protected void uninstallMouseHook()
		{
			MouseListener[] listeners = header.getMouseListeners();
			for (int i = 0; i < listeners.length; i++)
			{
				MouseListener l = listeners[i];
				if (l == this)
					listeners[i] = mouseDelegate;
				header.removeMouseListener(l);
			}
			Arrays.stream(listeners).forEach(header::addMouseListener);
		}
	}
}
