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
				lineInfo[i].chunksValid = false;
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

	//{{{ updateChunkLists() method
	void updateChunkLists(int firstScreenLine, int lastScreenLine)
	{
		// TODO this needs to be sorted out.
		if(lastScreenLine >= lineInfo.length)
			System.err.println("foo: " + lastScreenLine
				+ "::" + (lineInfo.length - 1));

		LineInfo info = null;

		int physicalLine;

		for(;;)
		{
			info = lineInfo[firstScreenLine];
			if(!info.chunksValid)
			{
				if(firstScreenLine == 0)
					physicalLine = textArea.virtualToPhysical(firstLine);
				else
				{
					physicalLine = textArea
						.getFoldVisibilityManager()
						.getNextVisibleLine(lineInfo[
						firstScreenLine - 1]
						.physicalLine);
				}

				break;
			}
			else if(firstScreenLine == lastScreenLine)
			{
				// it's all valid
				return;
			}
			else
				firstScreenLine++;
		}

		System.err.println(firstScreenLine + "::" + physicalLine);

		// TODO: Assumptions...

		// Note that we rely on the fact that when a line is
		// invalidated, all screen lines/subregions are
		// invalidated as well.

		Buffer buffer = textArea.getBuffer();
		TextAreaPainter painter = textArea.getPainter();

		out.clear();

		int subregion = 0;

		for(int i = firstScreenLine; i <= lastScreenLine; i++)
		{
			info = lineInfo[i];

			TextUtilities.Chunk chunks;

			if(out.size() == 0)
			{
				if(physicalLine != -1 && i != firstScreenLine)
				{
					physicalLine = textArea.getFoldVisibilityManager()
						.getNextVisibleLine(physicalLine);
				}

				if(physicalLine == -1)
				{
					info.chunks = null;
					info.chunksValid = true;
					info.physicalLine = -1;
					continue;
				}

				subregion = 0;

				buffer.getLineText(physicalLine,textArea.lineSegment);

				TextUtilities.lineToChunkList(textArea.lineSegment,
					buffer.markTokens(physicalLine).getFirstToken(),
					painter.getStyles(),painter.getFontRenderContext(),
					painter,0.0f,out);

				if(out.size() == 0)
					chunks = null;
				else
				{
					chunks = (TextUtilities.Chunk)out.get(0);
					out.remove(0);
				}
			}
			else
			{
				subregion++;

				chunks = (TextUtilities.Chunk)out.get(0);
				out.remove(0);
			}

			info.physicalLine = physicalLine;
			info.subregion = subregion;
			info.chunksValid = true;
		}
	} //}}}

	//{{{ getLineInfoForScreenLine() method
	LineInfo getLineInfoForScreenLine(int screenLine)
	{
		return lineInfo[screenLine];
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

	//{{{ Instance variables
	private JEditTextArea textArea;
	private int firstLine;
	private LineInfo[] lineInfo;
	private ArrayList out;
	//}}}

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
