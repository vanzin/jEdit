/*
 * FilteredTableModel.java - A Filtered table model decorator
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Shlomy Reinstein
 * Copyright (C) 2007 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.gjt.sp.jedit.gui;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.Vector;

/**
 * @author Shlomy Reinstein
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre11
 */
public abstract class FilteredTableModel<E extends TableModel> extends AbstractTableModel implements TableModelListener
{
	/** The delegated table model. */
	protected E delegated;

	private Vector<Integer> filteredIndices;

	private String filter;

	//{{{ FilteredTableModel() constructor
	protected FilteredTableModel(E delegated)
	{
		this.delegated = delegated;
		delegated.addTableModelListener(this);
		resetFilter();
	} //}}}

	//{{{ getDelegated() method
	public E getDelegated()
	{
		return delegated;
	} //}}}

	//{{{ setDelegated() method
	public void setDelegated(E delegated)
	{
		this.delegated.removeTableModelListener(this);
		delegated.addTableModelListener(this);
		this.delegated = delegated;
	} //}}}

	//{{{ resetFilter() method
	private void resetFilter()
	{
		int size = delegated.getRowCount();
		filteredIndices = new Vector<Integer>(size);
		for (int i = 0; i < size; i++)
			filteredIndices.add(Integer.valueOf(i));
	} //}}}

	//{{{ setFilter() method
	public void setFilter(String filter)
	{
		this.filter = filter;
		if (filter != null && filter.length() > 0)
		{
			int size = delegated.getRowCount();
			filter = prepareFilter(filter);
			Vector<Integer> indices = new Vector<Integer>(size);
			for (int i = 0; i < size; i++)
			{
				if (passFilter(i, filter))
					indices.add(Integer.valueOf(i));
			}
			filteredIndices = indices;
		}
		else
			resetFilter();
		fireTableDataChanged();
	} //}}}

	//{{{ prepareFilter() method
	public String prepareFilter(String filter)
	{
		return filter;
	} //}}}

	//{{{ passFilter() method
	/**
	 * This callback indicates if a row passes the filter.
	 *
	 * @param row the row number the delegate row count
	 * @param filter the filter string
	 * @return true if the row must be visible
	 */
	public abstract boolean passFilter(int row, String filter);
	//}}}


	//{{{ getRowCount() method
	public int getRowCount()
	{
		return filteredIndices.size();
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return delegated.getColumnCount();
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int columnIndex)
	{
		return delegated.getColumnName(columnIndex);
	} //}}}

	//{{{ getColumnClass() method
	public Class<?> getColumnClass(int columnIndex)
	{
		return delegated.getColumnClass(columnIndex);
	} //}}}

	//{{{ isCellEditable() method
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		int trueRowIndex = getTrueRow(rowIndex);
		return delegated.isCellEditable(trueRowIndex, columnIndex);
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		int trueRowIndex = getTrueRow(rowIndex);
		return delegated.getValueAt(trueRowIndex, columnIndex);
	} //}}}

	//{{{ setValueAt() method
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		int trueRowIndex = getTrueRow(rowIndex);
		delegated.setValueAt(aValue, trueRowIndex, columnIndex);
	} //}}}

	//{{{ getTrueRow() method
	public int getTrueRow(int rowIndex)
	{
		return filteredIndices.get(rowIndex).intValue();
	} //}}}

	/**
	 * This fine grain notification tells listeners the exact range
	 * of cells, rows, or columns that changed.
	 */
	public void tableChanged(TableModelEvent e)
	{
		setFilter(filter);
	}
}
