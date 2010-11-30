/*
 * FilteredTableModel.java - A Filtered table model decorator
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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
import javax.swing.event.*;
import java.util.*;

/**
 * This ListModel delegates another model to add some filtering features to any
 * JList.
 * To use it you must implement the abstract method passFilter().
 * This method is called for each row, and must return true if the row should be
 * visible, and false otherwise.
 * It is also possible to override the method prepareFilter() that allow you to
 * transform the filter String. Usually you can return it as lowercase
 * It is not mandatory but highly recommended to give the JList instance to the
 * model in order to keep the selection after the filter has been updated
 *
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre11
 */
public abstract class FilteredListModel<E extends ListModel> extends AbstractListModel implements ListDataListener
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

	private JList list;

	//{{{ FilteredTableModel() constructor
	protected FilteredListModel(E delegated)
	{
		this.delegated = delegated;
		delegated.addListDataListener(this);
		resetFilter();
	} //}}}

	//{{{ setList() method
	/**
	 * Set the JList that uses this model.
	 * It is used to restore the selection after the filter has been applied
	 * If it is null,
	 *
	 * @param list the list that uses the model
	 */
	public void setList(JList list)
	{
		if (list.getModel() != this)
			throw new IllegalArgumentException("The given list " + list + " doesn't use this model " + this);
		this.list = list;
	} //}}}

	//{{{ getDelegated() method
	public E getDelegated()
	{
		return delegated;
	} //}}}

	//{{{ setDelegated() method
	public void setDelegated(E delegated)
	{
		this.delegated.removeListDataListener(this);
		delegated.addListDataListener(this);
		this.delegated = delegated;
	} //}}}

	//{{{ resetFilter() method
	private void resetFilter()
	{
		filteredIndices = null;
	} //}}}

	//{{{ setFilter() method
	public void setFilter(final String filter)
	{
		Runnable runner = new Runnable()
		{
			public void run()
			{
				Set<Integer> selectedIndices = saveSelection();
				list.clearSelection();
				FilteredListModel.this.filter = filter;
				if (filter != null && filter.length() > 0)
				{
					int size = delegated.getSize();
					String prepped_filter = prepareFilter(filter);
					Vector<Integer> indices = new Vector<Integer>(size);
					Map<Integer, Integer> invertedIndices = new HashMap<Integer, Integer>();
					for (int i = 0; i < size; i++)
					{
						if (passFilter(i, prepped_filter))
						{
							Integer delegatedIndice = Integer.valueOf(i);
							indices.add(delegatedIndice);

							invertedIndices.put(delegatedIndice, indices.size() - 1);
						}
					}
					FilteredListModel.this.invertedIndices = invertedIndices;
					filteredIndices = indices;
				}
				else
					resetFilter();

				fireContentsChanged(this, 0, getSize() - 1);
				restoreSelection(selectedIndices);
			}
		};
		SwingUtilities.invokeLater(runner);
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
	protected Set<Integer> saveSelection()
	{
		if (list == null)
			return null;
		int[] rows = list.getSelectedIndices();
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
	protected void restoreSelection(Set<Integer> selectedIndices)
	{
		if (selectedIndices == null || getSize() == 0)
			return;

		// To correctly handle "single interval" selection mode,
		// each interval has to be selected using a single call to
		// setSelectionInterval; calling setSelectionInterval on
		// each item cancels the previous selection.
		// Sort the list of selected indices to simplify interval
		// identification.
		Vector<Integer> sel = new Vector<Integer>(selectedIndices);
		Collections.sort(sel);
		int from = -1;
		int to = -1;
		for (Integer selectedIndex : sel)
		{
			int i = getInternal2ExternalRow(selectedIndex.intValue());
			if (i != -1)
			{
				if (from == -1)
					from = to = i;
				else if (i == to + 1)
					to = i;
				else
				{
					list.setSelectionInterval(from, to);
					from = to = i;
				}
			}
		}
		if (from != -1)
			list.setSelectionInterval(from, to);
	}  //}}}

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

	//{{{ getElementAt() method
	public Object getElementAt(int index)
	{
		int trueRowIndex = getTrueRow(index);
		return delegated.getElementAt(trueRowIndex);
	} //}}}

	//{{{ getSize() method
	public int getSize()
	{
		if (filteredIndices == null)
			return delegated.getSize();
		return filteredIndices.size();
	} //}}}

	//{{{ contentsChanged() method
	public void contentsChanged(ListDataEvent e)
	{
		setFilter(filter);
	} //}}}

	//{{{ intervalAdded() method
	public void intervalAdded(ListDataEvent e)
	{
		setFilter(filter);
	} //}}}

	//{{{ intervalRemoved() method
	public void intervalRemoved(ListDataEvent e)
	{
		setFilter(filter);
	} //}}}
}
