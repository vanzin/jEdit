/*
 * Selection.java - Selected text
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

package org.gjt.sp.jedit.textarea;

//{{{ Imports
import javax.swing.text.Segment;
import java.util.ArrayList;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.MiscUtilities;
//}}}

/**
 * An interface representing a portion of the current selection.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 3.2pre1
 */
public abstract class Selection implements Cloneable
{
	//{{{ getStart() method
	/**
	 * Returns the start offset of this selection.
	 */
	public int getStart()
	{
		return start;
	} //}}}

	//{{{ getEnd() method
	/**
	 * Returns the end offset of this selection.
	 */
	public int getEnd()
	{
		return end;
	} //}}}

	//{{{ getStart() method
	/**
	 * Returns the start offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 * @since jEdit 4.1pre1
	 */
	public abstract int getStart(Buffer buffer, int line);
	//}}}

	//{{{ getEnd() method
	/**
	 * Returns the end offset of this selection on the specified
	 * line.
	 * @param buffer The buffer
	 * @param line The line number
	 * @since jEdit 4.1pre1
	 */
	public abstract int getEnd(Buffer buffer, int line);
	//}}}

	//{{{ getStartLine() method
	/**
	 * Returns the starting line number of this selection.
	 */
	public int getStartLine()
	{
		return startLine;
	} //}}}

	//{{{ getEndLine() method
	/**
	 * Returns the ending line number of this selection.
	 */
	public int getEndLine()
	{
		return endLine;
	} //}}}

	//{{{ overlaps() method
	/**
	 * Returns if this selection and the specified selection overlap.
	 * @param s The other selection
	 * @since jEdit 4.1pre1
	 */
	public boolean overlaps(Selection s)
	{
		if((start >= s.start && start <= s.end)
			|| (end >= s.start && end <= s.end))
			return true;
		else
			return false;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[start=" + start
			+ ",end=" + end + ",startLine=" + startLine
			+ ",endLine=" + endLine + "]";
	} //}}}

	//{{{ clone() method
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch(CloneNotSupportedException e)
		{
			throw new InternalError("I just drank a whole "
				+ "bottle of cough syrup and I feel "
				+ "funny!");
		}
	} //}}}

	//{{{ Package-private members
	int start, end;
	int startLine, endLine;

	//{{{ Selection constructor
	Selection()
	{
	} //}}}

	//{{{ Range constructor
	Selection(Selection sel)
	{
		this.start = sel.start;
		this.end = sel.end;
	} //}}}

	//{{{ Selection constructor
	Selection(int start, int end)
	{
		this.start = start;
		this.end = end;
	} //}}}

	// should the next two be public, maybe?
	abstract void getText(Buffer buffer, StringBuffer buf);
	abstract int setText(Buffer buffer, String text);
	//}}}

	//{{{ Range class
	/**
	 * An ordinary range selection.
	 * @since jEdit 3.2pre1
	 */
	public static class Range extends Selection
	{
		//{{{ Range constructor
		public Range()
		{
		} //}}}

		//{{{ Range constructor
		public Range(Selection sel)
		{
			super(sel);
		} //}}}

		//{{{ Range constructor
		public Range(int start, int end)
		{
			super(start,end);
		} //}}}

		//{{{ getStart() method
		public int getStart(Buffer buffer, int line)
		{
			if(line == startLine)
				return start;
			else
				return buffer.getLineStartOffset(line);
		} //}}}

		//{{{ getEnd() method
		public int getEnd(Buffer buffer, int line)
		{
			if(line == endLine)
				return end;
			else
				return buffer.getLineEndOffset(line) - 1;
		} //}}}

		//{{{ getText() method
		void getText(Buffer buffer, StringBuffer buf)
		{
			buf.append(buffer.getText(start,end - start));
		} //}}}

		//{{{ setText() method
		int setText(Buffer buffer, String text)
		{
			buffer.remove(start,end - start);
			if(text != null && text.length() != 0)
			{
				buffer.insert(start,text);
				return start + text.length();
			}
			else
				return start;
		} //}}}
	} //}}}

