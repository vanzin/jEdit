/*
 * UndoManager.java - Buffer undo manager
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

package org.gjt.sp.jedit.buffer;

//{{{ Imports
import java.util.ArrayList;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;
//}}}

/**
 * A class internal to jEdit's document model. You should not use it
 * directly. To improve performance, none of the methods in this class
 * check for out of bounds access, nor are they thread-safe. The
 * <code>Buffer</code> class, through which these methods must be
 * called through, implements such protection.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class UndoManager
{
	//{{{ UndoManager constructor
	public UndoManager(Buffer buffer)
	{
		this.buffer = buffer;
		undos = new ArrayList(100);
	} //}}}

	//{{{ setLimit() method
	public void setLimit(int limit)
	{
		this.limit = limit;
	} //}}}

	//{{{ clear() method
	public void clear()
	{
		undos.clear();
		undoPos = undoCount = 0;
	} //}}}

	//{{{ undo() method
	public boolean undo(JEditTextArea textArea)
	{
		if(insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");

		if(undoPos == 0)
			return false;
		else
		{
			boolean dirty = buffer.isDirty();
			Edit edit = (Edit)undos.get(--undoPos);
			int caret = edit.undo();
			if(caret != -1)
				textArea.setCaretPosition(caret);
			return true;
		}
	} //}}}

	//{{{ redo() method
	public boolean redo(JEditTextArea textArea)
	{
		if(insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");

		if(undoPos == undoCount)
			return false;
		else
		{
			Edit edit = (Edit)undos.get(undoPos++);
			int caret = edit.redo();
			if(caret != -1)
				textArea.setCaretPosition(caret);
			return true;
		}
	} //}}}

	//{{{ beginCompoundEdit() method
	public void beginCompoundEdit()
	{
		if(compoundEditCount == 0)
			compoundEdit = new CompoundEdit();

		compoundEditCount++;
	} //}}}

	//{{{ endCompoundEdit() method
	public void endCompoundEdit()
	{
		if(compoundEditCount == 0)
		{
			Log.log(Log.WARNING,this,new Exception("Unbalanced begin/endCompoundEdit()"));
			return;
		}
		else if(compoundEditCount == 1)
		{
			switch(compoundEdit.undos.size())
			{
			case 0:
				/* nothing done between begin/end calls */;
				break;
			case 1:
				addEdit((Edit)compoundEdit.undos.get(0));
				break;
			default:
				addEdit(compoundEdit);
			}

			compoundEdit = null;
		}

		compoundEditCount--;
	} //}}}

	//{{{ insideCompoundEdit() method
	public boolean insideCompoundEdit()
	{
		return compoundEditCount != 0;
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int offset, int length, String text, boolean clearDirty)
	{
		Edit toMerge = getLastEdit();

		if(!clearDirty && toMerge instanceof Insert)
		{
			Insert ins = (Insert)toMerge;
			if(ins.offset == offset)
			{
				ins.str = text.concat(ins.str);
				ins.length += length;
				return;
			}
			else if(ins.offset + ins.length == offset)
			{
				ins.str = ins.str.concat(text);
				ins.length += length;
				return;
			}
		}

		Insert ins = new Insert(offset,length,text);

		if(clearDirty)
		{
			redoClearDirty = toMerge;
			undoClearDirty = ins;
		}

		if(compoundEdit != null)
			compoundEdit.undos.add(ins);
		else
			addEdit(ins);
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int offset, int length, String text, boolean clearDirty)
	{
		Edit toMerge = getLastEdit();

		if(!clearDirty && toMerge instanceof Remove)
		{
			Remove rem = (Remove)toMerge;
			if(rem.offset == offset)
			{
				rem.str = rem.str.concat(text);
				rem.length += length;
				return;
			}
			else if(offset + length == rem.offset)
			{
				rem.str = text.concat(rem.str);
				rem.length += length;
				rem.offset = offset;
				return;
			}
		}

		Remove rem = new Remove(offset,length,text);
		if(clearDirty)
		{
			redoClearDirty = toMerge;
			undoClearDirty = rem;
		}

		if(compoundEdit != null)
			compoundEdit.undos.add(rem);
		else
			addEdit(rem);

		KillRing.add(rem);
	} //}}}

	//{{{ bufferSaved() method
	public void bufferSaved()
	{
		redoClearDirty = getLastEdit();
		if(undoPos == undoCount)
			undoClearDirty = null;
		else
		{
			Edit edit = (Edit)undos.get(undoPos);
			if(edit instanceof CompoundEdit)
			{
				undoClearDirty = (Edit)
					((CompoundEdit)edit)
					.undos.get(0);
			}
			else
				undoClearDirty = edit;
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Buffer buffer;
	private ArrayList undos;
	private int limit;
	private int undoPos;
	private int undoCount;
	private int compoundEditCount;
	private CompoundEdit compoundEdit;
	private Edit undoClearDirty, redoClearDirty;
	//}}}

	//{{{ addEdit() method
	private void addEdit(Edit edit)
	{
		//System.err.println("adding undo with position " + undoPos);
		undos.add(undoPos++,edit);

		for(int i = undoCount - 1; i >= undoPos; i--)
		{
			//System.err.println("removing undo with position " + i);
			undos.remove(i);
		}

		if(undoPos > limit)
		{
			//System.err.println("removing undo 0");
			undos.remove(0);
			undoPos--;
		}

		undoCount = undoPos;
	} //}}}

	//{{{ getLastEdit() method
	private Edit getLastEdit()
	{
		if(compoundEdit != null)
		{
			int size = compoundEdit.undos.size();
			if(size != 0)
				return (Edit)compoundEdit.undos.get(size - 1);
			else
				return null;
		}
		else if(undoCount != 0 && undoPos != 0)
		{
			Edit e = (Edit)undos.get(undoPos - 1);
			if(e instanceof CompoundEdit)
			{
				CompoundEdit c = (CompoundEdit)e;
				return (Edit)c.undos.get(c.undos.size() - 1);
			}
		}

		if(undoPos != 0)
			return (Edit)undos.get(undoPos - 1);
		else
			return null;
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ Edit class
	abstract class Edit
	{
		//{{{ undo() method
		abstract int undo();
		//}}}

		//{{{ redo() method
		abstract int redo();
		//}}}
	} //}}}

	//{{{ Insert class
	class Insert extends Edit
	{
		//{{{ Insert constructor
		Insert(int offset, int length, String str)
		{
			this.offset = offset;
			this.length = length;
			this.str = str;
		} //}}}

		//{{{ undo() method
		int undo()
		{
			buffer.remove(offset,length);
			if(undoClearDirty == this)
				buffer.setDirty(false);
			return offset;
		} //}}}

		//{{{ redo() method
		int redo()
		{
			buffer.insert(offset,str);
			if(redoClearDirty == this)
				buffer.setDirty(false);
			return offset + length;
		} //}}}

		int offset;
		int length;
		String str;
	} //}}}

	//{{{ Remove class
	class Remove extends Edit
	{
		//{{{ Remove constructor
		Remove(int offset, int length, String str)
		{
			this.offset = offset;
			this.length = length;
			this.str = str;
		} //}}}

		//{{{ undo() method
		int undo()
		{
			buffer.insert(offset,str);
			if(undoClearDirty == this)
				buffer.setDirty(false);
			return offset + length;
		} //}}}

		//{{{ redo() method
		int redo()
		{
			if(redoClearDirty == this)
				buffer.setDirty(false);
			buffer.remove(offset,length);
			return offset;
		} //}}}

		int offset;
		int length;
		String str;
	} //}}}

	//{{{ CompoundEdit class
	class CompoundEdit extends Edit
	{
		//{{{ undo() method
		public int undo()
		{
			int retVal = -1;
			for(int i = undos.size() - 1; i >= 0; i--)
			{
				retVal = ((Edit)undos.get(i)).undo();
			}
			return retVal;
		} //}}}

		//{{{ redo() method
		public int redo()
		{
			int retVal = -1;
			for(int i = 0; i < undos.size(); i++)
			{
				retVal = ((Edit)undos.get(i)).redo();
			}
			return retVal;
		} //}}}

		ArrayList undos = new ArrayList();
	} //}}}

	//}}}
}
