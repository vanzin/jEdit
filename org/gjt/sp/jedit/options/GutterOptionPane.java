/*
 * GutterOptionPane.java - Gutter options panel
 * Copyright (C) 2000 mike dillon
 * Portions copyright (C) 2001 Slava Pestov
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

public class GutterOptionPane extends AbstractOptionPane
{
	public GutterOptionPane()
	{
		super("gutter");
	}

	public void _init()
	{
		lineNumbersEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.lineNumbers"));
		lineNumbersEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		addComponent(lineNumbersEnabled);

		/* Font */
		String _fontFamily = jEdit.getProperty("view.gutter.font");

		int _fontStyle;
		try
		{
			_fontStyle = Integer.parseInt(jEdit.getProperty("view.gutter.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			_fontStyle = Font.PLAIN;
		}

		int _fontSize;
		try
		{
			_fontSize = Integer.parseInt(jEdit.getProperty("view.gutter.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			_fontSize = 14;
		}
		gutterFont = new FontSelector(new Font(_fontFamily,_fontStyle,_fontSize));

		addComponent(jEdit.getProperty("options.gutter.font"),gutterFont);

		gutterBorderWidth = new JTextField(jEdit.getProperty(
			"view.gutter.borderWidth"));
		addComponent(jEdit.getProperty("options.gutter.borderWidth"),
			gutterBorderWidth);

		gutterHighlightInterval = new JTextField(jEdit.getProperty(
			"view.gutter.highlightInterval"));
		addComponent(jEdit.getProperty("options.gutter.interval"),
			gutterHighlightInterval);

		String[] alignments = new String[] {
			"Left", "Center", "Right"
		};
		gutterNumberAlignment = new JComboBox(alignments);
		String alignment = jEdit.getProperty("view.gutter.numberAlignment");
		if("right".equals(alignment))
			gutterNumberAlignment.setSelectedIndex(2);
		else if("center".equals(alignment))
			gutterNumberAlignment.setSelectedIndex(1);
		else
			gutterNumberAlignment.setSelectedIndex(0);
		addComponent(jEdit.getProperty("options.gutter.numberAlignment"),
			gutterNumberAlignment);

		gutterCurrentLineHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.currentLineHighlight"));
		gutterCurrentLineHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		addComponent(gutterCurrentLineHighlightEnabled);

		gutterMarkerHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.markerHighlight"));
		gutterMarkerHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.markerHighlight"));
		addComponent(gutterMarkerHighlightEnabled);
	}

	public void _save()
	{
		Font _font = gutterFont.getFont();
		jEdit.setProperty("view.gutter.font",_font.getFamily());
		jEdit.setProperty("view.gutter.fontsize",String.valueOf(_font.getSize()));
		jEdit.setProperty("view.gutter.fontstyle",String.valueOf(_font.getStyle()));

		jEdit.setProperty("view.gutter.borderWidth",
			gutterBorderWidth.getText());
		jEdit.setProperty("view.gutter.highlightInterval",
			gutterHighlightInterval.getText());
		String alignment = null;
		switch(gutterNumberAlignment.getSelectedIndex())
		{
		case 2:
			alignment = "right";
			break;
		case 1:
			alignment = "center";
			break;
		case 0: default:
			alignment = "left";
		}
		jEdit.setProperty("view.gutter.numberAlignment", alignment);
		jEdit.setBooleanProperty("view.gutter.lineNumbers", lineNumbersEnabled
			.isSelected());
		jEdit.setBooleanProperty("view.gutter.highlightCurrentLine",
			gutterCurrentLineHighlightEnabled.isSelected());
		jEdit.setBooleanProperty("view.gutter.markerHighlight",
			gutterMarkerHighlightEnabled.isSelected());
	}

	// private members
	private FontSelector gutterFont;
	private JTextField gutterBorderWidth;
	private JTextField gutterHighlightInterval;
	private JComboBox gutterNumberAlignment;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox gutterCurrentLineHighlightEnabled;
	private JCheckBox gutterMarkerHighlightEnabled;
}
