/*
 * LineElement.java - For compatibility with Swing document API
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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

package org.gjt.sp.jedit.buffer;

//{{{ Imports
import javax.swing.text.*;
import org.gjt.sp.jedit.Buffer;
//}}}

/**
 * A class internal to jEdit's document model. You should not use it
 * directly.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class LineElement implements Element
{
	//{{{ LineElement constructor
	public LineElement(Buffer buffer, int line)
	{
		this.buffer = buffer;
		this.line = line;
	} //}}}

	//{{{ getDocument() method
	public Document getDocument()
	{
		return null;
	} //}}}

	//{{{ getParentElement() method
	public Element getParentElement()
	{
		return null;
	} //}}}

	//{{{ getName() method
	public String getName()
	{
		return null;
	} //}}}

	//{{{ getAttributes() method
	public AttributeSet getAttributes()
	{
		return null;
	} //}}}

	//{{{ getStartOffset() method
	public int getStartOffset()
	{
		return buffer.getLineStartOffset(line);
	} //}}}

	//{{{ getEndOffset() method
	public int getEndOffset()
	{
		return buffer.getLineEndOffset(line);
	} //}}}

	//{{{ getElementIndex() method
	public int getElementIndex(int offset)
	{
		return 0;
	} //}}}

	//{{{ getElementCount() method
	public int getElementCount()
	{
		return 0;
	} //}}}

	//{{{ getElement() method
	public Element getElement(int line)
	{
		return null;
	} //}}}

	//{{{ isLeaf() method
	public boolean isLeaf()
	{
		return true;
	} //}}}

	//{{{ Private members
	private Buffer buffer;
	private int line;
	//}}}
} //}}}
