/*
 * HyperSearchResult.java - HyperSearch result
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

import javax.swing.text.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;

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

	public HyperSearchResult(Buffer buffer, int line, int start, int end)
	{
		path = buffer.getPath();
		this.line = line;
		this.start = start;
		this.end = end;

		if(!buffer.isTemporary())
			bufferOpened(buffer);

		str = (line + 1) + ": " + getLine(buffer,
			buffer.getDefaultRootElement()
			.getElement(line));
	}

	String getLine(Buffer buffer, Element elem)
	{
		if(elem == null)
			return "";
		return buffer.getText(elem.getStartOffset(),
			elem.getEndOffset() -
			elem.getStartOffset() - 1)
			.replace('\t',' ');
	}

	public void bufferOpened(Buffer buffer)
	{
		this.buffer = buffer;
		Element map = buffer.getDefaultRootElement();
		Element elem = map.getElement(line);
		if(elem == null)
			elem = map.getElement(map.getElementCount()-1);
		startPos = buffer.createPosition(start);
		endPos = buffer.createPosition(end);
	}

	public void bufferClosed()
	{
		buffer = null;
		startPos = endPos = null;
	}

	public Buffer getBuffer()
	{
		if(buffer == null)
			buffer = jEdit.openFile(null,path);
		return buffer;
	}

	public String toString()
	{
		return str;
	}
}
