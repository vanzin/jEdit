/*
 * OffsetManager.java - Manages line info, line start offsets, positions
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
import javax.swing.text.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
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
public class OffsetManager
{
	//{{{ OffsetManager constructor
	public OffsetManager(Buffer buffer)
	{
		this.buffer = buffer;

		lineInfo = new long[1];
		// make first line visible by default
		lineInfo[0] = 1L | (0xffL << VISIBLE_SHIFT);
		lineContext = new TokenMarker.LineContext[1];
		lineCount = 1;

		positions = new PosBottomHalf[100];

		virtualLineCounts = new int[8];
		for(int i = 0; i < 8; i++)
			virtualLineCounts[i] = 1;
	} //}}}

	//{{{ getLineCount() method
	public final int getLineCount()
	{
		return lineCount;
	} //}}}

	//{{{ getVirtualLineCount() method
	public final int getVirtualLineCount(int index)
	{
		return virtualLineCounts[index];
	} //}}}

	//{{{ setVirtualLineCount() method
	public final void setVirtualLineCount(int index, int lineCount)
	{
		virtualLineCounts[index] = lineCount;
	} //}}}

	//{{{ getLineOfOffset() method
	public int getLineOfOffset(int offset)
	{
		int start = 0;
		int end = lineCount - 1;

		for(;;)
		{
			switch(end - start)
			{
			case 0:
				if(getLineEndOffset(start) <= offset)
					return start + 1;
				else
					return start;
			case 1:
				if(getLineEndOffset(start) <= offset)
				{
					if(getLineEndOffset(end) <= offset)
						return end + 1;
					else
						return end;
				}
				else
					return start;
			default:
				int pivot = (end + start) / 2;
				int value = getLineEndOffset(pivot);
				if(value == offset)
					return pivot + 1;
				else if(value < offset)
					start = pivot + 1;
				else
					end = pivot - 1;
				break;
			}
		}
	} //}}}

	//{{{ getLineEndOffset() method
	public final int getLineEndOffset(int line)
	{
		return (int)(lineInfo[line] & END_MASK);
	} //}}}

	//{{{ isFoldLevelValid() method
	public final boolean isFoldLevelValid(int line)
	{
		return (lineInfo[line] & FOLD_LEVEL_VALID_MASK) != 0;
	} //}}}

	//{{{ getFoldLevel() method
	public final int getFoldLevel(int line)
	{
		return (int)((lineInfo[line] & FOLD_LEVEL_MASK)
			>> FOLD_LEVEL_SHIFT);
	} //}}}

	//{{{ setFoldLevel() method
	// Also sets 'fold level valid' flag
	public final void setFoldLevel(int line, int level)
	{
		lineInfo[line] = ((lineInfo[line] & ~FOLD_LEVEL_MASK)
			| ((long)level << FOLD_LEVEL_SHIFT)
			| FOLD_LEVEL_VALID_MASK);
	} //}}}

	//{{{ isLineVisible() method
	public final boolean isLineVisible(int line, int index)
	{
		long mask = 1L << (index + VISIBLE_SHIFT);
		return (lineInfo[line] & mask) != 0;
	} //}}}

	//{{{ setLineVisible() method
	public final void setLineVisible(int line, int index, boolean visible)
	{
		long mask = 1L << (index + VISIBLE_SHIFT);
		if(visible)
			lineInfo[line] = (lineInfo[line] | mask);
		else
			lineInfo[line] = (lineInfo[line] & ~mask);
	} //}}}

	//{{{ isLineContextValid() method
	public final boolean isLineContextValid(int line)
	{
		return (lineInfo[line] & CONTEXT_VALID_MASK) != 0;
	} //}}}

	//{{{ getLineContext() method
	public final TokenMarker.LineContext getLineContext(int line)
	{
		return lineContext[line];
	} //}}}

	//{{{ setLineContext() method
	// Also sets 'context valid' to true
	public final void setLineContext(int line, TokenMarker.LineContext context)
	{
		lineContext[line] = context;
		lineInfo[line] |= CONTEXT_VALID_MASK;
	} //}}}

	//{{{ createPosition() method

	// note: Buffer.createPosition() grabs a read lock, so the buffer
	// will not change during this method. however, if two stops call
	// it, there can be contention issues unless this method is
	// synchronized.

	// I could make Buffer.createPosition() grab a write lock, but then
	// it would be necessary to implement grabbing write locks within
	// read locks, since HyperSearch for example does everything inside
	// a read lock.
	public synchronized Position createPosition(int offset)
	{
		PosBottomHalf bh = null;

		for(int i = 0; i < positionCount; i++)
		{
			PosBottomHalf _bh = positions[i];
			if(_bh.offset == offset)
			{
				bh = _bh;
				break;
			}
			else if(_bh.offset > offset)
			{
				bh = new PosBottomHalf(offset);
				growPositionArray();
				System.arraycopy(positions,i,positions,i+1,
					positionCount - i);
				positionCount++;
				positions[i] = bh;
				break;
			}
		}

		if(bh == null)
		{
			bh = new PosBottomHalf(offset);
			growPositionArray();
			positions[positionCount++] = bh;
		}

		return new PosTopHalf(bh);
	} //}}}

	//{{{ expandFolds() method
	/**
	 * Like <code>FoldVisibilityManager.expandFolds()</code>, but does
	 * it for all fold visibility managers viewing this buffer. Should
	 * only be called after loading.
	 */
	public void expandFolds(int foldLevel)
	{
		int newVirtualLineCount = 0;
		foldLevel = (foldLevel - 1) * buffer.getIndentSize() + 1;

		/* this ensures that the first line is always visible */
		boolean seenVisibleLine = false;

		for(int i = 0; i < getLineCount(); i++)
		{
			if(!seenVisibleLine || buffer.getFoldLevel(i) < foldLevel)
			{
				seenVisibleLine = true;
				// Since only called on load, it already has
				// the VISIBLE_MASK set
				//lineInfo[i] |= VISIBLE_MASK;
				newVirtualLineCount++;
			}
			else
				lineInfo[i] &= ~VISIBLE_MASK;
		}

		for(int i = 0; i < virtualLineCounts.length; i++)
		{
			virtualLineCounts[i] = newVirtualLineCount;
		}
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int startLine, int offset,
		int numLines, int length, IntegerArray endOffsets)
	{
		int endLine = startLine + numLines;

		//{{{ Update line info and line context arrays
		if(numLines > 0)
		{
			lineCount += numLines;

			if(lineInfo.length <= lineCount)
			{
				long[] lineInfoN = new long[(lineCount + 1) * 2];
				System.arraycopy(lineInfo,0,lineInfoN,0,
						 lineInfo.length);
				lineInfo = lineInfoN;

				TokenMarker.LineContext[] lineContextN
					= new TokenMarker.LineContext[(lineCount + 1) * 2];
				System.arraycopy(lineContext,0,lineContextN,0,
						 lineContext.length);
				lineContext = lineContextN;
			}

			System.arraycopy(lineInfo,startLine,lineInfo,
				endLine,lineCount - endLine);
			System.arraycopy(lineContext,startLine,lineContext,
				endLine,lineCount - endLine);

			//{{{ Find fold start of this line
			int foldLevel = buffer.getFoldLevel(startLine);
			long visible = (0xffL << VISIBLE_SHIFT);
			if(startLine != 0)
			{
				for(int i = startLine; i > 0; i--)
				{
					if(/* buffer.isFoldStart(i - 1)
						&& */ buffer.getFoldLevel(i) <= foldLevel)
					{
						visible = (lineInfo[i] & VISIBLE_MASK);
						break;
					}
				}
			} //}}}

			for(int i = 0; i < numLines; i++)
			{
				// need the line end offset to be in place
				// for following fold level calculations
				lineInfo[startLine + i] = ((offset + endOffsets.get(i) + 1)
					& ~(FOLD_LEVEL_VALID_MASK | CONTEXT_VALID_MASK)
					| visible);
			}

			//{{{ Unrolled
			if((visible & (1L << (VISIBLE_SHIFT + 0))) != 0)
				virtualLineCounts[0] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 1))) != 0)
				virtualLineCounts[1] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 2))) != 0)
				virtualLineCounts[2] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 3))) != 0)
				virtualLineCounts[3] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 4))) != 0)
				virtualLineCounts[4] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 5))) != 0)
				virtualLineCounts[5] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 6))) != 0)
				virtualLineCounts[6] += numLines;
			if((visible & (1L << (VISIBLE_SHIFT + 7))) != 0)
				virtualLineCounts[7] += numLines;
			//}}}
		} //}}}
		//{{{ Update remaining line start offsets
		for(int i = endLine; i < lineCount; i++)
		{
			setLineEndOffset(i,getLineEndOffset(i) + length);
		} //}}}

		updatePositionsForInsert(offset,length);
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int startLine, int offset,
		int numLines, int length)
	{
		//{{{ Update virtual line counts
		for(int i = 0; i < numLines; i++)
		{
			long info = lineInfo[startLine + i];

			// Unrolled for max efficency
			if((info & (1L << (VISIBLE_SHIFT + 0))) != 0)
				virtualLineCounts[0]--;
			if((info & (1L << (VISIBLE_SHIFT + 1))) != 0)
				virtualLineCounts[1]--;
			if((info & (1L << (VISIBLE_SHIFT + 2))) != 0)
				virtualLineCounts[2]--;
			if((info & (1L << (VISIBLE_SHIFT + 3))) != 0)
				virtualLineCounts[3]--;
			if((info & (1L << (VISIBLE_SHIFT + 4))) != 0)
				virtualLineCounts[4]--;
			if((info & (1L << (VISIBLE_SHIFT + 5))) != 0)
				virtualLineCounts[5]--;
			if((info & (1L << (VISIBLE_SHIFT + 6))) != 0)
				virtualLineCounts[6]--;
			if((info & (1L << (VISIBLE_SHIFT + 7))) != 0)
				virtualLineCounts[7]--;
		} //}}}

		//{{{ Update line info and line context arrays
		if(numLines > 0)
		{
			lineCount -= numLines;
			System.arraycopy(lineInfo,startLine + numLines,lineInfo,
				startLine,lineCount - startLine);
			System.arraycopy(lineContext,startLine + numLines,lineContext,
				startLine,lineCount - startLine);
		} //}}}

		//{{{ Update remaining line start offsets
		for(int i = startLine; i < lineCount; i++)
		{
			setLineEndOffset(i,getLineEndOffset(i) - length);
		} //}}}

		updatePositionsForRemove(offset,length);
	} //}}}

	//{{{ linesChanged() method
	public void linesChanged(int startLine, int numLines)
	{
		for(int i = 0; i < numLines; i++)
		{
			lineInfo[startLine + i] &= ~(FOLD_LEVEL_VALID_MASK
				| CONTEXT_VALID_MASK);
			lineContext[startLine + i] = null;
		}
	} //}}}

	//{{{ Private members

	/* {{{ Format of entires in line info array:
	 * 0-31: end offset
	 * 32-47: fold level
	 * 48-55: visibility bit flags
	 * 56: fold level valid flag
	 * 57: context valid flag
	 * 58-63: reserved
	 *
	 * Having all the info packed into a long is not very OO and makes the
	 * code somewhat more complicated, but it saves a lot of memory.
	 *
	 * The new document model has just 12 bytes of overhead per line.
	 * LineContext instances are now internalized, so only a few should
	 * actually be in the heap.
	 *
	 * In the old document model there were 5 objects per line, for a
	 * total of about 100 bytes, plus a cached token list, which used
	 * another 100 or so bytes.
	 * }}}*/
	private static final long END_MASK = 0x00000000ffffffffL;
	private static final long FOLD_LEVEL_MASK = 0x0000ffff00000000L;
	private static final int FOLD_LEVEL_SHIFT = 32;
	private static final long VISIBLE_MASK = 0x00ff000000000000L;
	private static final int VISIBLE_SHIFT = 48;
	private static final long FOLD_LEVEL_VALID_MASK = (1L<<56);
	private static final long CONTEXT_VALID_MASK = (1L<<57);

	//{{{ Instance variables
	private Buffer buffer;
	private long[] lineInfo;
	private TokenMarker.LineContext[] lineContext;

	private int lineCount;

	private PosBottomHalf[] positions;
	private int positionCount;

	private int[] virtualLineCounts;
	//}}}

	//{{{ setLineEndOffset() method
	private final void setLineEndOffset(int line, int end)
	{
		lineInfo[line] = ((lineInfo[line] & ~(END_MASK
			| FOLD_LEVEL_VALID_MASK | CONTEXT_VALID_MASK)) | end);
	} //}}}

	//{{{ growPositionArray() method
	private void growPositionArray()
	{
		if(positions.length < positionCount + 1)
		{
			PosBottomHalf[] newPositions = new PosBottomHalf[
				(positionCount + 1) * 2];
			System.arraycopy(positions,0,newPositions,0,positionCount);
			positions = newPositions;
		}
	} //}}}

	//{{{ removePosition() method
	private synchronized void removePosition(PosBottomHalf bh)
	{
		int index = -1;

		for(int i = 0; i < positionCount; i++)
		{
			if(positions[i] == bh)
			{
				index = i;
				break;
			}
		}

		System.arraycopy(positions,index + 1,positions,index,
			positionCount - index - 1);
		positions[--positionCount] = null;
	} //}}}

	//{{{ updatePositionsForInsert() method
	private void updatePositionsForInsert(int offset, int length)
	{
		if(positionCount == 0)
			return;

		int start = getPositionAtOffset(offset);

		for(int i = start; i < positionCount; i++)
		{
			PosBottomHalf bh = positions[i];
			if(bh.offset < offset)
				Log.log(Log.ERROR,this,"Screwed up: " + bh.offset);
			else
				bh.offset += length;
		}
	} //}}}

	//{{{ updatePositionsForRemove() method
	private void updatePositionsForRemove(int offset, int length)
	{
		if(positionCount == 0)
			return;

		int start = getPositionAtOffset(offset);

		for(int i = start; i < positionCount; i++)
		{
			PosBottomHalf bh = positions[i];
			if(bh.offset < offset)
				Log.log(Log.ERROR,this,"Screwed up: " + bh.offset);
			else if(bh.offset < offset + length)
				bh.offset = offset;
			else
				bh.offset -= length;
		}
	} //}}}

	//{{{ getPositionAtOffset() method
	private int getPositionAtOffset(int offset)
	{
		int start = 0;
		int end = positionCount - 1;

		PosBottomHalf bh;

loop:		for(;;)
		{
			switch(end - start)
			{
			case 0:
				bh = positions[start];
				if(bh.offset < offset)
					start++;
				break loop;
			case 1:
				bh = positions[end];
				if(bh.offset < offset)
				{
					start = end + 1;
				}
				else
				{
					bh = positions[start];
					if(bh.offset < offset)
					{
						start++;
					}
				}
				break loop;
			default:
				int pivot = (start + end) / 2;
				bh = positions[pivot];
				if(bh.offset > offset)
					end = pivot - 1;
				else
					start = pivot + 1;
				break;
			}
		}

		return start;
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ PosTopHalf class
	static class PosTopHalf implements Position
	{
		PosBottomHalf bh;

		//{{{ PosTopHalf constructor
		PosTopHalf(PosBottomHalf bh)
		{
			this.bh = bh;
			bh.ref();
		} //}}}

		//{{{ getOffset() method
		public int getOffset()
		{
			return bh.offset;
		} //}}}

		//{{{ finalize() method
		public void finalize()
		{
			bh.unref();
		} //}}}
	} //}}}

	//{{{ PosBottomHalf class
	class PosBottomHalf
	{
		int offset;
		int ref;

		//{{{ PosBottomHalf constructor
		PosBottomHalf(int offset)
		{
			this.offset = offset;
		} //}}}

		//{{{ ref() method
		void ref()
		{
			ref++;
		} //}}}

		//{{{ unref() method
		void unref()
		{
			if(--ref == 0)
				removePosition(this);
		} //}}}
	} //}}}

	//}}}
}
