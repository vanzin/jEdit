/*
 * BrowserColorsOptionPane.java - Browser colors options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.*;
//}}}

//{{{ BrowserColorsOptionPane class
/**
 * Browser color editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class BrowserColorsOptionPane extends AbstractOptionPane
{
	//{{{ BrowserColorsOptionPane constructor
	public BrowserColorsOptionPane()
	{
		super("browser.colors");
	} //}}}

	//{{{ Protected members

	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());

		colorsModel = new BrowserColorsModel();
		colorsTable = new JTable(colorsModel);
		colorsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		colorsTable.getTableHeader().setReorderingAllowed(false);
		colorsTable.addMouseListener(new MouseHandler());
		colorsTable.getSelectionModel().addListSelectionListener(
			new SelectionHandler());
		TableColumnModel tcm = colorsTable.getColumnModel();
		tcm.getColumn(1).setCellRenderer(new BrowserColorsModel.ColorRenderer());
		Dimension d = colorsTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(colorsTable);
		scroller.setPreferredSize(d);
		add(BorderLayout.CENTER,scroller);

		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3,0,0,0));
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		ActionHandler actionHandler = new ActionHandler();
		add = new RolloverButton(GUIUtilities.loadIcon("Plus.png"));
		add.setToolTipText(jEdit.getProperty("common.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(Box.createHorizontalStrut(6));
		remove = new RolloverButton(GUIUtilities.loadIcon("Minus.png"));
		remove.setToolTipText(jEdit.getProperty("common.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		moveUp = new RolloverButton(GUIUtilities.loadIcon("ArrowU.png"));
		moveUp.setToolTipText(jEdit.getProperty("common.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		buttons.add(Box.createHorizontalStrut(6));
		moveDown = new RolloverButton(GUIUtilities.loadIcon("ArrowD.png"));
		moveDown.setToolTipText(jEdit.getProperty("common.moveDown"));
		moveDown.addActionListener(actionHandler);
		buttons.add(moveDown);
		buttons.add(Box.createGlue());

		add(BorderLayout.SOUTH,buttons);

		updateEnabled();
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		colorsModel.save();
	} //}}}

	//}}}

	//{{{ Private members
	private BrowserColorsModel colorsModel;
	private JTable colorsTable;
	private JButton add;
	private JButton remove;
	private JButton moveUp;
	private JButton moveDown;

	//{{{ updateEnabled() method
	private void updateEnabled()
	{
		int selectedRow = colorsTable.getSelectedRow();
		remove.setEnabled(selectedRow != -1);
		moveUp.setEnabled(selectedRow > 0);
		moveUp.setEnabled(selectedRow != -1 && selectedRow !=
			colorsModel.getRowCount());
	} //}}}

	//{{{ setSelectedRow() method
	private void setSelectedRow(int row)
	{
		colorsTable.getSelectionModel().setSelectionInterval(row,row);
		colorsTable.scrollRectToVisible(colorsTable.getCellRect(row,0,true));
	} //}}}

	//}}}

	//{{{ SelectionHandler class
	class SelectionHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			updateEnabled();
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == add)
			{
				colorsModel.add();
			}
			else if(source == remove)
			{
				int selectedRow = colorsTable.getSelectedRow();
				colorsModel.remove(selectedRow);
				updateEnabled();
			}
			else if(source == moveUp)
			{
				int selectedRow = colorsTable.getSelectedRow();
				if(selectedRow != 0)
				{
					colorsModel.moveUp(selectedRow);
					setSelectedRow(selectedRow - 1);
				}
				updateEnabled();
			}
			else if(source == moveDown)
			{
				int selectedRow = colorsTable.getSelectedRow();
				if(selectedRow != colorsTable.getRowCount() - 1)
				{
					colorsModel.moveDown(selectedRow);
					setSelectedRow(selectedRow + 1);
				}
				updateEnabled();
			}
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			Point p = evt.getPoint();
			int row = colorsTable.rowAtPoint(p);
			int column = colorsTable.columnAtPoint(p);
			if(row == -1 || column != 1)
				return;

			Color color = JColorChooser.showDialog(
				BrowserColorsOptionPane.this,
				jEdit.getProperty("colorChooser.title"),
				(Color)colorsModel.getValueAt(row,1));
			if(color != null)
				colorsModel.setValueAt(color,row,1);
		}
	} //}}}
} //}}}

//{{{ BrowserColorsModel class
class BrowserColorsModel extends AbstractTableModel
{
	//{{{ BrowserColorsModel constructor
	BrowserColorsModel()
	{
		entries = new ArrayList<Entry>();

		int i = 0;
		String glob;
		while((glob = jEdit.getProperty("vfs.browser.colors." + i + ".glob")) != null)
		{
			entries.add(new Entry(glob,
				jEdit.getColorProperty(
				"vfs.browser.colors." + i + ".color",
				Color.black)));
			i++;
		}
	} //}}}

	//{{{ add() method
	void add()
	{
		entries.add(new Entry("",UIManager.getColor("Tree.foreground")));
		fireTableRowsInserted(entries.size() - 1,entries.size() - 1);
	} //}}}

	//{{{ remove() method
	void remove(int index)
	{
		entries.remove(index);
		fireTableRowsDeleted(entries.size(),entries.size());
	} //}}}

	//{{{ moveUp() method
	public void moveUp(int index)
	{
		Entry entry = entries.get(index);
		entries.remove(index);
		entries.add(index - 1,entry);
		fireTableRowsUpdated(index - 1,index);
	} //}}}

	//{{{ moveDown() method
	public void moveDown(int index)
	{
		Entry entry = entries.get(index);
		entries.remove(index);
		entries.add(index + 1,entry);
		fireTableRowsUpdated(index,index + 1);
	} //}}}

	//{{{ save() method
	void save()
	{
		int i;
		for(i = 0; i < entries.size(); i++)
		{
			Entry entry = entries.get(i);
			jEdit.setProperty("vfs.browser.colors." + i + ".glob",
				entry.glob);
			jEdit.setColorProperty("vfs.browser.colors." + i + ".color",
				entry.color);
		}
		jEdit.unsetProperty("vfs.browser.colors." + i + ".glob");
		jEdit.unsetProperty("vfs.browser.colors." + i + ".color");
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 2;
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		return entries.size();
	} //}}}

	//{{{ getValueAt() method
	public Object getValueAt(int row, int col)
	{
		Entry entry = entries.get(row);

		switch(col)
		{
		case 0:
			return entry.glob;
		case 1:
			return entry.color;
		default:
			return null;
		}
	} //}}}

	//{{{ isCellEditable() method
	public boolean isCellEditable(int row, int col)
	{
		return col == 0;
	} //}}}

	//{{{ setValueAt() method
	public void setValueAt(Object value, int row, int col)
	{
		Entry entry = entries.get(row);

		if(col == 0)
			entry.glob = (String)value;
		else
			entry.color = (Color)value;

		fireTableRowsUpdated(row,row);
	} //}}}

	//{{{ getColumnName() method
	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.browser.colors.glob");
		case 1:
			return jEdit.getProperty("options.browser.colors.color");
		default:
			return null;
		}
	} //}}}

	//{{{ getColumnClass() method
	public Class getColumnClass(int col)
	{
		switch(col)
		{
		case 0:
			return String.class;
		case 1:
			return Color.class;
		default:
			throw new InternalError();
		}
	} //}}}

	private List<Entry> entries;

	//{{{ Entry class
	private static class Entry
	{
		String glob;
		Color color;

		Entry(String glob, Color color)
		{
			this.glob = glob;
			this.color = color;
		}
	} //}}}

	//{{{ ColorRenderer class
	static class ColorRenderer extends JLabel
		implements TableCellRenderer
	{
		//{{{ ColorRenderer constructor
		ColorRenderer()
		{
			setOpaque(true);
			setBorder(SyntaxHiliteOptionPane.noFocusBorder);
		} //}}}

		//{{{ getTableCellRendererComponent() method
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean cellHasFocus,
			int row,
			int col)
		{
			if (isSelected)
			{
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else
			{
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}

			if (value != null)
				setBackground((Color)value);

			setBorder(cellHasFocus ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: SyntaxHiliteOptionPane.noFocusBorder);
			return this;
		} //}}}
	} //}}}
} //}}}
