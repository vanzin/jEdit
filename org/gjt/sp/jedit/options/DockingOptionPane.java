/*
 * DockingOptionPane.java - Dockable window options panel
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

public class DockingOptionPane extends AbstractOptionPane
{
	public DockingOptionPane()
	{
		super("docking");
	}

	public void _init()
	{
		Box box = new Box(BoxLayout.X_AXIS);
		ButtonGroup grp = new ButtonGroup();

		layout1 = new JToggleButton(GUIUtilities.loadIcon("dock_layout1.gif"));
		grp.add(layout1);
		box.add(layout1);

		box.add(Box.createHorizontalStrut(6));

		layout2 = new JToggleButton(GUIUtilities.loadIcon("dock_layout2.gif"));
		grp.add(layout2);
		box.add(layout2);

		if(jEdit.getBooleanProperty("view.docking.alternateLayout"))
			layout2.setSelected(true);
		else
			layout1.setSelected(true);

		addComponent(jEdit.getProperty("options.docking.layout"),box);

		// reuse properties defined by the general option pane
		String[] positions = {
			jEdit.getProperty("options.docking.top"),
			jEdit.getProperty("options.docking.bottom"),
		};

		tabsPos = new JComboBox(positions);
		tabsPos.setSelectedIndex(Integer.parseInt(jEdit.getProperty(
			"view.docking.tabsPos")));
		addComponent(jEdit.getProperty("options.docking.tabsPos"),tabsPos);

		addComponent(Box.createVerticalStrut(6));

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = 3;
		cons.gridwidth = cons.gridheight = GridBagConstraints.REMAINDER;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = cons.weighty = 1.0f;

		JScrollPane windowScroller = createWindowTableScroller();
		gridBag.setConstraints(windowScroller,cons);
		add(windowScroller);
	}

	public void _save()
	{
		jEdit.setBooleanProperty("view.docking.alternateLayout",
			layout2.isSelected());
		jEdit.setProperty("view.docking.tabsPos",String.valueOf(
			tabsPos.getSelectedIndex()));
		windowModel.save();
	}

	// private members
	private JToggleButton layout1;
	private JToggleButton layout2;
	private JComboBox tabsPos;
	private JTable windowTable;
	private WindowTableModel windowModel;

	private JScrollPane createWindowTableScroller()
	{
		windowModel = createWindowModel();
		windowTable = new JTable(windowModel);
		windowTable.getTableHeader().setReorderingAllowed(false);
		windowTable.setColumnSelectionAllowed(false);
		windowTable.setRowSelectionAllowed(false);
		windowTable.setCellSelectionEnabled(false);

		DockPositionCellRenderer comboBox = new DockPositionCellRenderer();
		comboBox.setRequestFocusEnabled(false);
		windowTable.setRowHeight(comboBox.getPreferredSize().height);
		TableColumn column = windowTable.getColumnModel().getColumn(1);
		column.setCellRenderer(comboBox);
		comboBox = new DockPositionCellRenderer();
		comboBox.setRequestFocusEnabled(false);
		column.setCellEditor(new DefaultCellEditor(comboBox));

		Dimension d = windowTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(windowTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private WindowTableModel createWindowModel()
	{
		return new WindowTableModel();
	}

	class DockPositionCellRenderer extends JComboBox
		implements TableCellRenderer
	{
		DockPositionCellRenderer()
		{
			super(new String[] {
				DockableWindowManager.FLOATING,
				DockableWindowManager.TOP,
				DockableWindowManager.LEFT,
				DockableWindowManager.BOTTOM,
				DockableWindowManager.RIGHT
			});
		}

		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
		{
			setSelectedItem(value);
			return this;
		}
	}
}

class WindowTableModel extends AbstractTableModel
{
	private Vector windows;

	WindowTableModel()
	{
		Object[] list = EditBus.getNamedList(DockableWindow.DOCKABLE_WINDOW_LIST);
		windows = new Vector(list.length);
		for(int i = 0; i < list.length; i++)
		{
			windows.addElement(new Entry((String)list[i]));
		}

		sort();
	}

	public void sort()
	{
		MiscUtilities.quicksort(windows,new WindowCompare());
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		return 3;
	}

	public int getRowCount()
	{
		return windows.size();
	}

	public Class getColumnClass(int col)
	{
		switch(col)
		{
		case 0:
		case 1:
			return String.class;
		case 2:
			return Boolean.class;
		default:
			throw new InternalError();
		}
	}

	public Object getValueAt(int row, int col)
	{
		Entry window = (Entry)windows.elementAt(row);
		switch(col)
		{
		case 0:
			return window.title;
		case 1:
			return window.dockPosition;
		case 2:
			return new Boolean(window.autoOpen);
		default:
			throw new InternalError();
		}
	}

	public boolean isCellEditable(int row, int col)
	{
		return (col != 0);
	}

	public void setValueAt(Object value, int row, int col)
	{
		if(col == 0)
			return;

		Entry window = (Entry)windows.elementAt(row);
		switch(col)
		{
		case 1:
			window.dockPosition = (String)value;
			break;
		case 2:
			window.autoOpen = ((Boolean)value).booleanValue();
			break;
		default:
			throw new InternalError();
		}

		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.docking.title");
		case 1:
			return jEdit.getProperty("options.docking.dockPosition");
		case 2:
			return jEdit.getProperty("options.docking.autoOpen");
		default:
			throw new InternalError();
		}
	}

	public void save()
	{
		for(int i = 0; i < windows.size(); i++)
		{
			((Entry)windows.elementAt(i)).save();
		}
	}

	class Entry
	{
		String name;
		String title;
		String dockPosition;
		boolean autoOpen;

		Entry(String name)
		{
			this.name = name;
			title = jEdit.getProperty(name + ".title");
			if(title == null)
				title = name;

			dockPosition = jEdit.getProperty(name + ".dock-position");
			if(dockPosition == null)
				dockPosition = DockableWindowManager.FLOATING;
			autoOpen = jEdit.getBooleanProperty(name + ".auto-open");
		}

		void save()
		{
			jEdit.setProperty(name + ".dock-position",dockPosition);
			jEdit.setBooleanProperty(name + ".auto-open",autoOpen);
		}
	}

	class WindowCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			Entry e1 = (Entry)obj1;
			Entry e2 = (Entry)obj2;

			return e1.title.compareTo(e2.title);
		}
	}
}
