/*
 * StatusBarOptionPane.java - Tool bar options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008-2021 Matthieu Casanova
 * Portions Copyright (C) 2000-2002 Slava Pestov
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
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.gui.statusbar.StatusWidgetFactory;
import org.gjt.sp.jedit.gui.statusbar.Widget;
import org.gjt.sp.jedit.jEdit;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
//}}}

/**
 * Status bar editor.
 * @author Matthieu Casanova
 * @version $Id$
 */
public class StatusBarOptionPane extends AbstractOptionPane
{

	private WidgetTableModel widgetTableModel;

	//{{{ StatusBarOptionPane constructor
	public StatusBarOptionPane()
	{
		super("status");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		setLayout(new BorderLayout());

		//{{{ North
		JPanel checkboxPanel = new JPanel(new GridLayout(2,1));
		showStatusbar = new JCheckBox(jEdit.getProperty(
			"options.status.visible"));
		showStatusbar.setSelected(jEdit.getBooleanProperty("view.status.visible"));
		checkboxPanel.add(showStatusbar);
		showStatusbarPlain = new JCheckBox(jEdit.getProperty(
			"options.status.plainview.visible"));
		showStatusbarPlain.setSelected(jEdit.getBooleanProperty("view.status.plainview.visible"));
		checkboxPanel.add(showStatusbarPlain);
		checkboxPanel.add(new JLabel(jEdit.getProperty(
			"options.status.caption")));

		JPanel north = new JPanel(new GridLayout(2,1));
		north.add(checkboxPanel);
		add(north, BorderLayout.NORTH);
		//}}}

		//{{{ Options panel
		AbstractOptionPane optionsPanel = new AbstractOptionPane("Status Options");
		/* Foreground color */
		optionsPanel.addComponent(jEdit.getProperty("options.status.foreground"),
			foregroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.foreground")),
			GridBagConstraints.VERTICAL);

		/* Background color */
		optionsPanel.addComponent(jEdit.getProperty("options.status.background"),
			backgroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.background")),
			GridBagConstraints.VERTICAL);

		/* Memory foreground color */
		optionsPanel.addComponent(jEdit.getProperty("options.status.memory.foreground"),
			memForegroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.memory.foreground")),
			GridBagConstraints.VERTICAL);

		/* Memory background color */
		optionsPanel.addComponent(jEdit.getProperty("options.status.memory.background"),
			memBackgroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.status.memory.background")),
			GridBagConstraints.VERTICAL);

		optionsPanel.addSeparator();
		optionsPanel.addComponent(new JLabel(jEdit.getProperty("options.status.caret.title", "Caret position display options:")));

		/*
		Caret position format: lineno,dot-virtual (caretpos/bufferlength)
		view.status.show-caret-linenumber -- true shows line number for caret (lineno)
		view.status.show-caret-dot -- true shows offset in line for caret (dot)
		view.status.show-caret-virtual -- true shows virtual offset in line for caret (virtual)
		view.status.show-caret-offset -- true shows caret offset from start of buffer (caretpos)
		view.status.show-caret-bufferlength -- true shows length of buffer (bufferlength)
		*/
		showCaretLineNumber = new JCheckBox(jEdit.getProperty("options.status.caret.linenumber", "Show caret line number"),
			jEdit.getBooleanProperty("view.status.show-caret-linenumber", true));
		showCaretLineNumber.setName("showCaretLineNumber");
		showCaretDot = new JCheckBox(jEdit.getProperty("options.status.caret.dot", "Show caret offset from start of line"),
			jEdit.getBooleanProperty("view.status.show-caret-dot", true));
		showCaretDot.setName("showCaretDot");
		showCaretVirtual = new JCheckBox(jEdit.getProperty("options.status.caret.virtual", "Show caret virtual offset from start of line"),
			jEdit.getBooleanProperty("view.status.show-caret-virtual", true));
		showCaretVirtual.setName("showCaretVirtual");
		showCaretOffset = new JCheckBox(jEdit.getProperty("options.status.caret.offset", "Show caret offset from start of file"),
			jEdit.getBooleanProperty("view.status.show-caret-offset", true));
		showCaretOffset.setName("showCaretOffset");
		showCaretBufferLength = new JCheckBox(jEdit.getProperty("options.status.caret.bufferlength", "Show length of file"),
			jEdit.getBooleanProperty("view.status.show-caret-bufferlength", true));
		showCaretBufferLength.setName("showCaretBufferLength");
		optionsPanel.addComponent(showCaretLineNumber);
		optionsPanel.addComponent(showCaretDot);
		optionsPanel.addComponent(showCaretVirtual);
		optionsPanel.addComponent(showCaretOffset);
		optionsPanel.addComponent(showCaretBufferLength);
		//}}}

		//{{{ widgets panel
		widgetTableModel = new WidgetTableModel();
		widgetTable = new JTable(widgetTableModel);
		widgetTable.setDefaultRenderer(WidgetTableModel.TableEntry.class, new WidgetCellRenderer());
		widgetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		widgetTable.setTableHeader(null);
		widgetTable.getSelectionModel().addListSelectionListener(e -> updateButtons());
		JPanel widgetsPanel = new JPanel(new BorderLayout());
		widgetsPanel.add(new JScrollPane(widgetTable), BorderLayout.CENTER);
		//}}}

		//{{{ Create buttons
		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new RolloverButton(GUIUtilities.loadIcon("ArrowU.png"));
		moveUp.setToolTipText(jEdit.getProperty("options.status.moveUp"));
		moveUp.addActionListener(e -> moveUp());
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new RolloverButton(GUIUtilities.loadIcon("ArrowD.png"));
		moveDown.setToolTipText(jEdit.getProperty("options.status.moveDown"));
		moveDown.addActionListener(e -> moveDown());
		buttons.add(moveDown);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(Box.createGlue());
		//}}}

		widgetsPanel.add(buttons, BorderLayout.SOUTH);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Options",optionsPanel);
		tabs.add("Widgets", widgetsPanel);

		add(tabs, BorderLayout.CENTER);
		updateButtons();
	} ///}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setColorProperty("view.status.foreground",foregroundColor
			.getSelectedColor());
		jEdit.setColorProperty("view.status.background",backgroundColor
			.getSelectedColor());
		jEdit.setColorProperty("view.status.memory.foreground",memForegroundColor
			.getSelectedColor());
		jEdit.setColorProperty("view.status.memory.background",memBackgroundColor
			.getSelectedColor());

		jEdit.setBooleanProperty("view.status.visible",showStatusbar
			.isSelected());

		jEdit.setBooleanProperty("view.status.plainview.visible",showStatusbarPlain
			.isSelected());

		saveWidgets("view.status-leading", WidgetTableModel.TableEntry::isSelectedLeading);
		saveWidgets("view.status", WidgetTableModel.TableEntry::isSelectedTrailing);

		jEdit.setBooleanProperty("view.status.show-caret-linenumber", showCaretLineNumber.isSelected());
		jEdit.setBooleanProperty("view.status.show-caret-dot", showCaretDot.isSelected());
		jEdit.setBooleanProperty("view.status.show-caret-virtual", showCaretVirtual.isSelected());
		jEdit.setBooleanProperty("view.status.show-caret-offset", showCaretOffset.isSelected());
		jEdit.setBooleanProperty("view.status.show-caret-bufferlength", showCaretBufferLength.isSelected());
	} //}}}

	private void saveWidgets(String propertyName, Predicate<WidgetTableModel.TableEntry> filter)
	{
		String[] tokens = StreamSupport.stream(widgetTableModel.spliterator(), false)
			.filter(filter)
			.map(WidgetTableModel.TableEntry::getWidget)
			.toArray(String[]::new);
		jEdit.setProperty(propertyName, String.join(" ", tokens));
	}

	//{{{ Private members

	//{{{ Instance variables
	private ColorWellButton foregroundColor;
	private ColorWellButton backgroundColor;
	private ColorWellButton memForegroundColor;
	private ColorWellButton memBackgroundColor;
	private JCheckBox showStatusbar;
	private JCheckBox showStatusbarPlain;
	private JTable widgetTable;
	private RolloverButton moveUp, moveDown;

	private JCheckBox showCaretLineNumber;
	private JCheckBox showCaretDot;
	private JCheckBox showCaretVirtual;
	private JCheckBox showCaretOffset;
	private JCheckBox showCaretBufferLength;
	//}}}

	//{{{ updateButtons() method
	private void updateButtons()
	{
		int index = widgetTable.getSelectedRow();
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != widgetTable.getRowCount() - 1);
	} //}}}

	//{{{ moveUp() method
	private void moveUp()
	{
		int index = widgetTable.getSelectedRow();
		widgetTableModel.moveUp(index);
		widgetTable.getSelectionModel().setSelectionInterval(index - 1, index - 1);
	} //}}}

	//{{{ moveDown() method
	private void moveDown()
	{
		int index = widgetTable.getSelectedRow();
		widgetTableModel.moveDown(index);
		widgetTable.getSelectionModel().setSelectionInterval(index + 1, index + 1);
	} //}}}

	//}}}

	//{{{ Inner classes
	private static class WidgetTableModel extends AbstractTableModel implements Iterable<WidgetTableModel.TableEntry>
	{
		private final List<TableEntry> widgets;

		WidgetTableModel()
		{
			String[] allWidgets = ServiceManager.getServiceNames(StatusWidgetFactory.class);
			Arrays.sort(allWidgets);
			Collection<String> allWidgetsList = new ArrayList<>(Arrays.asList(allWidgets));
			widgets = new ArrayList<>();
			String leadingStatusBar = jEdit.getProperty("view.status-leading");
			String trailingStatusBar = jEdit.getProperty("view.status");
			String[] usedleadingWidgets = leadingStatusBar.split(" ");
			for (String usedWidget : usedleadingWidgets)
				if (allWidgetsList.remove(usedWidget))
					widgets.add(new TableEntry(usedWidget, true, false));
			String[] usedTrailingWidgets = trailingStatusBar.split(" ");
			for (String usedWidget : usedTrailingWidgets)
				if (allWidgetsList.remove(usedWidget))
					widgets.add(new TableEntry(usedWidget, false, true));
			allWidgetsList
				.stream()
				.map(widget -> new TableEntry(widget, false, false))
				.forEach(widgets::add);
		}

		@Override
		@Nonnull
		public Iterator<TableEntry> iterator()
		{
			return widgets.iterator();
		}

		void moveUp(int index)
		{
			TableEntry entry = widgets.remove(index);
			widgets.add(index -1, entry);
			fireTableRowsUpdated(index - 1, index);
		}

		void moveDown(int index)
		{
			TableEntry entry = widgets.remove(index);
			widgets.add(index + 1, entry);
			fireTableRowsUpdated(index, index + 1);
		}

		@Override
		public int getRowCount()
		{
			return widgets.size();
		}

		@Override
		public int getColumnCount()
		{
			return 4;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 0 || columnIndex == 1;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			TableEntry tableEntry = widgets.get(rowIndex);
			if (columnIndex == 0)
			{
				tableEntry.setSelectedLeading((boolean) aValue);
				tableEntry.setSelectedTrailing(false);
			}
			else if (columnIndex == 1)
			{
				tableEntry.setSelectedLeading(false);
				tableEntry.setSelectedTrailing((boolean) aValue);
			}
			fireTableCellUpdated(rowIndex, 0);
			fireTableCellUpdated(rowIndex, 1);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
				case 1:
					return Boolean.class;
				case 2:
					return String.class;
				default:
					return TableEntry.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			TableEntry tableEntry = widgets.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return tableEntry.isSelectedLeading();
				case 1:
					return tableEntry.isSelectedTrailing();
				case 2:
					return tableEntry.getWidget();
				default:
					return tableEntry;
			}
		}

		private static class TableEntry
		{
			private boolean selectedLeading;
			private boolean selectedTrailing;
			private String widget;

			private TableEntry(String widget, boolean selectedLeading, boolean selectedTrailing)
			{
				this.widget = widget;
				this.selectedLeading = selectedLeading;
				this.selectedTrailing = selectedTrailing;
			}

			public boolean isSelectedLeading()
			{
				return selectedLeading;
			}

			public void setSelectedLeading(boolean selectedLeading)
			{
				this.selectedLeading = selectedLeading;
			}

			public boolean isSelectedTrailing()
			{
				return selectedTrailing;
			}

			public void setSelectedTrailing(boolean selectedTrailing)
			{
				this.selectedTrailing = selectedTrailing;
			}

			public String getWidget()
			{
				return widget;
			}

			public void setWidget(String widget)
			{
				this.widget = widget;
			}
		}
	}

	private static class WidgetCellRenderer extends DefaultTableCellRenderer
	{
		private final Map<String, JComponent> widgetsSamples;

		private WidgetCellRenderer()
		{
			widgetsSamples = new HashMap<>();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{

			WidgetTableModel.TableEntry tableEntry = (WidgetTableModel.TableEntry) value;
			String widgetName = tableEntry.getWidget();
			JComponent widgetComponent = widgetsSamples.get(widgetName);
			if (widgetComponent == null)
			{
				StatusWidgetFactory service = ServiceManager.getService(StatusWidgetFactory.class, widgetName);
				if (service != null)
				{
					Widget widget = service.getWidget(jEdit.getActiveView());
					widget.update();
					widgetComponent = widget.getComponent();
					widgetsSamples.put(widgetName, widgetComponent);
				}
				else
				{
					super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					String label = jEdit.getProperty("statusbar."+widgetName+".label", widgetName);
					setText(label);
					widgetComponent = this;
				}
			}
			return widgetComponent;
		}
	}
	//}}}
}

