/*
 * FirstLine.java
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
 * This Anchor is the first visible line of the textarea.
 *
 * @author Slava Pestov
 * @version $Id$
 */
class FirstLine extends Anchor
{

	// scrollLine + skew = vertical scroll bar position

	/**
	 * The skew is the scroll count from the beginning of the line.
	 * Used with soft wrap.
	 */
	private int skew;
	private int preContentRemovedNumLines;

	//{{{ FirstLine constructor
	FirstLine(DisplayManager displayManager,
		TextArea textArea)
	{
		super(displayManager,textArea);
	} //}}}

	@Override
	void preContentInserted(int startLine, int numLines)
	{
		int scrollLines = 0;
		int physicalLine = startLine;
		int currentPhysicalLine = getPhysicalLine();

		int numLinesVisible = 0;
		for(int i = 0;
			physicalLine < currentPhysicalLine;
			i++, physicalLine++)
		{
			if(getDisplayManager().isLineVisible(physicalLine))
			{
				scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
			}
			if(i < numLines)
				numLinesVisible++;
		}
		preContentInsertedScrollLines = scrollLines;
	}

	//{{{ contentInserted() method
	/**
	 * Some content is inserted.
	 *
	 * @param startLine the start of the insert
	 * @param numLines the number of inserted lines
	 */
	void contentInserted(int startLine, int numLines)
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"contentInserted() before:" + this);

		int currentPhysicalLine = getPhysicalLine();
		// The Anchor is changed only if the content was inserted before
		if(startLine == currentPhysicalLine)
			setCallChanged(true);
		else if(startLine < currentPhysicalLine)
		{
			int scrollLines = 0;
			int physicalLine = startLine;
			int endLine = currentPhysicalLine + numLines;
			int numLinesVisible = 0;

			for(int i = 0;physicalLine < endLine; physicalLine++, i++)
			{
				if(getDisplayManager().isLineVisible(physicalLine))
				{
					scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
				}
				if(i < numLines)
					numLinesVisible++;
			}
			movePhysicalLine(numLinesVisible);
			moveScrollLine(scrollLines - preContentInsertedScrollLines);
		}

		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"contentInserted() after:" + this);

		if(Debug.SCROLL_VERIFY)
			scrollVerify();
	} //}}}

	@Override
	void preContentRemoved(int startLine, int offset, int numLines)
	{
		int scrollLines = 0;
		int physicalLine = startLine;
		int currentPhysicalLine = getPhysicalLine();

		int numLinesVisible = 0;
		for(int i = 0;
			physicalLine < currentPhysicalLine;
			i++, physicalLine++)
		{
			if(getDisplayManager().isLineVisible(physicalLine))
			{
				scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
			}
			if(i < numLines)
				numLinesVisible++;
		}

		preContentRemovedScrollLines = scrollLines;
		preContentRemovedNumLines = numLinesVisible;
	}

	//{{{ contentRemoved() method
	/**
	 * Method called before a content is removed from a buffer.
	 *
	 * @param startLine the first line of the removed content
	 * @param offset the offset in the start line
	 * @param numLines the number of removed lines
	 */
	void contentRemoved(int startLine, int startOffset, int numLines)
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"contentRemoved() before:" + this);

		// The removed content starts before the Anchor, we need to pull the anchor up
		int currentPhysicalLine = getPhysicalLine();
		if(startLine == currentPhysicalLine)
			setCallChanged(true);
		else if(startLine < currentPhysicalLine)
		{
			int scrollLines = 0;
			int physicalLine = startLine;
			int endLine = currentPhysicalLine - numLines;

			for(;physicalLine < endLine; physicalLine++)
			{
				if(getDisplayManager().isLineVisible(physicalLine))
				{
					scrollLines += getDisplayManager().getScreenLineCount(physicalLine);
				}
			}
			movePhysicalLine(-preContentRemovedNumLines);
			moveScrollLine(scrollLines - preContentRemovedScrollLines);
		}
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"contentRemoved() after:" + this);
		if(Debug.SCROLL_VERIFY)
			scrollVerify();
	} //}}}

	//{{{ changed() method
	@Override
	public void changed()
	{
		//{{{ Debug code
		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"changed() before: "
				+ getPhysicalLine() + ':' + getScrollLine()
				+ ':' + getSkew());
		} //}}}

		if(Debug.SCROLL_VERIFY)
			scrollVerify();

		ensurePhysicalLineIsVisible();
		int currentPhysicalLine = getPhysicalLine();
		int screenLines = getDisplayManager().getScreenLineCount(currentPhysicalLine);

		if(getSkew() >= screenLines)
			setSkew(screenLines - 1);

		//{{{ Debug code
		if(Debug.SCROLL_VERIFY)
			scrollVerify();

		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"changed() after: "
				+ getPhysicalLine() + ':' + getScrollLine()
				+ ':' + getSkew());
		} //}}}
	} //}}}

	private void scrollVerify()
	{
		System.err.println("SCROLL_VERIFY");
		int verifyScrollLine = 0;
		int currentPhysicalLine = getPhysicalLine();

		for(int i = 0, n = getDisplayManager().getBuffer().getLineCount(); i < n && i < currentPhysicalLine; i++)
		{
			if(getDisplayManager().isLineVisible(i))
				verifyScrollLine += getDisplayManager().getScreenLineCount(i);
		}

		int scrollLine = getScrollLine();
		if(verifyScrollLine != scrollLine)
		{
			RuntimeException ex = new RuntimeException("ScrollLine is " + scrollLine + " but should be " + verifyScrollLine + " diff = " + (verifyScrollLine - scrollLine));
			Log.log(Log.ERROR,this,ex);
		}
	}

	//{{{ reset() method
	@Override
	public void reset()
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"reset()");

		int currentPhysicalLine = getPhysicalLine();

		int physicalLine = getDisplayManager().getFirstVisibleLine();
		int scrollLine = 0;

		while(physicalLine != -1)
		{
			if(physicalLine >= currentPhysicalLine)
				break;

			scrollLine += getDisplayManager().getScreenLineCount(physicalLine);

			int nextLine = getDisplayManager().getNextVisibleLine(physicalLine);
			if(nextLine == -1)
				break;
			else
				physicalLine = nextLine;
		}

		setPhysicalLine(physicalLine);
		setScrollLine(scrollLine);

		int screenLines = getDisplayManager().getScreenLineCount(physicalLine);
		if(getSkew() >= screenLines)
			setSkew(screenLines - 1);

		getTextArea().updateScrollBar();
	} //}}}

	//{{{ physDown() method
	// scroll down by physical line amount
	void physDown(int amount, int screenAmount)
	{
		int currentPhysicalLine = getPhysicalLine();
		int currentScrollLine = getScrollLine();

		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"physDown() start: "
				+ currentPhysicalLine + ':' + currentScrollLine);
		}

		setSkew(0);

		if(!getDisplayManager().isLineVisible(currentPhysicalLine))
		{
			int lastVisibleLine = getDisplayManager().getLastVisibleLine();
			if(currentPhysicalLine > lastVisibleLine)
				setPhysicalLine(lastVisibleLine);
			else
			{
				int nextPhysicalLine = getDisplayManager().getNextVisibleLine(currentPhysicalLine);
				assert nextPhysicalLine > 0;
				amount -= nextPhysicalLine - currentPhysicalLine;
				moveScrollLine(getDisplayManager().getScreenLineCount(currentPhysicalLine));
				setPhysicalLine(nextPhysicalLine);
			}
		}

		currentPhysicalLine = getPhysicalLine();
		int scrollLines = 0;
		for(;;)
		{
			int nextPhysicalLine = getDisplayManager().getNextVisibleLine(currentPhysicalLine);

			if(nextPhysicalLine == -1)
				break;
			else if(nextPhysicalLine > currentPhysicalLine + amount)
				break;
			else
			{
				scrollLines += getDisplayManager().getScreenLineCount(currentPhysicalLine);
				amount -= nextPhysicalLine - currentPhysicalLine;
				currentPhysicalLine = nextPhysicalLine;
			}
		}
		setPhysicalLine(currentPhysicalLine);
		moveScrollLine(scrollLines);

		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"physDown() end: "
				+ getPhysicalLine() + ':' + getScrollLine());
		}

		// JEditTextArea.scrollTo() needs this to simplify
		// its code
		if(screenAmount < 0)
			scrollUp(-screenAmount);
		else if(screenAmount > 0)
			scrollDown(screenAmount);
	} //}}}

	//{{{ physUp() method
	// scroll up by physical line amount
	void physUp(int amount, int screenAmount)
	{
		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"physUp() start: "
				+getPhysicalLine()+ ':' + getScrollLine());
		}

		setSkew(0);

		int currentPhysicalLine = getPhysicalLine();
		if(!getDisplayManager().isLineVisible(currentPhysicalLine))
		{
			int firstVisibleLine = getDisplayManager().getFirstVisibleLine();
			if(currentPhysicalLine < firstVisibleLine)
				setPhysicalLine(firstVisibleLine);
			else
			{
				int prevPhysicalLine = getDisplayManager().getPrevVisibleLine(currentPhysicalLine);
				amount -= currentPhysicalLine - prevPhysicalLine;
			}
		}

		currentPhysicalLine = getPhysicalLine();
		int scrollLines = 0;
		for(;;)
		{
			int prevPhysicalLine = getDisplayManager().getPrevVisibleLine(currentPhysicalLine);
			if(prevPhysicalLine == -1)
				break;
			else if(prevPhysicalLine < currentPhysicalLine - amount)
				break;
			else
			{
				scrollLines -= getDisplayManager().getScreenLineCount(prevPhysicalLine);
				amount -= currentPhysicalLine - prevPhysicalLine;
				currentPhysicalLine = prevPhysicalLine;
			}
		}
		setPhysicalLine(currentPhysicalLine);
		moveScrollLine(scrollLines);

		if(Debug.SCROLL_DEBUG)
		{
			Log.log(Log.DEBUG,this,"physUp() end: "
				+getPhysicalLine()+ ':' + getScrollLine());
		}

		// JEditTextArea.scrollTo() needs this to simplify
		// its code
		if(screenAmount < 0)
			scrollUp(-screenAmount);
		else if(screenAmount > 0)
			scrollDown(screenAmount);
	} //}}}

	//{{{ scrollDown() method
	// scroll down by screen line amount
	void scrollDown(int amount)
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"scrollDown()");

		ensurePhysicalLineIsVisible();

		amount += getSkew();

		setSkew(0);

		int physicalLine = getPhysicalLine();
		int screenLinesSum = 0;
		while(amount > 0)
		{
			int screenLines = getDisplayManager().getScreenLineCount(physicalLine);
			if(amount < screenLines)
			{
				setSkew(amount);
				break;
			}
			else
			{
				int nextLine = getDisplayManager().getNextVisibleLine(physicalLine);
				if(nextLine == -1)
					break;
				boolean visible = getDisplayManager().isLineVisible(physicalLine);
				physicalLine = nextLine;
				if(visible)
				{
					amount -= screenLines;
					screenLinesSum += screenLines;
				}
			}
		}
		setPhysicalLine(physicalLine);
		moveScrollLine(screenLinesSum);
	} //}}}

	//{{{ scrollUp() method
	// scroll up by screen line amount
	void scrollUp(int amount)
	{
		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"scrollUp() before:" + this);

		ensurePhysicalLineIsVisible();

		if(amount <= getSkew())
		{
			// the amount is less than the skew, so we stay in the same like, just going
			// upper
			setSkew(getSkew() - amount);
		}
		else
		{
			// moving to the first screen line of the current physical line
			amount -= getSkew();
			setSkew(0);

			int physicalLine = getPhysicalLine();
			int screenLinesSum = 0;
			while(amount > 0)
			{
				int prevLine = getDisplayManager().getPrevVisibleLine(physicalLine);
				if(prevLine == -1)
					break;
				// moving to the previous visible physical line
				physicalLine = prevLine;

				int screenLines = getDisplayManager().getScreenLineCount(physicalLine);
				screenLinesSum -= screenLines;

				if(amount < screenLines)
				{
					setSkew(screenLines - amount);
					break;
				}
				else
				{
					amount -= screenLines;
				}
			}
			setPhysicalLine(physicalLine);
			moveScrollLine(screenLinesSum);
		}

		if(Debug.SCROLL_DEBUG)
			Log.log(Log.DEBUG,this,"scrollUp() after:" + this);
	} //}}}

	//{{{ ensurePhysicalLineIsVisible() method
	void ensurePhysicalLineIsVisible()
	{
		int physicalLine = getPhysicalLine();
		if(!getDisplayManager().isLineVisible(physicalLine))
		{
			if(physicalLine > getDisplayManager().getLastVisibleLine())
			{
				setPhysicalLine(getDisplayManager().getLastVisibleLine());
				setScrollLine(getDisplayManager().getScrollLineCount() - 1);
			}
			else if(physicalLine < getDisplayManager().getFirstVisibleLine())
			{
				setPhysicalLine(getDisplayManager().getFirstVisibleLine());
				setScrollLine(0);
			}
			else
			{
				int nextLine = getDisplayManager().getNextVisibleLine(physicalLine);
				assert nextLine > 0;
				int screenLineCount = 0;
				screenLineCount = getDisplayManager().getScreenLineCount(nextLine);
				setPhysicalLine(nextLine);
				moveScrollLine(screenLineCount);
			}
		}
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return "FirstLine["+ getPhysicalLine() + ',' + getScrollLine() + ',' + getSkew() + ']';
	} //}}}

	int getSkew()
	{
		return skew;
	}

	void setSkew(int skew)
	{
		if(this.skew != skew)
		{
			this.skew = skew;
			setCallChanged(true);
		}
	}
}
