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
/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:37:55  spestov
 * Initial revision
 *
 * Revision 1.8  2000/11/24 06:48:35  sp
 * Caret position history
 *
 * Revision 1.7  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.6  1999/11/28 00:33:07  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.5  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.4  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.3  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 * Revision 1.2  1999/06/09 05:22:11  sp
 * Find next now supports multi-file searching, minor Perl mode tweak
 *
 * Revision 1.1  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 */
