/*
 * SearchFileSet.java - Abstract file matcher interface
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
 * An abstract interface for matching files.
 * @author Slava Pestov
 * @version $Id$
 */
public interface SearchFileSet
{
	/**
	 * Returns the first buffer to search.
	 * @param view The view performing the search
	 */
	Buffer getFirstBuffer(View view);

	/**
	 * Returns the next buffer to search.
	 * @param view The view performing the search
	 * @param buffer The last buffer searched
	 */
	Buffer getNextBuffer(View view, Buffer buffer);

	/**
	 * Called if the specified buffer was found to have a match.
	 * @param buffer The buffer
	 */
	void matchFound(Buffer buffer);

	/**
	 * Returns the number of buffers in this file set.
	 */
	int getBufferCount();

	/**
	 * Returns the BeanShell code that will recreate this file set.
	 * @since jEdit 2.7pre3
	 */
	String getCode();
}
