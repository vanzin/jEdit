/*
 * TextAreaOptionPane.java - Text area options panel
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import org.gjt.sp.jedit.textarea.AntiAlias;
import org.gjt.sp.util.GenericGUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.gui.FontSelectorDialog;
import org.gjt.sp.jedit.gui.ColorWellButton;
import org.gjt.sp.jedit.gui.RolloverButton;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 */
public class TextAreaOptionPane extends AbstractOptionPane
{
	//{{{ TextAreaOptionPane constructor
	public TextAreaOptionPane()
	{
		super("textarea");
	} //}}}

	//{{{ _init() method
	@Override
	public void _init()
	{
		/* Font */
		font = new FontSelector(jEdit.getFontProperty("view.font"));

		addComponent(jEdit.getProperty("options.textarea.font"),font);

		fontSubst = new JCheckBox(jEdit.getProperty("options.textarea.fontSubst"));
		fontSubst.setToolTipText(jEdit.getProperty("options.textarea.fontSubst.tooltip"));
		fontSubst.setSelected(jEdit.getBooleanProperty("view.enableFontSubst"));
		fontSubst.addActionListener(evt ->
		{
			fontSubstList.setVisible(fontSubst.isSelected());
			fontSubstSystemFonts.setVisible(fontSubst.isSelected());
		});
		addComponent(fontSubst);

		fontSubstList = new FontList();
		fontSubstList.setVisible(fontSubst.isSelected());
		addComponent(fontSubstList, GridBagConstraints.HORIZONTAL);

		fontSubstSystemFonts = new JCheckBox(jEdit.getProperty("options.textarea.fontSubstSystemFonts"));
		fontSubstSystemFonts.setSelected(jEdit.getBooleanProperty("view.enableFontSubstSystemFonts"));
		fontSubstSystemFonts.setVisible(fontSubst.isSelected());
		fontSubstSystemFonts.addActionListener(evt ->
		{
			if (!fontSubstSystemFonts.isSelected()
				&& (fontSubstList.listSize() == 0))
			{
				JOptionPane.showMessageDialog(fontSubstSystemFonts.getParent(),
					jEdit.getProperty("options.textarea.fontSubstWarning"),
					jEdit.getProperty("options.textarea.fontSubstWarning.label"),
					JOptionPane.WARNING_MESSAGE);
			}
		});
		addComponent(fontSubstSystemFonts, GridBagConstraints.HORIZONTAL);

		/* Anti-aliasing */
		antiAlias = new JComboBox<>(AntiAlias.comboChoices);

		antiAlias.setToolTipText(jEdit.getProperty("options.textarea.antiAlias.tooltip"));
		AntiAlias antiAliasValue = new AntiAlias(jEdit.getProperty("view.antiAlias"));
		font.setAntiAliasEnabled(antiAliasValue.val()>0);
		antiAlias.addActionListener(evt ->
		{
			int idx = antiAlias.getSelectedIndex();
			font.setAntiAliasEnabled(idx > 0);
			font.repaint();
		});
		antiAlias.setSelectedIndex(antiAliasValue.val());
		addComponent(jEdit.getProperty("options.textarea"+ ".antiAlias"), antiAlias);

		/* Fractional font metrics */
		fracFontMetrics = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".fracFontMetrics"));
		fracFontMetrics.setToolTipText(jEdit.getProperty("options.textarea.fracFontMetrics.tooltip"));
		fracFontMetrics.setSelected(jEdit.getBooleanProperty("view.fracFontMetrics"));
		addComponent(fracFontMetrics);

		/* Extra line spacing */
		IntegerInputVerifier integerInputVerifier = new IntegerInputVerifier();
		JPanel lineSpacingPanel = new JPanel();
		lineSpacing = new JTextField(String.valueOf(jEdit.getIntegerProperty("options.textarea.lineSpacing", 0)));
		lineSpacing.setColumns(4);
		lineSpacing.setHorizontalAlignment(JTextField.RIGHT);
		lineSpacing.setInputVerifier(integerInputVerifier);
		lineSpacingPanel.add(new JLabel(jEdit.getProperty("options.textarea.lineSpacing.label")));
		lineSpacingPanel.add(lineSpacing);
		addComponent(lineSpacingPanel);

		addSeparator();

		/* Text color */
		addComponent(jEdit.getProperty("options.textarea.foreground"),
			foregroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.fgColor")),
			GridBagConstraints.VERTICAL);

		/* Background color */
		addComponent(jEdit.getProperty("options.textarea.background"),
			backgroundColor = new ColorWellButton(
			jEdit.getColorProperty("view.bgColor")),
			GridBagConstraints.VERTICAL);

		/* Caret color, caret blink, block caret */
		blinkCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blinkCaret"));
		blinkCaret.setSelected(jEdit.getBooleanProperty("view.caretBlink"));

		blockCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blockCaret"));
		blockCaret.setSelected(jEdit.getBooleanProperty("view.blockCaret"));

		thickCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".thickCaret"));
		thickCaret.setSelected(jEdit.getBooleanProperty("view.thickCaret"));

		Box caretSettings = new Box(BoxLayout.X_AXIS);
		caretSettings.add(new JLabel(jEdit.getProperty(
			"options.textarea.caret")));
		caretSettings.add(Box.createHorizontalStrut(6));
		caretSettings.add(blinkCaret);
		caretSettings.add(blockCaret);
		caretSettings.add(thickCaret);

		addComponent(caretSettings,caretColor = new ColorWellButton(
			jEdit.getColorProperty("view.caretColor")),
			GridBagConstraints.VERTICAL);

		/* Selection color */
		addComponent(jEdit.getProperty("options.textarea.selection"),
			selectionColor = new ColorWellButton(
			jEdit.getColorProperty("view.selectionColor")),
			GridBagConstraints.VERTICAL);

		/* Multiple selection color */
		addComponent(jEdit.getProperty("options.textarea.multipleSelection"),
			multipleSelectionColor = new ColorWellButton(
			jEdit.getColorProperty("view.multipleSelectionColor")),
			GridBagConstraints.VERTICAL);

		/* Selection foreground color */
		selectionFg = new JCheckBox(jEdit.getProperty("options.textarea.selectionFg"));
		selectionFg.setName("selectionFg");
		selectionFg.setSelected(jEdit.getBooleanProperty("view.selectionFg"));
		addComponent(selectionFg, selectionFgColor = new ColorWellButton(
			jEdit.getColorProperty("view.selectionFgColor")),
			GridBagConstraints.VERTICAL);

		/* Line highlight */
		lineHighlight = new JCheckBox(jEdit.getProperty("options.textarea.lineHighlight"));
		lineHighlight.setSelected(jEdit.getBooleanProperty("view.lineHighlight"));
		addComponent(lineHighlight,lineHighlightColor = new ColorWellButton(
			jEdit.getColorProperty("view.lineHighlightColor")),
			GridBagConstraints.VERTICAL);

		/* Structure highlight */
		structureHighlight = new JCheckBox(jEdit.getProperty("options.textarea.structureHighlight"));
		structureHighlight.setSelected(jEdit.getBooleanProperty(
			"view.structureHighlight"));
		addComponent(structureHighlight,structureHighlightColor = new ColorWellButton(
			jEdit.getColorProperty("view.structureHighlightColor")),
			GridBagConstraints.VERTICAL);

		/* EOL markers */
		eolMarkers = new JCheckBox(jEdit.getProperty("options.textarea.eolMarkers"));
		eolMarkers.setSelected(jEdit.getBooleanProperty("view.eolMarkers"));
		addComponent(eolMarkers,eolMarkerColor =new ColorWellButton(
			jEdit.getColorProperty("view.eolMarkerColor")),
			GridBagConstraints.VERTICAL);

		/* Wrap guide */
		wrapGuide = new JCheckBox(jEdit.getProperty("options.textarea.wrapGuide"));
		wrapGuide.setSelected(jEdit.getBooleanProperty("view.wrapGuide"));
		addComponent(wrapGuide,wrapGuideColor = new ColorWellButton(
			jEdit.getColorProperty("view.wrapGuideColor")),
			GridBagConstraints.VERTICAL);

		/* page breaks */
		pageBreaks = new JCheckBox(jEdit.getProperty("options.textarea.pageBreaks"));
		pageBreaks.setSelected(jEdit.getBooleanProperty("view.pageBreaks", false));
		addComponent(pageBreaks, pageBreaksColor = new ColorWellButton(
			jEdit.getColorProperty("view.pageBreaksColor")),
			GridBagConstraints.VERTICAL);

		addSeparator();

		/* Electric borders */
		electricBorders = new JCheckBox(jEdit.getProperty("options.textarea.electricBorders"));
		electricBorders.setSelected(!"0".equals(jEdit.getProperty(
			"view.electricBorders")));
		addComponent(electricBorders);

		/* Strip trailing EOL */
		stripTrailingEOL = new JCheckBox(jEdit.getProperty(
			"options.textarea.stripTrailingEOL"));
		stripTrailingEOL.setSelected(jEdit.getBooleanProperty("stripTrailingEOL"));
		addComponent(stripTrailingEOL);

		completeFromAllBuffers = new JCheckBox(jEdit.getProperty(
			"options.textarea.completeFromAllBuffers"));
		completeFromAllBuffers.setSelected(jEdit.getBooleanProperty("completeFromAllBuffers"));
		addComponent(completeFromAllBuffers);
		
		insertCompletionWithDigit = new JCheckBox(jEdit.getProperty(
			"options.textarea.insertCompletionWithDigit"));
		insertCompletionWithDigit.setSelected(jEdit.getBooleanProperty("insertCompletionWithDigit"));
		addComponent(insertCompletionWithDigit);
	} //}}}

	//{{{ _save() method
	@Override
	public void _save()
	{
		jEdit.setFontProperty("view.font",font.getFont());
		jEdit.setBooleanProperty("view.enableFontSubst",fontSubst.isSelected());
		fontSubstList.save();
		jEdit.setBooleanProperty("view.enableFontSubstSystemFonts",
			fontSubstSystemFonts.isSelected());

		jEdit.setColorProperty("view.fgColor",foregroundColor
			.getSelectedColor());
		jEdit.setColorProperty("view.bgColor",backgroundColor
			.getSelectedColor());
		jEdit.setBooleanProperty("view.caretBlink",blinkCaret.isSelected());
		jEdit.setBooleanProperty("view.blockCaret",blockCaret.isSelected());
		jEdit.setBooleanProperty("view.thickCaret",thickCaret.isSelected());
		jEdit.setColorProperty("view.caretColor",caretColor
			.getSelectedColor());
		jEdit.setColorProperty("view.selectionColor",selectionColor
			.getSelectedColor());
		jEdit.setColorProperty("view.multipleSelectionColor",multipleSelectionColor
			.getSelectedColor());
		jEdit.setBooleanProperty("view.selectionFg",selectionFg.isSelected());
		jEdit.setColorProperty("view.selectionFgColor",selectionFgColor
				.getSelectedColor());
		jEdit.setBooleanProperty("view.lineHighlight",lineHighlight
			.isSelected());
		jEdit.setColorProperty("view.lineHighlightColor",
			lineHighlightColor.getSelectedColor());
		jEdit.setBooleanProperty("view.structureHighlight",structureHighlight
			.isSelected());
		jEdit.setColorProperty("view.structureHighlightColor",
			structureHighlightColor.getSelectedColor());
		jEdit.setBooleanProperty("view.eolMarkers",eolMarkers
			.isSelected());
		jEdit.setColorProperty("view.eolMarkerColor",
			eolMarkerColor.getSelectedColor());
		jEdit.setBooleanProperty("view.wrapGuide",wrapGuide
			.isSelected());
		jEdit.setColorProperty("view.wrapGuideColor",
			wrapGuideColor.getSelectedColor());
		jEdit.setBooleanProperty("view.pageBreaks", pageBreaks.isSelected());
		jEdit.setColorProperty("view.pageBreaksColor", pageBreaksColor.getSelectedColor());
		
		jEdit.setIntegerProperty("view.electricBorders",electricBorders
			.isSelected() ? 3 : 0);
		AntiAlias nv = new AntiAlias(jEdit.getProperty("view.antiAlias"));
		nv.set(antiAlias.getSelectedIndex());
		jEdit.setProperty("view.antiAlias", nv.toString());
		jEdit.setBooleanProperty("view.fracFontMetrics",fracFontMetrics.isSelected());
		jEdit.setBooleanProperty("stripTrailingEOL", stripTrailingEOL.isSelected());
		jEdit.setBooleanProperty("completeFromAllBuffers", completeFromAllBuffers.isSelected());
		jEdit.setBooleanProperty("insertCompletionWithDigit", insertCompletionWithDigit.isSelected());
		jEdit.setIntegerProperty("options.textarea.lineSpacing", Integer.parseInt(lineSpacing.getText()));
	} //}}}

	//{{{ Private members
	private FontSelector font;
	private JCheckBox fontSubst;
	private FontList fontSubstList;
	private JCheckBox fontSubstSystemFonts;
	private ColorWellButton foregroundColor;
	private ColorWellButton backgroundColor;
	private JCheckBox blinkCaret;
	private JCheckBox blockCaret;
	private JCheckBox thickCaret;
	private ColorWellButton caretColor;
	private ColorWellButton selectionColor;
	private JCheckBox selectionFg;
	private ColorWellButton selectionFgColor;
	private ColorWellButton multipleSelectionColor;
	private JCheckBox lineHighlight;
	private ColorWellButton lineHighlightColor;
	private JCheckBox structureHighlight;
	private ColorWellButton structureHighlightColor;
	private JCheckBox eolMarkers;
	private ColorWellButton eolMarkerColor;
	private JCheckBox wrapGuide;
	private ColorWellButton wrapGuideColor;
	private JCheckBox pageBreaks;
	private ColorWellButton pageBreaksColor;
	private JCheckBox electricBorders;
	private JComboBox<String> antiAlias;
	private JCheckBox fracFontMetrics;
	private JCheckBox stripTrailingEOL;
	private JCheckBox completeFromAllBuffers;
	private JCheckBox insertCompletionWithDigit;
	private JTextField lineSpacing;
	//}}}

	//{{{ FontList class
	/**
	 * The substitution font list widget. Shows a JList with the
	 * list of fonts and buttons that allow the user to manipulate
	 * the list.
	 */
	private static class FontList
		extends JPanel
		implements ActionListener
	{

		FontList()
		{
			int i = 0;

			setLayout(new BorderLayout());

			/* Label. */
			JLabel l = new JLabel(jEdit.getProperty("options.textarea.fontSubstList"));

			/* Substitution font list. */
			fontsModel = new DefaultListModel<>();
			fonts = new JList<>(fontsModel);
			fonts.setCellRenderer(new FontItemRenderer());
			Font f;
			while ((f = jEdit.getFontProperty("view.fontSubstList." + i)) != null)
			{
				fontsModel.addElement(f);
				i++;
			}

			/* Right-side button box. */
			Box buttons = new Box(BoxLayout.Y_AXIS);

			add = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.add.icon")));
			add.setToolTipText(jEdit.getProperty("common.add"));
			add.addActionListener(this);
			buttons.add(add);
			buttons.add(Box.createVerticalStrut(2));

			remove = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.remove.icon")));
			remove.setToolTipText(jEdit.getProperty("common.remove"));
			remove.addActionListener(this);
			buttons.add(remove);
			buttons.add(Box.createVerticalStrut(2));

			up = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveUp.icon")));
			up.setToolTipText(jEdit.getProperty("common.moveUp"));
			up.addActionListener(this);
			buttons.add(up);
			buttons.add(Box.createVerticalStrut(2));

			down = new RolloverButton(GUIUtilities.loadIcon(jEdit.getProperty("options.context.moveDown.icon")));
			down.setToolTipText(jEdit.getProperty("common.moveDown"));
			down.addActionListener(this);
			buttons.add(down);
			buttons.add(Box.createGlue());


			add(BorderLayout.NORTH, l);
			add(BorderLayout.CENTER, new JScrollPane(fonts));
			add(BorderLayout.EAST, buttons);
		}

		@Override
		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == add)
			{
				JDialog parent = GenericGUIUtilities.getParentDialog(this);
				Font selected =
					new FontSelectorDialog(parent, null).getSelectedFont();

				if (selected != null)
				{
					fontsModel.addElement(selected);
					fonts.setSelectedIndex(fontsModel.size() - 1);
				}
			}
			else if (ae.getSource() ==  remove)
			{
				int idx = fonts.getSelectedIndex();
				if (idx != -1)
					fontsModel.removeElementAt(idx);
			}
			else if (ae.getSource() == up)
			{
				int idx = fonts.getSelectedIndex();
				if (idx > 0)
				{
					Font font = fontsModel.getElementAt(idx);
					fontsModel.removeElementAt(idx);
					fontsModel.add(idx - 1, font);
					fonts.setSelectedIndex(idx - 1);
				}
			}
			else if (ae.getSource() == down)
			{
				int idx = fonts.getSelectedIndex();
				if (idx != -1 && idx < fontsModel.size() - 1)
				{
					Font font = fontsModel.getElementAt(idx);
					fontsModel.removeElementAt(idx);
					fontsModel.add(idx + 1, font);
					fonts.setSelectedIndex(idx + 1);
				}
			}
		}

		public void save()
		{
			int i = 0;
			while (jEdit.getFontProperty("view.fontSubstList." + i) != null)
			{
				jEdit.unsetProperty("view.fontSubstList." + i);
				i++;
			}
			for (i = 0; i < fontsModel.size(); i++)
			{
				Font f = fontsModel.getElementAt(i);
				jEdit.setFontProperty("view.fontSubstList." + i, f);
			}
		}

		public int listSize()
		{
			return fontsModel.size();
		}

		private final DefaultListModel<Font> fontsModel;
		private final JList<Font> fonts;
		private final JButton add;
		private final JButton remove;
		private final JButton up;
		private final JButton down;

		private static class FontItemRenderer extends DefaultListCellRenderer
		{
			@Override
			public Component getListCellRendererComponent(JList list,
								      Object value,
								      int index,
								      boolean isSelected,
								      boolean cellHasFocus)
			{
				Font f = (Font) value;
				super.getListCellRendererComponent(list,
								   value,
								   index,
								   isSelected,
								   cellHasFocus);
				setText(f.getFamily() + " " + f.getSize());
				return this;
			}
		}
	} //}}}
}
