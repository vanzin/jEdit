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

		int c = clickActionKeys.length;
		String[] clickActionNames = new String[c];
		for(int i = 0; i < c; i++)
		{
			clickActionNames[i] = jEdit.getProperty(
				"options.gutter."+clickActionKeys[i]);
		}

		c = clickModifierKeys.length;
		String[] clickModifierNames = new String[c];
		for(int i = 0; i < c; i++)
		{
			clickModifierNames[i] = jEdit.getProperty(
				"options.gutter."+clickModifierKeys[i]);
		}

		gutterClickActions = new JComboBox[c];

		for(int i = 0; i < c; i++)
		{
			JComboBox cb = new JComboBox(clickActionNames);
			gutterClickActions[i] = cb;

			String val = jEdit.getProperty("view.gutter."+clickModifierKeys[i]);
			for(int j = 0; j < clickActionKeys.length; j++)
			{
				if(val.equals(clickActionKeys[j]))
				{
					cb.setSelectedIndex(j);
				}
			}

			addComponent(clickModifierNames[i],cb);
		}

		gutterFont = new FontSelector(
			jEdit.getFontProperty("view.gutter.font",
			new Font("Monospaced",Font.PLAIN,10)));

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

		gutterBracketHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.bracketHighlight"));
		gutterBracketHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.bracketHighlight"));
		addComponent(gutterBracketHighlightEnabled);

		gutterMarkerHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.markerHighlight"));
		gutterMarkerHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.markerHighlight"));
		addComponent(gutterMarkerHighlightEnabled);
	}

	public void _save()
	{
		jEdit.setFontProperty("view.gutter.font",gutterFont.getFont());

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
		jEdit.setBooleanProperty("view.gutter.bracketHighlight",
			gutterBracketHighlightEnabled.isSelected());
		jEdit.setBooleanProperty("view.gutter.markerHighlight",
			gutterMarkerHighlightEnabled.isSelected());
		
		int c = clickModifierKeys.length;
		for(int i = 0; i < c; i++)
		{
			int idx = gutterClickActions[i].getSelectedIndex();
			jEdit.setProperty("view.gutter."+clickModifierKeys[i],
				clickActionKeys[idx]);
		}
	}

	// private members
	private FontSelector gutterFont;
	private JTextField gutterBorderWidth;
	private JTextField gutterHighlightInterval;
	private JComboBox gutterNumberAlignment;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox gutterCurrentLineHighlightEnabled;
	private JCheckBox gutterBracketHighlightEnabled;
	private JCheckBox gutterMarkerHighlightEnabled;
	private JComboBox[] gutterClickActions;
	
	private static final String[] clickActionKeys = new String[] {
		"toggleFold",
		"toggleFoldFully",
		"selectFold"
	};
	
	private static final String[] clickModifierKeys = new String[] {
		"gutterClick",
		"gutterShiftClick",
		"gutterControlClick",
		"gutterAltClick"
	};
}
