/*
 * LineManager.java - Manages line info, line start offsets, positions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
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
import org.gjt.sp.jedit.Debug;
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
 * @since jEdit 4.2pre3
 */
public class LineManager
{
	//{{{ LineManager constructor
	public LineManager()
	{
		endOffsets = new int[1];
		endOffsets[0] = 1;
		lineInfo = new int[1];
		lineInfo[0] = (1 << SCREEN_LINES_SHIFT);
		lineContext = new TokenMarker.LineContext[1];
		lineCount = 1;
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
		if(gapLine != -1 && line >= gapLine)
			return endOffsets[line] + gapWidth;
		else
			return endOffsets[line];
	} //}}}

	//{{{ getFoldLevel() method
	public final int getFoldLevel(int line)
	{
		return (lineInfo[line] & FOLD_LEVEL_MASK);
	} //}}}

	//{{{ setFoldLevel() method
	// Also sets 'fold level valid' flag
	public final void setFoldLevel(int line, int level)
	{
		if(level > 0xffff)
		{
			// limitations...
			level = 0xffff;
		}

		lineInfo[line] = ((lineInfo[line] & ~FOLD_LEVEL_MASK) | level);
	} //}}}

	//{{{ setFirstInvalidFoldLevel() method
	public void setFirstInvalidFoldLevel(int firstInvalidFoldLevel)
	{
		this.firstInvalidFoldLevel = firstInvalidFoldLevel;
	} //}}}

	//{{{ getFirstInvalidFoldLevel() method
	public int getFirstInvalidFoldLevel()
	{
		return firstInvalidFoldLevel;
	} //}}}

	//{{{ isScreenLineCountValid() method
	public final boolean isScreenLineCountValid(int line)
	{
		return (lineInfo[line] & SCREEN_LINES_VALID_MASK) != 0;
	} //}}}

	//{{{ getScreenLineCount() method
	public final int getScreenLineCount(int line)
	{
		return ((lineInfo[line] & SCREEN_LINES_MASK)
			>> SCREEN_LINES_SHIFT);
	} //}}}

	//{{{ setScreenLineCount() method
	public final void setScreenLineCount(int line, int count)
	{
		if(count > 0x7fff)
		{
			// limitations...
			count = 0x7fff;
		}

		if(Debug.SCREEN_LINES_DEBUG)
			Log.log(Log.DEBUG,this,new Exception("setScreenLineCount(" + line + "," + count + ")"));
		lineInfo[line] =
			((lineInfo[line] & ~SCREEN_LINES_MASK)
			| (count << SCREEN_LINES_SHIFT)
			| SCREEN_LINES_VALID_MASK);
	} //}}}

	//{{{ getLineContext() method
	public final TokenMarker.LineContext getLineContext(int line)
	{
		return lineContext[line];
	} //}}}

	//{{{ setLineContext() method
	public final void setLineContext(int line, TokenMarker.LineContext context)
	{
		lineContext[line] = context;
	} //}}}

	//{{{ setFirstInvalidLineContext() method
	public void setFirstInvalidLineContext(int firstInvalidLineContext)
	{
		this.firstInvalidLineContext = firstInvalidLineContext;
	} //}}}

	//{{{ getFirstInvalidLineContext() method
	public int getFirstInvalidLineContext()
	{
		return firstInvalidLineContext;
	} //}}}

	//{{{ invalidateScreenLineCounts() method
	public void invalidateScreenLineCounts()
	{
		for(int i = 0; i < lineCount; i++)
			lineInfo[i] &= ~SCREEN_LINES_VALID_MASK;
	} //}}}

	//{{{ _contentInserted() method
	public void _contentInserted(IntegerArray endOffsets)
	{
		gapLine = -1;
		gapWidth = 0;
		firstInvalidLineContext = firstInvalidFoldLevel = 0;
		lineCount = endOffsets.getSize();
		this.endOffsets = endOffsets.getArray();
		lineInfo = new int[lineCount];
		for(int i = 0; i < lineInfo.length; i++)
			lineInfo[i] = 1 << SCREEN_LINES_SHIFT;

		lineContext = new TokenMarker.LineContext[lineCount];
	} //}}}

