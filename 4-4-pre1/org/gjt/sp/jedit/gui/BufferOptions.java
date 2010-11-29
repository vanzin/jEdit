/*
 * BufferOptions.java - Buffer-specific options dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Arrays;

import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.options.BufferOptionPane;

import org.gjt.sp.jedit.*;

//}}}

/**

 * Buffer-specific options dialog.
 * @author Slava Pestov
 * @version $Id$
 * 
 */

public class BufferOptions extends EnhancedDialog
{
	//{{{ BufferOptions constructor

	public BufferOptions(View view, Buffer buffer)
	{
		super(view,jEdit.getProperty("buffer-options.title"),true);
		this.view = view;
		this.buffer = buffer;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		ActionHandler actionListener = new ActionHandler();
		panel = new BufferOptionPane();

		content.add(BorderLayout.NORTH,panel);

		//{{{ Buttons

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		buttons.add(ok);

		buttons.add(Box.createHorizontalStrut(6));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionListener);
		buttons.add(cancel);

		buttons.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttons);

		//}}}

		pack();
		setLocationRelativeTo(view);
		setVisible(true);
	} //}}}



	//{{{ ok() method

	public void ok()
	{
		panel.save();
		dispose();
	} //}}}

	//{{{ cancel() method

	public void cancel()
	{
		dispose();
	} //}}}

	//{{{ Private members

	private View view;
	private Buffer buffer;
	private BufferOptionPane panel;
	private JButton ok;
	private JButton cancel;

	//{{{ ActionHandler class

	class ActionHandler implements ActionListener
	{
		//{{{ actionPerformed() method
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
		} //}}}

	} //}}}

	//}}}
}

