/*
 * TextAreaOptionPane.java - Text area options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.*;
//}}}

public class TextAreaOptionPane extends AbstractOptionPane
{
	//{{{ TextAreaOptionPane constructor
	public TextAreaOptionPane()
	{
		super("textarea");
	} //}}}

	//{{{ _init() method
	public void _init()
	{
		/* Font */
		font = new FontSelector(jEdit.getFontProperty("view.font"));

		addComponent(jEdit.getProperty("options.textarea.font"),font);

		/* Line highlight */
		lineHighlight = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".lineHighlight"));
		lineHighlight.setSelected(jEdit.getBooleanProperty("view.lineHighlight"));
		addComponent(lineHighlight);

		/* Bracket highlight */
		bracketHighlight = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".bracketHighlight"));
		bracketHighlight.setSelected(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		addComponent(bracketHighlight);

		/* EOL markers */
		eolMarkers = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".eolMarkers"));
		eolMarkers.setSelected(jEdit.getBooleanProperty("view.eolMarkers"));
		addComponent(eolMarkers);

		/* Wrap guide */
		wrapGuide = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".wrapGuide"));
		wrapGuide.setSelected(jEdit.getBooleanProperty("view.wrapGuide"));
		addComponent(wrapGuide);

		/* Blinking caret */
		blinkCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blinkCaret"));
		blinkCaret.setSelected(jEdit.getBooleanProperty("view.caretBlink"));
		addComponent(blinkCaret);

		/* Block caret */
		blockCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blockCaret"));
		blockCaret.setSelected(jEdit.getBooleanProperty("view.blockCaret"));
		addComponent(blockCaret);

		/* Electric borders */
		electricBorders = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".electricBorders"));
		electricBorders.setSelected(!"0".equals(jEdit.getProperty(
			"view.electricBorders")));
		addComponent(electricBorders);

		/* Smart home/end */
		homeEnd = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".homeEnd"));
		homeEnd.setSelected(jEdit.getBooleanProperty("view.homeEnd"));
		addComponent(homeEnd);

		/* Middle mouse button click pastes % register */
		middleMousePaste = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".middleMousePaste"));
		middleMousePaste.setSelected(jEdit.getBooleanProperty(
			"view.middleMousePaste"));
		addComponent(middleMousePaste);

		/* Anti-aliasing */
		antiAlias = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".antiAlias"));
		antiAlias.setSelected(jEdit.getBooleanProperty("view.antiAlias"));
		addComponent(antiAlias);

		/* Fractional font metrics */
		fracFontMetrics = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".fracFontMetrics"));
		fracFontMetrics.setSelected(jEdit.getBooleanProperty(
			"view.fracFontMetrics"));
		addComponent(fracFontMetrics);

		/* Parse fully */
		parseFully = new JCheckBox(jEdit.getProperty(
			"options.textarea.parseFully"));
		parseFully.setSelected(jEdit.getBooleanProperty("parseFully"));
		addComponent(parseFully);
	} //}}}

	//{{{ _save() method
	public void _save()
	{
		jEdit.setFontProperty("view.font",font.getFont());

		jEdit.setBooleanProperty("view.lineHighlight",lineHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.bracketHighlight",bracketHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.eolMarkers",eolMarkers
			.isSelected());
		jEdit.setBooleanProperty("view.wrapGuide",wrapGuide
			.isSelected());
		jEdit.setBooleanProperty("view.caretBlink",blinkCaret.isSelected());
		jEdit.setBooleanProperty("view.blockCaret",blockCaret.isSelected());
		jEdit.setIntegerProperty("view.electricBorders",electricBorders
			.isSelected() ? 3 : 0);
		jEdit.setBooleanProperty("view.homeEnd",homeEnd.isSelected());
		jEdit.setBooleanProperty("view.middleMousePaste",
			middleMousePaste.isSelected());
		jEdit.setBooleanProperty("view.antiAlias",antiAlias.isSelected());
		jEdit.setBooleanProperty("view.fracFontMetrics",fracFontMetrics.isSelected());
		jEdit.setBooleanProperty("parseFully",parseFully.isSelected());
	} //}}}

	//{{{ Private members
	private FontSelector font;
	private JCheckBox lineHighlight;
	private JCheckBox bracketHighlight;
	private JCheckBox eolMarkers;
	private JCheckBox wrapGuide;
	private JCheckBox blinkCaret;
	private JCheckBox blockCaret;
	private JCheckBox electricBorders;
	private JCheckBox homeEnd;
	private JCheckBox middleMousePaste;
	private JCheckBox antiAlias;
	private JCheckBox fracFontMetrics;
	private JCheckBox parseFully;
	//}}}
}
