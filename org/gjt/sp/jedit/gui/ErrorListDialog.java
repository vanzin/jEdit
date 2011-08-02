/*
 * ErrorListDialog.java - Used to list I/O and plugin load errors
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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

//{{{ Imports
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.pluginmgr.PluginManager;

public class ErrorListDialog extends EnhancedDialog
{
	//{{{ ErrorListDialog constructor
	public ErrorListDialog(Frame frame, String title, String caption,
		Vector<ErrorEntry> messages, boolean pluginError)
	{
		super(frame,title,!pluginError);

		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(UIManager.getIcon("OptionPane.errorIcon")));
		iconBox.add(Box.createGlue());
		content.add(BorderLayout.WEST,iconBox);

		JPanel centerPanel = new JPanel(new BorderLayout());

		JLabel label = new JLabel(caption);
		label.setBorder(new EmptyBorder(0,0,6,0));
		centerPanel.add(BorderLayout.NORTH,label);

		JTextPane errors = new JTextPane();
		errors.setEditable(false);
		errors.setForeground(jEdit.getColorProperty("view.fgColor"));
		errors.setBackground(jEdit.getColorProperty("view.bgColor"));
		errors.setCaretColor(jEdit.getColorProperty("view.caretColor"));
		errors.setSelectionColor(jEdit.getColorProperty("view.selectionColor"));
		StyledDocument doc = errors.getStyledDocument();
		Font plainFont = new JLabel().getFont();
		SimpleAttributeSet plainFontAttrSet = new SimpleAttributeSet();
		StyleConstants.setFontFamily(plainFontAttrSet, plainFont.getFamily());
		SimpleAttributeSet boldFontAttrSet = (SimpleAttributeSet) plainFontAttrSet.clone();
		StyleConstants.setBold(boldFontAttrSet, true);
		for (ErrorEntry entry : messages)
		{
			try
			{
				doc.insertString(doc.getLength(), entry.path() + ":\n", boldFontAttrSet);
				for (String s: entry.messages())
					doc.insertString(doc.getLength(), s + "\n", plainFontAttrSet);
			}
			catch (BadLocationException e)
			{
			}
		}

		// need this bullshit scroll bar policy for the preferred size
		// hack to work
		JScrollPane scrollPane = new JScrollPane(errors,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		Dimension size = scrollPane.getPreferredSize();
		size.width = Math.min(size.width,400);
		scrollPane.setPreferredSize(size);

		centerPanel.add(BorderLayout.CENTER,scrollPane);

		content.add(BorderLayout.CENTER,centerPanel);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());

		if(pluginError)
		{
			pluginMgr = new JButton(jEdit.getProperty("error-list.plugin-manager"));
			pluginMgr.addActionListener(new ActionHandler());
			buttons.add(pluginMgr);
			buttons.add(Box.createHorizontalStrut(6));
		}

		buttons.add(ok);

		buttons.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttons);

		getRootPane().setDefaultButton(ok);

		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	} //}}}

	//{{{ ok() method
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members
	private JButton ok, pluginMgr;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				dispose();
			else if(evt.getSource() == pluginMgr)
			{
				PluginManager.showPluginManager(JOptionPane.getFrameForComponent(
					ErrorListDialog.this));
			}
		} //}}}
	} //}}}
}
