/*
 * ColorOptionPane.java - Color option pane
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
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
//}}}

//{{{ ColorOptionPane class
/**
 * Color option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class ColorOptionPane extends AbstractOptionPane
{
	public static final EmptyBorder noFocusBorder = new EmptyBorder(1,1,1,1);

	//{{{ ColorOptionPane constructor
	public ColorOptionPane()
	{
		super("color");
	} //}}}

	//{{{ Protected members

	//{{{ _init() method
	protected void _init()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,createColorTableScroller());
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		colorModel.save();
	} //}}}

	//}}}

	//{{{ Private members
	private ColorTableModel colorModel;
	private JTable colorTable;

	//{{{ createColorTableScroller() method
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
	} //}}}

	//{{{ createColorTableModel() method
	private ColorTableModel createColorTableModel()
	{
		return new ColorTableModel();
	} //}}}

	//}}}

	//{{{ MouseHandler class
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
	} //}}}
} //}}}

//{{{ ColorTableModel class
class ColorTableModel extends AbstractTableModel
{
	private Vector colorChoices;

	//{{{ ColorTableModel constructor
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
		addColorChoice("options.color.gutterBracketHighlightColor",
			"view.gutter.bracketHighlightColor");
		addColorChoice("options.color.gutterMarkerColor",
			"view.gutter.markerColor");
		addColorChoice("options.color.gutterFoldColor",
			"view.gutter.foldColor");
		addColorChoice("options.color.gutterFocusBorderColor",
			"view.gutter.focusBorderColor");
		addColorChoice("options.color.gutterNoFocusBorderColor",
			"view.gutter.noFocusBorderColor");
		addColorChoice("options.color.memoryForegroundColor",
			"view.status.memory.foreground");
		addColorChoice("options.color.memoryBackgroundColor",
			"view.status.memory.background");
		MiscUtilities.quicksort(colorChoices,new MiscUtilities.StringCompare());
	} //}}}

	//{{{ getColumnCount() method
	public int getColumnCount()
	{
		return 2;
	} //}}}

	//{{{ getRowCount() method
	public int getRowCount()
	{
		return colorChoices.size();
	} //}}}

	//{{{ getValueAt() method
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
	} //}}}

	//{{{ setValueAt() method
	public void setValueAt(Object value, int row, int col)
	{
		ColorChoice ch = (ColorChoice)colorChoices.elementAt(row);
		if(col == 1)
			ch.color = (Color)value;
		fireTableRowsUpdated(row,row);
	} //}}}

	//{{{ getColumnName() method
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
	} //}}}

	//{{{ save() method
	public void save()
	{
		for(int i = 0; i < colorChoices.size(); i++)
		{
			ColorChoice ch = (ColorChoice)colorChoices
				.elementAt(i);
			jEdit.setColorProperty(ch.property,ch.color);
		}
	} //}}}

	//{{{ addColorChoice() method
	private void addColorChoice(String label, String property)
	{
		colorChoices.addElement(new ColorChoice(jEdit.getProperty(label),
			property,jEdit.getColorProperty(property)));
	} //}}}

	//{{{ ColorChoice class
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

		// for sorting
		public String toString()
		{
			return label;
		}
	} //}}}

	//{{{ ColorRenderer class
	static class ColorRenderer extends JLabel
		implements TableCellRenderer
	{
		//{{{ ColorRenderer constructor
		public ColorRenderer()
		{
			setOpaque(true);
			setBorder(StyleOptionPane.noFocusBorder);
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

			setBorder((cellHasFocus) ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: StyleOptionPane.noFocusBorder);
			return this;
		} //}}}
	} //}}}
} //}}}
