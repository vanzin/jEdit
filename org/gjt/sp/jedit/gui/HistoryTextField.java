/*
 * HistoryTextField.java - Text field with a history
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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

/**
 * Text field with an arrow-key accessable history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryTextField extends JTextField
{
	/**
	 * Creates a new history text field.
	 * @since jEdit 3.2pre5
	 */
	public HistoryTextField()
	{
		this(null);
	}

	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 */
	public HistoryTextField(String name)
	{
		this(name,false,true);
	}

	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 * @param instantPopup If true, selecting a value from the history
	 * popup will immediately fire an ActionEvent. If false, the user
	 * will have to press 'Enter' first
	 *
	 * @since jEdit 2.2pre5
	 */
	public HistoryTextField(String name, boolean instantPopups)
	{
		this(name,instantPopups,true);
	}

	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 * @param instantPopup If true, selecting a value from the history
	 * popup will immediately fire an ActionEvent. If false, the user
	 * will have to press 'Enter' first
	 * @param enterAddsToHistory If true, pressing the Enter key will
	 * automatically add the currently entered text to the history.
	 *
	 * @since jEdit 2.6pre5
	 */
	public HistoryTextField(String name, boolean instantPopups,
		boolean enterAddsToHistory)
	{
		setBorder(new CompoundBorder(getBorder(),new HistoryBorder()));

		if(name != null)
			historyModel = HistoryModel.getModel(name);

		addMouseMotionListener(new MouseHandler());

		this.instantPopups = instantPopups;
		this.enterAddsToHistory = enterAddsToHistory;

		index = -1;
	}

	/**
	 * Sets the history list model.
	 * @param name The model name
	 * @since jEdit 2.3pre3
	 */
	public void setModel(String name)
	{
		if(name == null)
			historyModel = null;
		else
			historyModel = HistoryModel.getModel(name);
		index = -1;
		repaint();
	}

	/**
	 * Adds the currently entered item to the history.
	 */
	public void addCurrentToHistory()
	{
		if(historyModel != null)
			historyModel.addItem(getText());
		index = 0;
	}

	/**
	 * Sets the displayed text.
	 */
	public void setText(String text)
	{
		super.setText(text);
		index = -1;
	}

	/**
	 * Returns the underlying history model.
	 */
	public HistoryModel getModel()
	{
		return historyModel;
	}

	/**
	 * Fires an action event to all listeners. This is public so
	 * that inner classes can access it.
	 */
	public void fireActionPerformed()
	{
		super.fireActionPerformed();
	}

	// protected members
	protected void processKeyEvent(KeyEvent evt)
	{
		if(!isEnabled())
			return;

		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				if(enterAddsToHistory)
					addCurrentToHistory();

				if(evt.getModifiers() == 0)
				{
					fireActionPerformed();
					evt.consume();
				}
			}
			else if(evt.getKeyCode() == KeyEvent.VK_UP)
			{
				if(evt.isShiftDown())
					doBackwardSearch();
				else
					historyPrevious();
				evt.consume();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_DOWN)
			{
				if(evt.isShiftDown())
					doForwardSearch();
				else
					historyNext();
				evt.consume();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_TAB
				&& evt.isControlDown())
			{
				doBackwardSearch();
				evt.consume();
			}
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	}

	protected void processMouseEvent(MouseEvent evt)
	{
		if(!isEnabled())
			return;

		switch(evt.getID())
		{
		case MouseEvent.MOUSE_PRESSED:
			Border border = getBorder();
			Insets insets = border.getBorderInsets(HistoryTextField.this);

			if(evt.getX() >= getWidth() - insets.right
				|| GUIUtilities.isPopupTrigger(evt))
			{
				if(evt.isShiftDown())
					showPopupMenu(getText().substring(0,
						getSelectionStart()),0,getHeight());
				else
					showPopupMenu("",0,getHeight());
			}
			else
				super.processMouseEvent(evt);

			break;
		case MouseEvent.MOUSE_EXITED:
			setCursor(Cursor.getDefaultCursor());
			super.processMouseEvent(evt);
			break;
		default:
			super.processMouseEvent(evt);
			break;
		}
	}

	// private members
	private HistoryModel historyModel;
	private JPopupMenu popup;
	private boolean instantPopups;
	private boolean enterAddsToHistory;
	private String current;
	private int index;

	private void doBackwardSearch()
	{
		if(historyModel == null)
			return;

		if(getSelectionEnd() != getDocument().getLength())
		{
			setCaretPosition(getDocument().getLength());
		}

		String text = getText().substring(0,getSelectionStart());
		if(text == null)
		{
			historyPrevious();
			return;
		}

		for(int i = index + 1; i < historyModel.getSize(); i++)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				replaceSelection(item.substring(text.length()));
				select(text.length(),getDocument().getLength());
				index = i;
				return;
			}
		}

		getToolkit().beep();
	}

	private void doForwardSearch()
	{
		if(historyModel == null)
			return;

		if(getSelectionEnd() != getDocument().getLength())
		{
			setCaretPosition(getDocument().getLength());
		}

		String text = getText().substring(0,getSelectionStart());
		if(text == null)
		{
			historyNext();
			return;
		}

		for(int i = index - 1; i >= 0; i--)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				replaceSelection(item.substring(text.length()));
				select(text.length(),getDocument().getLength());
				index = i;
				return;
			}
		}

		getToolkit().beep();
	}

	private void historyPrevious()
	{
		if(historyModel == null)
			return;

		if(index == historyModel.getSize() - 1)
			getToolkit().beep();
		else if(index == -1)
		{
			current = getText();
			setText(historyModel.getItem(0));
			index = 0;
		}
		else
		{
			// have to do this because setText() sets index to -1
			int newIndex = index + 1;
			setText(historyModel.getItem(newIndex));
			index = newIndex;
		}
	}

	private void historyNext()
	{
		if(historyModel == null)
			return;

		if(index == -1)
			getToolkit().beep();
		else if(index == 0)
			setText(current);
		else
		{
			// have to do this because setText() sets index to -1
			int newIndex = index - 1;
			setText(historyModel.getItem(newIndex));
			index = newIndex;
		}
	}

	private void showPopupMenu(String text, int x, int y)
	{
		if(historyModel == null)
			return;

		requestFocus();

		if(popup != null && popup.isVisible())
		{
			popup.setVisible(false);
			return;
		}

		ActionHandler actionListener = new ActionHandler();

		popup = new JPopupMenu();
		JMenuItem caption = new JMenuItem(jEdit.getProperty(
			"history.caption"));
		caption.getModel().setEnabled(false);
 		popup.add(caption);
 		popup.addSeparator();

		for(int i = 0; i < historyModel.getSize(); i++)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				JMenuItem menuItem = new JMenuItem(item);
				menuItem.setActionCommand(String.valueOf(i));
				menuItem.addActionListener(actionListener);
				popup.add(menuItem);
			}
		}

		GUIUtilities.showPopupMenu(popup,this,x,y);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			int ind = Integer.parseInt(evt.getActionCommand());
			if(ind == -1)
			{
				if(index != -1)
					setText(current);
			}
			else
			{
				setText(historyModel.getItem(ind));
				index = ind;
			}
			if(instantPopups)
			{
				addCurrentToHistory();
				fireActionPerformed();
			}
		}
	}

	class MouseHandler extends MouseMotionAdapter
	{
		public void mouseMoved(MouseEvent evt)
		{
			Border border = getBorder();
			Insets insets = border.getBorderInsets(HistoryTextField.this);

			if(evt.getX() >= getWidth() - insets.right)
				setCursor(Cursor.getDefaultCursor());
			else
				setCursor(Cursor.getPredefinedCursor(
					Cursor.TEXT_CURSOR));
		}
	}

	static class HistoryBorder extends AbstractBorder
	{
		static final int WIDTH = 16;

		public void paintBorder(Component c, Graphics g,
			int x, int y, int w, int h)
		{
			g.translate(x+w-WIDTH,y-1);

			//if(c.isEnabled())
			//{
			//	// vertical separation line
			//	g.setColor(UIManager.getColor("controlDkShadow"));
			//	g.drawLine(0,0,0,h);
			//}

			// down arrow
			int w2 = WIDTH/2;
			int h2 = h/2;
			g.setColor(UIManager.getColor(c.isEnabled()
				&& ((HistoryTextField)c).getModel() != null
				? "Menu.foreground" : "Menu.disabledForeground"));
			g.drawLine(w2-5,h2-2,w2+4,h2-2);
			g.drawLine(w2-4,h2-1,w2+3,h2-1);
			g.drawLine(w2-3,h2  ,w2+2,h2  );
			g.drawLine(w2-2,h2+1,w2+1,h2+1);
			g.drawLine(w2-1,h2+2,w2  ,h2+2);

			g.translate(-(x+w-WIDTH),-(y-1));
		}

		public Insets getBorderInsets(Component c)
		{
			return new Insets(0,0,0,WIDTH);
		}
	}
}