	//{{{ Rect class
	/**
	 * A rectangular selection.
	 * @since jEdit 3.2pre1
	 */
	// this class is not very fast...
	public static class Rect extends Selection
	{
		//{{{ Rect constructor
		public Rect()
		{
			super();
		} //}}}

		//{{{ Rect constructor
		public Rect(Selection sel)
		{
			super(sel);
		} //}}}

		//{{{ Rect constructor
		public Rect(int start, int end)
		{
			super(start,end);
		} //}}}

		//{{{ Rect constructor
		public Rect(int startLine, int start, int endLine, int end)
		{
			this.startLine = startLine;
			this.start = start;
			this.endLine = endLine;
			this.end = end;
		} //}}}

		//{{{ Rect constructor
		public Rect(Buffer buffer, int startLine, int startColumn,
			int endLine, int endColumn)
		{
			this.startLine = startLine;
			this.endLine = endLine;

			int[] width = new int[1];
			int startOffset = buffer.getOffsetOfVirtualColumn(startLine,
				startColumn,width);
			if(startOffset == -1)
			{
				extraStartVirt = startColumn - width[0];
				startOffset = buffer.getLineEndOffset(startLine) - 1;
			}
			else
				startOffset += buffer.getLineStartOffset(startLine);

			int endOffset = buffer.getOffsetOfVirtualColumn(endLine,
				endColumn,width);
			if(endOffset == -1)
			{
				extraEndVirt = endColumn - width[0];
				endOffset = buffer.getLineEndOffset(endLine) - 1;
			}
			else
				endOffset += buffer.getLineStartOffset(endLine);
		} //}}}

		//{{{ getStartColumn() method
		public int getStartColumn(Buffer buffer)
		{
			int virtColStart = buffer.getVirtualWidth(startLine,
				start - buffer.getLineStartOffset(startLine)) + extraStartVirt;
			int virtColEnd = buffer.getVirtualWidth(endLine,
				end - buffer.getLineStartOffset(endLine)) + extraEndVirt;
			return Math.min(virtColStart,virtColEnd);
		} //}}}

		//{{{ getEndColumn() method
		public int getEndColumn(Buffer buffer)
		{
			int virtColStart = buffer.getVirtualWidth(startLine,
				start - buffer.getLineStartOffset(startLine)) + extraStartVirt;
			int virtColEnd = buffer.getVirtualWidth(endLine,
				end - buffer.getLineStartOffset(endLine)) + extraEndVirt;
			return Math.max(virtColStart,virtColEnd);
		} //}}}

		//{{{ getStart() method
		public int getStart(Buffer buffer, int line)
		{
			return getColumnOnOtherLine(buffer,startLine,line,
				getStartColumn(buffer));
		} //}}}

		//{{{ getEnd() method
		public int getEnd(Buffer buffer, int line)
		{
			return getColumnOnOtherLine(buffer,startLine,line,
				getEndColumn(buffer));
		} //}}}

		//{{{ Package-private members
		int extraStartVirt;
		int extraEndVirt;

		//{{{ getText() method
		void getText(Buffer buffer, StringBuffer buf)
		{
			int start = getStartColumn(buffer);
			int end = getEndColumn(buffer);

			for(int i = startLine; i <= endLine; i++)
			{
				int lineStart = buffer.getLineStartOffset(i);
				int lineLen = buffer.getLineLength(i);

				int rectStart = buffer.getOffsetOfVirtualColumn(
					i,start,null);
				if(rectStart == -1)
					rectStart = lineLen;

				int rectEnd = buffer.getOffsetOfVirtualColumn(
					i,end,null);
				if(rectEnd == -1)
					rectEnd = lineLen;

				buf.append(buffer.getText(lineStart + rectStart,
					rectEnd - rectStart));

				if(i != endLine)
					buf.append('\n');
			}
		} //}}}

