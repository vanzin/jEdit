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
		lineContext = new Object[1];
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
		return (lineInfo[line] & (index + VISIBLE_SHIFT)) != 0;
	} //}}}

	//{{{ setLineVisible() method
	public final void setLineVisible(int line, int index, boolean visible)
	{
		int shift = 1 << (index + VISIBLE_SHIFT);
		if(visible)
			lineInfo[line] = (lineInfo[line] | shift);
		else
			lineInfo[line] = (lineInfo[line] | ~shift);
	} //}}}

	//{{{ getLineContext() method
	public TokenMarker.LineContext getLineContext(int line)
	{
		Object obj = lineContext[line];

		// to avoid creating a line context for each line
		// (= memory waste) we only create a separate instance
		// for delegated lines, using the SIMPLE_CONTEXT static
		// instance for other lines.
		if(obj instanceof TokenMarker.LineContext)
			return (TokenMarker.LineContext)obj;
		else if(obj instanceof ParserRule)
		{
			SIMPLE_CONTEXT.inRule = (ParserRule)obj;
			SIMPLE_CONTEXT.rules =
				buffer.getTokenMarker().getMainRuleSet();
			SIMPLE_CONTEXT.parent = null;
			return SIMPLE_CONTEXT;
		}
		else
			return null;
	} //}}}

	//{{{ setLineContext() method
	// Also sets the 'context valid' flag
	public void setLineContext(int line, TokenMarker.LineContext context)
	{
		// to avoid creating a line context for each line
		// (= memory waste) we only create a separate instance
		// for delegated lines, using the SIMPLE_CONTEXT static
		// instance for other lines.
		if(context.parent != null)
		{
			if(context == SIMPLE_CONTEXT)
				context = (TokenMarker.LineContext)context.clone();
			lineContext[line] = context;
		}
		else
			lineContext[line] = context.inRule;

		lineInfo[line] = lineInfo[line] |= CONTEXT_VALID_MASK;
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

				Object[] lineContextN = new Object[(lineCount + 1) * 2];
				System.arraycopy(lineContext,0,lineContextN,0,
						 lineContext.length);
				lineContext = lineContextN;
			}

			long prev = (startLine == 0
				? 0L : lineInfo[startLine - 1]);

			System.arraycopy(lineInfo,startLine,lineInfo,endLine,
				lineInfo.length - endLine);

			for(int i = 0; i < numLines; i++)
			{
				int start = (i == 0 ? getLineStartOffset(startLine)
					+ startOffsets[0]
					: startOffsets[i]);

				lineInfo[startLine + i] = (startOffsets[i]
					| (prev & VISIBLE_MASK));
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

	//{{{ Static variables
	private static final TokenMarker.LineContext SIMPLE_CONTEXT
		= new TokenMarker.LineContext();

	/* {{{ Format of entires in this array:
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
	 * jEdit has 12 bytes of overhead per line; with objects, that would be
	 * about 30, plus the garbage collector overhead.
	 * }}}*/
	private static final long START_MASK = 0x00000000ffffffffL;
	private static final long FOLD_LEVEL_MASK = 0x0000ffff00000000L;
	private static final int FOLD_LEVEL_SHIFT = 32;
	private static final long VISIBLE_MASK = 0x00ff000000000000L;
	private static final int VISIBLE_SHIFT = 48;
	private static final long FOLD_LEVEL_VALID_MASK = (1L<<56);
	private static final long CONTEXT_VALID_MASK = (1L<<57);

	//}}}

	//{{{ Instance variables
	private Buffer buffer;
	private long[] lineInfo;

	// entries in this array are either ParserRule or LineContext instances
	private Object[] lineContext;

	private int lineCount;
	//}}}

	//{{{ setLineStartOffset() method
	public final void setLineStartOffset(int line, int start)
	{
		lineInfo[line] = ((lineInfo[line] & ~START_MASK) | start);
	} //}}}

	//}}}
}
