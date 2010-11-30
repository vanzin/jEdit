/*
 * Anchor.java - A base point for physical line <-> screen line conversion
 * :tabSize=8:indentSize=8:noTabs=false:
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
 * A base point for physical line/screen line conversion.
 * @author Slava Pestov
 * @version $Id$
 */
abstract class Anchor
{
	protected final DisplayManager displayManager;
	protected final TextArea textArea;
	/** The physical line. */
	int physicalLine;
	/**
	 * The visible line index. (from the top of the buffer). It can be different from physical line
	 * when using soft wrap.
	 */
	int scrollLine;
	/** 
	 * If this is set to true, the changed() method will be called in
	 * {@link DisplayManager#notifyScreenLineChanges()}
	 */
	boolean callChanged;
	/** 
	 * If this is set to true, the reset() method will be called in
	 * {@link DisplayManager#notifyScreenLineChanges()}
	 */
	boolean callReset;

	//{{{ Anchor constructor
	protected Anchor(DisplayManager displayManager,
		TextArea textArea)
	{
		this.displayManager = displayManager;
		this.textArea = textArea;
	} //}}}

	/** This method recalculates the scrollLine from the beginning. */
	abstract void reset();
	abstract void changed();

	//{{{ toString() method
	@Override
	public String toString()
	{
		return getClass().getName() + '[' + physicalLine + ','
		       + scrollLine + ']';
	} //}}}

	//{{{ contentInserted() method
	/**
	 * Some content is inserted.
	 *
	 * @param startLine the start of the insert
	 * @param numLines the number of insterted lines
	 */
	void contentInserted(int startLine, int numLines)
	{
		// The Anchor is changed only if the content was inserted before
		if(physicalLine >= startLine)
		{
			if(physicalLine != startLine)
				physicalLine += numLines;
			callChanged = true;
		}
	} //}}}

	//{{{ preContentRemoved() method
	/**
	 * Method called before a content is removed from a buffer.
	 *
	 * @param startLine the first line of the removed content
	 * @param offset the offset in the start line
	 * @param numLines the number of removed lines
	 */
	void preContentRemoved(int startLine, int offset, int numLines)
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"preContentRemoved() before:" + this);
		// The removed content starts before the Anchor, we need to pull the anchor up
		if(physicalLine >= startLine)
		{
			if(physicalLine == startLine)
				callChanged = true;
			else
			{
				int end = Math.min(startLine + numLines, physicalLine);
				//Check the lines from the beginning of the removed content to the end (or the physical
				//line of the Anchor if it is before the end of the removed content

				//int loopStart = startLine + 1;

				//{{{ treatment if the beginning of the deleted content is inside a physical line that has several line counts
				/*if (displayManager.isLineVisible(startLine))
				{
					int screenLineCount = displayManager.screenLineMgr.getScreenLineCount(startLine);
					if (screenLineCount > 1)
					{
						int lineStartOffset = textArea.getLineStartOffset(startLine);

						int startScreenLine = textArea.getScreenLineOfOffset(lineStartOffset);
						int deleteStartScreenLine = textArea.getScreenLineOfOffset(offset);
						if (startScreenLine != deleteStartScreenLine)
						{
							loopStart = startLine + 2;
							scrollLine -= screenLineCount - deleteStartScreenLine + startScreenLine;
						}
					}
				}*/
				//}}}

				for(int i = startLine + 1; i <= end; i++)
				{
					//XXX
					if(displayManager.isLineVisible(i))
					{
						scrollLine -=
							displayManager
								.screenLineMgr
								.getScreenLineCount(i);
					}
				}
				physicalLine -= end - startLine;
				callChanged = true;
			}
		}
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"preContentRemoved() after:" + this);
	} //}}}
}
