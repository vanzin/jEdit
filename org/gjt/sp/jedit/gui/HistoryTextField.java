/*
 * HistoryTextField.java - Text field with a history
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
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Text field with an arrow-key accessable history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryTextField extends JTextField
{
	//{{{ HistoryTextField constructor
	/**
	 * Creates a new history text field.
	 * @since jEdit 3.2pre5
	 */
	public HistoryTextField()
	{
		this(null);
	} //}}}

	//{{{ HistoryTextField constructor
	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 */
	public HistoryTextField(String name)
	{
		this(name,false,true);
	} //}}}

	//{{{ HistoryTextField constructor
	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 * @param instantPopups If true, selecting a value from the history
	 * popup will immediately fire an ActionEvent. If false, the user
	 * will have to press 'Enter' first
	 *
	 * @since jEdit 2.2pre5
	 */
	public HistoryTextField(String name, boolean instantPopups)
	{
		this(name,instantPopups,true);
	} //}}}

	//{{{ HistoryTextField constructor
	/**
	 * Creates a new history text field.
	 * @param name The history model name
	 * @param instantPopups If true, selecting a value from the history
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
		// set sane minumum number of columns
		super(4);
		controller = new HistoryText(this,null)
		{
			public void fireActionPerformed()
			{
				HistoryTextField.this.fireActionPerformed();
			}
		};

		setModel(name);
		MouseHandler mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);

		setInstantPopups(instantPopups);
		setEnterAddsToHistory(enterAddsToHistory);
	} //}}}

	//{{{ setInstantPopups() method
	/**
	 * Sets if selecting a value from the popup should immediately fire
	 * an ActionEvent.
	 * @since jEdit 4.0pre3
	 */
	public void setInstantPopups(boolean instantPopups)
	{
		controller.setInstantPopups(instantPopups);
	} //}}}

	//{{{ getInstantPopups() method
	/**
	 * Returns if selecting a value from the popup should immediately fire
	 * an ActionEvent.
	 * @since jEdit 4.0pre3
	 */
	public boolean getInstantPopups()
	{
		return controller.getInstantPopups();
	} //}}}

	//{{{ setEnterAddsToHistory() method
	/**
	 * Sets if pressing Enter should automatically add the currently
	 * entered text to the history.
	 * @since jEdit 4.0pre3
	 */
	public void setEnterAddsToHistory(boolean enterAddsToHistory)
	{
		this.enterAddsToHistory = enterAddsToHistory;
	} //}}}

	//{{{ getEnterAddsToHistory() method
	/**
	 * Returns if pressing Enter should automatically add the currently
	 * entered text to the history.
	 * @since jEdit 4.0pre3
	 */
	public boolean setEnterAddsToHistory()
	{
		return enterAddsToHistory;
	} //}}}

	//{{{ setSelectAllOnFocus() method
	/**
	 * Sets if all text should be selected when the field gets focus.
	 * @since jEdit 4.0pre3
	 */
	public void setSelectAllOnFocus(boolean selectAllOnFocus)
	{
		this.selectAllOnFocus = selectAllOnFocus;
	} //}}}

	//{{{ getSelectAllOnFocus() method
	/**
	 * Returns if all text should be selected when the field gets focus.
	 * @since jEdit 4.0pre3
	 */
	public boolean setSelectAllOnFocus()
	{
		return selectAllOnFocus;
	} //}}}

	//{{{ getModel() method
	/**
	 * Returns the underlying history model.
	 */
	public HistoryModel getModel()
	{
		return controller.getModel();
	} //}}}

	//{{{ setModel() method
	/**
	 * Sets the history list model.
	 * @param name The model name
	 * @since jEdit 2.3pre3
	 */
	public void setModel(String name)
	{
		controller.setModel(name);

		if(name != null)
		{
			setBorder(new CompoundBorder(this.getBorder(), new HistoryBorder()));
		}
		
		repaint();
	} //}}}

	//{{{ addCurrentToHistory() method
	/**
	 * Adds the currently entered item to the history.
	 */
	public void addCurrentToHistory()
	{
		controller.addCurrentToHistory();
	} //}}}

	//{{{ setText() method
	/**
	 * Sets the displayed text.
	 */
	public void setText(String text)
	{
		super.setText(text);
		controller.setIndex(-1);
	} //}}}

	//{{{ fireActionPerformed() method
	/**
	 * Make it public.
	 */
	public void fireActionPerformed()
	{
		super.fireActionPerformed();
	} //}}}

	//{{{ Protected members

	//{{{ processKeyEvent() method
	protected void processKeyEvent(KeyEvent evt)
	{
		if(!isEnabled())
			return;

		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_ENTER:
				if(enterAddsToHistory)
					addCurrentToHistory();

				if(evt.getModifiers() == 0)
				{
					fireActionPerformed();
					evt.consume();
				}
				break;
			case KeyEvent.VK_UP:
				if(evt.isShiftDown())
					controller.doBackwardSearch();
				else
					controller.historyPrevious();
				evt.consume();
				break;
			case KeyEvent.VK_DOWN:
				if(evt.isShiftDown())
					controller.doForwardSearch();
				else if(evt.isAltDown())
				{
					controller.showPopupMenu(
						evt.isShiftDown());
				}
				else
					controller.historyNext();
				evt.consume();
				break;
			case KeyEvent.VK_TAB:
				if(evt.isControlDown())
				{
					controller.doBackwardSearch();
					evt.consume();
				}
				break;
			}
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	} //}}}

	//{{{ processMouseEvent() method
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
				controller.showPopupMenu(evt.isShiftDown());
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
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private HistoryText controller;
	private boolean enterAddsToHistory;
	private boolean selectAllOnFocus;
	//}}}

	//}}}

	//{{{ Inner classes

	//{{{ MouseHandler class
	class MouseHandler extends MouseInputAdapter
	{
		boolean selectAll;

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent evt)
		{
			selectAll = (!hasFocus() && selectAllOnFocus);
		} //}}}

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent evt)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if(selectAll)
						selectAll();
				}
			});
		} //}}}

		//{{{ mouseMoved() method
		public void mouseMoved(MouseEvent evt)
		{
			Border border = getBorder();
			Insets insets = border.getBorderInsets(HistoryTextField.this);

			if(evt.getX() >= getWidth() - insets.right)
				setCursor(Cursor.getDefaultCursor());
			else
				setCursor(Cursor.getPredefinedCursor(
					Cursor.TEXT_CURSOR));
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent evt)
		{
			selectAll = false;
		} //}}}
	} //}}}

	//{{{ HistoryBorder class
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
				? "TextField.foreground" : "TextField.disabledForeground"));
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
	} //}}}

	//}}}
}
