/*
 * CurrentBufferSet.java - Current buffer matcher
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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
 * A file set for searching the current buffer.
 * @author Slava Pestov
 * @version $Id$
 */
public class CurrentBufferSet implements SearchFileSet
{
	//{{{ getFirstFile() method
	public String getFirstFile(View view)
	{
		return view.getBuffer().getPath();
	} //}}}

	//{{{ getNextFile() method
	public String getNextFile(View view, String file)
	{
		if(file == null)
			return view.getBuffer().getPath();
		else
			return null;
	} //}}}

	//{{{ getFiles() method
	public String[] getFiles(View view)
	{
		return new String[] { view.getBuffer().getPath() };
	} //}}}

	//{{{ getFileCount() method
	public int getFileCount(View view)
	{
		return 1;
	} //}}}

	//{{{ getCode() method
	public String getCode()
	{
		return "new CurrentBufferSet()";
	} //}}}
}
