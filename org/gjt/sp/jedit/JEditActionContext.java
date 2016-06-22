/*
 * JEditActionContext.java - For code sharing between jEdit and VFSBrowser
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
 * Portions copyright (C) 2007 Matthieu Casanova
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

import org.gjt.sp.util.StandardUtilities;

import java.lang.reflect.Array;
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
 * @since jEdit 4.3pre13
 * @author Slava Pestov
 * @version $Id: ActionContext.java 6884 2006-09-06 02:38:55Z ezust $
 */
public abstract class JEditActionContext<F extends JEditAbstractEditAction, E extends JEditActionSet<F>>
{
	//{{{ invokeAction() method
	/**
	 * Invokes the given action in response to a user-generated event.
	 * @param evt The event
	 * @param action The action
	 * @since jEdit 4.3pre13
	 */
	public abstract void invokeAction(EventObject evt, F action);
	//}}}

	//{{{ addActionSet() method
	/**
	 * @param actionSet Adds a new action set to the context.
	 * @since jEdit 4.3pre13
	 */
	public void addActionSet(E actionSet)
	{
		actionNames = null;
		actionSets.addElement(actionSet);
		actionSet.context = this;
		String[] actions = actionSet.getActionNames();
		for (String action : actions)
		{
			/* Is it already there? */
			if (actionHash.containsKey(action))
			{
				/* Save it for plugin unloading time */
				E oldAction = actionHash.get(action);
				overriddenActions.put(action, oldAction);
			}
			actionHash.put(action, actionSet);
		}
	} //}}}

	//{{{ removeActionSet() method
	/**
	 * @param actionSet Removes an action set from the context.
	 * @since jEdit 4.23pre13
	 */
	public void removeActionSet(E actionSet)
	{
		actionNames = null;
		actionSets.removeElement(actionSet);
		actionSet.context = null;
		String[] actions = actionSet.getActionNames();
		for (String action : actions)
		{
			actionHash.remove(action);
			if (overriddenActions.containsKey(action))
			{
				E oldAction = overriddenActions.remove(action);
				actionHash.put(action, oldAction);
			}
		}
	} //}}}

	//{{{ getActionSets() method
	/**
	 * @return all registered action sets.
	 * @since jEdit 4.3pre13
	 */
	@SuppressWarnings({"unchecked"}) 
	public E[] getActionSets()
	{
		if (actionSets.isEmpty())
			return null;
		Class clazz = actionSets.get(0).getClass();
		E[] retVal =(E[]) Array.newInstance(clazz, actionSets.size());
		actionSets.copyInto(retVal);
		return retVal;
	} //}}}

	//{{{ getAction() method
	/**
	 * Returns the specified action.
	 * @param name The action name
	 * @return a JEditAbstractEditAction or null if it doesn't exist
	 * @since jEdit 4.3pre13
	 */
	public F getAction(String name)
	{
		E set = actionHash.get(name);
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
	 * @return the actionSet that contains the given action
	 * @since jEdit 4.3pre13
	 */
	public E getActionSetForAction(String action)
	{
		return actionHash.get(action);
	} //}}}

	//{{{ getActionNames() method
	/**
	 * @return all registered action names.
	 */
	public String[] getActionNames()
	{
		if(actionNames == null)
		{
			List<String> vec = new LinkedList<String>();
			for(int i = 0; i < actionSets.size(); i++)
				(actionSets.elementAt(i)).getActionNames(vec);

			actionNames = vec.toArray(new String[vec.size()]);
			Arrays.sort(actionNames,
				new StandardUtilities.StringCompare<String>(true));
		}

		return Arrays.copyOf(actionNames, actionNames.length);
	} //}}}

	//{{{ Package-private members
	String[] actionNames;
	/** 
	 * This map contains as key an action name, 
	 * and as value the JEditActionSet that contains this action
	 */
	Hashtable<String, E> actionHash = new Hashtable<String, E>();
	
	/** A map of built-in actions that were overridden by plugins. */
	Hashtable<String, E> overriddenActions = new Hashtable<String, E>(); 
	//}}}

	//{{{ Private members
	private final Vector<E> actionSets = new Vector<E>();
	//}}}
}
