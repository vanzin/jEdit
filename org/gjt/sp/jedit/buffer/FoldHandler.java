/*
 * FoldHandler.java - Fold handler interface
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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

import java.util.List;

import javax.swing.text.Segment;

/**
 * Interface for obtaining the fold level of a specified line.<p>
 *
 * Plugins can provide fold handlers by defining entries in their
 * <code>services.xml</code> files like so:
 *
 * <pre>&lt;SERVICE CLASS="org.gjt.sp.jedit.buffer.FoldHandler" NAME="<i>name</i>"&gt;
 *    new <i>MyFoldHandler<i>();
 *&lt;/SERVICE&gt;</pre>
 *
 * See {@link org.gjt.sp.jedit.ServiceManager} for details.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.3pre3
 */
public abstract class FoldHandler
{
	/** The FoldHandlerProvider. */
	public static FoldHandlerProvider foldHandlerProvider;

	//{{{ getName() method
	/**
	 * Returns the internal name of this FoldHandler
	 * @return The internal name of this FoldHandler
	 * @since jEdit 4.0pre6
	 */
	public String getName()
	{
		return name;
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
	public abstract int getFoldLevel(JEditBuffer buffer, int lineIndex, Segment seg);
	//}}}

	//{{{ getPrecedingFoldLevels() method
	/**
	 * Returns the fold levels of the lines preceding the specified line,
	 * which depend on the specified line.
	 * @param buffer The buffer in question
	 * @param lineIndex The line index
	 * @param seg A segment the fold handler can use to obtain any
	 * @param lineFoldLevel The fold level of the specified line
	 * @return The fold levels of the preceding lines, in decreasing line
	 * number order (i.e. bottomost line first).
	 * @since jEdit 4.3pre18
	 */
	public List<Integer> getPrecedingFoldLevels(JEditBuffer buffer,
		int lineIndex, Segment seg, int lineFoldLevel)
	{
		return null;
	}
	//}}}

	//{{{ equals() method
	/**
	 * Returns if the specified fold handler is equal to this one.
	 * @param o The object
	 */
	public boolean equals(Object o)
	{
		// Default implementation... subclasses can extend this.
		if(o == null)
			return false;
		else
			return getClass() == o.getClass();
	} //}}}

	//{{{ hashCode() method
	public int hashCode()
	{
		return getClass().hashCode();
	} //}}}

	//{{{ getFoldHandler() method
	/**
	 * Returns the fold handler with the specified name, or null if
	 * there is no registered handler with that name.
	 * @param name The name of the desired fold handler
	 * @since jEdit 4.0pre6
	 */
	public static FoldHandler getFoldHandler(String name)
	{
		return foldHandlerProvider.getFoldHandler(name);
	}
	//}}}

	//{{{ getFoldModes() method
	/**
	 * Returns an array containing the names of all registered fold
	 * handlers.
	 *
	 * @since jEdit 4.0pre6
	 */
	public static String[] getFoldModes()
	{
		return foldHandlerProvider.getFoldModes();
	}
	//}}}

	//{{{ FoldHandler() constructor
	protected FoldHandler(String name)
	{
		this.name = name;
	}
	//}}}

	//{{{ toString() method
	public String toString()
	{
		return name;
	} //}}}

	private final String name;
}
