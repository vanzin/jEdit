/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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

public class ColumnBlockLine
{
	private int line;
	int colStartIndex;
	int colEndIndex;
	float lineLength;
	
	//{{{ ColumnBlockLine() method
	public ColumnBlockLine(int line,int lineStartIndex,int lineEndIndex)
	{
		this.line = line;
		this.colEndIndex  = lineEndIndex;
		this.colStartIndex = lineStartIndex;
	}//}}}
	
	//{{{  getLine() method
	public int getLine()
	{
		return line;
	}//}}}
	
	//{{{ getColumnStartIndex() method
	public int getColumnStartIndex()
	{
		return colStartIndex;
	}//}}}
	
	//{{{ getColumnEndIndex() method
	public int getColumnEndIndex()
	{
		return colEndIndex;
	}//}}}
	
	//{{{ setLineLength() method
	public void setLineLength(float lineLength)
	{
		this.lineLength = lineLength;
	}//}}}
	
	//{{{  getLineLength() method
	public float getLineLength()
	{
		return lineLength;
	}//}}}
	
	//{{{ toString() method
	public String toString()
	{
		return "[ColumnBlockLine]colStartIndex:"+colStartIndex+"  colEndIndex:"+ colEndIndex+" lineLength:"+lineLength+" line:"+line;
	}//}}}
	
	//{{{ updateLineNo() method
	public void updateLineNo(int line )
	{
		this.line+=line;
	}//}}}
}
