/*
 * AboutDialog.java - About jEdit dialog box
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class AboutDialog extends EnhancedDialog
{
	public AboutDialog(View view)
	{
		super(view,jEdit.getProperty("about.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(Color.white);
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(Color.white);
		String[] args = { jEdit.getVersion() };
		JLabel label = new JLabel(jEdit.getProperty("about.version",args),
			SwingConstants.CENTER);
		label.setBorder(new EmptyBorder(0,0,12,0));
		panel.add(BorderLayout.NORTH,label);

		JLabel splash = new JLabel(new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/jedit_logo.gif")));
		//splash.setBorder(new MatteBorder(1,1,1,1,Color.black));
		panel.add(BorderLayout.CENTER,splash);

		label = new JLabel(jEdit.getProperty("about.caption"),
			SwingConstants.CENTER);
		label.setBorder(new EmptyBorder(12,0,12,0));
		panel.add(BorderLayout.SOUTH,label);

		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(close);
		box.add(close);
		box.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,box);

		pack();
		setResizable(false);
		setLocationRelativeTo(view);
		show();
	}

	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	// private members
	private JButton close;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			dispose();
		}
	}
}
