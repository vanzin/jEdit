/*
 * ActionSet.java - A set of actions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
 * A set of actions.<p>
 *
 * Action sets are read from <code>actions.xml</code> files
 * contained inside plugin JARs. An action definition file has the following
 * form:
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE ACTIONS SYSTEM "actions.dtd"&gt;
 *&lt;ACTIONS&gt;
 *    &lt;ACTION NAME="some-action"&gt;
 *        &lt;CODE&gt;
 *            // Action code
 *        &lt;/CODE&gt;
 *    &lt;/ACTION&gt;
 *    &lt;ACTION NAME="some-toggle-action"&gt;
 *        &lt;CODE&gt;
 *            // Action code
 *        &lt;/CODE&gt;
 *        &lt;IS_SELECTED&gt;
 *            // Returns true or false
 *        &lt;/IS_SELECTED&gt;
 *    &lt;/ACTION&gt;
 *&lt;/ACTIONS&gt;</pre>
 *
 * The following elements are valid:
 *
 * <ul>
 * <li>
 * <code>ACTIONS</code> is the top-level element and refers
 * to the set of actions used by the plugin.
 * </li>
 * <li>
 * An <code>ACTION</code> contains the data for a particular action.
 * It has three attributes: a required <code>NAME</code>;
 * an optional <code>NO_REPEAT</code>, which is a flag
 * indicating whether the action should not be repeated with the
 * <b>C+ENTER</b> command; and an optional
 * <code>NO_RECORD</code> which is a a flag indicating whether the
 * action should be recorded if it is invoked while the user is recording a
 * macro. The two flag attributes
 * can have two possible values, "TRUE" or
 * "FALSE". In both cases, "FALSE" is the
 * default if the attribute is not specified.
 * </li>
 * <li>
 * An <code>ACTION</code> can have two child elements
 * within it: a required <code>CODE</code> element which
 * specifies the
 * BeanShell code that will be executed when the action is invoked,
 * and an optional <code>IS_SELECTED</code> element, used for
 * checkbox
 * menu items.  The <code>IS_SELECTED</code> element contains
 * BeanShell code that returns a boolean flag that will
 * determine the state of the checkbox.
 * </li>
 * </ul>
 *
 * Each action must have a property <code><i>name</i>.label</code> containing
 * the action's menu item label. The action code may use any predefined
 * BeanShell variable; see {@link BeanShell}.
 *
 * @see jEdit#getActionSets()
 * @see jEdit#addActionSet(ActionSet)
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 * @since jEdit 4.0pre1
 */
public class ActionSet
{
	//{{{ ActionSet constructor
	/**
	 * Creates a new action set.
	 * @since jEdit 4.0pre1
	 */
	public ActionSet()
	{
		this(null);
	} //}}}

	//{{{ ActionSet constructor
	/**
	 * Creates a new action set.
	 * @param label The label, shown in the shortcuts option pane
	 * @since jEdit 4.0pre1
	 */
	public ActionSet(String label)
	{
		this.label = label;
		actions = new Hashtable();
	} //}}}

	//{{{ getLabel() method
	/**
	 * Return the action source label.
	 * @since jEdit 4.0pre1
	 */
	public String getLabel()
	{
		return label;
	} //}}}

	//{{{ setLabel() method
	/**
	 * Sets the action source label.
	 * @param label The label
	 * @since jEdit 4.0pre1
	 */
	public void setLabel(String label)
	{
		this.label = label;
	} //}}}

	//{{{ addAction() method
	/**
	 * Adds an action to the action set.
	 * @param action The action
	 * @since jEdit 4.0pre1
	 */
	public void addAction(EditAction action)
	{
		actions.put(action.getName(),action);
		if(added)
			jEdit.actionHash.put(action.getName(),action);
	} //}}}

	//{{{ removeAction() method
	/**
	 * Removes an action from the action set.
	 * @param name The action name
	 * @since jEdit 4.0pre1
	 */
	public void removeAction(String name)
	{
		actions.remove(name);
		if(added)
			jEdit.actionHash.remove(name);
	} //}}}

	//{{{ removeAllActions() method
	/**
	 * Removes all actions from the action set.
	 * @since jEdit 4.0pre1
	 */
	public void removeAllActions()
	{
		EditAction[] actions = getActions();
		for(int i = 0; i < actions.length; i++)
		{
			jEdit.actionHash.remove(actions[i].getName());
		}
		this.actions.clear();
	} //}}}

	//{{{ getAction() method
	/**
	 * Returns an action with the specified name.
	 * @param name The action name
	 * @since jEdit 4.0pre1
	 */
	public EditAction getAction(String name)
	{
		return (EditAction)actions.get(name);
	} //}}}

	//{{{ getActionCount() method
	/**
	 * Returns the number of actions in the set.
	 * @since jEdit 4.0pre1
	 */
	public int getActionCount()
	{
		return actions.size();
	} //}}}

	//{{{ getActions() method
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
	} //}}}

	//{{{ contains() method
	/**
	 * Returns if this action set contains the specified action.
	 * @param action The action
	 * @since jEdit 4.0pre1
	 */
	public boolean contains(EditAction action)
	{
		return actions.contains(action);
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return label;
	} //}}}

	//{{{ Package-private members
	boolean added;

	void getActions(Vector vec)
	{
		Enumeration enum = actions.elements();
		while(enum.hasMoreElements())
			vec.addElement(enum.nextElement());
	} //}}}

	//{{{ Private members
	private String label;
	private Hashtable actions;
	//}}}
}
