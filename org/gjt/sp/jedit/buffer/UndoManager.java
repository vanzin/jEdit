/*
 * UndoManager.java - Buffer undo manager
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

package org.gjt.sp.jedit.buffer;

//{{{ Imports
import java.util.Vector;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.Buffer;
//}}}

public class UndoManager
{
	//{{{ UndoManager constructor
	public UndoManager(Buffer buffer)
	{
		this.buffer = buffer;
		undos = new Vector(100);
	} //}}}

	//{{{ clear() method
	public void clear()
	{
		undos.removeAllElements();
	} //}}}

	//{{{ undo() method
	public boolean undo(JEditTextArea textArea)
	{
		return false;
	} //}}}

	//{{{ redo() method
	public boolean redo(JEditTextArea textArea)
	{
		return false;
	} //}}}

	//{{{ beginCompoundEdit() method
	public void beginCompoundEdit()
	{
		
	} //}}}

	//{{{ endCompoundEdit() method
	public void endCompoundEdit()
	{
		
	} //}}}

	//{{{ insideCompoundEdit() method
	public boolean insideCompoundEdit()
	{
		return compoundEditCount != 0;
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int offset, int length, String text)
	{
		
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int offset, int length, String text)
	{
		
	} //}}}

	//{{{ Private members
	private Buffer buffer;
	private Vector undos;
	private int compoundEditCount;
	private CompoundEdit compoundEdit;
	//}}}

	//{{{ Inner classes

	//{{{ Edit interface
	interface Edit
	{
		//{{{ undo() method
		int undo(Buffer buffer, JEditTextArea textArea);
		//}}}

		//{{{ redo() method
		int redo(Buffer buffer, JEditTextArea textArea);
		//}}}
	} //}}}

	//{{{ Insert class
	public static class Insert implements Edit
	{
		//{{{ Insert constructor
		public Insert(int offset, int length, String str)
		{
			this.offset = offset;
			this.length = length;
			this.str = str;
		} //}}}

		//{{{ undo() method
		public int undo(Buffer buffer, JEditTextArea textArea)
		{
			return offset;
		} //}}}

		//{{{ redo() method
		public int redo(Buffer buffer, JEditTextArea textArea)
		{
			return offset + length;
		} //}}}

		private int offset;
		private int length;
		private String str;
	} //}}}

	//{{{ Remove class
	public static class Remove implements Edit
	{
		//{{{ Remove constructor
		public Remove(int offset, int length, String str)
		{
			this.offset = offset;
			this.length = length;
			this.str = str;
		} //}}}

		//{{{ undo() method
		public int undo(Buffer buffer, JEditTextArea textArea)
		{
			return offset + length;
		} //}}}

		//{{{ redo() method
		public int redo(Buffer buffer, JEditTextArea textArea)
		{
			return offset;
		} //}}}

		private int offset;
		private int length;
		private String str;
	} //}}}

	//{{{ CompoundEdit class
	public static class CompoundEdit implements Edit
	{
		//{{{ undo() method
		public int undo(Buffer buffer, JEditTextArea textArea)
		{
			return 0;
		} //}}}

		//{{{ redo() method
		public int redo(Buffer buffer, JEditTextArea textArea)
		{
			return 0;
		} //}}}

		//{{{ getSize() method
		public int getSize()
		{
			return undos.size();
		}

		private Vector undos = new Vector();
	} //}}}

	//}}}
}
