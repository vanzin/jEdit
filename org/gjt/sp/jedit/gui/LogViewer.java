/*
 * LogViewer.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

public class LogViewer extends JPanel
{
	//{{{ LogViewer constructor
	public LogViewer()
	{
		super(new BorderLayout());

		Box captionBox = Box.createHorizontalBox();

		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			String[] args = { MiscUtilities.constructPath(
				settingsDirectory, "activity.log") };
			JLabel label = new JLabel(jEdit.getProperty(
				"log-viewer.caption",args));
			captionBox.add(label);
		}

		captionBox.add(Box.createHorizontalGlue());

		tailIsOn = jEdit.getBooleanProperty("log-viewer.tail", false);
		tail = new JCheckBox(
			jEdit.getProperty("log-viewer.tail.label"),tailIsOn);
		tail.addActionListener(new ActionHandler());
		captionBox.add(tail);

		textArea = new JTextArea(24,80);
		textArea.setDocument(Log.getLogDocument());
		textArea.getDocument().addDocumentListener(
			new DocumentHandler());
		//textArea.setEditable(false);

		add(BorderLayout.NORTH,captionBox);
		add(BorderLayout.CENTER,new JScrollPane(textArea));
	} //}}}

	//{{{ requestDefaultFocus() method
	public boolean requestDefaultFocus()
	{
		textArea.requestFocus();
		return true;
	} //}}}

	//{{{ Private members
	private JTextArea textArea;
	private JCheckBox tail;
	private boolean tailIsOn;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			tailIsOn = !tailIsOn;
			jEdit.setBooleanProperty("log-viewer.tail",tailIsOn);
			if(tailIsOn)
				textArea.setCaretPosition(
					textArea.getDocument().getLength());
		}
	} //}}}

	//{{{ DocumentHandler class
	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent e)
		{
			if(tailIsOn)
				textArea.setCaretPosition(
					textArea.getDocument().getLength());
		}

		public void changedUpdate(DocumentEvent e) {}
		public void removeUpdate(DocumentEvent e) {}
	} //}}}
}
