/*
 * ScrollLineCount.java
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

		physicalLine = displayManager.getFirstVisibleLine();
		int scrollLine = 0;
		while(physicalLine != -1)
		{
			scrollLine += displayManager
				.getScreenLineCount(physicalLine);
			physicalLine = displayManager
				.getNextVisibleLine(physicalLine);
		}

		this.scrollLine = scrollLine;
		physicalLine = displayManager.getBuffer().getLineCount();
	} //}}}
}
