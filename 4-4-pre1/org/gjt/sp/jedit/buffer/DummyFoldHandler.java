/*
 * DummyFoldHandler.java - Fold handler used when folding is switched off
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

import javax.swing.text.Segment;

/**
 * A fold handler that does nothing.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class DummyFoldHandler extends FoldHandler
{
	//{{{ DummyFoldHandler constructor
	public DummyFoldHandler()
	{
		super("none");
	}
	//}}}

	//{{{ getFoldLevel() method
	/**
	 * Returns the fold level of the specified line.
	 * @param buffer The buffer in question
	 * @param lineIndex The line index
	 * @param seg A segment the fold handler can use to obtain any
	 * text from the buffer, if necessary
	 * @return The fold level of the specified line
	 * @since jEdit 4.0pre1
	 */
	public int getFoldLevel(JEditBuffer buffer, int lineIndex, Segment seg)
	{
		return 0;
	} //}}}
}
