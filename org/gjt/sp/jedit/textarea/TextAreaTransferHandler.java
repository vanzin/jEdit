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
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

class TextAreaTransferHandler extends TransferHandler
{
	/* I assume that there can be only one drag operation at the time */
	private static JEditTextArea dragSource;
	private static boolean compoundEdit;

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
	}

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
	}

	private boolean importText(JComponent c, Transferable t)
		throws Exception
	{
		Log.log(Log.DEBUG,this,"=> String");
		String str = (String)t.getTransferData(
			DataFlavor.stringFlavor);

		JEditTextArea textArea = (JEditTextArea)c;

		if(dragSource != null
			&& textArea.getBuffer()
			== dragSource.getBuffer())
		{
			compoundEdit = true;
			textArea.getBuffer().beginCompoundEdit();
		}

		int caret = textArea.getCaretPosition();
		Selection s = textArea.getSelectionAtOffset(caret);

		/* if user drops into the same
		selection where they started, do
		nothing. */
		if(s != null)
		{
			if(textArea == dragSource)
				return false;
			/* if user drops into a selection,
			replace selection */
			textArea.setSelectedText(s,str);
		}
		/* otherwise just insert the text */
		else
			textArea.getBuffer().insert(caret,str);
		textArea.scrollToCaret(true);

		return true;
	}

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
				if(action == MOVE)
					textArea.setSelectedText(null,false);
				else
					textArea.selectNone();
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
	}

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
	}

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
