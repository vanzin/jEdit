/*
 * ChunkCache.java - Caches TextUtilities.Chunk lists
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
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import java.util.ArrayList;
//}}}

/**
 * A class used internally by the text area.
 * @author Slava Pestov
 * @version $Id$
 */
class ChunkCache
{
	//{{{ ChunkCache constructor
	ChunkCache(JEditTextArea textArea)
	{
		this.textArea = textArea;
		out = new ArrayList();
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		lineInfo = new LineInfo[textArea.getVisibleLines() + 1];
		for(int i = 0; i < lineInfo.length; i++)
			lineInfo[i] = new LineInfo();
	} //}}}

	//{{{ setFirstLine() method
	void setFirstLine(int firstLine)
	{
		if(Math.abs(firstLine - this.firstLine) >= lineInfo.length)
		{
			for(int i = 0; i < lineInfo.length; i++)
			{
				lineInfo[i] = new LineInfo();
			}
		}
		else if(firstLine > this.firstLine)
		{
			System.arraycopy(lineInfo,firstLine - this.firstLine,
				lineInfo,0,lineInfo.length - firstLine
				+ this.firstLine);

			for(int i = lineInfo.length - firstLine
				+ this.firstLine; i < lineInfo.length; i++)
			{
				lineInfo[i] = new LineInfo();
			}
		}
		else if(this.firstLine > firstLine)
		{
			System.arraycopy(lineInfo,0,lineInfo,this.firstLine - firstLine,
				lineInfo.length - this.firstLine + firstLine);

			for(int i = 0; i < this.firstLine - firstLine; i++)
			{
				lineInfo[i] = new LineInfo();
			}
		}

		this.firstLine = firstLine;
	} //}}}

	//{{{ invalidateAll() method
	void invalidateAll()
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			lineInfo[i].chunksValid = false;
			lineInfo[i].chunks = null;
		}
	} //}}}

	//{{{ invalidateLineRange() method
	void invalidateLineRange(int startLine, int endLine)
	{
		startLine -= firstLine;
		startLine = Math.min(lineInfo.length - 1,Math.max(0,startLine));
		endLine -= firstLine;
		endLine = Math.min(lineInfo.length - 1,Math.max(0,endLine));

		for(int i = startLine; i <= endLine; i++)
		{
			lineInfo[i].chunksValid = false;
			lineInfo[i].chunks = null;
		}
	} //}}}

	//{{{ getLineInfo() method
	LineInfo getLineInfo(int virtualLineIndex, int physicalLineIndex)
	{
		LineInfo info;

		if(virtualLineIndex < firstLine
			|| virtualLineIndex >= firstLine + lineInfo.length)
			info = new LineInfo();
		else
			info = lineInfo[virtualLineIndex - firstLine];

		if(!info.chunksValid)
		{
			out.clear();
			Buffer buffer = textArea.getBuffer();
			buffer.getLineText(physicalLineIndex,textArea.lineSegment);

			TextAreaPainter painter = textArea.getPainter();
			TextUtilities.lineToChunkList(textArea.lineSegment,
				buffer.markTokens(physicalLineIndex).getFirstToken(),
				painter.getStyles(),painter.getFontRenderContext(),
				painter,0.0f,out);

			if(out.size() == 0)
				info.chunks = null;
			else
				info.chunks = (TextUtilities.Chunk)out.get(0);

			info.chunksValid = true;
		}

		return info;
	} //}}}

	//{{{ Private members
	private JEditTextArea textArea;
	private int firstLine;
	private LineInfo[] lineInfo;
	private ArrayList out;
	//}}}

	//{{{ LineInfo class
	static class LineInfo
	{
		int physicalLine;
		int subregion;
		boolean chunksValid;
		TextUtilities.Chunk chunks;
		int width;
	} //}}}
}
