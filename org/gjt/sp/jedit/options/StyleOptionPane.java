/*
 * StyleOptionPane.java - Style option pane
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.*;

/**
 * Style option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class StyleOptionPane extends AbstractOptionPane
{
	public static final EmptyBorder noFocusBorder = new EmptyBorder(1,1,1,1);

	public StyleOptionPane()
	{
		super("style");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,createStyleTableScroller());
	}

	protected void _save()
	{
		styleModel.save();
	}

	// private members
	private StyleTableModel styleModel;
	private JTable styleTable;

	private JScrollPane createStyleTableScroller()
	{
		styleModel = createStyleTableModel();
		styleTable = new JTable(styleModel);
		styleTable.setRowSelectionAllowed(false);
		styleTable.setColumnSelectionAllowed(false);
		styleTable.setCellSelectionEnabled(false);
		styleTable.getTableHeader().setReorderingAllowed(false);
		styleTable.addMouseListener(new MouseHandler());
		TableColumnModel tcm = styleTable.getColumnModel();
 		TableColumn styleColumn = tcm.getColumn(1);
		styleColumn.setCellRenderer(new StyleTableModel.StyleRenderer());
		Dimension d = styleTable.getPreferredSize();
		d.height = Math.min(d.height,100);
		JScrollPane scroller = new JScrollPane(styleTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private StyleTableModel createStyleTableModel()
	{
		return new StyleTableModel();
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			int row = styleTable.rowAtPoint(evt.getPoint());
			if(row == -1)
				return;

			SyntaxStyle style = new StyleEditor(
				StyleOptionPane.this,
				(SyntaxStyle)styleModel.getValueAt(
				row,1)).getStyle();
			if(style != null)
				styleModel.setValueAt(style,row,1);
		}
	}
}

class StyleTableModel extends AbstractTableModel
{
	private Vector styleChoices;

	StyleTableModel()
	{
		styleChoices = new Vector(13);
		addStyleChoice("options.style.comment1Style","view.style.comment1");
		addStyleChoice("options.style.comment2Style","view.style.comment2");
		addStyleChoice("options.style.literal1Style","view.style.literal1");
		addStyleChoice("options.style.literal2Style","view.style.literal2");
		addStyleChoice("options.style.labelStyle","view.style.label");
		addStyleChoice("options.style.keyword1Style","view.style.keyword1");
		addStyleChoice("options.style.keyword2Style","view.style.keyword2");
		addStyleChoice("options.style.keyword3Style","view.style.keyword3");
		addStyleChoice("options.style.functionStyle","view.style.function");
		addStyleChoice("options.style.markupStyle","view.style.markup");
		addStyleChoice("options.style.operatorStyle","view.style.operator");
		addStyleChoice("options.style.digitStyle","view.style.digit");
		addStyleChoice("options.style.invalidStyle","view.style.invalid");
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return styleChoices.size();
	}

	public Object getValueAt(int row, int col)
	{
		StyleChoice ch = (StyleChoice)styleChoices.elementAt(row);
		switch(col)
		{
		case 0:
			return ch.label;
		case 1:
			return ch.style;
		default:
			return null;
		}
	}

	public void setValueAt(Object value, int row, int col)
	{
		StyleChoice ch = (StyleChoice)styleChoices.elementAt(row);
		if(col == 1)
			ch.style = (SyntaxStyle)value;
		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.style.object");
		case 1:
			return jEdit.getProperty("options.style.style");
		default:
			return null;
		}
	}

	public void save()
	{
		for(int i = 0; i < styleChoices.size(); i++)
		{
			StyleChoice ch = (StyleChoice)styleChoices
				.elementAt(i);
			jEdit.setProperty(ch.property,
				GUIUtilities.getStyleString(ch.style));
		}
	}

	private void addStyleChoice(String label, String property)
	{
		styleChoices.addElement(new StyleChoice(jEdit.getProperty(label),
			property,
			GUIUtilities.parseStyle(jEdit.getProperty(property),
			"Dialog",12)));
	}

	static class StyleChoice
	{
		String label;
		String property;
		SyntaxStyle style;

		StyleChoice(String label, String property, SyntaxStyle style)
		{
			this.label = label;
			this.property = property;
			this.style = style;
		}
	}

	static class StyleRenderer extends JLabel
		implements TableCellRenderer
	{
		public StyleRenderer()
		{
			setOpaque(true);
			setBorder(StyleOptionPane.noFocusBorder);
			setText("Hello World");
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
			if (value != null)
			{
				SyntaxStyle style = (SyntaxStyle)value;
				setForeground(style.getForegroundColor());
				if (style.getBackgroundColor() != null) 
					setBackground(style.getBackgroundColor());
				else
				{
					// this part sucks
					setBackground(jEdit.getColorProperty(
						"view.bgColor"));
				}
				setFont(style.getFont());
			}

			setBorder((cellHasFocus) ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: StyleOptionPane.noFocusBorder);
			return this;
		}
		// end TableCellRenderer implementation
	}
}

class StyleEditor extends EnhancedDialog implements ActionListener
{
	StyleEditor(Component comp, SyntaxStyle style)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("style-editor.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		GridBagLayout layout = new GridBagLayout();
		JPanel panel = new JPanel(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = 2;
		cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 0.0f;
		cons.insets = new Insets(0,0,12,0);

		italics = new JCheckBox(jEdit.getProperty("style-editor.italics"));
		italics.setSelected(style.getFont().isItalic());
		layout.setConstraints(italics,cons);
		panel.add(italics);

		bold = new JCheckBox(jEdit.getProperty("style-editor.bold"));
		bold.setSelected(style.getFont().isBold());
		layout.setConstraints(bold,cons);
		panel.add(bold);

		cons.gridwidth = 1;
		Color fg = style.getForegroundColor();
		fgColorCheckBox = new JCheckBox(jEdit.getProperty("style-editor.fgColor"));
		fgColorCheckBox.setSelected(fg != null);
		layout.setConstraints(fgColorCheckBox,cons);
		panel.add(fgColorCheckBox);

		cons.gridx++;
		fgColor = new JButton("    ");
		fgColor.setEnabled(fg != null);
		fgColor.setRequestFocusEnabled(false);
		fgColor.addActionListener(this);
		fgColor.setMargin(new Insets(0,0,0,0));
		if(fg == null)
			fgColor.setBackground(jEdit.getColorProperty("view.fgColor"));
		else
			fgColor.setBackground(fg);
		layout.setConstraints(fgColor,cons);
		panel.add(fgColor);

		cons.gridx = 0;
		cons.gridy++;
		Color bg = style.getBackgroundColor();
		bgColorCheckBox = new JCheckBox(jEdit.getProperty("style-editor.bgColor"));
		bgColorCheckBox.setSelected(bg != null);
		layout.setConstraints(bgColorCheckBox,cons);
		panel.add(bgColorCheckBox);

		cons.gridx++;
		bgColor = new JButton("    ");
		bgColor.setEnabled(bg != null);
		bgColor.setRequestFocusEnabled(false);
		bgColor.addActionListener(this);
		bgColor.setMargin(new Insets(0,0,0,0));
		if(bg == null)
			bgColor.setBackground(jEdit.getColorProperty("view.bgColor"));
		else
			bgColor.setBackground(bg);
		layout.setConstraints(bgColor,cons);
		panel.add(bgColor);

		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		box.add(ok = new JButton(jEdit.getProperty("common.ok")));
		getRootPane().setDefaultButton(ok);
		ok.addActionListener(this);
		box.add(Box.createHorizontalStrut(6));
		box.add(cancel = new JButton(jEdit.getProperty("common.cancel")));
		cancel.addActionListener(this);
		box.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,box);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
		show();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
		else if(source == fgColor || source == bgColor)
		{
			JButton b = (JButton)source;
			Color c = JColorChooser.showDialog(this,
				jEdit.getProperty("colorChooser.title"),
				b.getBackground());
			if(c != null)
				b.setBackground(c);
		}
		else if(source == fgColorCheckBox)
			fgColor.setEnabled(fgColorCheckBox.isSelected());
		else if(source == bgColorCheckBox)
			bgColor.setEnabled(bgColorCheckBox.isSelected());
	}

	// EnhancedDialog implementation
	public void ok()
	{
		okClicked = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	public SyntaxStyle getStyle()
	{
		if(!okClicked)
			return null;

		Color foreground = (fgColorCheckBox.isSelected()
			? fgColor.getBackground()
			: null);

		Color background = (bgColorCheckBox.isSelected()
			? bgColor.getBackground()
			: null);

		return new SyntaxStyle(foreground,background,
				new Font("Dialog",
				(italics.isSelected() ? Font.ITALIC : 0)
				| (bold.isSelected() ? Font.BOLD : 0),
				12));
	}

	// private members
	private JCheckBox italics;
	private JCheckBox bold;
	private JCheckBox fgColorCheckBox;
	private JButton fgColor;
	private JCheckBox bgColorCheckBox;
	private JButton bgColor;
	private JButton ok;
	private JButton cancel;
	private boolean okClicked;
}
