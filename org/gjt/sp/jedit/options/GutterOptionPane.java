/*
 * GutterOptionPane.java - Gutter options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000 mike dillon
 * Portions copyright (C) 2001, 2002 Slava Pestov
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
import javax.swing.*;

import java.awt.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
//}}}

public class GutterOptionPane extends AbstractOptionPane
{
	//{{{ GutterOptionPane constructor
	public GutterOptionPane()
	{
		super("gutter");
	} //}}}

	//{{{ _init() method
	public void _init()
	{
		/* Gutter enable */
		gutterEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.enabled"));
		gutterEnabled.setSelected(isGutterEnabled());
		addComponent(gutterEnabled);

		/* Line numbering */
		lineNumbersEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.lineNumbers"));
		lineNumbersEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		addComponent(lineNumbersEnabled);

		/* Text font */
		gutterFont = new FontSelector(
			jEdit.getFontProperty("view.gutter.font",
			new Font("Monospaced",Font.PLAIN,10)));

		addComponent(jEdit.getProperty("options.gutter.font"),gutterFont);

		/* Text color */
		addComponent(jEdit.getProperty("options.gutter.foreground"),
			gutterForeground = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.fgColor")),
			GridBagConstraints.VERTICAL);

		/* Background color */
		addComponent(jEdit.getProperty("options.gutter.background"),
			gutterBackground = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.bgColor")),
			GridBagConstraints.VERTICAL);

		/* Border width */
		/* gutterBorderWidth = new JTextField(jEdit.getProperty(
			"view.gutter.borderWidth"));
		addComponent(jEdit.getProperty("options.gutter.borderWidth"),
			gutterBorderWidth); */

		/* Number alignment */
		/* String[] alignments = new String[] {
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
			gutterNumberAlignment); */

		/* Current line highlight */
		gutterCurrentLineHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.currentLineHighlight"));
		gutterCurrentLineHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		addComponent(gutterCurrentLineHighlightEnabled,
			gutterCurrentLineHighlight = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.currentLineColor")),
			GridBagConstraints.VERTICAL);

		/* Highlight interval and color */
		gutterHighlightInterval = new JTextField(jEdit.getProperty(
			"view.gutter.highlightInterval"),3);

		Box gutterHighlightBox = new Box(BoxLayout.X_AXIS);
		gutterHighlightBox.add(new JLabel(jEdit.getProperty(
			"options.gutter.interval-1")));
		gutterHighlightBox.add(Box.createHorizontalStrut(3));
		gutterHighlightBox.add(gutterHighlightInterval);
		gutterHighlightBox.add(Box.createHorizontalStrut(3));
		gutterHighlightBox.add(new JLabel(jEdit.getProperty(
			"options.gutter.interval-2")));
		gutterHighlightBox.add(Box.createHorizontalStrut(12));

		addComponent(gutterHighlightBox,gutterHighlightColor
			= new ColorWellButton(jEdit.getColorProperty(
			"view.gutter.highlightColor")),
			GridBagConstraints.VERTICAL);

		/* Structure highlight */
		gutterStructureHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.structureHighlight"));
		gutterStructureHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.structureHighlight"));
		addComponent(gutterStructureHighlightEnabled,
			gutterStructureHighlight = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.structureHighlightColor")),
			GridBagConstraints.VERTICAL);

		/* Marker highlight */
		gutterMarkerHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.markerHighlight"));
		gutterMarkerHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.markerHighlight"));
		addComponent(gutterMarkerHighlightEnabled,
			gutterMarkerHighlight = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.markerColor")),
			GridBagConstraints.VERTICAL);

		/* Fold marker color */
		addComponent(jEdit.getProperty("options.gutter.foldColor"),
			gutterFoldMarkers = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.foldColor")),
			GridBagConstraints.VERTICAL);

		/* Focused border color */
		addComponent(jEdit.getProperty("options.gutter.focusBorderColor"),
			gutterFocusBorder = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.focusBorderColor")),
			GridBagConstraints.VERTICAL);

		/* unfocused border color */
		addComponent(jEdit.getProperty("options.gutter.noFocusBorderColor"),
			gutterNoFocusBorder = new ColorWellButton(
			jEdit.getColorProperty("view.gutter.noFocusBorderColor")),
			GridBagConstraints.VERTICAL);
		
		addFoldStyleChooser();
	} //}}}

	//{{{ _save() method
	public void _save()
	{
		jEdit.setBooleanProperty("view.gutter.lineNumbers", lineNumbersEnabled
			.isSelected());

		jEdit.setFontProperty("view.gutter.font",gutterFont.getFont());
		jEdit.setColorProperty("view.gutter.fgColor",gutterForeground
			.getSelectedColor());
		jEdit.setColorProperty("view.gutter.bgColor",gutterBackground
			.getSelectedColor());

		/* jEdit.setProperty("view.gutter.borderWidth",
			gutterBorderWidth.getText());

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
		jEdit.setProperty("view.gutter.numberAlignment", alignment); */

		jEdit.setBooleanProperty("view.gutter.highlightCurrentLine",
			gutterCurrentLineHighlightEnabled.isSelected());
		jEdit.setColorProperty("view.gutter.currentLineColor",
			gutterCurrentLineHighlight.getSelectedColor());
		jEdit.setProperty("view.gutter.highlightInterval",
			gutterHighlightInterval.getText());
		jEdit.setColorProperty("view.gutter.highlightColor",
			gutterHighlightColor.getSelectedColor());

		jEdit.setBooleanProperty("view.gutter.structureHighlight",
			gutterStructureHighlightEnabled.isSelected());
		jEdit.setColorProperty("view.gutter.structureHighlightColor",
			gutterStructureHighlight.getSelectedColor());
		jEdit.setBooleanProperty("view.gutter.markerHighlight",
			gutterMarkerHighlightEnabled.isSelected());
		jEdit.setColorProperty("view.gutter.markerColor",
			gutterMarkerHighlight.getSelectedColor());
		jEdit.setColorProperty("view.gutter.foldColor",
			gutterFoldMarkers.getSelectedColor());
		jEdit.setProperty(JEditTextArea.FOLD_PAINTER_PROPERTY,
			painters[foldPainter.getSelectedIndex()]);
		jEdit.setColorProperty("view.gutter.focusBorderColor",
			gutterFocusBorder.getSelectedColor());
		jEdit.setColorProperty("view.gutter.noFocusBorderColor",
			gutterNoFocusBorder.getSelectedColor());
		jEdit.setBooleanProperty(GUTTER_ENABLED_PROPERTY,
			gutterEnabled.isSelected());
	} //}}}

	//{{{ addFoldStyleChooser() method
	private void addFoldStyleChooser()
	{
		painters = ServiceManager.getServiceNames(JEditTextArea.FOLD_PAINTER_SERVICE);
		foldPainter = new JComboBox();
		String current = JEditTextArea.getFoldPainterName();
		int selected = 0;
		for (int i = 0; i < painters.length; i++)
		{
			String painter = painters[i];
			foldPainter.addItem(jEdit.getProperty(
				"options.gutter.foldStyleNames." + painter, painter));
			if (painter.equals(current))
				selected = i;
		}
		foldPainter.setSelectedIndex(selected);
		addComponent(new JLabel(jEdit.getProperty("options.gutter.foldStyle.label")), foldPainter);
	} //}}}

	//{{{ isGutterEnabled() method
	public static boolean isGutterEnabled()
	{
		return jEdit.getBooleanProperty(GUTTER_ENABLED_PROPERTY);
	} //}}}
	
	//{{{ Private members
	private static final String GUTTER_ENABLED_PROPERTY =
		"view.gutter.enabled";
	private FontSelector gutterFont;
	private ColorWellButton gutterForeground;
	private ColorWellButton gutterBackground;
	private JTextField gutterHighlightInterval;
	private ColorWellButton gutterHighlightColor;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox gutterCurrentLineHighlightEnabled;
	private ColorWellButton gutterCurrentLineHighlight;
	private JCheckBox gutterStructureHighlightEnabled;
	private ColorWellButton gutterStructureHighlight;
	private JCheckBox gutterMarkerHighlightEnabled;
	private ColorWellButton gutterMarkerHighlight;
	private ColorWellButton gutterFoldMarkers;
	private JComboBox foldPainter;
	private ColorWellButton gutterFocusBorder;
	private ColorWellButton gutterNoFocusBorder;
	private String [] painters;
	private JCheckBox gutterEnabled;
	//}}}
}