	//{{{ contentInserted() method
	public void contentInserted(int startLine, int offset,
		int numLines, int length, IntegerArray endOffsets)
	{
		int endLine = startLine + numLines;
		lineInfo[startLine] &= ~SCREEN_LINES_VALID_MASK;

		//{{{ Update line info and line context arrays
		if(numLines > 0)
		{
			//moveGap(-1,0,"contentInserted");

			lineCount += numLines;

			if(this.endOffsets.length <= lineCount)
			{
				int[] endOffsetsN = new int[(lineCount + 1) * 2];
				System.arraycopy(this.endOffsets,0,endOffsetsN,0,
						 this.endOffsets.length);
				this.endOffsets = endOffsetsN;
			}

			if(lineInfo.length <= lineCount)
			{
				int[] lineInfoN = new int[(lineCount + 1) * 2];
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

			System.arraycopy(this.endOffsets,startLine,
				this.endOffsets,endLine,lineCount - endLine);
			System.arraycopy(lineInfo,startLine,lineInfo,
				endLine,lineCount - endLine);
			System.arraycopy(lineContext,startLine,lineContext,
				endLine,lineCount - endLine);

			if(startLine <= gapLine)
				gapLine += numLines;
			else if(gapLine != -1)
				offset -= gapWidth;

			if(startLine < firstInvalidLineContext)
				firstInvalidLineContext += numLines;

			for(int i = 0; i < numLines; i++)
			{
				this.endOffsets[startLine + i] = (offset + endOffsets.get(i));
				lineInfo[startLine + i] = 0;
			}
		} //}}}

		if(firstInvalidFoldLevel == -1 || firstInvalidFoldLevel > startLine)
			firstInvalidFoldLevel = startLine;
		moveGap(endLine,length,"contentInserted");
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
			//moveGap(-1,0,"contentRemoved");

			if(startLine + numLines < gapLine)
				gapLine -= numLines;
			else if(startLine < gapLine)
				gapLine = startLine;

			if(startLine + numLines < firstInvalidLineContext)
				firstInvalidLineContext -= numLines;
			else if(startLine < firstInvalidLineContext)
				firstInvalidLineContext = startLine - 1;

			lineCount -= numLines;

			System.arraycopy(endOffsets,endLine,endOffsets,
				startLine,lineCount - startLine);
			System.arraycopy(lineInfo,endLine,lineInfo,
				startLine,lineCount - startLine);
			System.arraycopy(lineContext,endLine,lineContext,
				startLine,lineCount - startLine);
		} //}}}

		if(firstInvalidFoldLevel == -1 || firstInvalidFoldLevel > startLine)
			firstInvalidFoldLevel = startLine;
		moveGap(startLine,-length,"contentRemoved");
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	/* Having all the info packed into an int is not very OO and makes the
	 * code somewhat more complicated, but it saves a lot of memory.
	 *
	 * The new document model has just 12 bytes of overhead per line.
	 * LineContext instances are now internalized, so only a few should
	 * actually be in the heap.
	 *
	 * In the old document model there were 5 objects per line, for a
	 * total of about 100 bytes, plus a cached token list, which used
	 * another 100 or so bytes. */
	private static final int FOLD_LEVEL_MASK         = 0x00ffff;
	private static final int SCREEN_LINES_MASK       = 0x7fff00;
	private static final int SCREEN_LINES_SHIFT      = 16;
	private static final int SCREEN_LINES_VALID_MASK = 1<<31;

	private int[] endOffsets;
	private int[] lineInfo;
	private TokenMarker.LineContext[] lineContext;

	private int lineCount;

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
	private int firstInvalidLineContext;

	/**
	 * If -1, all fold levels are valid. Otherwise, all lines after this
	 * have an invalid fold level.
	 */
	private int firstInvalidFoldLevel;
	//}}}

	//{{{ setLineEndOffset() method
	private final void setLineEndOffset(int line, int end)
	{
		endOffsets[line] = end;
	} //}}}

	//{{{ moveGap() method
	private final void moveGap(int newGapLine, int newGapWidth, String method)
	{
		if(gapLine == -1)
			gapWidth = newGapWidth;
		else if(newGapLine == -1)
		{
			if(gapWidth != 0)
			{
				if(Debug.OFFSET_DEBUG && gapLine != lineCount)
					Log.log(Log.DEBUG,this,method + ": update from " + gapLine + " to " + lineCount + " width " + gapWidth);
				for(int i = gapLine; i < lineCount; i++)
					setLineEndOffset(i,getLineEndOffset(i));
			}

			gapWidth = newGapWidth;
		}
		else if(newGapLine < gapLine)
		{
			if(gapWidth != 0)
			{
				if(Debug.OFFSET_DEBUG && newGapLine != gapLine)
					Log.log(Log.DEBUG,this,method + ": update from " + newGapLine + " to " + gapLine + " width " + gapWidth);
				for(int i = newGapLine; i < gapLine; i++)
					setLineEndOffset(i,getLineEndOffset(i) - gapWidth);
			}
			gapWidth += newGapWidth;
		}
		else //if(newGapLine >= gapLine)
		{
			if(gapWidth != 0)
			{
				if(Debug.OFFSET_DEBUG && gapLine != newGapLine)
					Log.log(Log.DEBUG,this,method + ": update from " + gapLine + " to " + newGapLine + " width " + gapWidth);
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

	//}}}
}
