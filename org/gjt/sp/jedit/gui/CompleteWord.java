/*
 * CompleteWord.java - Complete word dialog
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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class CompleteWord extends JWindow
{
	public CompleteWord(View view, String word, Vector completions,
		Point location)
	{
		super(view);

		this.view = view;
		this.word = word;

		words = new JList(completions);

		words.setVisibleRowCount(Math.min(completions.size(),8));

		words.addMouseListener(new MouseHandler());
		words.setSelectedIndex(0);
		words.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// stupid scrollbar policy is an attempt to work around
		// bugs people have been seeing with IBM's JDK -- 7 Sep 2000
		JScrollPane scroller = new JScrollPane(words,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		getContentPane().add(scroller, BorderLayout.CENTER);

		GUIUtilities.requestFocus(this,words);

		pack();
		setLocation(location);
		show();

		KeyHandler keyHandler = new KeyHandler();
		addKeyListener(keyHandler);
		getRootPane().addKeyListener(keyHandler);
		words.addKeyListener(keyHandler);
		view.setKeyEventInterceptor(keyHandler);
	}

	public void dispose()
	{
		view.setKeyEventInterceptor(null);
		super.dispose();
	}

	// private members
	private View view;
	private String word;
	private JList words;

	private void insertSelected()
	{
		view.getTextArea().setSelectedText(((String)words
			.getSelectedValue()).substring(word.length()));
		dispose();
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_TAB:
			case KeyEvent.VK_ENTER:
				insertSelected();
				evt.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				dispose();
				evt.consume();
				break;
			case KeyEvent.VK_UP:
				if(getFocusOwner() == words)
					return;

				int selected = words.getSelectedIndex();
				if(selected == 0)
					return;

				selected = selected - 1;
	
				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			case KeyEvent.VK_DOWN:
				if(getFocusOwner() == words)
					return;

				selected = words.getSelectedIndex();
				if(selected == words.getModel().getSize() - 1)
					return;

				selected = selected + 1;

				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			default:
				dispose();
				view.processKeyEvent(evt);
				break;
			}
		}

		public void keyTyped(KeyEvent evt)
		{
			evt = KeyEventWorkaround.processKeyEvent(evt);
			if(evt == null)
				return;
			else
			{
				dispose();
				view.processKeyEvent(evt);
			}
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			insertSelected();
		}
	}
}
