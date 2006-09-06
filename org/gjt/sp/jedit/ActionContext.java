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

import java.util.*;

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
public abstract class ActionContext
{
	//{{{ invokeAction() method
	/**
	 * Invokes the given action in response to a user-generated event.
	 * @param evt The event
	 * @param action The action
	 * @since jEdit 4.2pre1
	 */
	public abstract void invokeAction(EventObject evt, EditAction action);
	//}}}

	//{{{ addActionSet() method
	/**
	 * Adds a new action set to the context.
	 * @since jEdit 4.2pre1
	 */
	public void addActionSet(ActionSet actionSet)
	{
		actionNames = null;
		actionSets.addElement(actionSet);
		actionSet.context = this;
		String[] actions = actionSet.getActionNames();
		for(int i = 0; i < actions.length; i++)
		{
			/* Is it already there? */
			if (actionHash.containsKey(actions[i])) 
			{
				/* Save it for plugin unloading time */
				ActionSet oldAction = actionHash.get(actions[i]);
				overriddenActions.put(actions[i], oldAction);
			}
			actionHash.put(actions[i],actionSet);
		}
	} //}}}

	//{{{ removeActionSet() method
	/**
	 * Removes an action set from the context.
	 * @since jEdit 4.2pre1
	 */
	public void removeActionSet(ActionSet actionSet)
	{
		actionNames = null;
		actionSets.removeElement(actionSet);
		actionSet.context = null;
		String[] actions = actionSet.getActionNames();
		for(int i = 0; i < actions.length; i++)
		{
			actionHash.remove(actions[i]);
			if (overriddenActions.containsKey(actions[i])) 
			{
				ActionSet oldAction = overriddenActions.remove(actions[i]);
				actionHash.put(actions[i], oldAction);
			}
		}
	} //}}}

	//{{{ getActionSets() method
	/**
	 * Returns all registered action sets.
	 * @since jEdit 4.2pre1
	 */
	public ActionSet[] getActionSets()
	{
		ActionSet[] retVal = new ActionSet[actionSets.size()];
		actionSets.copyInto(retVal);
		return retVal;
	} //}}}

	//{{{ getAction() method
	/**
	 * Returns the specified action.
	 * @param name The action name
	 * @since jEdit 4.2pre1
	 */
	public EditAction getAction(String name)
	{
		ActionSet set = (ActionSet)actionHash.get(name);
		if(set == null)
			return null;
		else
			return set.getAction(name);
	} //}}}

	//{{{ getActionSetForAction() method
	/**
	 * Returns the action set that contains the specified action.
	 *
	 * @param action The action
	 * @since jEdit 4.2pre1
	 */
	public ActionSet getActionSetForAction(String action)
	{
		return (ActionSet)actionHash.get(action);
	} //}}}

	//{{{ getActionNames() method
	/**
	 * Returns all registered action names.
	 */
	public String[] getActionNames()
	{
		if(actionNames == null)
		{
			List vec = new LinkedList();
			for(int i = 0; i < actionSets.size(); i++)
				(actionSets.elementAt(i)).getActionNames(vec);

			actionNames = (String[])vec.toArray(
				new String[vec.size()]);
			Arrays.sort(actionNames,
				new MiscUtilities.StringICaseCompare());
		}

		return actionNames;
	} //}}}

	//{{{ Package-private members
	String[] actionNames;
	Hashtable<String, ActionSet> actionHash = new Hashtable<String, ActionSet>();
	
	/** A map of built-in actions that were overridden by plugins. */
	Hashtable<String, ActionSet> overriddenActions = new Hashtable<String, ActionSet>(); 
	//}}}

	//{{{ Private members
	private Vector<ActionSet> actionSets = new Vector<ActionSet>();
	//}}}
}
