/*
 * StyleOptionPane.java - Style option pane
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
					setBackground(GUIUtilities.parseColor(
						jEdit.getProperty("view.bgColor")));
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

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(0,0,12,0));
		panel.add(italics = new JCheckBox(
			jEdit.getProperty("style-editor.italics")));
		italics.setSelected(style.getFont().isItalic());
		panel.add(Box.createHorizontalStrut(2));
		panel.add(bold = new JCheckBox(
			jEdit.getProperty("style-editor.bold")));
		bold.setSelected(style.getFont().isBold());
		panel.add(Box.createHorizontalStrut(12));
		panel.add(new JLabel(jEdit.getProperty("style-editor.fgColor")));
		panel.add(Box.createHorizontalStrut(12));
		panel.add(fgColor = new JButton("    "));
		fgColor.setBackground(style.getForegroundColor());
		fgColor.setRequestFocusEnabled(false);
		fgColor.addActionListener(this);
		fgColor.setMargin(new Insets(0,0,0,0));
		panel.add(Box.createHorizontalStrut(12));
		panel.add(new JLabel(jEdit.getProperty("style-editor.bgColor")));
		panel.add(Box.createHorizontalStrut(12));
		panel.add(bgColor = new JButton("    "));
		if(style.getBackgroundColor() == null)
			bgColor.setBackground(GUIUtilities.parseColor(jEdit.getProperty("view.bgColor")));
		else
			bgColor.setBackground(style.getBackgroundColor());
		bgColor.setRequestFocusEnabled(false);
		bgColor.addActionListener(this);
		bgColor.setMargin(new Insets(0,0,0,0));

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
		else if (source == fgColor || source == bgColor)
		{
			JButton b = (JButton)source;
			Color c = JColorChooser.showDialog(this,
				jEdit.getProperty("colorChooser.title"),
				b.getBackground());
			if(c != null)
				b.setBackground(c);
		}
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

		Color background = bgColor.getBackground();

		if (background.equals(GUIUtilities.parseColor(jEdit.getProperty("view.bgColor"))))
			background = null;

		return new SyntaxStyle(fgColor.getBackground(),background,
				new Font("Dialog",
				(italics.isSelected() ? Font.ITALIC : 0)
				| (bold.isSelected() ? Font.BOLD : 0),
				12));
	}

	// private members
	private JCheckBox italics;
	private JCheckBox bold;
	private JButton fgColor;
	private JButton bgColor;
	private JButton ok;
	private JButton cancel;
	private boolean okClicked;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2001/09/02 05:37:53  spestov
 * Initial revision
 *
 * Revision 1.28  2001/07/15 09:55:14  sp
 * AWT/2D text render code split almost finished
 *
 * Revision 1.27  2001/02/05 09:15:30  sp
 * Improved shortcut option pane, various other 31337 stuff
 *
 * Revision 1.26  2001/01/22 05:35:08  sp
 * bug fixes galore
 *
 * Revision 1.25  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.24  2000/11/05 05:25:46  sp
 * Word wrap, format and remove-trailing-ws commands from TextTools moved into core
 *
 * Revision 1.23  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.22  2000/10/12 09:28:27  sp
 * debugging and polish
 *
 * Revision 1.21  2000/08/10 08:30:41  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.20  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.19  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.18  2000/07/12 09:11:38  sp
 * macros can be added to context menu and tool bar, menu bar layout improved
 *
 * Revision 1.17  2000/06/03 07:28:26  sp
 * User interface updates, bug fixes
 *
 * Revision 1.16  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.15  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 */