		//{{{ setText() method
		int setText(Buffer buffer, String text)
		{
			int startColumn = getStartColumn(buffer);
			int endColumn = getEndColumn(buffer);

			int[] total = new int[1];

			int tabSize = buffer.getTabSize();

			int maxWidth = 0;
			int totalLines = 0;
			ArrayList lines = new ArrayList();

			//{{{ Split the text into lines
			if(text != null)
			{
				int lastNewline = 0;
				int currentWidth = startColumn;
				for(int i = 0; i < text.length(); i++)
				{
					char ch = text.charAt(i);
					if(ch == '\n')
					{
						totalLines++;
						lines.add(text.substring(
							lastNewline,i));
						lastNewline = i + 1;
						maxWidth = Math.max(maxWidth,currentWidth);
						lines.add(new Integer(currentWidth));
						currentWidth = startColumn;
					}
					else if(ch == '\t')
						currentWidth += tabSize - (currentWidth % tabSize);
					else
						currentWidth++;
				}

				if(lastNewline != text.length())
				{
					totalLines++;
					lines.add(text.substring(lastNewline));
					lines.add(new Integer(currentWidth));
					maxWidth = Math.max(maxWidth,currentWidth);
				}
			} //}}}

			//{{{ Insert the lines into the buffer
			int endOffset = 0;
			int lastLine = Math.max(startLine + totalLines - 1,endLine);
			for(int i = startLine; i <= lastLine; i++)
			{
				if(i == buffer.getLineCount())
					buffer.insert(buffer.getLength(),"\n");

				int lineStart = buffer.getLineStartOffset(i);
				int lineLen = buffer.getLineLength(i);

				int rectStart = buffer.getOffsetOfVirtualColumn(
					i,startColumn,total);
				int startWhitespace;
				if(rectStart == -1)
				{
					startWhitespace = (startColumn - total[0]);
					rectStart = lineLen;
				}
				else
					startWhitespace = 0;

				int rectEnd = buffer.getOffsetOfVirtualColumn(
					i,endColumn,null);
				if(rectEnd == -1)
					rectEnd = lineLen;

				buffer.remove(rectStart + lineStart,rectEnd - rectStart);

				int index = 2 * (i - startLine);

				int endWhitespace;
				if(rectEnd == lineLen)
					endWhitespace = 0;
				else if(i - startLine >= totalLines)
					endWhitespace = maxWidth - startColumn;
				else
				{
					endWhitespace = maxWidth
						- ((Integer)lines.get(index+1))
						.intValue();
				}

				String str = (i - startLine >= totalLines
					? "" : (String)lines.get(index));
				if(startWhitespace != 0)
				{
					buffer.insert(rectStart + lineStart,
						MiscUtilities.createWhiteSpace(startWhitespace,0));
				}

				buffer.insert(rectStart + lineStart + startWhitespace,str);

				if(endWhitespace != 0)
				{
					buffer.insert(rectStart + lineStart
						+ startWhitespace + str.length(),
						MiscUtilities.createWhiteSpace(endWhitespace,0));
				}

				endOffset = rectStart + lineStart
					+ startWhitespace
					+ endWhitespace
					+ str.length();
			} //}}}

			//{{{ Move the caret down a line
			if(lastLine != buffer.getLineCount() - 1)
			{
				int offset = buffer.getOffsetOfVirtualColumn(
					lastLine + 1,startColumn,null);
				return buffer.getLineStartOffset(lastLine + 1) + offset;
			}
			else
				return endOffset;
			//}}}
		} //}}}

		//}}}

		//{{{ Private members

		//{{{ getColumnOnOtherLine() method
		private int getColumnOnOtherLine(Buffer buffer, int line1,
			int line2, int col)
		{
			int returnValue = buffer.getOffsetOfVirtualColumn(
				line2,col,null);
			if(returnValue == -1)
				return buffer.getLineEndOffset(line2) - 1;
			else
				return buffer.getLineStartOffset(line2) + returnValue;
		} //}}}

		//}}}
	} //}}}
}
