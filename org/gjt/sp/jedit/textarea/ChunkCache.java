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
import org.gjt.sp.jedit.syntax.*;
//}}}

/**
 * A "chunk" is a run of text with a specified font style and color. This class
 * contains various static methods for manipulating chunks and chunk lists. It
 * also has a number of package-private instance methods used internally by the
 * text area for painting text.
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
			for(int i = 0; i < lineInfo.length; i++)
			{
				LineInfo info = getLineInfo(i);
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
		LineInfo[] newLineInfo = new LineInfo[textArea.getVisibleLines() + 1];

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

	//{{{ setFirstLine() method
	public static boolean DEBUG = false;
	/**
	 * This method takes care of shifting the cached tokens so that
	 * scrolling doesn't cause all visible lines, only newly exposed
	 * ones, to be retokenized.
	 */
	void setFirstLine(int firstLine, int physFirstLine, boolean bufferSwitch)
	{
		if(DEBUG)
		{
			System.err.println("old: " + this.firstLine + ",new: " +
				firstLine + ",phys: " + physFirstLine + ",bs: " + bufferSwitch);
		}

		int visibleLines = lineInfo.length;
		// rely on the fact that when we're called physLastLine not updated yet
		if(bufferSwitch || physFirstLine > textArea.getLastPhysicalLine())
		{
			if(DEBUG)
				System.err.println("too far");
			for(int i = 0; i < visibleLines; i++)
			{
				lineInfo[i].chunksValid = false;
			}
		}
		else if(firstLine > this.firstLine)
		{
			boolean invalidateAll = false;

			int firstScreenLine = 0;
			for(int i = 0; i < visibleLines; i++)
			{
				// can't do much if the physical line we are
				// looking for isn't in the cache... so in
				// that case just invalidate everything.
				if(!lineInfo[i].chunksValid)
				{
					invalidateAll = true;
					break;
				}

				if(lineInfo[i].physicalLine == physFirstLine)
				{
					firstScreenLine = i;
					break;
				}
			}

			if(invalidateAll)
			{
				invalidateAll();
			}
			else
			{
				int lastValidLine = -1;

				// chunk cache does not allow only the last
				// (visibleLines - lastValidLine) lines to
				// be invalid; only entire physical lines can
				// be invalidated.
				for(int i = visibleLines - 1; i >= 0; i--)
				{
					if(DEBUG)
					{
						System.err.println("Scan " + i);
					}
					if(lineInfo[i].lastSubregion)
						break;
					else
						lineInfo[i].chunksValid = false;
				}

				if(firstScreenLine != visibleLines)
				{
					System.arraycopy(lineInfo,firstScreenLine,
						lineInfo,0,visibleLines - firstScreenLine);
				}

				for(int i = visibleLines - firstScreenLine; i < visibleLines; i++)
				{
					lineInfo[i] = new LineInfo();
				}
			}

			if(DEBUG)
			{
				System.err.println("f > t.f: only " + firstScreenLine
					+ " need updates");
			}
		}
		else if(this.firstLine > firstLine)
		{
			LinkedList list = new LinkedList();
			for(int i = firstLine; i < this.firstLine; i++)
			{
				if(i >= textArea.getVirtualLineCount()
					|| list.size() >= visibleLines)
				{
					break;
				}

				int physicalLine = textArea.virtualToPhysical(i);

				out.clear();
				lineToChunkList(physicalLine,out);
				if(out.size() == 0)
					out.add(null);

				getLineInfosForPhysicalLine(physicalLine,list);
			}

			if(list.size() < visibleLines)
			{
				System.arraycopy(lineInfo,0,lineInfo,list.size(),
					visibleLines - list.size());
			}

			int firstScreenLine = Math.min(list.size(),visibleLines);

			Iterator iter = list.iterator();
			for(int i = 0; i < visibleLines && iter.hasNext(); i++)
			{
				lineInfo[i] = (LineInfo)iter.next();
			}

			if(DEBUG)
			{
				System.err.println("t.f > f: only " + firstScreenLine
					+ " need updates");
			}
		}

		lastScreenLine = lastScreenLineP = -1;
		this.firstLine = firstLine;
	} //}}}

	//{{{ invalidateAll() method
	void invalidateAll()
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			if(!lineInfo[i].chunksValid)
			{
				// remainder are also invalid
				break;
			}
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

		lastScreenLine = lastScreenLineP = -1;
	} //}}}

	//{{{ invalidateChunksFromPhys() method
	void invalidateChunksFromPhys(int physicalLine)
	{
		for(int i = 0; i < lineInfo.length; i++)
		{
			LineInfo info = lineInfo[i];
			if(!info.chunksValid)
				break;

			if(info.physicalLine >= physicalLine)
			{
				invalidateChunksFrom(i);
				break;
			}
		}
	} //}}}

	//{{{ getLineInfo() method
	LineInfo getLineInfo(int screenLine)
	{
		updateChunksUpTo(screenLine);
		LineInfo info = lineInfo[screenLine];
		if(!info.chunksValid)
			throw new InternalError("Not up-to-date: " + screenLine);
		return info;
	} //}}}

	//{{{ getScreenLineCount() method
	int getScreenLineCount(int physicalLine)
	{
		Buffer buffer = textArea.getBuffer();
		int screenLines = buffer.getScreenLineCount(physicalLine);
		if(screenLines == 0)
		{
			LineInfo[] infos = getLineInfosForPhysicalLine(physicalLine);
			screenLines = infos.length;
			buffer.setScreenLineCount(physicalLine,screenLines);
		}
		return screenLines;
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
		int offset = Chunk.xToOffset(info.chunks,
			x - textArea.getHorizontalOffset(),round);
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
		return (int)(textArea.getHorizontalOffset() + Chunk.offsetToX(
			info.chunks,offset));
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
	 */
	int getBelowPosition(int physicalLine, int offset, int x)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(physicalLine);

		int subregion = getSubregionOfOffset(offset,lineInfos);

		if(subregion != lineInfos.length - 1)
		{
			return textArea.getLineStartOffset(physicalLine)
				+ xToSubregionOffset(lineInfos[subregion + 1],
				x,true);
		}
		else
		{
			int nextLine = textArea.getFoldVisibilityManager()
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
	 */
	int getAbovePosition(int physicalLine, int offset, int x)
	{
		LineInfo[] lineInfos = getLineInfosForPhysicalLine(physicalLine);

		int subregion = getSubregionOfOffset(offset,lineInfos);

		if(subregion != 0)
		{
			return textArea.getLineStartOffset(physicalLine)
				+ xToSubregionOffset(lineInfos[subregion - 1],
				x,true);
		}
		else
		{
			int prevLine = textArea.getFoldVisibilityManager()
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

	//{{{ Private members

	//{{{ Instance variables
	private JEditTextArea textArea;
	private int firstLine;
	private LineInfo[] lineInfo;
	private ArrayList out;

	private int lastScreenLineP;
	private int lastScreenLine;

	private boolean needFullRepaint;

	private DisplayTokenHandler tokenHandler;
	//}}}

	//{{{ getLineInfosForPhysicalLine() method
	private LineInfo[] getLineInfosForPhysicalLine(int physicalLine)
	{
		out.clear();
		lineToChunkList(physicalLine,out);

		if(out.size() == 0)
			out.add(null);

		ArrayList returnValue = new ArrayList(out.size());
		getLineInfosForPhysicalLine(physicalLine,returnValue);
		return (LineInfo[])returnValue.toArray(new LineInfo[out.size()]);
	} //}}}

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

			info.chunksValid = true;
			info.chunks = chunks;

			list.add(info);
		}
	} //}}}

	//{{{ updateChunksUpTo() method
	private void updateChunksUpTo(int lastScreenLine)
	{
		if(lineInfo[lastScreenLine].chunksValid)
			return;

		int firstScreenLine = 0;

		for(int i = lastScreenLine; i >= 0; i--)
		{
			if(lineInfo[i].chunksValid)
			{
				firstScreenLine = i + 1;
				break;
			}
		}

		int physicalLine;

		if(firstScreenLine == 0)
		{
			physicalLine = textArea.getFirstPhysicalLine();
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

				lineToChunkList(physicalLine,out);

				info.firstSubregion = true;

				if(out.size() == 0)
				{
					textArea.getBuffer().setScreenLineCount(
						physicalLine,1);
					chunks = null;
					offset = 0;
					length = 1;
				}
				else
				{
					textArea.getBuffer().setScreenLineCount(
						physicalLine,out.size());
					chunks = (Chunk)out.get(0);
					out.remove(0);
					offset = 0;
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
				/* If this line has become longer or shorter
				 * (in which case the new physical line number
				 * is different from the cached one) we need to:
				 * - continue updating past the last line
				 * - advise the text area to repaint
				 * On the other hand, if the line wraps beyond
				 * lastScreenLine, we need to keep updating the
				 * chunk list to ensure proper alignment of
				 * invalidation flags (see start of method) */
				if(info.physicalLine != physicalLine
					|| info.lastSubregion != lastSubregion)
				{
					lastScreenLine++;
					needFullRepaint = true;
				}
				else if(out.size() != 0)
					lastScreenLine++;
			}

			info.physicalLine = physicalLine;
			info.lastSubregion = lastSubregion;
			info.offset = offset;
			info.length = length;
			info.chunks = chunks;
			info.chunksValid = true;
		}
	} //}}}

	//{{{ lineToChunkList() method
	private void lineToChunkList(int physicalLine, List out)
	{
		TextAreaPainter painter = textArea.getPainter();
		Buffer buffer = textArea.getBuffer();

		buffer.getLineText(physicalLine,textArea.lineSegment);
		tokenHandler.init(textArea.lineSegment,painter.getStyles(),
			painter.getFontRenderContext(),
			painter,out,
			(textArea.softWrap ? textArea.wrapMargin : 0.0f));
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
		boolean chunksValid;
		Chunk chunks;
		int width;
	} //}}}
}
