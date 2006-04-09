/*
 * ViewUpdate.java - View update message
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001, 2002 Slava Pestov
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.*;

/**
 * Message sent when a view-related change occurs.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class ViewUpdate extends EBMessage
{
	/**
	 * View created.
	 */
	public static final Object CREATED = "CREATED";

	/**
	 * View closed.
	 */
	public static final Object CLOSED = "CLOSED";

	/**
	 * Active edit pane changed.
	 * @since jEdit 4.1pre1
	 */
	public static final Object EDIT_PANE_CHANGED = "EDIT_PANE_CHANGED";

	/**
	 * Active view changed.
	 * @since jEdit 4.3pre4
	 */
	public static final Object ACTIVATED = "VIEW_ACTIVATED";

	//{{{ ViewUpdate constructor
	/**
	 * Creates a new view update message.
	 * @param view The view
	 * @param what What happened
	 */
	public ViewUpdate(View view, Object what)
	{
		super(view);

		if(what == null)
			throw new NullPointerException("What must be non-null");

		this.what = what;
	} //}}}

	//{{{ getWhat() method
	/**
	 * Returns what caused this view update.
	 */
	public Object getWhat()
	{
		return what;
	} //}}}

	//{{{ getView() method
	/**
	 * Returns the view involved.
	 */
	public View getView()
	{
		return (View)getSource();
	} //}}}

	//{{{ paramString() method
	public String paramString()
	{
		return "what=" + what + "," + super.paramString();
	} //}}}

	//{{{ Private members
	private Object what;
	//}}}
}
