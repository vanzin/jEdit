/*
 * OffsetManager.java - Manages line info, line start offsets, positions
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
import javax.swing.text.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.LongArray;
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
	public static final long MAX_DISPLAY_COUNT = 8;

	/* Having all the info packed into a long is not very OO and makes the
	 * code somewhat more complicated, but it saves a lot of memory.
	 *
	 * The new document model has just 12 bytes of overhead per line.
	 * LineContext instances are now internalized, so only a few should
	 * actually be in the heap.
	 *
	 * In the old document model there were 5 objects per line, for a
	 * total of about 100 bytes, plus a cached token list, which used
	 * another 100 or so bytes. */
	public static final long END_MASK                = 0x00000000ffffffffL;
	public static final long FOLD_LEVEL_MASK         = 0x000000ff00000000L;
	public static final int FOLD_LEVEL_SHIFT         = 32;
	public static final long VISIBLE_MASK            = 0x0000ff0000000000L;
	public static final int VISIBLE_SHIFT            = 40;
	public static final long SCREEN_LINES_MASK       = 0x00ff000000000000L;
	public static final long SCREEN_LINES_SHIFT      = 48;
	public static final long SCREEN_LINES_VALID_MASK = (1L<<60);
	public static final long FOLD_LEVEL_VALID_MASK   = (1L<<61);
	public static final long LINE_CONTEXT_VALID_MASK = (1L<<62);

	//{{{ OffsetManager constructor
	public OffsetManager(Buffer buffer)
	{
		this.buffer = buffer;

		positions = new PosBottomHalf[100];
	} //}}}

	//{{{ getLineCount() method
	public final int getLineCount()
	{
		return lineCount;
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
		int end = (int)(lineInfo[line] & END_MASK);
		if(gapLine != -1 && line >= gapLine)
			return end + gapWidth;
		else
			return end;
	} //}}}

	//{{{ isFoldLevelValid() method
	public final boolean isFoldLevelValid(int line)
	{
		if(gapLine != -1 && line >= gapLine)
			return false;

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
		if(level > 0xff)
		{
			// limitations...
			level = 0xff;
		}

		if(gapLine != -1 && line >= gapLine)
			moveGap(line + 1,0,"setFoldLevel");

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
		long info = lineInfo[line];
		long mask = 1L << (index + VISIBLE_SHIFT);
		boolean oldVisible = ((info & mask) != 0);
		if(visible)
		{
			if(!oldVisible)
			{
				int screenLines = getScreenLineCount(line);
				Anchor anchor = anchors;
				for(;;)
				{
					if(anchor == null || anchor.physicalLine < line)
						break;

					if(anchor.index == index)
					{
						anchor.scrollLine += screenLines;
						anchor.callChanged = true;
					}
					anchor = anchor.next;
				}
			}
			lineInfo[line] = (info | mask);
		}
		else
		{
			if(oldVisible)
			{
				int screenLines = getScreenLineCount(line);
				Anchor anchor = anchors;
				for(;;)
				{
					if(anchor == null || anchor.physicalLine < line)
						break;

					if(anchor.index == index)
					{
						anchor.scrollLine -= screenLines;
						anchor.callChanged = true;
					}
					anchor = anchor.next;
				}
			}
			lineInfo[line] = (info & ~mask);
		}
	} //}}}

	//{{{ isScreenLineCountValid() method
	public final boolean isScreenLineCountValid(int line)
	{
		return (lineInfo[line] & SCREEN_LINES_VALID_MASK) != 0;
	} //}}}

	//{{{ getScreenLineCount() method
	public final int getScreenLineCount(int line)
	{
		return (int)((lineInfo[line] & SCREEN_LINES_MASK)
			>> SCREEN_LINES_SHIFT);
	} //}}}

	//{{{ setScreenLineCount() method
	public final void setScreenLineCount(int line, int count)
	{
		long info = lineInfo[line];
		int oldCount = (int)((info & SCREEN_LINES_MASK) >> SCREEN_LINES_SHIFT);
		if(oldCount != count)
		{
			Anchor anchor = anchors;
			for(;;)
			{
				if(anchor == null || anchor.physicalLine <= line)
					break;

				long anchorVisibilityMask = (1L << (anchor.index + VISIBLE_SHIFT));
				if((info & anchorVisibilityMask) != 0)
				{
					//System.err.println("anchor screen shift from "
					//	+ anchor.scrollLine + " to "
					//	+ (anchor.scrollLine + (count - oldCount)));
					if(count != oldCount)
					{
						anchor.scrollLine += (count - oldCount);
						anchor.callChanged = true;
					}
				}
				anchor = anchor.next;
			}
		}
		lineInfo[line] = ((info & ~SCREEN_LINES_MASK)
			| ((long)count << SCREEN_LINES_SHIFT)
			| SCREEN_LINES_VALID_MASK);
	} //}}}

	//{{{ isLineContextValid() method
	public final boolean isLineContextValid(int line)
	{
		return line <= lastValidLineContext
			&& (lineInfo[line] & LINE_CONTEXT_VALID_MASK) != 0;
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
		if(line > lastValidLineContext)
		{
			// maybe this loop only ever happends in text mode?
			for(int i = lastValidLineContext + 1; i < line; i++)
			{
				lineInfo[i] &= ~LINE_CONTEXT_VALID_MASK;
			}

			lastValidLineContext = line;
		}

		lineContext[line] = context;
		lineInfo[line] |= LINE_CONTEXT_VALID_MASK;
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

		if(foldLevel == 0)
		{
			newVirtualLineCount = lineCount;

			for(int i = 0; i < lineCount; i++)
				lineInfo[i] |= VISIBLE_MASK;
		}
		else
		{
			if(buffer.getFoldHandler() instanceof IndentFoldHandler)
				foldLevel = (foldLevel - 1) * buffer.getIndentSize() + 1;

			/* this ensures that the first line is always visible */
			boolean seenVisibleLine = false;

			for(int i = 0; i < lineCount; i++)
			{
				if(!seenVisibleLine || buffer.getFoldLevel(i) < foldLevel)
				{
					seenVisibleLine = true;
					lineInfo[i] |= VISIBLE_MASK;
					newVirtualLineCount++;
				}
				else
					lineInfo[i] &= ~VISIBLE_MASK;
			}
		}

		Anchor anchor = anchors;
		while(anchor != null)
		{
			anchor.reset();
			anchor = anchor.next;
		}
	} //}}}

	//{{{ invalidateScreenLineCounts() method
	public void invalidateScreenLineCounts()
	{
		for(int i = 0; i < lineCount; i++)
			lineInfo[i] &= ~SCREEN_LINES_VALID_MASK;
	} //}}}

	//{{{ addLineEndOffset() method
	public static void addLineEndOffset(LongArray endOffsets, int offset)
	{
		endOffsets.add(offset | (0xffL << VISIBLE_SHIFT)
			| (1L << SCREEN_LINES_SHIFT));
	} //}}}

	//{{{ _contentInserted() method
	public void _contentInserted(LongArray endOffsets)
	{
		gapLine = lastValidLineContext = -1;
		gapWidth = 0;
		lineCount = endOffsets.getSize();
		lineInfo = endOffsets.getArray();
		if(lineContext == null || lineContext.length <= lineCount)
			lineContext = new TokenMarker.LineContext[lineCount];

		for(int i = 0; i < positionCount; i++)
			positions[i].offset = 0;

		Anchor anchor = anchors;
		while(anchor != null)
		{
			anchor.reset();
			anchor = anchor.next;
		}
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int startLine, int offset,
		int numLines, int length, LongArray endOffsets)
	{
		int endLine = startLine + numLines;
		lineInfo[startLine] &= ~SCREEN_LINES_VALID_MASK;

		//{{{ Update line info and line context arrays
		if(numLines > 0)
		{
			moveGap(-1,0,"contentInserted");

			lineCount += numLines;
			if(startLine < lastValidLineContext)
				lastValidLineContext += numLines;

			if(lineInfo.length <= lineCount)
			{
				long[] lineInfoN = new long[(lineCount + 1) * 2];
				System.arraycopy(lineInfo,0,lineInfoN,0,
						 lineInfo.length);
				lineInfo = lineInfoN;
			}

			if(lineContext.length <= lineCount)
			{
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
				lineInfo[startLine + i] = (offset
					+ endOffsets.get(i))
					| visible;
			}

			Anchor anchor = anchors;
			for(;;)
			{
				if(anchor == null || anchor.physicalLine <= startLine)
					break;

				anchor.physicalLine += numLines;
				anchor.callChanged = true;
				anchor = anchor.next;
			}
		} //}}}

		moveGap(endLine,length,"contentInserted");

		updatePositionsForInsert(offset,length);
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int startLine, int offset,
		int numLines, int length)
	{
		int endLine = startLine + numLines;
		lineInfo[startLine] &= ~SCREEN_LINES_VALID_MASK;

		//{{{ Update line info and line context arrays
		if(numLines > 0)
		{
			moveGap(-1,0,"contentRemoved");

			lineCount -= numLines;
			if(startLine < lastValidLineContext)
				lastValidLineContext -= numLines;
			else if(startLine >= lastValidLineContext
				&& startLine + numLines < lastValidLineContext)
				lastValidLineContext = startLine - 1;

			// if anchor's physical line > startLine,
			// count screen lines from startLine to Math.min(startLine + numLines,line)
			Anchor anchor = anchors;
			for(;;)
			{
				if(anchor == null || anchor.physicalLine <= startLine)
					break;

				int end = Math.min(endLine,anchor.physicalLine);
				for(int i = startLine; i < end; i++)
				{
					if(isLineVisible(i,anchor.index))
						anchor.scrollLine -= getScreenLineCount(i);
					anchor.physicalLine--;
					anchor.callChanged = true;
				}

				anchor = anchor.next;
			}

			System.arraycopy(lineInfo,endLine,lineInfo,
				startLine,lineCount - startLine);
			System.arraycopy(lineContext,endLine,lineContext,
				startLine,lineCount - startLine);
		} //}}}

		moveGap(startLine,-length,"contentRemoved");

		updatePositionsForRemove(offset,length);
	} //}}}

	//{{{ lineInfoChangedFrom() method
	public void lineInfoChangedFrom(int startLine)
	{
		moveGap(startLine,0,"lineInfoChangedFrom");
	} //}}}

	//{{{ lineContextInvalidFrom() method
	public void lineContextInvalidFrom(int startLine)
	{
		lastValidLineContext = Math.min(lastValidLineContext,startLine);
	} //}}}

	//{{{ getLastValidLineContext() method
	public int getLastValidLineContext()
	{
		return lastValidLineContext;
	} //}}}

	//{{{ addAnchor() method
	/* note the suttle optimization: this method sorts anchors in decreasing
	 * order so a change on line n can stop checking anchors once reaching
	 * one before line n. */
	public void addAnchor(Anchor anchor)
	{
		Anchor prev = null;
		Anchor current = anchors;
		for(;;)
		{
			if(current == null || current.physicalLine < anchor.physicalLine)
			{
				if(prev != null)
					prev.next = anchor;
				else
					anchors = anchor;
				anchor.next = current;
				return;
			}
			prev = current;
			current = current.next;
		}
	} //}}}

	//{{{ removeAnchor() method
	public void removeAnchor(Anchor anchor)
	{
		Anchor current = anchors;
		Anchor prev = null;
		while(current != null)
		{
			if(current == anchor)
			{
				if(prev != null)
					prev.next = current.next;
				else
					anchors = current.next;
				return;
			}
			prev = current;
			current = current.next;
		}
	} //}}}

	//{{{ notifyScreenLineChanges() method
	public void notifyScreenLineChanges()
	{
		Anchor anchor = anchors;
		while(anchor != null)
		{
			if(anchor.callChanged)
			{
				anchor.callChanged = false;
				anchor.changed();
			}
			anchor = anchor.next;
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Buffer buffer;
	private long[] lineInfo;
	private TokenMarker.LineContext[] lineContext;

	private int lineCount;

	private PosBottomHalf[] positions;
	private int positionCount;

	private Anchor anchors;

	/**
	 * If -1, then there is no gap.
	 * Otherwise, all lines from this line onwards need to have gapWidth
	 * added to their end offsets.
	 */
	private int gapLine;
	private int gapWidth;

	/**
	 * If -1, all contexts are valid. Otherwise, all lines after this have
	 * an invalid context.
	 */
	private int lastValidLineContext;
	//}}}

	//{{{ setLineEndOffset() method
	private final void setLineEndOffset(int line, int end)
	{
		lineInfo[line] = ((lineInfo[line] & ~(END_MASK
			| FOLD_LEVEL_VALID_MASK)) | end);
		// what is the point of this -- DO NOT UNCOMMENT THIS IT
		// CAUSES A PERFORMANCE LOSS; nextLineRequested becomes true
		//lineContext[line] = null;
	} //}}}

	//{{{ moveGap() method
	private final void moveGap(int newGapLine, int newGapWidth, String method)
	{
		if(gapLine == -1)
			gapWidth = newGapWidth;
		else if(newGapLine == -1)
		{
			//if(gapWidth != 0)
			{
				//if(DEBUG && gapLine != lineCount)
				//	System.err.println(method + ": update from " + gapLine + " to " + lineCount);
				for(int i = gapLine; i < lineCount; i++)
					setLineEndOffset(i,getLineEndOffset(i));
			}

			gapWidth = newGapWidth;
		}
		else if(newGapLine < gapLine)
		{
			//if(gapWidth != 0)
			{
				//if(DEBUG && newGapLine != gapLine)
				//	System.err.println(method + ": update from " + newGapLine + " to " + gapLine);
				for(int i = newGapLine; i < gapLine; i++)
					setLineEndOffset(i,getLineEndOffset(i) - gapWidth);
			}
			gapWidth += newGapWidth;
		}
		else //if(newGapLine >= gapLine)
		{
			//if(gapWidth != 0)
			{
				//if(DEBUG && gapLine != newGapLine)
				//	System.err.println(method + ": update from " + gapLine + " to " + newGapLine);
				for(int i = gapLine; i < newGapLine; i++)
					setLineEndOffset(i,getLineEndOffset(i));
			}

			gapWidth += newGapWidth;
		}

		if(newGapLine == lineCount)
			gapLine = -1;
		else
			gapLine = newGapLine;
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

	//{{{ Anchor class
	/**
	 * An anchor is a floating position retaining a scroll line number.
	 */
	public static abstract class Anchor
	{
		public Anchor(int index)
		{
			this.index = index;
		}

		public Anchor next;

		public int physicalLine;
		public int scrollLine;
		public int index;
		public boolean callChanged;

		public abstract void reset();
		public abstract void changed();
	} //}}}

	//}}}
}
