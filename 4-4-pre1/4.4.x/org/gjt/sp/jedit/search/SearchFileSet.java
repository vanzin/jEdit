/*
 * SearchFileSet.java - Abstract file matcher interface
 * Copyright (C) 1999, 2001 Slava Pestov
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
 * An abstract interface representing a set of files.
 * @author Slava Pestov
 * @version $Id$
 */
public interface SearchFileSet
{
	/**
	 * Returns the first file to search.
	 * @param view The view performing the search
	 */
	String getFirstFile(View view);

	/**
	 * Returns the next file to search.
	 * @param view The view performing the search
	 * @param path The last file searched
	 */
	String getNextFile(View view, String path);

	/**
	 * Returns all path names in this file set.
	 * @param view The view performing the search
	 */
	String[] getFiles(View view);

	/**
	 * Returns the number of files in this file set.
	 */
	int getFileCount(View view);

	/**
	 * Returns the BeanShell code that will recreate this file set.
	 */
	String getCode();
}
