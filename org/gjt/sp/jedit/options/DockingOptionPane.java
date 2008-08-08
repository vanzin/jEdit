/*
 * DockingOptionPane.java - Dockable window options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.StandardUtilities;
//}}}

//{{{ DockingOptionPane class
public class DockingOptionPane extends AbstractOptionPane
{
	//{{{ DockingOptionPane constructor
	public DockingOptionPane()
	{
		super("docking");
	} //}}}

	//{{{ _init() method
	public void _init()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.NORTH,createDockingOptionsPanel());
		add(BorderLayout.CENTER,createWindowTableScroller());
	} //}}}

	//{{{ _save() method
	public void _save()
	{
		jEdit.setProperty(View.VIEW_DOCKING_FRAMEWORK_PROPERTY,
			(String) dockingFramework.getSelectedItem());
		jEdit.setBooleanProperty(AUTO_LOAD_MODE_LAYOUT_PROP, autoLoadModeLayout.isSelected());
		jEdit.setBooleanProperty(AUTO_SAVE_MODE_LAYOUT_PROP, autoSaveModeLayout.isSelected());
		windowModel.save();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JTable windowTable;
	private WindowTableModel windowModel;
	private JComboBox dockingFramework;
	private JCheckBox autoLoadModeLayout;
	private JCheckBox autoSaveModeLayout;
	//}}}

	private static final String DOCKING_OPTIONS_PREFIX = "options.docking.";
	public static final String AUTO_LOAD_MODE_LAYOUT_PROP = DOCKING_OPTIONS_PREFIX + "autoLoadModeLayout";
	private static final String AUTO_LOAD_MODE_LAYOUT_LABEL = AUTO_LOAD_MODE_LAYOUT_PROP + ".label";
	public static final String AUTO_SAVE_MODE_LAYOUT_PROP = DOCKING_OPTIONS_PREFIX + "autoSaveModeLayout";
	private static final String AUTO_SAVE_MODE_LAYOUT_LABEL = AUTO_SAVE_MODE_LAYOUT_PROP + ".label";
	
	private JPanel createDockingOptionsPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(0, 1));
		p.add(createDockingFrameworkChooser());
		boolean autoLoadModeLayoutProp = jEdit.getBooleanProperty(
			AUTO_LOAD_MODE_LAYOUT_PROP, false);
		autoLoadModeLayout = new JCheckBox(
			jEdit.getProperty(AUTO_LOAD_MODE_LAYOUT_LABEL),
			autoLoadModeLayoutProp);
		p.add(autoLoadModeLayout);
		autoSaveModeLayout = new JCheckBox(
			jEdit.getProperty(AUTO_SAVE_MODE_LAYOUT_LABEL),
			jEdit.getBooleanProperty(AUTO_SAVE_MODE_LAYOUT_PROP, false));
		p.add(autoSaveModeLayout);
		autoSaveModeLayout.setEnabled(autoLoadModeLayoutProp);
		autoLoadModeLayout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoSaveModeLayout.setEnabled(autoLoadModeLayout.isSelected());
			}
		});
		return p;
	}
	private JPanel createDockingFrameworkChooser()
	{
		JPanel p = new JPanel();
		p.add(new JLabel(jEdit.getProperty("options.docking.selectFramework.label")));
		String [] frameworks =
			ServiceManager.getServiceNames(View.DOCKING_FRAMEWORK_PROVIDER_SERVICE);
		dockingFramework = new JComboBox(frameworks);
		String framework = View.getDockingFrameworkName();
		for (int i = 0; i < frameworks.length; i++)
		{
			if (frameworks[i].equals(framework))
			{
				dockingFramework.setSelectedIndex(i);
				break;
			}
		}
		p.add(dockingFramework);
		return p;
	}
	//{{{ createWindowTableScroller() method
	private JScrollPane createWindowTableScroller()
	{
		windowModel = createWindowModel();
		windowTable = new JTable(windowModel);
		windowTable.getTableHeader().setReorderingAllowed(false);
		windowTable.setColumnSelectionAllowed(false);
		windowTable.setRowSelectionAllowed(false);
		windowTable.setCellSelectionEnabled(false);

		DockPositionCellRenderer comboBox = new DockPositionCellRenderer();
		windowTable.setRowHeight(comboBox.getPreferredSize().height);
		TableColumn column = windowTable.getColumnModel().getColumn(1);
		column.setCellRenderer(comboBox);
		column.setCellEditor(new DefaultCellEditor(new DockPositionCellRenderer()));

		Dimension d = windowTable.getPreferredSize();
		d.height = Math.min(d.height,50);
		JScrollPane scroller = new JScrollPane(windowTable);
		scroller.setPreferredSize(d);
		return scroller;
	} //}}}

	//{{{ createWindowModel() method
	private static WindowTableModel createWindowModel()
	{
		return new WindowTableModel();
	} //}}}

	//}}}

	//{{{ DockPositionCellRenderer class
	static class DockPositionCellRenderer extends JComboBox
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
			DockPositionCellRenderer.this.setRequestFocusEnabled(false);
		}

		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column)
		{
			setSelectedItem(value);
			return this;
		}
	} //}}}
} //}}}

//{{{ WindowTableModel class
class WindowTableModel extends AbstractTableModel
{
	private Vector windows;

	//{{{ WindowTableModel constructor
	WindowTableModel()
	{
		windows = new Vector();

		String[] dockables = DockableWindowManager.getRegisteredDockableWindows();
		for(int i = 0; i < dockables.length; i++)
		{
			windows.addElement(new Entry(dockables[i]));
		}

		sort();
	} //}}}

	//{{{ sort() method
	public void sort()
	{
		Collections.sort(windows,new WindowCompare());
		fireTableDataChanged();
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 2;
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		return windows.size();
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int col)
	{
		switch(col)
		{
		case 0:
		case 1:
			return String.class;
		default:
			throw new InternalError();
		}
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int row, int col)
	{
		Entry window = (Entry)windows.elementAt(row);
		switch(col)
		{
		case 0:
			return window.title;
		case 1:
			return window.dockPosition;
		default:
			throw new InternalError();
		}
	} //}}}

	//{{{ isCellEditable() method
	public boolean isCellEditable(int row, int col)
	{
		return col != 0;
	} //}}}

	//{{{ setValueAt() method
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
		default:
			throw new InternalError();
		}

		fireTableRowsUpdated(row,row);
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.docking.title");
		case 1:
			return jEdit.getProperty("options.docking.dockPosition");
		default:
			throw new InternalError();
		}
	} //}}}

	//{{{ save() method
	public void save()
	{
		for(int i = 0; i < windows.size(); i++)
		{
			((Entry)windows.elementAt(i)).save();
		}
	} //}}}

	//{{{ Entry class
	static class Entry
	{
		String name;
		String title;
		String dockPosition;

		Entry(String name)
		{
			this.name = name;
			title = jEdit.getProperty(name + ".title");
			if(title == null)
				title = name;

			dockPosition = jEdit.getProperty(name + ".dock-position");
			if(dockPosition == null)
				dockPosition = DockableWindowManager.FLOATING;
		}

		void save()
		{
			jEdit.setProperty(name + ".dock-position",dockPosition);
		}
	} //}}}

	//{{{ WindowCompare class
	static class WindowCompare implements Comparator
	{
		public int compare(Object obj1, Object obj2)
		{
			Entry e1 = (Entry)obj1;
			Entry e2 = (Entry)obj2;

			return StandardUtilities.compareStrings(
				e1.title,e2.title,true);
		}
	} //}}}
} //}}}
