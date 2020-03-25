/*
 * DockingOptionPane.java - Dockable window options panel
 * :tabSize=4:indentSize=4:noTabs=false:
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

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.*;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.util.StandardUtilities;
//}}}

//{{{ DockingOptionPane class
@SuppressWarnings("serial")
public class DockingOptionPane extends AbstractOptionPane
{
	//{{{ DockingOptionPane constructor
	public DockingOptionPane()
	{
		super("docking");
	} //}}}

	//{{{ _init() method
	@Override
	public void _init()
	{
		setLayout(new BorderLayout());

		add(BorderLayout.NORTH,createDockingOptionsPanel());
		add(BorderLayout.CENTER,createWindowTableScroller());
		add(BorderLayout.SOUTH, createDockingFrameworkChooser());

		dockableSetSelection.setModel(
			new DefaultComboBoxModel<>(windowModel.getDockableSets().toArray(StandardUtilities.EMPTY_STRING_ARRAY)));
	} //}}}

	//{{{ _save() method
	@Override
	public void _save()
	{
		jEdit.setBooleanProperty(AUTO_LOAD_MODE_LAYOUT_PROP, autoLoadModeLayout.isSelected());
		jEdit.setBooleanProperty(AUTO_SAVE_MODE_LAYOUT_PROP, autoSaveModeLayout.isSelected());
		jEdit.setProperty(View.VIEW_DOCKING_FRAMEWORK_PROPERTY,
			(String) dockingFramework.getSelectedItem());
		windowModel.save();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JComboBox<String> dockingFramework;
	private WindowTableModel windowModel;
	private JCheckBox autoLoadModeLayout;
	private JCheckBox autoSaveModeLayout;
	private JComboBox<String> dockableSetSelection;
	//}}}

	private static final String DOCKING_OPTIONS_PREFIX = "options.docking.";
	public static final String AUTO_LOAD_MODE_LAYOUT_PROP = DOCKING_OPTIONS_PREFIX + "autoLoadModeLayout";
	private static final String AUTO_LOAD_MODE_LAYOUT_LABEL = AUTO_LOAD_MODE_LAYOUT_PROP + ".label";
	public static final String AUTO_SAVE_MODE_LAYOUT_PROP = DOCKING_OPTIONS_PREFIX + "autoSaveModeLayout";
	private static final String AUTO_SAVE_MODE_LAYOUT_LABEL = AUTO_SAVE_MODE_LAYOUT_PROP + ".label";

	private JPanel createDockingFrameworkChooser()
	{
		String [] frameworks =
			ServiceManager.getServiceNames(View.DOCKING_FRAMEWORK_PROVIDER_SERVICE);
		dockingFramework = new JComboBox<>(frameworks);

		String framework = View.getDockingFrameworkName();
		for (int i = 0; i < frameworks.length; i++)
		{
			if (frameworks[i].equals(framework))
			{
				dockingFramework.setSelectedIndex(i);
				break;
			}
		}
		dockingFramework.setToolTipText(jEdit.getProperty("options.docking.system-change.note"));
		JPanel p = new JPanel();
		p.setToolTipText(jEdit.getProperty("options.docking.system-change.note"));
		p.setLayout(new FlowLayout());
		p.add(new JLabel(jEdit.getProperty("options.docking.selectFramework.label")));
		p.add(dockingFramework);

		return p;
	}

	private JPanel createDockingOptionsPanel()
	{
		JPanel p = new JPanel(new GridLayout(0, 1));
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
		autoLoadModeLayout.addActionListener(e -> autoSaveModeLayout.setEnabled(autoLoadModeLayout.isSelected()));
		Box vSetSelection = Box.createVerticalBox();
		p.add(vSetSelection);
		Box setSelection = Box.createHorizontalBox();
		vSetSelection.add(setSelection);
		setSelection.add(Box.createHorizontalStrut(6));
		setSelection.add(new JLabel(jEdit.getProperty(
			"options.docking.selectSet.label")));
		setSelection.add(Box.createHorizontalStrut(6));
		dockableSetSelection = new JComboBox<>();
		setSelection.add(dockableSetSelection);
		dockableSetSelection.addItemListener(e -> windowModel.showSet((String) dockableSetSelection.getSelectedItem()));
		setSelection.add(Box.createHorizontalStrut(6));
		vSetSelection.add(Box.createVerticalStrut(6));
		return p;
	}
	//{{{ createWindowTableScroller() method
	private JScrollPane createWindowTableScroller()
	{
		windowModel = createWindowModel();
		JTable windowTable = new JTable(windowModel);
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
	private static class DockPositionCellRenderer extends JComboBox<String> implements TableCellRenderer
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

		@Override
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
@SuppressWarnings("serial")
class WindowTableModel extends AbstractTableModel
{
	private static final String PLUGIN_SET_PREFIX = "Plugin: ";
	private static final String CORE_DOCKABLE_SET = "Core";
	private static final String ALL_DOCKABLE_SET = "All";
	private final Map<String, List<Entry>> dockableSets;
	private List<Entry> windows;

	//{{{ WindowTableModel constructor
	WindowTableModel()
	{
		dockableSets = new HashMap<>();
		List<Entry> all = new ArrayList<>();
		dockableSets.put(ALL_DOCKABLE_SET, all);
		windows = new ArrayList<>();
		String[] dockables = DockableWindowManager.getRegisteredDockableWindows();
		for (String dockable: dockables)
		{
			String plugin = DockableWindowManager.
				getDockableWindowPluginName(dockable);
			String set;
			if (plugin != null)
				set = PLUGIN_SET_PREFIX + plugin;
			else
				set = CORE_DOCKABLE_SET;

			List<Entry> currentSetDockables = dockableSets.computeIfAbsent(set, k -> new ArrayList<>());
			Entry entry = new Entry(dockable);
			currentSetDockables.add(entry);
			all.add(entry);
		}
		showSet(ALL_DOCKABLE_SET);
	} //}}}

	public List<String> getDockableSets()
	{
		List<String> sets = new ArrayList<>(dockableSets.keySet());
		sets.remove(ALL_DOCKABLE_SET);
		sets.remove(CORE_DOCKABLE_SET);
		Collections.sort(sets);
		sets.add(0, CORE_DOCKABLE_SET);
		sets.add(0, ALL_DOCKABLE_SET);
		return sets;
	}

	//{{{ showSet() method
	public void showSet(String set)
	{
		windows = dockableSets.get(set);
		windows.sort(new WindowCompare());
		fireTableDataChanged();
	} //}}}

	//{{{ getColumnCount() method
	@Override
	public int getColumnCount()
	{
		return 2;
	} //}}}

	//{{{ getRowCount() method
	@Override
	public int getRowCount()
	{
		return windows.size();
	} //}}}

	//{{{ getColumnClass() method
	@Override
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
	@Override
	public Object getValueAt(int row, int col)
	{
		Entry window = windows.get(row);
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
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return col != 0;
	} //}}}

	//{{{ setValueAt() method
	@Override
	public void setValueAt(Object value, int row, int col)
	{
		if(col == 0)
			return;

		Entry window = windows.get(row);
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
	@Override
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
		windows.forEach(Entry::save);
	} //}}}

	//{{{ Entry class
	private static class Entry
	{
		final String name;
		private String title;
		private String dockPosition;

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
	static class WindowCompare implements Comparator<Object>
	{
		@Override
		public int compare(Object obj1, Object obj2)
		{
			Entry e1 = (Entry)obj1;
			Entry e2 = (Entry)obj2;

			return StandardUtilities.compareStrings(
				e1.title,e2.title,true);
		}
	} //}}}
} //}}}
