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

import com.microstar.xml.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.util.Log;

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
	 * @param jar The plugin
	 * @param uri The actions.xml URI
	 * @param cachedActionNames The list of cached action names
	 * @since jEdit 4.2pre1
	 */
	public ActionSet(EditPlugin.JAR jar, URL uri,
		String[] cachedActionNames)
	{
		this();
		this.jar = jar;
		this.uri = uri;
		if(cachedActionNames != null)
		{
			for(int i = 0; i < cachedActionNames.length; i++)
			{
				actions.put(cachedActionNames[i],placeholder);
			}
		}
		loaded = false;
	} //}}}

	//{{{ ActionSet constructor
	/**
	 * Creates a new action set.
	 * @since jEdit 4.0pre1
	 */
	public ActionSet()
	{
		actions = new Hashtable();
		loaded = true;
	} //}}}

	//{{{ ActionSet constructor
	/**
	 * Creates a new action set.
	 * @param label The label, shown in the shortcuts option pane
	 * @since jEdit 4.0pre1
	 */
	public ActionSet(String label)
	{
		this();
		this.label = label;
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
			jEdit.actionHash.put(action.getName(),this);
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
		String[] actions = getActionNames();
		for(int i = 0; i < actions.length; i++)
		{
			jEdit.actionHash.remove(actions[i]);
		}
		this.actions.clear();
	} //}}}

	//{{{ getAction() method
	/**
	 * Returns an action with the specified name.<p>
	 *
	 * <b>Deferred loading:</b> this will load the action set if necessary.
	 *
	 * @param name The action name
	 * @since jEdit 4.0pre1
	 */
	public EditAction getAction(String name)
	{
		Object obj = actions.get(name);
		if(obj == placeholder)
		{
			load();
			obj = actions.get(name);
			if(obj == placeholder)
			{
				Log.log(Log.WARNING,this,"Outdated cache");
				obj = null;
			}
		}

		return (EditAction)obj;
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

	//{{{ getActionNames() method
	/**
	 * Returns an array of all action names in this action set.
	 * @since jEdit 4.2pre1
	 */
	public String[] getActionNames()
	{
		String[] retVal = new String[actions.size()];
		Enumeration enum = actions.keys();
		int i = 0;
		while(enum.hasMoreElements())
		{
			retVal[i++] = (String)enum.nextElement();
		}
		return retVal;
	} //}}}

	//{{{ getActions() method
	/**
	 * Returns an array of all actions in this action set.<p>
	 *
	 * <b>Deferred loading:</b> this will load the action set if necessary.
	 *
	 * @since jEdit 4.0pre1
	 */
	public EditAction[] getActions()
	{
		load();

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
	 * @since jEdit 4.2pre1
	 */
	public boolean contains(String action)
	{
		return actions.containsKey(action);
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return label;
	} //}}}

	//{{{ initKeyBindings() method
	/**
	 * Initializes the action set's key bindings. Plugins and macros do not
	 * need to call this method, since jEdit calls it automatically for
	 * known action sets.
	 * @since jEdit 4.2pre1
	 */
	public void initKeyBindings()
	{
		InputHandler inputHandler = jEdit.getInputHandler();

		Iterator iter = actions.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String name = (String)entry.getKey();

			String shortcut1 = jEdit.getProperty(name + ".shortcut");
			if(shortcut1 != null)
				inputHandler.addKeyBinding(shortcut1,name);

			String shortcut2 = jEdit.getProperty(name + ".shortcut2");
			if(shortcut2 != null)
				inputHandler.addKeyBinding(shortcut2,name);
		}
	} //}}}

	//{{{ load() method
	/**
	 * Forces the action set to be loaded. Plugins and macros should not
	 * call this method.
	 * @since jEdit 4.2pre1
	 */
	public void load()
	{
		if(loaded)
			return;

		loaded = true;
		actions.clear();

		try
		{
			Log.log(Log.DEBUG,jEdit.class,"Loading actions from " + uri);

			ActionListHandler ah = new ActionListHandler(uri.toString(),this);
			XmlParser parser = new XmlParser();
			parser.setHandler(ah);
			parser.parse(null, null, new BufferedReader(
				new InputStreamReader(
				uri.openStream())));
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,this,uri + ":" + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,uri,e);
		}
	} //}}}

	//{{{ Package-private members
	boolean added;

	//{{{ getActions() method
	void getActions(ArrayList vec)
	{
		load();

		Enumeration enum = actions.elements();
		while(enum.hasMoreElements())
			vec.add(enum.nextElement());
	} //}}}

	//{{{ getActionNames() method
	void getActionNames(ArrayList vec)
	{
		Enumeration enum = actions.keys();
		while(enum.hasMoreElements())
			vec.add(enum.nextElement());
	} //}}}

	//}}}

	//{{{ Private members
	private String label;
	private Hashtable actions;
	private EditPlugin.JAR jar;
	private URL uri;
	private boolean loaded;

	private static final Object placeholder = new Object();

	//}}}
}
