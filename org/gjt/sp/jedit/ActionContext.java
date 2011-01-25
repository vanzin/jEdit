/*
 * ActionContext.java - For code sharing between jEdit and VFSBrowser
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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

package org.gjt.sp.jedit;

/**
 * Manages a collection of action sets. There are two instances of this class
 * in jEdit:
 * <ul>
 * <li>{@link org.gjt.sp.jedit.jEdit#getActionContext()} - editor actions
 * <li>{@link org.gjt.sp.jedit.browser.VFSBrowser#getActionContext()} - browser
 * actions
 * </ul>
 *
 * @since jEdit 4.2pre1
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class ActionContext extends JEditActionContext<EditAction, ActionSet>
{
	//{{{ getActionSetForAction() method
	/**
	 * Returns the action set that contains the specified action.
	 * This method is still here for binary compatility
	 *
	 * @param action The action
	 * @return the actionSet that contains the given action
	 * @since jEdit 4.2pre1
	 */
	@Override
	public ActionSet getActionSetForAction(String action)
	{
		return super.getActionSetForAction(action);
	} //}}}

	//{{{ getAction() method
	/**
	 * Returns the specified action.
	 * @param name The action name
	 * @return a EditAction or null if it doesn't exist
	 * @since jEdit 4.2pre1
	 */
	@Override
	public EditAction getAction(String name)
	{
		return super.getAction(name);
	} //}}}
	
	
}
