/*
 * HyperSearchResult.java - HyperSearch result
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

//{{{ Imports
import javax.swing.text.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
//}}}

/**
 * An occurrence of the search string.
 */
public class HyperSearchResult
{
	public String path;
	public Buffer buffer;
	public int line;
	public int start;
	public int end;
	public Position startPos;
	public Position endPos;
	public String str; // cached for speed

	//{{{ HyperSearchResult method
	public HyperSearchResult(Buffer buffer, int line, int start, int end)
	{
		path = buffer.getPath();
		this.line = line;
		this.start = start;
		this.end = end;

		if(!buffer.isTemporary())
			bufferOpened(buffer);

		str = (line + 1) + ": " + buffer.getLineText(line)
			.replace('\t',' ').trim();
	} //}}}

	//{{{ bufferOpened() method
	public void bufferOpened(Buffer buffer)
	{
		this.buffer = buffer;
		startPos = buffer.createPosition(Math.min(buffer.getLength(),start));
		endPos = buffer.createPosition(Math.min(buffer.getLength(),end));
	} //}}}

	//{{{ bufferClosed() method
	public void bufferClosed()
	{
		buffer = null;
		start = startPos.getOffset();
		end = endPos.getOffset();
		startPos = endPos = null;
	} //}}}

	//{{{ getBuffer() method
	public Buffer getBuffer()
	{
		if(buffer == null)
			buffer = jEdit.openFile(null,path);
		return buffer;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return str;
	} //}}}
}
