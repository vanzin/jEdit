/*
 * HistoryTextArea.java - Text area with a history
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2004 Slava Pestov
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
import java.util.Collections;
import org.gjt.sp.jedit.*;
//}}}

/**
 * Text area with a history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryTextArea extends JTextArea
{
	//{{{ HistoryTextArea constructor
	public HistoryTextArea(String name)
	{
		super(3,15);
		controller = new HistoryText(this,name);
		setFocusTraversalKeys(
			KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
			Collections.singleton(
				KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0)));
		setFocusTraversalKeys(
			KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
			Collections.singleton(
				KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
					InputEvent.SHIFT_MASK)));
	} //}}}

	//{{{ getModel() method
	/**
	 * Returns the underlying history controller.
	 * @since jEdit 4.3pre1
	 */
	public HistoryModel getModel()
	{
		return controller.getModel();
	} //}}}

	//{{{ setModel() method
	/**
	 * Sets the history list controller.
	 * @param name The model name
	 * @since jEdit 4.3pre1
	 */
	public void setModel(String name)
	{
		controller.setModel(name);
	} //}}}

	//{{{ setInstantPopups() method
	/**
	 * Sets if selecting a value from the popup should immediately fire
	 * an ActionEvent.
	 */
	public void setInstantPopups(boolean instantPopups)
	{
		controller.setInstantPopups(instantPopups);
	} //}}}

	//{{{ getInstantPopups() method
	/**
	 * Returns if selecting a value from the popup should immediately fire
	 * an ActionEvent.
	 */
	public boolean getInstantPopups()
	{
		return controller.getInstantPopups();
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
				if(evt.isControlDown())
				{
					replaceSelection("\n");
					evt.consume();
				}
				break;
			case KeyEvent.VK_TAB:
				if(evt.isControlDown())
				{
					replaceSelection("\t");
					evt.consume();
				}
				break;
			case KeyEvent.VK_PAGE_UP:
				if(evt.isShiftDown())
					controller.doBackwardSearch();
				else
					controller.historyPrevious();
				evt.consume();
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if(evt.isShiftDown())
					controller.doForwardSearch();
				else
					controller.historyNext();
				evt.consume();
				break;
			case KeyEvent.VK_UP:
				if(evt.isAltDown())
				{
					controller.showPopupMenu(
						evt.isShiftDown());
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
			if(GUIUtilities.isPopupTrigger(evt))
				controller.showPopupMenu(evt.isShiftDown());
			else
				super.processMouseEvent(evt);

			break;
		default:
			super.processMouseEvent(evt);
			break;
		}
	} //}}}
	
	//}}}

	//{{{ Private variables
	private HistoryText controller;
	//}}}
}
