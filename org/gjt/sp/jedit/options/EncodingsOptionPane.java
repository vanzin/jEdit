/*
 * EncodingsOptionPane.java - Encodings options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Björn Kautler
 * Portions copyright (C) 2007 Matthieu Casanova
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

package org.gjt.sp.jedit.options;

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
//}}}

//{{{ EncodingsOptionPane class
/**
 * Encodings editor.
 * @author Björn Kautler
 * @author Matthieu Casanova
 * @since jEdit 4.3pre6
 * @version $Id$
 */
public class EncodingsOptionPane extends AbstractOptionPane
{
	private JTable table;

	private int selectedCount;

	//{{{ EncodingsOptionPane constructor
	public EncodingsOptionPane()
	{
		super("encodings");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());

		add(new JLabel(jEdit.getProperty("options.encodings.selectEncodings")),BorderLayout.NORTH);

		String[] encodings = MiscUtilities.getEncodings(false);
		Arrays.sort(encodings,new MiscUtilities.StringICaseCompare());
		this.encodings = new EncodingTableModel(encodings);

		table = new JTable(this.encodings);

		TableColumn col1 = table.getColumnModel().getColumn(0);
		col1.setPreferredWidth(30);
		col1.setMinWidth(30);
		col1.setMaxWidth(30);
		col1.setResizable(false);

		JScrollPane encodingsScrollPane = new JScrollPane(table);
		Dimension d = table.getPreferredSize();
		d.height = Math.min(d.height,200);
		encodingsScrollPane.setPreferredSize(d);
		add(encodingsScrollPane,BorderLayout.CENTER);

		ActionHandler actionHandler = new ActionHandler();
		Box buttonsBox = Box.createHorizontalBox();
		selectAllButton = new JButton(jEdit.getProperty("options.encodings.selectAll"));
		selectAllButton.addActionListener(actionHandler);
		buttonsBox.add(selectAllButton);
		selectNoneButton = new JButton(jEdit.getProperty("options.encodings.selectNone"));
		selectNoneButton.addActionListener(actionHandler);
		buttonsBox.add(selectNoneButton);
		add(buttonsBox,BorderLayout.SOUTH);
		updateButton();
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		for (int i=0, c=encodings.getRowCount(); i<c ; i++)
		{
			boolean encodingValue = ((Boolean) encodings.getValueAt(i, 0)).booleanValue();
			String encoding = (String) encodings.getValueAt(i, 1);
			if (encodingValue)
			{
				jEdit.unsetProperty("encoding.opt-out."+encoding);
			}
			else
			{
				jEdit.setBooleanProperty("encoding.opt-out."+encoding,true);
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JButton selectAllButton;
	private JButton selectNoneButton;
	private EncodingTableModel encodings;
	//}}}

	//{{{ updateButton() method
	private void updateButton()
	{
		selectAllButton.setEnabled(selectedCount != encodings.encodingsSelected.length);
		selectNoneButton.setEnabled(selectedCount != 0);
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			boolean select;
			Object source = e.getSource();
			if (source == selectAllButton)
			{
				select = true;
				selectedCount = encodings.encodingsSelected.length;
			}
			else if (source == selectNoneButton)
			{
				select = false;
				selectedCount = 0;
			}
			else
			{
				return;
			}
			int rowCount = encodings.getRowCount();
			for (int i=0 ; i < rowCount; i++)
			{
				encodings.encodingsSelected[i] = select;
			}
			updateButton();
			encodings.fireTableDataChanged();
			table.repaint();

		}
	} //}}}

	//}}}

	//{{{ EncodingTableModel class
	private class EncodingTableModel extends AbstractTableModel
	{
		private final String[] encodings;

		private final boolean[] encodingsSelected;

		EncodingTableModel(String[] encodings)
		{
			this.encodings = encodings;
			encodingsSelected = new boolean[encodings.length];

			int encodingsAmount = encodings.length;
			for (int i=0 ; i<encodingsAmount ; i++) {
				String encoding = encodings[i];
				boolean selected = !jEdit.getBooleanProperty("encoding.opt-out." + encoding, false);
				encodingsSelected[i] = selected;
				if (selected)
					selectedCount++;
			}
		}

		public String getColumnName(int columnIndex)
		{
			return null;
		}

		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0 : return Boolean.class;
				case 1 : return String.class;
				default: throw new IllegalArgumentException("Unexpected column value " + columnIndex);
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 0;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if (columnIndex == 0)
			{
				boolean value = ((Boolean) aValue).booleanValue();
				if (value != encodingsSelected[rowIndex])
				{
					encodingsSelected[rowIndex] = value;
					fireTableCellUpdated(rowIndex, columnIndex);
					if (value)
						selectedCount++;
					else
						selectedCount--;
					updateButton();
				}
			}
		}

		public void addTableModelListener(TableModelListener l)
		{
		}

		public void removeTableModelListener(TableModelListener l)
		{
		}

		public int getRowCount()
		{
			return encodings.length;
		}

		public int getColumnCount()
		{
			return 2;
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			switch (columnIndex)
			{
				case 0 : return Boolean.valueOf(encodingsSelected[rowIndex]);
				case 1 : return encodings[rowIndex];
				default: throw new IllegalArgumentException("Unexpected column value " + columnIndex);
			}
		}
	} //}}}
} //}}}
