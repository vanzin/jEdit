/*
 * EditAbbrevDialog.java - Displayed when editing abbrevs
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

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class EditAbbrevDialog extends JDialog
{
	public EditAbbrevDialog(Component comp, String abbrev, String expansion)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("edit-abbrev.title"),true);

		this.comp = comp;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"edit-abbrev.caption", new String[] { abbrev })));
		editor = new AbbrevEditor();
		editor.setExpansion(expansion);
		editor.setBorder(new EmptyBorder(0,0,12,0));
		content.add(BorderLayout.CENTER,editor);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		box.add(ok);
		box.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		box.add(cancel);
		box.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,box);

		KeyListener listener = new KeyHandler();
		addKeyListener(listener);
		editor.getBeforeCaretTextArea().addKeyListener(listener);
		editor.getAfterCaretTextArea().addKeyListener(listener);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GUIUtilities.requestFocus(this,editor.getBeforeCaretTextArea());
		pack();
		setLocationRelativeTo(comp);
		show();
	}

	public String getExpansion()
	{
		if(!isOK)
			return null;

		return editor.getExpansion();
	}

	// private members
	private Component comp;
	private AbbrevEditor editor;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				isOK = true;

			dispose();
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
				dispose();
		}
	}
}
