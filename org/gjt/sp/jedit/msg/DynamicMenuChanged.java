/*
 * DynamicMenuChanged.java - Message that causes dynamic menus to be
 * reconstructed
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
 * Sending this message will cause the specified dynamic menu to be recreated.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 4.2pre2
 */
public class DynamicMenuChanged extends EBMessage
{
	//{{{ DynamicMenuChanged constructor
	/**
	 * Creates a new dynamic menu changed message.
	 * @param name The menu name. All dynamic menus with this name will be
	 * recreated next time they are displayed.
	 */
	public DynamicMenuChanged(String name)
	{
		super(null);

		this.name = name;
	} //}}}

	//{{{ getMenuName() method
	/**
	 * Returns the name of the menu in question.
	 */
	public String getMenuName()
	{
		return name;
	} //}}}

	//{{{ paramString() method
	public String paramString()
	{
		return "menu=" + name + "," + super.paramString();
	} //}}}

	//{{{ Private members
	private String name;
	//}}}
}
