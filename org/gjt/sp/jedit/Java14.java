/*
 * Java14.java - Java 2 version 1.4 API calls
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
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

package org.gjt.sp.jedit;

//{{{ Imports
import javax.swing.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.util.Log;
//}}}

/**
 * This file must be compiled with a JDK 1.4 or higher javac. If you are using
 * an older Java version and wish to compile from source, you can safely leave
 * this file out.
 * @since jEdit 4.0pre4
 * @author Slava Pestov
 * @version $Id$
 */
class Java14
{
	//{{{ init() method
	public static void init()
	{
		JFrame.setDefaultLookAndFeelDecorated(
			jEdit.getBooleanProperty("decorate.frames"));
		JDialog.setDefaultLookAndFeelDecorated(
			jEdit.getBooleanProperty("decorate.dialogs"));

		KeyboardFocusManager.setCurrentKeyboardFocusManager(
			new MyFocusManager());

		EditBus.addToBus(new EBComponent()
		{
			public void handleMessage(EBMessage msg)
			{
				if(msg instanceof ViewUpdate)
				{
					ViewUpdate vu = (ViewUpdate)msg;
					if(vu.getWhat() == ViewUpdate.CREATED)
					{
						vu.getView().setFocusTraversalPolicy(
							new MyFocusTraversalPolicy());
					}
				}
				else if(msg instanceof EditPaneUpdate)
				{
					EditPaneUpdate eu = (EditPaneUpdate)msg;
					if(eu.getWhat() == EditPaneUpdate.CREATED)
					{
						eu.getEditPane().getTextArea()
							.addMouseWheelListener(
							new MouseWheelHandler());
					}
				}
			}
		});

		Clipboard selection = Toolkit.getDefaultToolkit().getSystemSelection();
		if(selection != null)
		{
			Log.log(Log.DEBUG,Java14.class,"Setting % register"
				+ " to system selection");
			Registers.setRegister('%',new Registers.ClipboardRegister(selection));
		}
	} //}}}

	//{{{ MyFocusManager class
	static class MyFocusManager extends DefaultKeyboardFocusManager
	{
		MyFocusManager()
		{
			setDefaultFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
		}

		public boolean postProcessKeyEvent(KeyEvent evt)
		{
			if(!evt.isConsumed())
			{
				Component comp = (Component)evt.getSource();
				if(!comp.isShowing())
					return true;

				for(;;)
				{
					if(comp instanceof View)
					{
						((View)comp).processKeyEvent(evt);
						return true;
					}
					else if(comp == null || comp instanceof Window
						|| comp instanceof JEditTextArea)
					{
						break;
					}
					else
						comp = comp.getParent();
				}
			}

			return super.postProcessKeyEvent(evt);
		}
	} //}}}

	//{{{ MyFocusTraversalPolicy class
	static class MyFocusTraversalPolicy extends LayoutFocusTraversalPolicy
	{
		public Component getDefaultComponent(Container focusCycleRoot)
		{
			return GUIUtilities.getView(focusCycleRoot).getTextArea();
		}
	} //}}}

	//{{{ WheelScrollListener class
	static class MouseWheelHandler implements MouseWheelListener
	{
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			JEditTextArea textArea = (JEditTextArea)e.getSource();

			/****************************************************
			 * move caret depending on pressed control-keys:
			 * - Alt: move cursor, do not select
			 * - Alt+(shift or control): move cursor, select
			 * - shift: scroll page
			 * - control: scroll single line
			 * - <else>: scroll 3 lines
			 ****************************************************/
			if(e.isAltDown())
			{
				moveCaret(textArea,e.getWheelRotation(),
					e.isShiftDown() || e.isControlDown());
			}
			else if(e.isShiftDown())
				scrollPage(textArea,e.getWheelRotation());
			else if(e.isControlDown())
				scrollLine(textArea,e.getWheelRotation());
			else if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
				scrollLine(textArea,e.getUnitsToScroll());
			else
				scrollLine(textArea,3 * e.getWheelRotation());
		}

		private void scrollLine(JEditTextArea textArea, int amt)
		{
			int newpos = textArea.getFirstLine() + amt;
			newpos = Math.max(newpos, 0);
			newpos = Math.min(newpos, textArea.getDisplayManager()
				.getScrollLineCount());
			textArea.setFirstLine(newpos);
		}

		private void scrollPage(JEditTextArea textArea, int amt)
		{
			if(amt > 0)
				textArea.scrollDownPage();
			else
				textArea.scrollUpPage();
		}

		private void moveCaret(JEditTextArea textArea, int amt, boolean select)
		{
			if (amt < 0)
				textArea.goToPrevLine(select);
			else
				textArea.goToNextLine(select);
		}
	} //}}}
}
