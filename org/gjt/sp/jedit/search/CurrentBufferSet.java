/*
 * CurrentBufferSet.java - Current buffer matcher
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit.search;

import org.gjt.sp.jedit.*;

/**
 * A file set for searching the current buffer.
 * @author Slava Pestov
 * @version $Id$
 */
public class CurrentBufferSet implements SearchFileSet
{
	/**
	 * Returns the first buffer to search.
	 * @param view The view performing the search
	 */
	public Buffer getFirstBuffer(View view)
	{
		return view.getBuffer();
	}

	/**
	 * Returns the next buffer to search.
	 * @param view The view performing the search
	 * @param buffer The last buffer searched
	 */
	public Buffer getNextBuffer(View view, Buffer buffer)
	{
		if(buffer == null)
			return view.getBuffer();
		else
			return null;
	}

	/**
	 * Called if the specified buffer was found to have a match.
	 * @param buffer The buffer
	 */
	public void matchFound(Buffer buffer) {}

	/**
	 * Returns the number of buffers in this file set.
	 */
	public int getBufferCount()
	{
		return 1;
	}

	/**
	 * Returns the BeanShell code that will recreate this file set.
	 * @since jEdit 2.7pre3
	 */
	public String getCode()
	{
		return "new CurrentBufferSet()";
	}
}
