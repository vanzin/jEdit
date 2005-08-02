/*
 * TextAreaTransferHandler.java - Drag and drop support
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

package org.gjt.sp.jedit.textarea;

//{{{ Imports
import javax.swing.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
//}}}

class TextAreaTransferHandler extends TransferHandler
{
	/* I assume that there can be only one drag operation at the time */
	private static JEditTextArea dragSource;
	private static boolean compoundEdit;
	private static boolean sameTextArea;
	private static int insertPos;
	private static int insertOffset;

	//{{{ createTransferable
	protected Transferable createTransferable(JComponent c)
	{
		Log.log(Log.DEBUG,this,"createTransferable()");
		JEditTextArea textArea = (JEditTextArea)c;
		if(textArea.getSelectionCount() == 0)
			return null;
		else
		{
			dragSource = textArea;
			return new TextAreaSelection(textArea);
		}
	} //}}}

	//{{{ getSourceActions
	public int getSourceActions(JComponent c)
	{
		return COPY_OR_MOVE;
	} //}}}

	//{{{ importData
	public boolean importData(JComponent c, Transferable t)
	{
		Log.log(Log.DEBUG,this,"Import data");
		if(!canImport(c,t.getTransferDataFlavors()))
			return false;

		boolean returnValue;

		try
		{
			if(t.isDataFlavorSupported(
				DataFlavor.javaFileListFlavor))
			{
				returnValue = importFile(c,t);
			}
			else
			{
				returnValue = importText(c,t);
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			returnValue = false;
		}

		GUIUtilities.getView(c).toFront();
		GUIUtilities.getView(c).requestFocus();
		c.requestFocus();

		return returnValue;
	} //}}}

	//{{{ importFile
	private boolean importFile(JComponent c, Transferable t)
		throws Exception
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

		return true;
	} //}}}

	//{{{ importText
	private boolean importText(JComponent c, Transferable t)
		throws Exception
	{
		Log.log(Log.DEBUG,this,"=> String");
		String str = (String)t.getTransferData(
			DataFlavor.stringFlavor);

		JEditTextArea textArea = (JEditTextArea)c;
		if (dragSource == null)
		{
			// Only examine the string for a URL if it came from
			// outside of jEdit.
			org.gjt.sp.jedit.io.VFS vfs = org.gjt.sp.jedit.io.VFSManager.getVFSForPath(str);
			if (!(vfs instanceof org.gjt.sp.jedit.io.FileVFS) || str.startsWith("file://")) 
			{
				str = str.replace('\n',' ').replace('\r',' ').trim();
				if (str.startsWith("file://")) 
				{
					str = str.substring(7);
				}
				
				org.gjt.sp.jedit.io.VFSManager.runInWorkThread(
					new DraggedURLLoader(textArea,str)
					);
				
				return true;
			}
		}

		if(dragSource != null
			&& textArea.getBuffer()
			== dragSource.getBuffer())
		{
			compoundEdit = true;
			textArea.getBuffer().beginCompoundEdit();
		}
		
		
		sameTextArea = (textArea == dragSource);

		int caret = textArea.getCaretPosition();
		Selection s = textArea.getSelectionAtOffset(caret);

		/* if user drops into the same
		selection where they started, do
		nothing. */
		if(s != null)
		{
			if(sameTextArea)
				return false;
			/* if user drops into a selection,
			replace selection */
			int startPos = s.start;
			textArea.setSelectedText(s,str);
			textArea.setSelection(new Selection.Range(startPos,startPos+str.length()));
		}
		/* otherwise just insert the text */
		else
		{
			if (sameTextArea)
			{
				insertPos = caret;
				insertOffset = 0;
				Selection[] selections = textArea.getSelection();
				for (int i=0;i<selections.length;i++)
				{
					if (selections[i].end<(insertPos+insertOffset))
						insertOffset-=(selections[i].end-selections[i].start);
				}
			}
			else
			{
				textArea.getBuffer().insert(caret,str);
				textArea.setSelection(new Selection.Range(caret,caret+str.length()));
			}
		}
		textArea.scrollToCaret(true);

		return true;
	} //}}}

	//{{{ exportDone
	protected void exportDone(JComponent c, Transferable t,
		int action)
	{
		Log.log(Log.DEBUG,this,"Export done");

		JEditTextArea textArea = (JEditTextArea)c;

		try
		{
			if(t == null)
			{
				Log.log(Log.DEBUG,this,"=> Null transferrable");
				textArea.selectNone();
			}
			else if(t.isDataFlavorSupported(
				DataFlavor.stringFlavor))
			{
				Log.log(Log.DEBUG,this,"=> String");
				if (sameTextArea)
				{
					if(action == MOVE)
					{
						textArea.setSelectedText(null,false);
						insertPos += insertOffset;
					} 
					try
					{
						String str = (String)t.getTransferData(DataFlavor.stringFlavor);
						textArea.getBuffer().insert(insertPos,str);
						textArea.setSelection(new Selection.Range(insertPos,insertPos+str.length()));
					}
					catch(Exception e)
					{
						Log.log(Log.DEBUG,this,"exportDone in sameTextArea");
						Log.log(Log.DEBUG,this,e);
					}
				}
				else
				{
					if(action == MOVE)
						textArea.setSelectedText(null,false);
					else
						textArea.selectNone();
				}
			}
		}
		finally
		{
			if(compoundEdit)
			{
				compoundEdit = false;
				textArea.getBuffer().endCompoundEdit();
			}
		}

		dragSource = null;
	} //}}}

	//{{{ canImport
	public boolean canImport(JComponent c, DataFlavor[] flavors)
	{
		JEditTextArea textArea = (JEditTextArea)c;

		// correctly handle text flavor + file list flavor
		// + text area read only, do an or of all flags
		boolean returnValue = false;

		for(int i = 0; i < flavors.length; i++)
		{
			if(flavors[i].equals(
				DataFlavor.javaFileListFlavor))
			{
				returnValue = true;
			}
			else if(flavors[i].equals(
				DataFlavor.stringFlavor))
			{
				if(textArea.isEditable())
					returnValue = true;
			}
		}

		Log.log(Log.DEBUG,this,"canImport() returning "
			+ returnValue);
		return returnValue;
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

	//{{{ DraggedURLLoader
	class DraggedURLLoader extends org.gjt.sp.util.WorkRequest
	{
		private JEditTextArea textArea;
		private String url;
		
		public DraggedURLLoader(JEditTextArea textArea, String url)
		{
			super();
			this.textArea = textArea;
			this.url = url;
		}
		public void run()
		{
			jEdit.openFile(textArea.getView(),url);
		}
	} //}}}

}
