/*
 * StyleEditor.java - Style editor dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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
package org.gjt.sp.jedit.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.DefaultTokenHandler;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.buffer.JEditBuffer;

//{{{ StyleEditor class
public class StyleEditor extends EnhancedDialog implements ActionListener
{
	//{{{ invokeForCaret() method
	/**
	 * Edit the syntax style of the token under the caret.
	 *
	 * @param textArea the textarea where your caret is
	 * @since jEdit 4.4pre1
	 */
	public static void invokeForCaret(JEditTextArea textArea)
	{
		JEditBuffer buffer = textArea.getBuffer();
		int lineNum = textArea.getCaretLine();
		int start = buffer.getLineStartOffset(lineNum);
		int position = textArea.getCaretPosition();

		DefaultTokenHandler tokenHandler = new DefaultTokenHandler();
		buffer.markTokens(lineNum,tokenHandler);
		Token token = tokenHandler.getTokens();

		while(token.id != Token.END)
		{
			int next = start + token.length;
			if (start <= position && next > position)
				break;
			start = next;
			token = token.next;
		}
		if (token.id == Token.END || token.id == Token.NULL)
		{
			JOptionPane.showMessageDialog(textArea.getView(),
				jEdit.getProperty("syntax-style-no-token.message"),
				jEdit.getProperty("syntax-style-no-token.title"),
				JOptionPane.PLAIN_MESSAGE);
			return;
		}
		String typeName = Token.tokenToString(token.id);
		String property = "view.style." + typeName.toLowerCase();
		SyntaxStyle currentStyle = GUIUtilities.parseStyle(
				jEdit.getProperty(property), "Dialog",12);
		SyntaxStyle style = new StyleEditor(textArea.getView(),
				currentStyle, typeName).getStyle();
		if(style != null)
		{
			jEdit.setProperty(property, GUIUtilities.getStyleString(style));
			jEdit.propertiesChanged();
		}
	} //}}}

	//{{{ StyleEditor constructor
	public StyleEditor(JDialog parent, SyntaxStyle style, String styleName)
	{
		super(parent, jEdit.getProperty("style-editor.title"),true);
		initialize(parent, style, styleName);
	}
	public StyleEditor(JFrame parent, SyntaxStyle style, String styleName)
	{
		super(parent, jEdit.getProperty("style-editor.title"),true);
		initialize(parent, style, styleName);
	}
	private void initialize(Component comp, SyntaxStyle style, String styleName)
	{
		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel panel = new JPanel(new GridLayout(5,2,12,12));

		panel.add(new JLabel(jEdit.getProperty("style-editor.tokenType")));
		panel.add(new JLabel(styleName));
		
		italics = new JCheckBox(jEdit.getProperty("style-editor.italics"));
		italics.setSelected(style.getFont().isItalic());
		panel.add(italics);
		panel.add(new JLabel());

		bold = new JCheckBox(jEdit.getProperty("style-editor.bold"));
		bold.setSelected(style.getFont().isBold());
		panel.add(bold);
		panel.add(new JLabel());

		Color fg = style.getForegroundColor();

		fgColorCheckBox = new JCheckBox(jEdit.getProperty("style-editor.fgColor"));
		fgColorCheckBox.setSelected(fg != null);
		fgColorCheckBox.addActionListener(this);
		panel.add(fgColorCheckBox);

		fgColor = new ColorWellButton(fg);
		fgColor.setEnabled(fg != null);
		panel.add(fgColor);

		Color bg = style.getBackgroundColor();
		bgColorCheckBox = new JCheckBox(jEdit.getProperty("style-editor.bgColor"));
		bgColorCheckBox.setSelected(bg != null);
		bgColorCheckBox.addActionListener(this);
		panel.add(bgColorCheckBox);

		bgColor = new ColorWellButton(bg);
		bgColor.setEnabled(bg != null);
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

		pack();
		setLocationRelativeTo(comp);

		setResizable(false);
		setVisible(true);
	} //}}}

	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
		else if(source == fgColorCheckBox)
			fgColor.setEnabled(fgColorCheckBox.isSelected());
		else if(source == bgColorCheckBox)
			bgColor.setEnabled(bgColorCheckBox.isSelected());
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		okClicked = true;
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ getStyle() method
	public SyntaxStyle getStyle()
	{
		if(!okClicked)
			return null;

		Color foreground = (fgColorCheckBox.isSelected()
			? fgColor.getSelectedColor()
			: null);

		Color background = (bgColorCheckBox.isSelected()
			? bgColor.getSelectedColor()
			: null);

		return new SyntaxStyle(foreground,background,
				new Font("Dialog",
				(italics.isSelected() ? Font.ITALIC : 0)
				| (bold.isSelected() ? Font.BOLD : 0),
				12));
	} //}}}

	//{{{ Private members
	private JCheckBox italics;
	private JCheckBox bold;
	private JCheckBox fgColorCheckBox;
	private ColorWellButton fgColor;
	private JCheckBox bgColorCheckBox;
	private ColorWellButton bgColor;
	private JButton ok;
	private JButton cancel;
	private boolean okClicked;
	//}}}
} //}}}
