/*
 * PrintOptionPane.java - Printing options panel
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

import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.*;

public class PrintOptionPane extends AbstractOptionPane
{
	public PrintOptionPane()
	{
		super("print");
	}

	// protected members
	protected void _init()
	{
		/* Font */
		String _fontFamily = jEdit.getProperty("print.font");
		int _fontStyle;
		try
		{
			_fontStyle = Integer.parseInt(jEdit.getProperty("print.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			_fontStyle = Font.PLAIN;
		}
		int _fontSize;
		try
		{
			_fontSize = Integer.parseInt(jEdit.getProperty("print.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			_fontSize = 14;
		}
		font = new FontSelector(new Font(_fontFamily,_fontStyle,_fontSize));
		addComponent(jEdit.getProperty("options.print.font"),font);

		/* Header */
		printHeader = new JCheckBox(jEdit.getProperty("options.print"
			+ ".header"));
		printHeader.setSelected(jEdit.getBooleanProperty("print.header"));
		addComponent(printHeader);

		/* Footer */
		printFooter = new JCheckBox(jEdit.getProperty("options.print"
			+ ".footer"));
		printFooter.setSelected(jEdit.getBooleanProperty("print.footer"));
		addComponent(printFooter);

		/* Line numbering */
		printLineNumbers = new JCheckBox(jEdit.getProperty("options.print"
			+ ".lineNumbers"));
		printLineNumbers.setSelected(jEdit.getBooleanProperty("print.lineNumbers"));
		addComponent(printLineNumbers);

		/* Syntax highlighting */
		style = new JCheckBox(jEdit.getProperty("options.print"
			+ ".style"));
		style.setSelected(jEdit.getBooleanProperty("print.style"));
		addComponent(style);

		color = new JCheckBox(jEdit.getProperty("options.print"
			+ ".color"));
		color.setSelected(jEdit.getBooleanProperty("print.color"));
		addComponent(color);

		addSeparator("options.print.margins");

		/* Margins */
		topMargin = new JTextField(jEdit.getProperty("print.margin.top"));
		addComponent(jEdit.getProperty("options.print.margin.top"),topMargin);
		leftMargin = new JTextField(jEdit.getProperty("print.margin.left"));
		addComponent(jEdit.getProperty("options.print.margin.left"),leftMargin);
		bottomMargin = new JTextField(jEdit.getProperty("print.margin.bottom"));
		addComponent(jEdit.getProperty("options.print.margin.bottom"),bottomMargin);
		rightMargin = new JTextField(jEdit.getProperty("print.margin.right"));
		addComponent(jEdit.getProperty("options.print.margin.right"),rightMargin);
	}

	protected void _save()
	{
		Font _font = font.getFont();
		jEdit.setProperty("print.font",_font.getFamily());
		jEdit.setProperty("print.fontsize",String.valueOf(_font.getSize()));
		jEdit.setProperty("print.fontstyle",String.valueOf(_font.getStyle()));

		jEdit.setBooleanProperty("print.header",printHeader.isSelected());
		jEdit.setBooleanProperty("print.footer",printFooter.isSelected());
		jEdit.setBooleanProperty("print.lineNumbers",printLineNumbers.isSelected());
		jEdit.setBooleanProperty("print.style",style.isSelected());
		jEdit.setBooleanProperty("print.color",color.isSelected());
		jEdit.setProperty("print.margin.top",topMargin.getText());
		jEdit.setProperty("print.margin.left",leftMargin.getText());
		jEdit.setProperty("print.margin.bottom",bottomMargin.getText());
		jEdit.setProperty("print.margin.right",rightMargin.getText());
	}

	// private members
	private FontSelector font;
	private JCheckBox printHeader;
	private JCheckBox printFooter;
	private JCheckBox printLineNumbers;
	private JCheckBox style;
	private JCheckBox color;
	private JTextField topMargin;
	private JTextField leftMargin;
	private JTextField bottomMargin;
	private JTextField rightMargin;
}
