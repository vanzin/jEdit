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
import java.util.ArrayList;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.util.Log;
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

	//{{{ getMaxHorizontalScrollWidth() method
	int getMaxHorizontalScrollWidth()
	{
		int max = 0;
		for(int i = 0; i < lineInfo.length; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.chunksValid && info.width > max)
				max = info.width;
		}
		return max;
	} //}}}

	//{{{ getScreenLineOfOffset() method
	int getScreenLineOfOffset(int line, int offset)
	{
		int screenLine;

		if(line == lastScreenLineP)
		{
			LineInfo last = lineInfo[lastScreenLine];

			if(offset >= last.offset
				&& offset <= last.offset + last.length)
			{
				updateChunksUpTo(lastScreenLine);
				return lastScreenLine;
			}
		}

		if(line < textArea.getFirstPhysicalLine())
		{
			return -1;
		}
		else if(line > textArea.getLastPhysicalLine())
		{
			return -1;
		}
		else
		{
			screenLine = -1;

			// Find the screen line containing this offset
			for(int i = 0; i < lineInfo.length - 1; i++)
			{
				updateChunksUpTo(i);

				LineInfo info = lineInfo[i];
				if(info.physicalLine > line)
				{
					// line is invisible?
					if(i == 0)
						screenLine = 0;
					else
						screenLine = i - 1;
					break;
				}
				else if(info.physicalLine == line)
				{
					if(offset >= info.offset
						&& offset <= info.offset + info.length)
					{
						screenLine = i;
						break;
					}
				}
			}

			if(screenLine == -1)
				screenLine = lineInfo.length - 1;
		}

		lastScreenLineP = line;
		lastScreenLine = screenLine;

		return screenLine;
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		lineInfo = new LineInfo[textArea.getVisibleLines() + 1];
		for(int i = 0; i < lineInfo.length; i++)
			lineInfo[i] = new LineInfo();

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ setFirstLine() method
	void setFirstLine(int firstLine)
	{
		invalidateAll();
		this.firstLine = firstLine;
	} //}}}

	//{{{ invalidateAll() method
	void invalidateAll()
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			lineInfo[i].chunksValid = false;
		}

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFrom() method
	void invalidateChunksFrom(int screenLine)
	{
		for(int i = screenLine; i < lineInfo.length; i++)
		{
			lineInfo[i].chunksValid = false;
		}
	} //}}}

	//{{{ invalidateChunksFromPhys() method
	void invalidateChunksFromPhys(int physicalLine)
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			if(lineInfo[i].physicalLine >= physicalLine)
			{
				invalidateChunksFrom(i);
				break;
			}
		}

		if(lastScreenLineP >= physicalLine)
			lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ updateChunksUpTo() method
	void updateChunksUpTo(int lastScreenLine)
	{
		// TODO
		if(lastScreenLine >= lineInfo.length)
			textArea.recalculateVisibleLines();

		LineInfo info = null;

		int firstScreenLine = 0;

		for(int i = lastScreenLine; i >= 0; i--)
		{
			info = lineInfo[i];
			if(info.chunksValid)
			{
				if(i == lastScreenLine)
					return;
				else
					firstScreenLine = i + 1;
				break;
			}
		}

		int physicalLine;

		if(firstScreenLine == 0)
		{
			physicalLine = textArea.virtualToPhysical(firstLine);
		}
		else
		{
			int prevPhysLine = lineInfo[
				firstScreenLine - 1]
				.physicalLine;
			if(prevPhysLine == -1)
				physicalLine = -1;
			else
			{
				physicalLine = textArea
					.getFoldVisibilityManager()
					.getNextVisibleLine(prevPhysLine);
			}
		}

		// TODO: Assumptions...

		// Note that we rely on the fact that when a line is
		// invalidated, all screen lines/subregions are
		// invalidated as well.

		Buffer buffer = textArea.getBuffer();
		TextAreaPainter painter = textArea.getPainter();

		out.clear();

		int subregion = 0;
		int offset = 0;
		int length = 0;

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
					painter,textArea.softWrap
					? textArea.maxLineLen
					: 0.0f,out);

				if(out.size() == 0)
				{
					chunks = null;
					offset = 0;
					length = 0;
				}
				else
				{
					chunks = (TextUtilities.Chunk)out.get(0);
					out.remove(0);
					offset = 0;
					if(out.size() != 0)
						length = ((TextUtilities.Chunk)out.get(0)).offset - offset;
					else
						length = buffer.getLineLength(physicalLine);
				}
			}
			else
			{
				subregion++;

				chunks = (TextUtilities.Chunk)out.get(0);
				out.remove(0);
				offset = chunks.offset;
				if(out.size() != 0)
					length = ((TextUtilities.Chunk)out.get(0)).offset - offset;
				else
					length = buffer.getLineLength(physicalLine);
			}

			info.physicalLine = physicalLine;
			info.subregion = subregion;
			info.offset = offset;
			info.length = length;
			info.chunks = chunks;
			info.chunksValid = true;
		}
	} //}}}

	//{{{ getLineInfo() method
	LineInfo getLineInfo(int screenLine)
	{
		if(!lineInfo[screenLine].chunksValid)
			Log.log(Log.ERROR,this,"Not up-to-date: " + screenLine);
		return lineInfo[screenLine];
	} //}}}

	//{{{ getLineInfoBackwardsCompatibility() method
	LineInfo getLineInfoBackwardsCompatibility(int physicalLineIndex)
	{
		LineInfo info = new LineInfo();

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

		info.physicalLine = physicalLineIndex;
		info.chunksValid = true;

		return info;
	} //}}}

	//{{{ Private members
	private JEditTextArea textArea;
	private int firstLine;
	private LineInfo[] lineInfo;
	private ArrayList out;

	private int lastScreenLineP;
	private int lastScreenLine;
	//}}}

	//{{{ LineInfo class
	static class LineInfo
	{
		int physicalLine;
		int offset;
		int length;
		int subregion;
		boolean chunksValid;
		TextUtilities.Chunk chunks;
		int width;
	} //}}}
}
