/*
 * ActionSet.java - A set of actions
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

package org.gjt.sp.jedit;

import java.util.*;

/**
 * A set of actions.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class ActionSet
{
	/**
	 * Creates a new action set.
	 * @since jEdit 4.0pre1
	 */
	public ActionSet()
	{
		this(null);
	}

	/**
	 * Creates a new action set.
	 * @param label The label, shown in the shortcuts option pane
	 * @since jEdit 4.0pre1
	 */
	public ActionSet(String label)
	{
		this.label = label;
		actions = new Hashtable();
	}

	/**
	 * Return the action source label.
	 * @since jEdit 4.0pre1
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Sets the action source label.
	 * @param label The label
	 * @since jEdit 4.0pre1
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Adds an action to the action set.
	 * @param action The action
	 * @since jEdit 4.0pre1
	 */
	public void addAction(EditAction action)
	{
		actions.put(action.getName(),action);
	}

	/**
	 * Removes an action from the action set.
	 * @param name The action name
	 * @since jEdit 4.0pre1
	 */
	public void removeAction(String name)
	{
		actions.remove(name);
	}

	/**
	 * Removes all actions from the action set.
	 * @since jEdit 4.0pre1
	 */
	public void removeAllActions()
	{
		actions.clear();
	}

	/**
	 * Returns an action with the specified name.
	 * @param name The action name
	 * @since jEdit 4.0pre1
	 */
	public EditAction getAction(String name)
	{
		return (EditAction)actions.get(name);
	}

	/**
	 * Returns the number of actions in the set.
	 * @since jEdit 4.0pre1
	 */
	public int getActionCount()
	{
		return actions.size();
	}

	/**
	 * Returns an array of all actions in this action set.
	 * @since jEdit 4.0pre1
	 */
	public EditAction[] getActions()
	{
		EditAction[] retVal = new EditAction[actions.size()];
		Enumeration enum = actions.elements();
		int i = 0;
		while(enum.hasMoreElements())
		{
			retVal[i++] = (EditAction)enum.nextElement();
		}
		return retVal;
	}

	/**
	 * Returns if this action set contains the specified action.
	 * @param action The action
	 * @since jEdit 4.0pre1
	 */
	public boolean contains(EditAction action)
	{
		return actions.contains(action);
	}

	public String toString()
	{
		return label;
	}

	// package-private members
	void getActions(Vector vec)
	{
		Enumeration enum = actions.elements();
		while(enum.hasMoreElements())
			vec.addElement(enum.nextElement());
	}

	// private members
	private String label;
	private Hashtable actions;
}
