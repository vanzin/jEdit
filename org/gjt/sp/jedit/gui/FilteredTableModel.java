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

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * This TableModel delegates another model to add some filtering features to any
 * JTable.
 * To use it you must implement the abstract method passFilter().
 * This method is called for each row, and must return true if the row should be
 * visible, and false otherwise.
 * It is also possible to override the method prepareFilter() that allow you to
 * transform the filter String. Usually you can return it as lowercase
 * <p/>
 * Here is an example of how to use it extracted from the InstallPanel
 * <code>
 * PluginTableModel tableModel = new PluginTableModel();
 * filteredTableModel = new FilteredTableModel<PluginTableModel>(tableModel)
 * {
 * public String prepareFilter(String filter)
 * {
 * return filter.toLowerCase();
 * }
 * <p/>
 * public boolean passFilter(int row, String filter)
 * {
 * String pluginName = (String) delegated.getValueAt(row, 1);
 * return pluginName.toLowerCase().contains(filter);
 * }
 * };
 * table = new JTable(filteredTableModel);
 * filteredTableModel.setTable(table);
 * </code>
 * It is not mandatory but highly recommended to give the JTable instance to the
 * model in order to keep the selection after the filter has been updated
 *
 * @author Shlomy Reinstein
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre11
 */
public abstract class FilteredTableModel<E extends TableModel> extends AbstractTableModel implements TableModelListener
{
	/**
	 * The delegated table model.
	 */
	protected E delegated;

	private Vector<Integer> filteredIndices;

	/**
	 * This map contains the delegated indices as key and true indices as values.
	 */
	private Map<Integer, Integer> invertedIndices;

	private String filter;

	private JTable table;

	//{{{ FilteredTableModel() constructors
	protected FilteredTableModel(E delegated)
	{
		this.delegated = delegated;
		delegated.addTableModelListener(this);
		resetFilter();
	}

	protected FilteredTableModel()
	{
	} //}}}

	//{{{ setTable() method
	/**
	 * Set the JTable that uses this model.
	 * It is used to restore the selection after the filter has been applied
	 * If it is null,
	 *
	 * @param table the table that uses the model
	 */
	public void setTable(JTable table)
	{
		if (table.getModel() != this)
			throw new IllegalArgumentException("The given table " + table + " doesn't use this model " + this);
		this.table = table;
	} //}}}


	//{{{ getDelegated() method
	public E getDelegated()
	{
		return delegated;
	} //}}}

	//{{{ setDelegated() method
	public void setDelegated(E delegated)
	{
		if (this.delegated != null)
			this.delegated.removeTableModelListener(this);
		delegated.addTableModelListener(this);
		this.delegated = delegated;
	} //}}}

	//{{{ resetFilter() method
	private void resetFilter()
	{
		filteredIndices = null;
	} //}}}

	//{{{ setFilter() method
	public void setFilter(String filter)
	{
		Set<Integer> selectedIndices = saveSelection();
		this.filter = filter;
		if (filter != null && filter.length() > 0)
		{
			int size = delegated.getRowCount();
			filter = prepareFilter(filter);
			Vector<Integer> indices = new Vector<Integer>(size);
			Map<Integer, Integer> invertedIndices = new HashMap<Integer, Integer>();
			for (int i = 0; i < size; i++)
			{
				if (passFilter(i, filter))
				{
					Integer delegatedIndice = Integer.valueOf(i);
					indices.add(delegatedIndice);

					invertedIndices.put(delegatedIndice, indices.size() - 1);
				}
			}
			this.invertedIndices = invertedIndices;
			filteredIndices = indices;
		}
		else
			resetFilter();

		fireTableDataChanged();
		restoreSelection(selectedIndices);
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
	 * @param row    the row number the delegate row count
	 * @param filter the filter string
	 * @return true if the row must be visible
	 */
	public abstract boolean passFilter(int row, String filter);
	//}}}

	//{{{ saveSelection()

	private Set<Integer> saveSelection()
	{
		if (table == null)
			return null;
		int[] rows = table.getSelectedRows();
		if (rows.length == 0)
			return null;

		Set<Integer> selectedRows = new HashSet<Integer>(rows.length);
		for (int row : rows)
		{
			selectedRows.add(getTrueRow(row));
		}
		return selectedRows;
	} //}}}

	//{{{ restoreSelection() method
	private void restoreSelection(Set<Integer> selectedIndices)
	{
		if (selectedIndices == null || getRowCount() == 0)
			return; 
		
		for (Integer selectedIndex : selectedIndices)
		{
			int i = getInternal2ExternalRow(selectedIndex.intValue());
			if (i != -1)
				table.getSelectionModel().setSelectionInterval(i, i);
		}
	}  //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		if (filteredIndices == null)
			return delegated.getRowCount();
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
	/**
	 * Converts a row index from the JTable to an internal row index from the delegated model.
	 *
	 * @param rowIndex the row index
	 * @return the row index in the delegated model
	 */
	public int getTrueRow(int rowIndex)
	{
		if (filteredIndices == null)
			return rowIndex;
		return filteredIndices.get(rowIndex).intValue();
	} //}}}

	//{{{ getInternal2ExternalRow() method
	/**
	 * Converts a row index from the delegated table model into a row index of the JTable.
	 *
	 * @param internalRowIndex the internal row index
	 * @return the table row index or -1 if this row is not visible
	 */
	public int getInternal2ExternalRow(int internalRowIndex)
	{
		if (invertedIndices == null)
			return internalRowIndex;

		Integer externalRowIndex = invertedIndices.get(internalRowIndex);
		if (externalRowIndex == null)
			return -1;

		return externalRowIndex.intValue();
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
