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
import javax.swing.text.Segment;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
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
		lineInfo[0] = (0xff << VISIBLE_SHIFT);
		lineContext = new TokenMarker.LineContext[1];
		lineCount = 1;
	} //}}}

	//{{{ getLineCount() method
	public final int getLineCount()
	{
		return lineCount;
	} //}}}

	//{{{ getLineStartOffset() method
	public final int getLineStartOffset(int line)
	{
		return (int)(lineInfo[line] & START_MASK);
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
		long info = lineInfo[line];
		lineInfo[line] = (info & ~FOLD_LEVEL_MASK
			| (level << FOLD_LEVEL_SHIFT)
			| FOLD_LEVEL_VALID_MASK);
	} //}}}

	//{{{ isLineVisible() method
	public final boolean isLineVisible(int line, int index)
	{
		int mask = 1 << (index + VISIBLE_SHIFT);
		return (lineInfo[line] & mask) != 0;
	} //}}}

	//{{{ setLineVisible() method
	public final void setLineVisible(int line, int index, boolean visible)
	{
		int mask = 1 << (index + VISIBLE_SHIFT);
		if(visible)
			lineInfo[line] = (lineInfo[line] | mask);
		else
			lineInfo[line] = (lineInfo[line] | ~mask);
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

	//{{{ contentInserted() method
	public void contentInserted(int startLine, int offset,
		int numLines, int length, int[] startOffsets)
	{
		if(numLines > 0)
		{
			int endLine = startLine + numLines;

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

			System.arraycopy(lineInfo,startLine,lineInfo,endLine,
				lineInfo.length - endLine);

			for(int i = 0; i < numLines; i++)
			{
				lineInfo[startLine + i] = ((offset + startOffsets[i])
					| (0xff << VISIBLE_SHIFT));
			}
		}

		// TODO: update remaining line start offsets
		// TODO: positions

		linesChanged(startLine,lineCount - startLine);
	} //}}}

	//{{{ contentRemoved() method
	public void contentRemoved(int startLine, int offset,
		int numLines, int length)
	{
		if(numLines > 0)
		{
			lineCount -= numLines;
			System.arraycopy(lineInfo,startLine + numLines,lineInfo,
				startLine,lineInfo.length - startLine - numLines);
		}

		// TODO: update line start offsets
		// TODO: positions

		linesChanged(startLine,lineCount - startLine);
	} //}}}

	//{{{ linesChanged() method
	public void linesChanged(int startLine, int numLines)
	{
		for(int i = 0; i < numLines; i++)
		{
			lineInfo[startLine + i] &= ~(FOLD_LEVEL_VALID_MASK
				| CONTEXT_VALID_MASK);
		}
	} //}}}

	//{{{ Private members

	/* {{{ Format of entires in line info array:
	 * 0-31: start
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
	private static final long START_MASK = 0x00000000ffffffffL;
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
	//}}}

	//{{{ setLineStartOffset() method
	private final void setLineStartOffset(int line, int start)
	{
		lineInfo[line] = ((lineInfo[line] & ~START_MASK) | start);
	} //}}}

	//}}}
}
