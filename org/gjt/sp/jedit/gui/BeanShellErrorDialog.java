/*
 * BeanShellErrorDialog.java - BeanShell execution error dialog box
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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

public class BeanShellErrorDialog extends EnhancedDialog
{
	public BeanShellErrorDialog(View view, String message)
	{
		super(view,jEdit.getProperty("beanshell-error.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel caption = new JPanel(new GridLayout(2,1,3,3));
		caption.setBorder(new EmptyBorder(0,0,3,0));
		caption.add(new JLabel(jEdit.getProperty("beanshell-error.message.1")));
		caption.add(new JLabel(jEdit.getProperty("beanshell-error.message.2")));
		content.add(BorderLayout.NORTH,caption);

		JTextArea textArea = new JTextArea(10,60);
		textArea.setText(message);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		content.add(BorderLayout.CENTER,new JScrollPane(textArea));

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		JButton ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		panel.add(ok);
		panel.add(Box.createGlue());
		content.add(panel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(ok);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			dispose();
		}
	}
}
