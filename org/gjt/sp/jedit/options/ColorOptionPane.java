/*
 * ColorOptionPane.java - Color option pane
 * Copyright (C) 1999, 2000 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.*;

/**
 * Color option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class ColorOptionPane extends AbstractOptionPane
{
	public static final EmptyBorder noFocusBorder = new EmptyBorder(1,1,1,1);

	public ColorOptionPane()
	{
		super("color");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,createColorTableScroller());
	}

	protected void _save()
	{
		colorModel.save();
	}

	// private members
	private ColorTableModel colorModel;
	private JTable colorTable;

	private JScrollPane createColorTableScroller()
	{
		colorModel = createColorTableModel();
		colorTable = new JTable(colorModel);
		colorTable.setRowSelectionAllowed(false);
		colorTable.setColumnSelectionAllowed(false);
		colorTable.setCellSelectionEnabled(false);
		colorTable.getTableHeader().setReorderingAllowed(false);
		colorTable.addMouseListener(new MouseHandler());
		TableColumnModel tcm = colorTable.getColumnModel();
 		TableColumn colorColumn = tcm.getColumn(1);
		colorColumn.setCellRenderer(new ColorTableModel.ColorRenderer());
		Dimension d = colorTable.getPreferredSize();
		d.height = Math.min(d.height,100);
		JScrollPane scroller = new JScrollPane(colorTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private ColorTableModel createColorTableModel()
	{
		return new ColorTableModel();
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			int row = colorTable.rowAtPoint(evt.getPoint());
			if(row == -1)
				return;

			Color color = JColorChooser.showDialog(
				ColorOptionPane.this,
				jEdit.getProperty("colorChooser.title"),
				(Color)colorModel.getValueAt(row,1));
			if(color != null)
				colorModel.setValueAt(color,row,1);
		}
	}
}

class ColorTableModel extends AbstractTableModel
{
	private Vector colorChoices;

	ColorTableModel()
	{
		colorChoices = new Vector(16);
		addColorChoice("options.color.bgColor","view.bgColor");
		addColorChoice("options.color.fgColor","view.fgColor");
		addColorChoice("options.color.caretColor","view.caretColor");
		addColorChoice("options.color.selectionColor",
			"view.selectionColor");
		addColorChoice("options.color.lineHighlightColor",
			"view.lineHighlightColor");
		addColorChoice("options.color.bracketHighlightColor",
			"view.bracketHighlightColor");
		addColorChoice("options.color.eolMarkerColor",
			"view.eolMarkerColor");
		addColorChoice("options.color.wrapGuideColor",
			"view.wrapGuideColor");
		addColorChoice("options.color.gutterBgColor",
			"view.gutter.bgColor");
		addColorChoice("options.color.gutterFgColor",
			"view.gutter.fgColor");
		addColorChoice("options.color.gutterHighlightColor",
			"view.gutter.highlightColor");
		addColorChoice("options.color.gutterCurrentLineColor",
			"view.gutter.currentLineColor");
		addColorChoice("options.color.gutterMarkerColor",
			"view.gutter.markerColor");
		addColorChoice("options.color.gutterFoldColor",
			"view.gutter.foldColor");
		addColorChoice("options.color.gutterFocusBorderColor",
			"view.gutter.focusBorderColor");
		addColorChoice("options.color.gutterNoFocusBorderColor",
			"view.gutter.noFocusBorderColor");
		if(!(UIManager.getLookAndFeel() instanceof MetalLookAndFeel))
		{
			addColorChoice("options.color.dockingBorderColor",
				"view.docking.borderColor");
		}
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return colorChoices.size();
	}

	public Object getValueAt(int row, int col)
	{
		ColorChoice ch = (ColorChoice)colorChoices.elementAt(row);
		switch(col)
		{
		case 0:
			return ch.label;
		case 1:
			return ch.color;
		default:
			return null;
		}
	}

	public void setValueAt(Object value, int row, int col)
	{
		ColorChoice ch = (ColorChoice)colorChoices.elementAt(row);
		if(col == 1)
			ch.color = (Color)value;
		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.color.object");
		case 1:
			return jEdit.getProperty("options.color.color");
		default:
			return null;
		}
	}

	public void save()
	{
		for(int i = 0; i < colorChoices.size(); i++)
		{
			ColorChoice ch = (ColorChoice)colorChoices
				.elementAt(i);
			jEdit.setProperty(ch.property,
				GUIUtilities.getColorHexString(ch.color));
		}
	}

	private void addColorChoice(String label, String property)
	{
		colorChoices.addElement(new ColorChoice(jEdit.getProperty(label),
			property,GUIUtilities.parseColor(jEdit.getProperty(property))));
	}

	static class ColorChoice
	{
		String label;
		String property;
		Color color;

		ColorChoice(String label, String property, Color color)
		{
			this.label = label;
			this.property = property;
			this.color = color;
		}
	}

	static class ColorRenderer extends JLabel
		implements TableCellRenderer
	{
		public ColorRenderer()
		{
			setOpaque(true);
			setBorder(StyleOptionPane.noFocusBorder);
		}

		// TableCellRenderer implementation
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

			setBorder((cellHasFocus) ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: StyleOptionPane.noFocusBorder);
			return this;
		}
		// end TableCellRenderer implementation
	}
}
