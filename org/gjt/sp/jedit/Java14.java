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
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.*;
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
public class Java14
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
						initTextArea(eu.getEditPane()
							.getTextArea());
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

	//{{{ dragAndDropCallback() method
	/**
	 * Called by the text area via reflection to initiate a text drag and
	 * drop operation using the JDK 1.4 API.
	 * @since jEdit 4.2pre5
	 */
	public static void dragAndDropCallback(JEditTextArea textArea,
		InputEvent evt, boolean copy)
	{
		Log.log(Log.DEBUG,Java14.class,"Drag and drop callback");
		TransferHandler handler = textArea.getTransferHandler();
		handler.exportAsDrag(textArea,evt,
			copy ? TransferHandler.COPY
			: TransferHandler.MOVE);
	} //}}}

	//{{{ initTextArea() method
	static void initTextArea(JEditTextArea textArea)
	{
		textArea.addMouseWheelListener(new MouseWheelHandler());

		// drag and drop support
		// I'd just move the code to
		// JEditTextArea but it
		// depends on JDK 1.4 APIs
		textArea.setTransferHandler(new TextAreaTransferHandler());

		try
		{
			textArea.getDropTarget().addDropTargetListener(
				new DropHandler(textArea));
			textArea.setDragAndDropCallback(
				Java14.class.getMethod("dragAndDropCallback",
				new Class[] { JEditTextArea.class,
				InputEvent.class, boolean.class }));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,Java14.class,e);
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
						((View)comp).processKeyEvent(evt,
							View.VIEW);
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

	//{{{ MouseWheelHandler class
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
			textArea.setFirstLine(textArea.getFirstLine() + amt);
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

	//{{{ TextAreaTransferHandler class
	static class TextAreaTransferHandler extends TransferHandler
	{
		protected Transferable createTransferable(JComponent c)
		{
			JEditTextArea textArea = (JEditTextArea)c;
			if(textArea.getSelectionCount() == 0)
				return null;
			else
			{
				return new TextAreaSelection(textArea);
			}
		}

		public int getSourceActions(JComponent c)
		{
			return COPY_OR_MOVE;
		}

		public boolean importData(JComponent c, Transferable t)
		{
			Log.log(Log.DEBUG,this,"Import data");
			if(!canImport(c,t.getTransferDataFlavors()))
				return false;

			try
			{
				if(t.isDataFlavorSupported(
					DataFlavor.javaFileListFlavor))
				{
					Log.log(Log.DEBUG,this,"=> File list");
					EditPane editPane = (EditPane)
						GUIUtilities.getComponentParent(
						c,EditPane.class);

					Buffer buffer = null;

					Object data = t.getTransferData(
						DataFlavor.javaFileListFlavor);

					Iterator iterator = ((List)data)
						.iterator();

					while(iterator.hasNext())
					{
						File file = (File)
							iterator.next();
						Buffer _buffer = jEdit.openFile(null,
							file.getPath());
						if(_buffer != null)
							buffer = _buffer;
					}

					if(buffer != null)
						editPane.setBuffer(buffer);
					editPane.getView().toFront();
					editPane.getView().requestFocus();
					editPane.requestFocus();
				}
				else
				{
					Log.log(Log.DEBUG,this,"=> String");
					String str = (String)t.getTransferData(
						DataFlavor.stringFlavor);

					JEditTextArea textArea = (JEditTextArea)c;

					int caret = textArea.getCaretPosition();
					Selection s = textArea.getSelectionAtOffset(caret);

					/* if user drops into a selection,
					replace selection */
					if(s != null)
						textArea.setSelectedText(s,str);
					/* otherwise just insert the text */
					else
						textArea.getBuffer().insert(caret,str);
					textArea.scrollToCaret(true);
				}
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}

			GUIUtilities.getView(c).toFront();
			GUIUtilities.getView(c).requestFocus();
			c.requestFocus();

			return true;
		}

		protected void exportDone(JComponent c, Transferable t,
			int action)
		{
			Log.log(Log.DEBUG,this,"Export done");

			if(t == null)
			{
				Log.log(Log.DEBUG,this,"=> Null transferrable");
				JEditTextArea textArea = (JEditTextArea)c;
				textArea.selectNone();
			}
			else if(t.isDataFlavorSupported(
				DataFlavor.stringFlavor))
			{
				Log.log(Log.DEBUG,this,"=> String");
				JEditTextArea textArea = (JEditTextArea)c;
				if(action == MOVE)
					textArea.setSelectedText(null,false);
				else
					textArea.selectNone();
			}
		}

		public boolean canImport(JComponent c, DataFlavor[] flavors)
		{
			JEditTextArea textArea = (JEditTextArea)c;
			if(!textArea.isEditable())
				return false;

			for(int i = 0; i < flavors.length; i++)
			{
				if(DataFlavor.stringFlavor.equals(flavors[i]))
				{
					return textArea.isEditable();
				}
				else if(DataFlavor.javaFileListFlavor
					.equals(flavors[i]))
				{
					return true;
				}
			}
			return false;
		}
	} //}}}

	//{{{ DropHandler class
	static class DropHandler extends DropTargetAdapter
	{
		JEditTextArea textArea;
		int savedCaret;

		DropHandler(JEditTextArea textArea)
		{
			this.textArea = textArea;
		}

		public void dragEnter(DropTargetDragEvent dtde)
		{
			Log.log(Log.DEBUG,this,"Drag enter");
			textArea.setDragInProgress(true);
			//textArea.getBuffer().beginCompoundEdit();
			savedCaret = textArea.getCaretPosition();
		}

		public void dragOver(DropTargetDragEvent dtde)
		{
			Point p = dtde.getLocation();
			int pos = textArea.xyToOffset(p.x,p.y,
				!(textArea.getPainter().isBlockCaretEnabled()
				|| textArea.isOverwriteEnabled()));
			if(pos != -1)
			{
				textArea.moveCaretPosition(pos,
					JEditTextArea.NO_SCROLL);
			}
		}

		public void dragExit(DropTargetEvent dtde)
		{
			Log.log(Log.DEBUG,this,"Drag exit");
			textArea.setDragInProgress(false);
			//textArea.getBuffer().endCompoundEdit();
			textArea.moveCaretPosition(savedCaret,
				JEditTextArea.NO_SCROLL);
		}

		public void drop(DropTargetDropEvent dtde)
		{
			Log.log(Log.DEBUG,this,"Drop");
			textArea.setDragInProgress(false);
			//textArea.getBuffer().endCompoundEdit();
		}
	} //}}}

	//{{{ TextAreaSelection class
	static class TextAreaSelection extends StringSelection
	{
		JEditTextArea textArea;

		TextAreaSelection(JEditTextArea textArea)
		{
			super(textArea.getSelectedText());
			this.textArea = textArea;
		}
	} //}}}
}
