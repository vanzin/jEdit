/*
 * ChunkCache.java - Intermediate layer between token lists from a TokenMarker
 * and what you see on screen
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
import java.util.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * Manages low-level text display tasks.
 *
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
		tokenHandler = new DisplayTokenHandler();
	} //}}}

	//{{{ getMaxHorizontalScrollWidth() method
	int getMaxHorizontalScrollWidth()
	{
		int max = 0;
		for(int i = 0; i < firstInvalidLine; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.width > max)
				max = info.width;
		}
		return max;
	} //}}}

	//{{{ getScreenLineOfOffset() method
	int getScreenLineOfOffset(int line, int offset)
	{
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
			int screenLine;

			if(line == lastScreenLineP)
			{
				LineInfo last = getLineInfo(lastScreenLine);

				if(offset >= last.offset
					&& offset < last.offset + last.length)
				{
					return lastScreenLine;
				}
			}

			screenLine = -1;

			// Find the screen line containing this offset
			for(int i = 0; i < textArea.getVisibleLines(); i++)
			{
				LineInfo info = getLineInfo(i);
				if(info.physicalLine > line)
				{
					// line is invisible?
					return i - 1;
					//return -1;
				}
				else if(info.physicalLine == line)
				{
					if(offset >= info.offset
						&& offset < info.offset + info.length)
					{
						screenLine = i;
						break;
					}
				}
			}

			if(screenLine == -1)
				return -1;
			else
			{
				lastScreenLineP = line;
				lastScreenLine = screenLine;

				return screenLine;
			}
		}
	} //}}}

	//{{{ recalculateVisibleLines() method
	void recalculateVisibleLines()
	{
		LineInfo[] newLineInfo = new LineInfo[textArea.getVisibleLines()];

		int start;
		if(lineInfo == null)
			start = 0;
		else
		{
			start = Math.min(lineInfo.length,newLineInfo.length);
			System.arraycopy(lineInfo,0,newLineInfo,0,start);
		}

		for(int i = start; i < newLineInfo.length; i++)
			newLineInfo[i] = new LineInfo();

		lineInfo = newLineInfo;

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ setBuffer() method
	void setBuffer(Buffer buffer)
	{
		this.buffer = buffer;
		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ scrollDown() method
	void scrollDown(int amount)
	{
		int visibleLines = textArea.getVisibleLines();

		System.arraycopy(lineInfo,amount,lineInfo,0,visibleLines - amount);

		for(int i = visibleLines - amount; i < visibleLines; i++)
		{
			lineInfo[i] = new LineInfo();
		}

		firstInvalidLine -= amount;
		if(firstInvalidLine < 0)
			firstInvalidLine = 0;

		if(Debug.CHUNK_CACHE_DEBUG)
		{
			System.err.println("f > t.f: only " + amount
				+ " need updates");
		}

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ scrollUp() method
	void scrollUp(int amount)
	{
		System.arraycopy(lineInfo,0,lineInfo,amount,
			textArea.getVisibleLines() - amount);

		for(int i = 0; i < amount; i++)
		{
			lineInfo[i] = new LineInfo();
		}

		// don't try this at home
		int oldFirstInvalidLine = firstInvalidLine;
		firstInvalidLine = 0;
		updateChunksUpTo(amount);
		firstInvalidLine = oldFirstInvalidLine + amount;
		if(firstInvalidLine > textArea.getVisibleLines())
			firstInvalidLine = textArea.getVisibleLines();

		if(Debug.CHUNK_CACHE_DEBUG)
		{
			Log.log(Log.DEBUG,this,"f > t.f: only " + amount
				+ " need updates");
		}

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateAll() method
	void invalidateAll()
	{
		firstInvalidLine = 0;
		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFrom() method
	void invalidateChunksFrom(int screenLine)
	{
		if(Debug.CHUNK_CACHE_DEBUG)
			Log.log(Log.DEBUG,this,"Invalidate from " + screenLine);
		firstInvalidLine = Math.min(screenLine,firstInvalidLine);

		if(screenLine <= lastScreenLine)
			lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFromPhys() method
	void invalidateChunksFromPhys(int physicalLine)
	{
		for(int i = 0; i < firstInvalidLine; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.physicalLine == -1 || info.physicalLine >= physicalLine)
			{
				firstInvalidLine = i;
				if(i <= lastScreenLine)
					lastScreenLine = lastScreenLineP = -1;
				break;
			}
		}
	} //}}}

	//{{{ getLineInfo() method
	LineInfo getLineInfo(int screenLine)
	{
		updateChunksUpTo(screenLine);
		return lineInfo[screenLine];
	} //}}}

	//{{{ getLineSubregionCount() method
	int getLineSubregionCount(int physicalLine)
	{
		if(!textArea.displayManager.softWrap)
			return 1;

		out.clear();
		lineToChunkList(physicalLine,out);

		int size = out.size();
		if(size == 0)
			return 1;
		else
			return size;
	} //}}}

	//{{{ getSubregionOfOffset() method
	/**
	 * Returns the subregion containing the specified offset. A subregion
	 * is a subset of a physical line. Each screen line corresponds to one
	 * subregion. Unlike the {@link #getScreenLineOfOffset()} method,
	 * this method works with non-visible lines too.
	 */
	int getSubregionOfOffset(int offset, LineInfo[] lineInfos)
	{
		for(int i = 0; i < lineInfos.length; i++)
		{
			LineInfo info = lineInfos[i];
			if(offset >= info.offset && offset < info.offset + info.length)
				return i;
		}

		return -1;
	} //}}}

	//{{{ xToSubregionOffset() method
	/**
	 * Converts an x co-ordinate within a subregion into an offset from the
	 * start of that subregion.
	 * @param physicalLine The physical line number
	 * @param subregion The subregion; if -1, then this is the last
	 * subregion.
	 * @param x The x co-ordinate
	 * @param round Round up to next character if x is past the middle of a
	 * character?
	 */
	int xToSubregionOffset(int physicalLine, int subregion, int x,
		boolean round)
	{
		LineInfo[] infos = getLineInfosForPhysicalLine(physicalLine);
		if(subregion == -1)
			subregion += infos.length;
		return xToSubregionOffset(infos[subregion],x,round);
	} //}}}

	//{{{ xToSubregionOffset() method
	/**
	 * Converts an x co-ordinate within a subregion into an offset from the
	 * start of that subregion.
	 * @param info The line info object
	 * @param x The x co-ordinate
	 * @param round Round up to next character if x is past the middle of a
	 * character?
	 */
	int xToSubregionOffset(LineInfo info, int x,
		boolean round)
	{
		int offset = Chunk.xToOffset(info.chunks,x,round);
		if(offset == -1 || offset == info.offset + info.length)
			offset = info.offset + info.length - 1;

		return offset;
	} //}}}

	//{{{ subregionOffsetToX() method
	/**
	 * Converts an offset within a subregion into an x co-ordinate.
	 * @param physicalLine The physical line
	 * @param offset The offset
	 */
	int subregionOffsetToX(int physicalLine, int offset)
	{
		LineInfo[] infos = getLineInfosForPhysicalLine(physicalLine);
		LineInfo info = infos[getSubregionOfOffset(offset,infos)];
		return subregionOffsetToX(info,offset);
	} //}}}

	//{{{ subregionOffsetToX() method
	/**
	 * Converts an offset within a subregion into an x co-ordinate.
	 * @param info The line info object
	 * @param offset The offset
	 */
	int subregionOffsetToX(LineInfo info, int offset)
	{
		return (int)Chunk.offsetToX(info.chunks,offset);
	} //}}}

	//{{{ getSubregionStartOffset() method
	/**
	 * Returns the start offset of the specified subregion of the specified
	 * physical line.
	 * @param line The physical line number
	 * @param offset An offset
	 */
	int getSubregionStartOffset(int line, int offset)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(line);
		LineInfo info = lineInfos[getSubregionOfOffset(offset,lineInfos)];
		return textArea.getLineStartOffset(info.physicalLine)
			+ info.offset;
	} //}}}

	//{{{ getSubregionEndOffset() method
	/**
	 * Returns the end offset of the specified subregion of the specified
	 * physical line.
	 * @param line The physical line number
	 * @param offset An offset
	 */
	int getSubregionEndOffset(int line, int offset)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(line);
		LineInfo info = lineInfos[getSubregionOfOffset(offset,lineInfos)];
		return textArea.getLineStartOffset(info.physicalLine)
			+ info.offset + info.length;
	} //}}}

	//{{{ getBelowPosition() method
	/**
	 * @param physicalLine The physical line number
	 * @param offset The offset
	 * @param x The location
	 * @param ignoreWrap If true, behave as if soft wrap is off even if it
	 * is on
	 */
	int getBelowPosition(int physicalLine, int offset, int x,
		boolean ignoreWrap)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(physicalLine);

		int subregion = getSubregionOfOffset(offset,lineInfos);

		if(subregion != lineInfos.length - 1 && !ignoreWrap)
		{
			return textArea.getLineStartOffset(physicalLine)
				+ xToSubregionOffset(lineInfos[subregion + 1],
				x,true);
		}
		else
		{
			int nextLine = textArea.displayManager
				.getNextVisibleLine(physicalLine);

			if(nextLine == -1)
				return -1;
			else
			{
				return textArea.getLineStartOffset(nextLine)
					+ xToSubregionOffset(nextLine,0,
					x,true);
			}
		}
	} //}}}

	//{{{ getAbovePosition() method
	/**
	 * @param physicalLine The physical line number
	 * @param offset The offset
	 * @param x The location
	 * @param ignoreWrap If true, behave as if soft wrap is off even if it
	 * is on
	 */
	int getAbovePosition(int physicalLine, int offset, int x,
		boolean ignoreWrap)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(physicalLine);

		int subregion = getSubregionOfOffset(offset,lineInfos);

		if(subregion != 0 && !ignoreWrap)
		{
			return textArea.getLineStartOffset(physicalLine)
				+ xToSubregionOffset(lineInfos[subregion - 1],
				x,true);
		}
		else
		{
			int prevLine = textArea.displayManager
				.getPrevVisibleLine(physicalLine);

			if(prevLine == -1)
				return -1;
			else
			{
				return textArea.getLineStartOffset(prevLine)
					+ xToSubregionOffset(prevLine,-1,
					x,true);
			}
		}
	} //}}}

	//{{{ needFullRepaint() method
	/**
	 * The needFullRepaint variable becomes true when the number of screen
	 * lines in a physical line changes.
	 */
	boolean needFullRepaint()
	{
		boolean retVal = needFullRepaint;
		needFullRepaint = false;
		return retVal;
	} //}}}

	//{{{ getLineInfosForPhysicalLine() method
	LineInfo[] getLineInfosForPhysicalLine(int physicalLine)
	{
		out.clear();

		if(buffer.isLoaded())
			lineToChunkList(physicalLine,out);

		if(out.size() == 0)
			out.add(null);

		ArrayList returnValue = new ArrayList(out.size());
		getLineInfosForPhysicalLine(physicalLine,returnValue);
		return (LineInfo[])returnValue.toArray(new LineInfo[out.size()]);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JEditTextArea textArea;
	private Buffer buffer;
	private LineInfo[] lineInfo;
	private ArrayList out;

	private int firstInvalidLine;
	private int lastScreenLineP;
	private int lastScreenLine;

	private boolean needFullRepaint;

	private DisplayTokenHandler tokenHandler;
	//}}}

	//{{{ getLineInfosForPhysicalLine() method
	private void getLineInfosForPhysicalLine(int physicalLine, List list)
	{
		for(int i = 0; i < out.size(); i++)
		{
			Chunk chunks = (Chunk)out.get(i);
			LineInfo info = new LineInfo();
			info.physicalLine = physicalLine;
			if(i == 0)
			{
				info.firstSubregion = true;
				info.offset = 0;
			}
			else
				info.offset = chunks.offset;

			if(i == out.size() - 1)
			{
				info.lastSubregion = true;
				info.length = textArea.getLineLength(physicalLine)
					- info.offset + 1;
			}
			else
			{
				info.length = ((Chunk)out.get(i + 1)).offset
					- info.offset;
			}

			info.chunks = chunks;

			list.add(info);
		}
	} //}}}

	//{{{ updateChunksUpTo() method
	private void updateChunksUpTo(int lastScreenLine)
	{
		// this method is a nightmare
		if(lastScreenLine >= lineInfo.length)
		{
			throw new ArrayIndexOutOfBoundsException(lastScreenLine);
		}

		// if one line's chunks are invalid, remaining lines are also
		// invalid
		if(lastScreenLine < firstInvalidLine)
			return;

		// find a valid line closest to the last screen line
		int firstScreenLine = 0;

		for(int i = firstInvalidLine - 1; i >= 0; i--)
		{
			if(lineInfo[i].lastSubregion)
			{
				firstScreenLine = i + 1;
				break;
			}
		}

		int physicalLine;

		// for the first line displayed, take its physical line to be
		// the text area's first physical line
		if(firstScreenLine == 0)
		{
			physicalLine = textArea.getFirstPhysicalLine();
		}
		// otherwise, determine the next visible line
		else
		{
			int prevPhysLine = lineInfo[
				firstScreenLine - 1]
				.physicalLine;
			// if -1, the empty space at the end of the text area
			// when the buffer has less lines than are visible
			if(prevPhysLine == -1)
				physicalLine = -1;
			else
			{
				physicalLine = textArea
					.displayManager
					.getNextVisibleLine(prevPhysLine);
			}
		}

		if(Debug.CHUNK_CACHE_DEBUG)
		{
			Log.log(Log.DEBUG,this,"Updating chunks from " + firstScreenLine
				+ " to " + lastScreenLine);
		}

		// Note that we rely on the fact that when a physical line is
		// invalidated, all screen lines/subregions of that line are
		// invalidated as well. See below comment for code that tries
		// to uphold this assumption.

		out.clear();

		int offset = 0;
		int length = 0;

		for(int i = firstScreenLine; i <= lastScreenLine; i++)
		{
			LineInfo info = lineInfo[i];

			Chunk chunks;

			// get another line of chunks
			if(out.size() == 0)
			{
				// unless this is the first time, increment
				// the line number
				if(physicalLine != -1 && i != firstScreenLine)
				{
					physicalLine = textArea.displayManager
						.getNextVisibleLine(physicalLine);
				}

				// empty space
				if(physicalLine == -1)
				{
					info.chunks = null;
					info.physicalLine = -1;
					continue;
				}

				// chunk the line.
				lineToChunkList(physicalLine,out);

				info.firstSubregion = true;

				// if the line has no text, out.size() == 0
				if(out.size() == 0)
				{
					textArea.displayManager
						.setScreenLineCount(
						physicalLine,1);
					if(i == 0)
					{
						if(textArea.displayManager.firstLine.skew > 0)
						{
							Log.log(Log.ERROR,this,"BUG: skew=" + textArea.displayManager.firstLine.skew + ",out.size()=" + out.size());
							textArea.displayManager.firstLine.skew = 0;
							needFullRepaint = true;
							lastScreenLine = lineInfo.length - 1;
						}
					}
					chunks = null;
					offset = 0;
					length = 1;
				}
				// otherwise, the number of subregions
				else
				{
					textArea.displayManager
						.setScreenLineCount(
						physicalLine,out.size());
					if(i == 0)
					{
						int skew = textArea.displayManager.firstLine.skew;
						if(skew >= out.size())
						{
							Log.log(Log.ERROR,this,"BUG: skew=" + skew + ",out.size()=" + out.size());
							skew = 0;
							needFullRepaint = true;
							lastScreenLine = lineInfo.length - 1;
						}
						else if(skew > 0)
						{
							info.firstSubregion = false;
							for(int j = 0; j < skew; j++)
								out.remove(0);
						}
					}
					chunks = (Chunk)out.get(0);
					out.remove(0);
					offset = chunks.offset;
					if(out.size() != 0)
						length = ((Chunk)out.get(0)).offset - offset;
					else
						length = textArea.getLineLength(physicalLine) - offset + 1;
				}
			}
			else
			{
				info.firstSubregion = false;

				chunks = (Chunk)out.get(0);
				out.remove(0);
				offset = chunks.offset;
				if(out.size() != 0)
					length = ((Chunk)out.get(0)).offset - offset;
				else
					length = textArea.getLineLength(physicalLine) - offset + 1;
			}

			boolean lastSubregion = (out.size() == 0);

			if(i == lastScreenLine
				&& lastScreenLine != lineInfo.length - 1)
			{
				/* if the user changes the syntax token at the
				 * end of a line, need to do a full repaint. */
				if(tokenHandler.getLineContext() !=
					info.lineContext)
				{
					lastScreenLine++;
					needFullRepaint = true;
				}
				/* If this line has become longer or shorter
				 * (in which case the new physical line number
				 * is different from the cached one) we need to:
				 * - continue updating past the last line
				 * - advise the text area to repaint
				 * On the other hand, if the line wraps beyond
				 * lastScreenLine, we need to keep updating the
				 * chunk list to ensure proper alignment of
				 * invalidation flags (see start of method) */
				else if(info.physicalLine != physicalLine
					|| info.lastSubregion != lastSubregion)
				{
					lastScreenLine++;
					needFullRepaint = true;
				}
				/* We only cache entire physical lines at once;
				 * don't want to split a physical line into
				 * screen lines and only have some valid. */
				else if(out.size() != 0)
					lastScreenLine++;
			}

			info.physicalLine = physicalLine;
			info.lastSubregion = lastSubregion;
			info.offset = offset;
			info.length = length;
			info.chunks = chunks;
			info.lineContext = tokenHandler.getLineContext();
		}

		firstInvalidLine = Math.max(lastScreenLine + 1,firstInvalidLine);
	} //}}}

	//{{{ lineToChunkList() method
	private void lineToChunkList(int physicalLine, List out)
	{
		TextAreaPainter painter = textArea.getPainter();

		tokenHandler.init(painter.getStyles(),
			painter.getFontRenderContext(),
			painter,out,
			(textArea.displayManager.softWrap
			? textArea.displayManager.wrapMargin : 0.0f));
		buffer.markTokens(physicalLine,tokenHandler);
	} //}}}

	//}}}

	//{{{ LineInfo class
	static class LineInfo
	{
		int physicalLine;
		int offset;
		int length;
		boolean firstSubregion;
		boolean lastSubregion;
		Chunk chunks;
		int width;
		TokenMarker.LineContext lineContext;
	} //}}}
}
