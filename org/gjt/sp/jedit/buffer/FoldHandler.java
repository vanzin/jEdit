/*
 * FoldHandler.java - Fold handler interface
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

import java.util.*;
import javax.swing.text.Segment;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;

/**
 * Interface for obtaining the fold level of a specified label.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public abstract class FoldHandler
{
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
	public abstract int getFoldLevel(Buffer buffer, int lineIndex, Segment seg);
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

	//{{{ registerFoldHandler() method
	/**
	 * Adds a fold handler to the list of registered handlers
	 * @param handler The fold handler to add
	 * @since jEdit 4.0pre6
	 */
	public static void registerFoldHandler(FoldHandler handler)
	{
		if (getFoldHandler(handler.getName()) != null)
		{
			Log.log(Log.ERROR, FoldHandler.class, "Cannot register more than one fold handler with the same name");
			return;
		}

		foldHandlers.add(handler);
	}
	//}}}

	//{{{ getFoldHandlers() method
	/**
	 * Returns an array containing the list of registered fold handlers
	 * @since jEdit 4.0pre6
	 */
	public static FoldHandler[] getFoldHandlers()
	{
		FoldHandler[] handlers = new FoldHandler[foldHandlers.size()];
		return (FoldHandler[])foldHandlers.toArray(handlers);
	}
	//}}}

	//{{{ getFoldHandler() method
	/**
	 * Returns the fold handler with the specified name, or null if
	 * there is no registered handler with that name.
	 * @param name The name of the desired fold handler
	 * @since jEdit 4.0pre6
	 */
	public static FoldHandler getFoldHandler(String name)
	{
		FoldHandler handler;

		Iterator i = foldHandlers.iterator();
		while (i.hasNext())
		{
			handler = (FoldHandler)i.next();
			if (name.equals(handler.getName())) return handler;
		}

		return null;
	}
	//}}}

	//{{{ getFoldModes() method
	/**
	 * Returns an array containing the names of all registered fold handlers
	 * @since jEdit 4.0pre6
	 */
	public static String[] getFoldModes()
	{
		FoldHandler[] handlers = getFoldHandlers();
		String[] foldModes = new String[handlers.length];

		for (int i = 0; i < foldModes.length; i++)
		{
			foldModes[i] = handlers[i].getName();
		}

		return foldModes;
	}
	//}}}

	//{{{ FoldHandler() constructor
	protected FoldHandler(String name)
	{
		this.name = name;
	}
	//}}}

	//{{{ Private members
	private String name;

	private static ArrayList foldHandlers;
	//}}}

	//{{{ Static initializer
	static
	{
		foldHandlers = new ArrayList();
		registerFoldHandler(new DummyFoldHandler());
		registerFoldHandler(new IndentFoldHandler());
		registerFoldHandler(new ExplicitFoldHandler());
	}
	//}}}
}
