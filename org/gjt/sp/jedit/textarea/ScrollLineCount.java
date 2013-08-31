/*
 * ScrollLineCount.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov
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

import org.gjt.sp.jedit.Debug;
import org.gjt.sp.util.Log;

/**
 * Maintains the vertical scrollbar.
 */
class ScrollLineCount extends Anchor
{

	//{{{ ScrollLineCount constructor
	ScrollLineCount(DisplayManager displayManager,
		TextArea textArea)
	{
		super(displayManager,textArea);
	} //}}}

	@Override
	public void changed() {}

	//{{{ reset() method
	@Override
	public void reset()
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"reset()");

		int scrollLine = 0;
		int physicalLine = getDisplayManager().getFirstVisibleLine();

		while(physicalLine != -1)
		{
			scrollLine += getDisplayManager().getScreenLineCount(physicalLine);
			physicalLine = getDisplayManager().getNextVisibleLine(physicalLine);
		}

		setPhysicalLine(getDisplayManager().getBuffer().getLineCount());
		setScrollLine(scrollLine);
	} //}}}

	@Override
	void preContentInserted(int startLine, int numLines)
	{
		preContentInsertedScrollLines = 0;
		int physicalLine = startLine;
		
		if(!getDisplayManager().isLineVisible(physicalLine))
			physicalLine = getDisplayManager().getNextVisibleLine(physicalLine);

		preContentInsertedScrollLines = getDisplayManager().getScreenLineCount(physicalLine);
	}

	@Override
	void contentInserted(int startLine, int numLines)
	{
		int scrollLines = 0;
		int physicalLine = startLine;

		for(int i = 0, n = numLines; i < n; i++, physicalLine++)
		{
			if(getDisplayManager().isLineVisible(physicalLine))
				scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
		}
		// process next visible line
		if(!getDisplayManager().isLineVisible(physicalLine))
			physicalLine = getDisplayManager().getNextVisibleLine(physicalLine);
		if(physicalLine >= 0)
			scrollLines += getDisplayManager().getScreenLineCount(physicalLine);

		scrollLines -= preContentInsertedScrollLines;

		movePhysicalLine(numLines);
		moveScrollLine(scrollLines);
	}

	@Override
	void preContentRemoved(int startLine, int offset, int numLines)
	{
		int scrollLines = 0;
		int physicalLine = startLine;
		int numLinesVisible = 0;

		for(int i = 0, n = numLines; i < n; i++, physicalLine++)
		{
			if(getDisplayManager().isLineVisible(physicalLine))
			{
				scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
				numLinesVisible++;
			}
		}
		// process next visible line
		if(!getDisplayManager().isLineVisible(physicalLine))
			physicalLine = getDisplayManager().getNextVisibleLine(physicalLine);
		if(physicalLine >= 0)
		{
			scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
			numLinesVisible++;
		}

		preContentRemovedScrollLines = scrollLines;
	}

	@Override
	void contentRemoved(int startLine, int startOffset, int numLines)
	{
		int scrollLines = 0; 
		int physicalLine = startLine;
		if(!getDisplayManager().isLineVisible(physicalLine))
			physicalLine = getDisplayManager().getNextVisibleLine(physicalLine);
		scrollLines = getDisplayManager().getScreenLineCount(physicalLine);

		scrollLines -= preContentRemovedScrollLines;
		movePhysicalLine(-numLines);
		moveScrollLine(scrollLines);
	}
}
