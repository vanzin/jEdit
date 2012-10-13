/*
 * Anchor.java - A base point for physical line <-> screen line conversion
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

/**
 * A base point for physical line/screen line conversion.
 * @author Slava Pestov
 * @version $Id$
 */
abstract class Anchor
{
	private final DisplayManager displayManager;
	private final TextArea textArea;

	/**
	 * Class ScrollLineCount:
	 * The total number of physical lines in this buffer
	 * (visible and invisible lines)
	 *
	 * Class FirstLine:
	 * The physical line number of this Anchor in the Buffer.
	 * The first visible physical line index
	 * (only visible lines are processed).
	 */
	private int physicalLine;

	/**
	 * Class ScrollLineCount:
	 * The number of visible lines (from the top of the buffer).
	 * It can be different from physical line when using soft wrap.
	 * or when using folding, if the foldings are collapsed
	 *
	 * Class FirstLine:
	 * Physical line number of the scroll line
	 * (only visible lines are processed)
	 */
	private int scrollLine;

	/** 
	 * If this is set to true, the changed() method will be called in
	 * {@link DisplayManager#notifyScreenLineChanges()}
	 */
	private boolean callChanged;
	/** 
	 * If this is set to true, the reset() method will be called in
	 * {@link DisplayManager#notifyScreenLineChanges()}
	 */
	private boolean callReset;

	int preContentInsertedScrollLines;
	int preContentRemovedScrollLines;

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
		return getClass().getName() + '[' + getPhysicalLine() + ',' + getScrollLine() + ']';
	} //}}}

	void movePhysicalLine(int numLines)
	{
		if(numLines == 0)
			return;
		setPhysicalLine(getPhysicalLine() + numLines);
	}

	void moveScrollLine(int numLines)
	{
		if(numLines == 0)
			return;
		setScrollLine(getScrollLine() + numLines);
	}

	//{{{ preContentInserted() method
	/**
	 * Some content is inserted.
	 *
	 * @param startLine the start of the insert
	 * @param numLines the number of inserted lines
	 */
	abstract void preContentInserted(int startLine, int numLines);
	//}}}

	//{{{ contentInserted() method
	/**
	 * Some content is inserted.
	 *
	 * @param startLine the start of the insert
	 * @param numLines the number of inserted lines
	 */
	abstract void contentInserted(int startLine, int numLines);
	//}}}

	//{{{ preContentRemoved() method
	/**
	 * Method called before a content is removed from a buffer.
	 *
	 * @param startLine the first line of the removed content
	 * @param offset the offset in the start line
	 * @param numLines the number of removed lines
	 */

	abstract void preContentRemoved(int startLine, int offset, int numLines);
	//}}}

	//{{{ preContentRemoved() method
	/**
	 * Method called before a content is removed from a buffer.
	 *
	 * @param startLine the first line of the removed content
	 * @param offset the offset in the start line
	 * @param numLines the number of removed lines
	 */
	abstract void contentRemoved(int startLine, int offset, int numLines);
	//}}}

	int getPhysicalLine()
	{
		return physicalLine;
	}

	void setPhysicalLine(int physicalLine)
	{
		assert physicalLine >= 0;
		if(this.physicalLine != physicalLine)
		{
			setCallChanged(true);
			this.physicalLine = physicalLine;
		}
	}

	int getScrollLine()
	{
		return scrollLine;
	}

	void setScrollLine(int scrollLine)
	{
		assert scrollLine >= 0;
		if(this.scrollLine != scrollLine)
		{
			setCallChanged(true);
			this.scrollLine = scrollLine;
		}
	}

	boolean isCallChanged()
	{
		return callChanged;
	}

	void setCallChanged(boolean callChanged)
	{
		this.callChanged = callChanged;
	}

	boolean isCallReset() {
		return callReset;
	}

	void setCallReset(boolean callReset)
	{
		this.callReset = callReset;
	}

	DisplayManager getDisplayManager()
	{
		return displayManager;
	}

	TextArea getTextArea()
	{
		return textArea;
	}

	void resetCallState()
	{
		callChanged = false;
		callReset = false;
	}
}
