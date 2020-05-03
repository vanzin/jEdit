/*
 * ErrorListDialog.java - Used to list I/O and plugin load errors
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.pluginmgr.PluginManager;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * Used to list I/O and plugin load errors
 */
public class ErrorListDialog extends EnhancedDialog
{
	//{{{ ErrorEntry class
	public static class ErrorEntry
	{
		String path;
		String[] messages;
		
		/** An entry with default urgency <code>Log.ERROR</code> */
		public ErrorEntry(String path, String messageProp, Object[] args)
		{
			this(path, messageProp, args, Log.ERROR);
		}
		
		/** @since 5.0pre1 */
		public ErrorEntry(String path, String messageProp, Object[] args,
		                  int urgency)
		{
			this.path = path;

			String message = jEdit.getProperty(messageProp,args);
			if(message == null)
				message = "Undefined property: " + messageProp;

			Log.log(urgency, this, path + ":");
			Log.log(urgency, this, message);

			Collection<String> tokenizedMessage = new ArrayList<>();
			int lastIndex = -1;
			for(int i = 0; i < message.length(); i++)
			{
				if(message.charAt(i) == '\n')
				{
					tokenizedMessage.add(message.substring(lastIndex + 1,i));
					lastIndex = i;
				}
			}

			if(lastIndex != message.length())
			{
				tokenizedMessage.add(message.substring(lastIndex + 1));
			}


			messages = tokenizedMessage.toArray(StandardUtilities.EMPTY_STRING_ARRAY);
		}

		public boolean equals(Object o)
		{
			if(o instanceof ErrorEntry)
			{
				ErrorEntry e = (ErrorEntry)o;
				return e.path.equals(path);
			}
			else
				return false;
		}

		// This enables users to copy the error messages to
		// clipboard with Ctrl+C on Windows. But it works only
		// if the entry is selected by a mouse click.
		public String toString()
		{
			return path + ":\n" + String.join("\n", messages);
		}
	} //}}}

	//{{{ JTextPaneSized class
	/**
	 * This text pane sets its size to a constant amount of 80x25 chars,
	 * when used inside a scrollpane.
	 */
	protected static class JTextPaneSized extends JTextPane
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			FontMetrics metrics = getFontMetrics(getFont());
			int width = 80 * metrics.charWidth('X');
			int height = 25 * metrics.getHeight();
			return new Dimension(width, height);
		}
	} //}}}

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

		JTextPane errors = new JTextPaneSized();
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
				doc.insertString(doc.getLength(), entry.path + ":\n", boldFontAttrSet);
				for (String s: entry.messages)
					doc.insertString(doc.getLength(), s + "\n", plainFontAttrSet);
			}
			catch (BadLocationException e)
			{
			}
		}

		JScrollPane scrollPane = new JScrollPane(errors);
		centerPanel.add(BorderLayout.CENTER,scrollPane);

		content.add(BorderLayout.CENTER,centerPanel);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());

		JButton ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(e -> dispose());

		if(pluginError)
		{
			JButton pluginMgr = new JButton(jEdit.getProperty("error-list.plugin-manager"));
			pluginMgr.addActionListener(e -> PluginManager.showPluginManager(JOptionPane.getFrameForComponent(this)));
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
	@Override
	public void ok()
	{
		dispose();
	} //}}}

	//{{{ cancel() method
	@Override
	public void cancel()
	{
		dispose();
	} //}}}
}
