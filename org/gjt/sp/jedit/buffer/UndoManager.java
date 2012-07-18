/*
 * UndoManager.java - Buffer undo manager
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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
import org.gjt.sp.util.IntegerArray;
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
	public UndoManager(JEditBuffer buffer)
	{
		this.buffer = buffer;
	} //}}}

	//{{{ setLimit() method
	public void setLimit(int limit)
	{
		this.limit = limit;
	} //}}}

	//{{{ clear() method
	public void clear()
	{
		undosFirst = undosLast = redosFirst = null;
		undoCount = 0;
	} //}}}

	//{{{ canUndo() method
	public boolean canUndo()
	{
		return (undosLast != null);
	} //}}}

	//{{{ undo() method
	public int undo()
	{
		if(insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");

		if(undosLast == null)
			return -1;
		else
		{
			reviseUndoId();
			undoCount--;

			int caret = undosLast.undo(this);
			redosFirst = undosLast;
			undosLast = undosLast.prev;
			if(undosLast == null)
				undosFirst = null;
			return caret;
		}
	} //}}}

	//{{{ canRedo() method
	public boolean canRedo()
	{
		return (redosFirst != null);
	} //}}}

	//{{{ redo() method
	public int redo()
	{
		if(insideCompoundEdit())
			throw new InternalError("Unbalanced begin/endCompoundEdit()");

		if(redosFirst == null)
			return -1;
		else
		{
			reviseUndoId();
			undoCount++;

			int caret = redosFirst.redo(this);
			undosLast = redosFirst;
			if(undosFirst == null)
				undosFirst = undosLast;
			redosFirst = redosFirst.next;
			return caret;
		}
	} //}}}

	//{{{ beginCompoundEdit() method
	public void beginCompoundEdit()
	{
		if(compoundEditCount == 0)
		{
			compoundEdit = new CompoundEdit();
			reviseUndoId();
		}

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
			if(compoundEdit.first == null)
				/* nothing done between begin/end calls */;
			else if(compoundEdit.first == compoundEdit.last)
				addEdit(compoundEdit.first);
			else
				addEdit(compoundEdit);

			compoundEdit = null;
		}

		compoundEditCount--;
	} //}}}

	//{{{ insideCompoundEdit() method
	public boolean insideCompoundEdit()
	{
		return compoundEditCount != 0;
	} //}}}

	//{{{ getUndoId() method
	public Object getUndoId()
	{
		return undoId;
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int offset, int length, String text, boolean clearDirty)
	{
		Edit toMerge = getMergeEdit();

		if(!clearDirty && toMerge instanceof Insert
			&& redosFirst == null)
		{
			Insert ins = (Insert)toMerge;
			if(ins.offset == offset)
			{
				ins.str = text.concat(ins.str);
				return;
			}
			else if(ins.offset + ins.str.length() == offset)
			{
				ins.str = ins.str.concat(text);
				return;
			}
		}

		Insert ins = new Insert(offset,text);

		if(clearDirty)
		{
			redoClearDirty = getLastEdit();
			undoClearDirty = ins;
		}

		if(compoundEdit != null)
			compoundEdit.add(this, ins);
		else
		{
			reviseUndoId();
			addEdit(ins);
		}
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int offset, int length, String text, boolean clearDirty)
	{
		Edit toMerge = getMergeEdit();

		if(!clearDirty && toMerge instanceof Remove
			&& redosFirst == null)
		{
			Remove rem = (Remove)toMerge;
			if(rem.offset == offset)
			{
				String newStr = rem.str.concat(text);
				KillRing.getInstance().changed(rem.str, newStr);
				rem.str = newStr;
				return;
			}
			else if(offset + length == rem.offset)
			{
				String newStr = text.concat(rem.str);
				KillRing.getInstance().changed(rem.str, newStr);
 				rem.offset = offset;
				rem.str = newStr;
				return;
			}
		}

		// use String.intern() here as new Strings are created in
		// JEditBuffer.remove() via undoMgr.contentRemoved(... getText() ...);
		Remove rem = new Remove(offset,text.intern());

		if(clearDirty)
		{
			redoClearDirty = getLastEdit();
			undoClearDirty = rem;
		}

		if(compoundEdit != null)
			compoundEdit.add(this, rem);
		else
		{
			reviseUndoId();
			addEdit(rem);
		}

		KillRing.getInstance().add(rem.str);
	} //}}}

	//{{{ resetClearDirty method
	public void resetClearDirty()
	{
		redoClearDirty = getLastEdit();
		if(redosFirst instanceof CompoundEdit)
			undoClearDirty = ((CompoundEdit)redosFirst).first;
		else
			undoClearDirty = redosFirst;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JEditBuffer buffer;

	// queue of undos. last is most recent, first is oldest
	private Edit undosFirst;
	private Edit undosLast;

	// queue of redos. first is most recent, last is oldest
	private Edit redosFirst;

	private int limit;
	private int undoCount;
	private int compoundEditCount;
	private CompoundEdit compoundEdit;
	private Edit undoClearDirty, redoClearDirty;
	private Object undoId;
	//}}}

	//{{{ addEdit() method
	private void addEdit(Edit edit)
	{
		if(undosFirst == null)
			undosFirst = undosLast = edit;
		else
		{
			undosLast.next = edit;
			edit.prev = undosLast;
			undosLast = edit;
		}

		redosFirst = null;

		undoCount++;

		while(undoCount > limit)
		{
			undoCount--;

			if(undosFirst == undosLast)
				undosFirst = undosLast = null;
			else
			{
				undosFirst.next.prev = null;
				undosFirst = undosFirst.next;
			}
		}
	} //}}}

	//{{{ getMergeEdit() method
	private Edit getMergeEdit()
	{
		return (compoundEdit != null ? compoundEdit.last : getLastEdit());
	} //}}}

	//{{{ getLastEdit() method
	private Edit getLastEdit()
	{
		if(undosLast instanceof CompoundEdit)
			return ((CompoundEdit)undosLast).last;
		else
			return undosLast;
	} //}}}

	//{{{ reviseUndoId()
	/*
	 * Revises a unique undoId for a the undo operation that is being
	 * created as a result of a buffer content change, or that is being
	 * used for undo/redo. Content changes that belong to the same undo
	 * operation will have the same undoId.
	 * 
	 * This method should be called whenever:
	 * - a buffer content change causes a new undo operation to be created;
	 *   i.e. whenever a content change is not included in the same undo
	 *   operation as the previous.
	 * - an undo/redo is performed.
	 */
	private void reviseUndoId()
	{
		undoId = new Object();
	} //}}}

	//{{{ getReplaceFromRemoveInsert() method
	// a Replace Edit is a Remove Edit and then an Insert Edit
	private Replace getReplaceFromRemoveInsert(Edit lastElement, Edit newElement)
	{
		if(lastElement instanceof Remove && newElement instanceof Insert)
		{
			// don't fold a undoClearDirty Remove Edit, because
			// it's the identity is significant.
			if(lastElement == undoClearDirty)
				return null;

			/* newElement is guaranteed to be an Compound-Insert Edit, redoClearDirty will be an Normal-Insert, Normal-Remove,
			 * Compound-Remove-Insert-Edit or Compound-Replace-Edit (all possible edit operations)
			 * redoClearDirty cannot become equal to newElement because:
			 * - redoClearDirty will be set after the file has been saved and the first new change is made, which
			 *   could be an Normal-Insert, Normal-Remove, Compound-Replace-Edit, Compound-Remove-Insert-Edit,
			 *   or null, if this is the first change in the file at all.
			 *   For Compound-Edit case it will be the last element of the Compound edit.
			 * - As the first Remove&Insert sequence of a Compound-Edit is never compacted by above if statement,
			 *   redoClearDirty can never be any of the following Remove&Insert elements, as the user as no option to save the
			 *   file after the first Remove&Insert sequence, because the GUI is blocked by the search&replace all operation.
			 */  
			assert newElement  != redoClearDirty;
			assert lastElement != redoClearDirty;
			/* search for nothing (via regexp) will results just in Insert-Edits.
			 * So we won't get here as lastElement will be an Insert, too
			 */
			assert newElement  != undoClearDirty;

			Remove rem = (Remove) lastElement;
			Insert ins = (Insert) newElement;
		
			if(rem.offset == ins.offset)
			{
				return new Replace(rem.offset, rem.str, ins.str);
			}
		}
		return null;
	} //}}}

	//{{{ getCompressedReplaceFromReplaceReplace() method
	// a CompressedReplace Edit is one to many Replace Edit compressed via offsets
	private CompressedReplace getCompressedReplaceFromReplaceReplace(Edit lastElement, Edit newElement)
	{

		if(newElement instanceof Replace)
		{
			CompressedReplace rep = null;
			// try to pack the next Replace into the CompressedReplace
			if(lastElement instanceof CompressedReplace)
			{
				rep = (CompressedReplace) lastElement;
				rep.add((Replace) newElement);
				return rep;
			}
	
			// try to create a compressed Replace
			if(lastElement instanceof Replace)
			{
				rep = new CompressedReplace((Replace)lastElement);
				rep.add((Replace) newElement);
				return rep;
			}
		}
		return null;
	} //}}}

	//{{{ Inner classes

	//{{{ Edit class
	private abstract static class Edit
	{
		Edit prev, next;

		//{{{ undo() method
		abstract int undo(UndoManager mgr);
		//}}}

		//{{{ redo() method
		abstract int redo(UndoManager mgr);
		//}}}
	} //}}}

	//{{{ Insert class
	private static class Insert extends Edit
	{
		//{{{ Insert constructor
		Insert(int offset, String str)
		{
			this.offset = offset;
			this.str = str;
		} //}}}

		//{{{ undo() method
		int undo(UndoManager mgr)
		{
			mgr.buffer.remove(offset,str.length());
			if(mgr.undoClearDirty == this)
				mgr.buffer.setDirty(false);
			return offset;
		} //}}}

		//{{{ redo() method
		int redo(UndoManager mgr)
		{
			mgr.buffer.insert(offset,str);
			if(mgr.redoClearDirty == this)
				mgr.buffer.setDirty(false);
			return offset + str.length();
		} //}}}

		int offset;
		String str;
	} //}}}

	//{{{ Remove class
	private static class Remove extends Edit
	{
		//{{{ Remove constructor
		Remove(int offset, String str)
		{
			this.offset = offset;
			this.str = str;
		} //}}}

		//{{{ undo() method
		int undo(UndoManager mgr)
		{
			mgr.buffer.insert(offset,str);
			if(mgr.undoClearDirty == this)
				mgr.buffer.setDirty(false);
			return offset + str.length();
		} //}}}

		//{{{ redo() method
		int redo(UndoManager mgr)
		{
			mgr.buffer.remove(offset,str.length());
			if(mgr.redoClearDirty == this)
				mgr.buffer.setDirty(false);
			return offset;
		} //}}}

		int offset;
		String str;
	} //}}}

	//{{{ Replace class
	private static class Replace extends Edit
	{
		//{{{ Replace constructor
		Replace(int offset, String strRemove, String strInsert)
		{
			this.offset = offset;
			this.strRemove = strRemove;
			this.strInsert = strInsert;
		} //}}}

		//{{{ undo() method
		int undo(UndoManager mgr)
		{
			mgr.buffer.remove(offset,strInsert.length());
			mgr.buffer.insert(offset,strRemove);
			assert mgr.undoClearDirty != this;
			return offset + strRemove.length();
		} //}}}

		//{{{ redo() method
		int redo(UndoManager mgr)
		{
			mgr.buffer.remove(offset,strRemove.length());
			mgr.buffer.insert(offset,strInsert);
			if(mgr.redoClearDirty == this)
				mgr.buffer.setDirty(false);
			return offset + strInsert.length();
		} //}}}

		int offset;
		String strRemove, strInsert;
	} //}}}

	//{{{ CompressedReplace class
	private static class CompressedReplace extends Replace
	{
		//{{{ CompressedReplace constructor
		CompressedReplace(Replace r1)
		{
			super(r1.offset, r1.strRemove, r1.strInsert);
			offsets = new IntegerArray(4);
			offsets.add(r1.offset);
		} //}}}

		//{{{ add() method
		CompressedReplace add(Replace rep)
		{
			if(this.strInsert.equals(rep.strInsert) && this.strRemove.equals(rep.strRemove))
			{
				offsets.add(rep.offset);
				return this;
			}
			return null;
		} //}}}

		//{{{ undo() method
		int undo(UndoManager mgr)
		{
			int caret = -1;
			for(int i = offsets.getSize() - 1; i >= 0; i--)
			{
				offset = offsets.get(i);
				caret = super.undo(mgr);
			}
			return caret;
		} //}}}

		//{{{ redo() method
		int redo(UndoManager mgr)
		{
			int caret = -1;
			for(int i = 0; i < offsets.getSize(); i++)
			{
				offset = offsets.get(i);
				caret = super.redo(mgr);
			}
			return caret;
		} //}}}

		IntegerArray offsets;
	} //}}}

	//{{{ CompoundEdit class
	private static class CompoundEdit extends Edit
	{
		//{{{ undo() method
		public int undo(UndoManager mgr)
		{
			int retVal = -1;
			Edit edit = last;
			while(edit != null)
			{
				retVal = edit.undo(mgr);
				edit = edit.prev;
			}
			return retVal;
		} //}}}

		//{{{ redo() method
		public int redo(UndoManager mgr)
		{
			int retVal = -1;
			Edit edit = first;
			while(edit != null)
			{
				retVal = edit.redo(mgr);
				edit = edit.next;
			}
			return retVal;
		} //}}}

		//{{{ _add() method
		private void _add(Edit edit)
		{
			if(first == null)
				first = last = edit;
			else
			{
				edit.prev = last;
				last.next = edit;
				last = edit;
			}
		} //}}}

		//{{{ add() method
		public void add(UndoManager mgr, Edit edit)
		{
			_add(edit);

			// try to compact a sequence of Remove and Insert into a Replace
			// Edit to save memory for large search&replace operations
			if(last.prev != null)
			{
				Edit rep = mgr.getReplaceFromRemoveInsert(last.prev, last);
				if(rep != null)
					exchangeLastElement(rep);
			}

			// try to compress a sequence of Replace and Replace into a "CompressedReplace"
			if(last.prev != null)
			{
				Edit rep = mgr.getCompressedReplaceFromReplaceReplace(last.prev, last);
				if(rep != null)
					exchangeLastElement(rep);
			}
			
			// try to compress a sequence of CompressedReplace and Replace into a "CompressedReplace"
			if(last.prev != null)
			{
				Edit rep = mgr.getCompressedReplaceFromReplaceReplace(last.prev, last);
				if(rep != null)
					exchangeLastElement(rep);
			}
		} //}}}

		//{{{ exchangeLastElement() method
		private void exchangeLastElement(Edit edit)
		{
			// remove last
			if(first == last)
				first = last = null;
			else
			{
				last.prev.next = null;
				last = last.prev;
			}

			// exchange current last
			if(first == null || first == last)
				first = last = edit;
			else
			{
				edit.prev = last.prev;
				last.prev.next = edit;
				last = edit;
			}
		} //}}}

		Edit first, last;
	} //}}}

	//}}}

	//}}}
}
