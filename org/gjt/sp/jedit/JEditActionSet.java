/*
 * JEditActionSet.java - A set of actions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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

import java.io.*;
import java.net.URL;
import java.util.*;

import org.gjt.sp.jedit.input.AbstractInputHandler;
import org.gjt.sp.jedit.input.InputHandlerProvider;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;

/**
 * A set of actions, either loaded from an XML file, or constructed at runtime
 * by a plugin. <p>
 *
 * <h3>Action sets loaded from XML files</h3>
 *
 * Action sets are read from these files inside the plugin JAR:
 * <ul>
 * <li><code>actions.xml</code> - actions made available for use in jEdit views,
 * including the view's <b>Plugins</b> menu, the tool bar, etc.</li>
 * <li><code>browser.actions.xml</code> - actions for the file system browser's
 * <b>Plugins</b> menu.</li>
 * </ul>
 *
 * An action definition file has the following form:
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE ACTIONS SYSTEM "actions.dtd"&gt;
 *&lt;ACTIONS&gt;
 *    &lt;ACTION NAME="some-action"&gt;
 *        &lt;CODE&gt;
 *            // BeanShell code evaluated when the action is invoked
 *        &lt;/CODE&gt;
 *    &lt;/ACTION&gt;
 *    &lt;ACTION NAME="some-toggle-action"&gt;
 *        &lt;CODE&gt;
 *            // BeanShell code evaluated when the action is invoked
 *        &lt;/CODE&gt;
 *        &lt;IS_SELECTED&gt;
 *            // BeanShell code that should evaluate to true or false
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
 * the action's menu item label.
 *
 * <h3>View actions</h3>
 *
 * Actions defined in <code>actions.xml</code> can be added to the view's
 * <b>Plugins</b> menu; see {@link EditPlugin}.
 * The action code may use any standard predefined
 * BeanShell variable; see {@link BeanShell}.
 *
 * <h3>File system browser actions</h3>
 *
 * Actions defined in <code>actions.xml</code> can be added to the file
 * system browser's <b>Plugins</b> menu; see {@link EditPlugin}.
 * The action code may use any standard predefined
 * BeanShell variable, in addition to a variable <code>browser</code> which
 * contains a reference to the current
 * {@link org.gjt.sp.jedit.browser.VFSBrowser} instance.<p>
 *
 * File system browser actions should not define
 * <code>&lt;IS_SELECTED&gt;</code> blocks.
 *
 * <h3>Custom action sets</h3>
 *
 * Call {@link jEdit#addActionSet(ActionSet)} to add a custom action set to
 * jEdit's action context. You must also call {@link #initKeyBindings()} for new
 * action sets. Don't forget to call {@link jEdit#removeActionSet(ActionSet)}
 * before your plugin is unloaded, too.
 *
 * @see jEdit#getActionContext()
 * @see org.gjt.sp.jedit.browser.VFSBrowser#getActionContext()
 * @see ActionContext#getActionNames()
 * @see ActionContext#getAction(String)
 * @see jEdit#addActionSet(ActionSet)
 * @see jEdit#removeActionSet(ActionSet)
 * @see PluginJAR#getActionSet()
 * @see BeanShell
 * @see View
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id: ActionSet.java 9529 2007-05-12 15:06:52Z ezust $
 * @since jEdit 4.3pre13
 */
public abstract class JEditActionSet<E extends JEditAbstractEditAction> implements InputHandlerProvider
{
	//{{{ JEditActionSet constructor
	/**
	 * Creates a new action set.
	 * @since jEdit 4.3pre13
	 */
	public JEditActionSet()
	{
		actions = new Hashtable<String, Object>();
		loaded = true;
	} //}}}
	
	//{{{ JEditActionSet constructor
	/**
	 * Creates a new action set.
	 * @param cachedActionNames The list of cached action names
	 * @param uri The actions.xml URI
	 * @since jEdit 4.3pre13
	 */
	public JEditActionSet(String[] cachedActionNames, URL uri)
	{
		this();
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

	//{{{ addAction() method
	/**
	 * Adds an action to the action set.
	 * @param action The action
	 * @since jEdit 4.0pre1
	 */
	public void addAction(E action)
	{
		actions.put(action.getName(),action);
		if(context != null)
		{
			context.actionNames = null;
			context.actionHash.put(action.getName(),this);
		}
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
		if(context != null)
		{
			context.actionNames = null;
			context.actionHash.remove(name);
		}
	} //}}}

	//{{{ removeAllActions() method
	/**
	 * Removes all actions from the action set.
	 * @since jEdit 4.0pre1
	 */
	public void removeAllActions()
	{
		if(context != null)
		{
			context.actionNames = null;
			String[] actions = getActionNames();
			for(int i = 0; i < actions.length; i++)
			{
				context.actionHash.remove(actions[i]);
			}
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
	public E getAction(String name)
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

		return (E) obj;
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
		Enumeration e = actions.keys();
		int i = 0;
		while(e.hasMoreElements())
		{
			retVal[i++] = (String)e.nextElement();
		}
		return retVal;
	} //}}}

	//{{{ getCacheableActionNames() method
	/**
	 * Returns an array of all action names in this action set that should
	 * be cached; namely, <code>BeanShellAction</code>s.
	 * @since jEdit 4.2pre1
	 */
	public String[] getCacheableActionNames()
	{
		LinkedList<String> retVal = new LinkedList<String>();
		Enumeration e = actions.elements();
		while(e.hasMoreElements())
		{
			Object obj = e.nextElement();
			if(obj == placeholder)
			{
				// ??? this should only be called with
				// fully loaded action set
				Log.log(Log.WARNING,this,"Action set not up "
					+ "to date");
			}
			else if(obj instanceof JEditBeanShellAction)
				retVal.add(((JEditBeanShellAction)obj).getName());
		}
		return retVal.toArray(new String[retVal.size()]);
	} //}}}
	
	//{{{ getArray() method
	/**
	 * Returns an empty array E[].
	 * I know it is bad, if you find a method to instantiate a generic Array,
	 * tell me
	 * @param size the size of the array
	 * @return the empty array
	 */
	protected abstract E[] getArray(int size);		
	//}}}

	//{{{ getActions() method
	/**
	 * Returns an array of all actions in this action set.<p>
	 *
	 * <b>Deferred loading:</b> this will load the action set if necessary.
	 *
	 * @since jEdit 4.0pre1
	 */
	public E[] getActions()
	{
		load();
		E[] retVal = getArray(actions.size());
		Enumeration e = actions.elements();
		int i = 0;
		while(e.hasMoreElements())
		{
			retVal[i++] = (E) e.nextElement();
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
		boolean retval = actions.containsKey(action);
		return retval;
//		return actions.containsKey(action);
	} //}}}

	//{{{ size() method
	/**
	 * Returns the number of actions in this action set.
	 * @since jEdit 4.2pre2
	 */
	public int size()
	{
		return actions.size();
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
		//actions.clear();

		if (uri == null)
			return;
		try
		{
			Log.log(Log.DEBUG,this,"Loading actions from " + uri);
			ActionListHandler ah = new ActionListHandler(uri.toString(),this);
			if ( XMLUtilities.parseXML(uri.openStream(), ah))
			{
				Log.log(Log.ERROR, this, "Unable to parse: " + uri);
			}
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,this,uri,e);
		}
	} //}}}
	
	//{{{ createBeanShellAction() method
	/**
	 * This method should be implemented to return an action that will execute
	 * the given code
	 * @since 4.3pre13
	 */
	protected abstract JEditAbstractEditAction createBeanShellAction(String actionName,
									   String code,
									   String selected,
									   boolean noRepeat,
									   boolean noRecord,
									   boolean noRememberLast);
	//}}}
	
	//{{{ initKeyBindings() method
	/**
	 * Initializes the action set's key bindings.
	 * jEdit calls this method for all registered action sets when the
	 * user changes key bindings in the <b>Global Options</b> dialog box.<p>
	 *
	 * Note if your plugin adds a custom action set to jEdit's collection,
	 * it must also call this method on the action set after adding it.
	 *
	 * @since jEdit 4.2pre1
	 */
	public void initKeyBindings()
	{
		AbstractInputHandler inputHandler = getInputHandler();

		Iterator<Map.Entry<String,Object>> iter = actions.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry<String,Object> entry = iter.next();
			String name = entry.getKey();

			String shortcut1 = getProperty(name + ".shortcut");
			if(shortcut1 != null)
				inputHandler.addKeyBinding(shortcut1,name);

			String shortcut2 = getProperty(name + ".shortcut2");
			if(shortcut2 != null)
				inputHandler.addKeyBinding(shortcut2,name);
		}
	} //}}}
	
	//{{{ getProperty() method
	/**
	 * Returns a property for the given name.
	 * In jEdit it will returns a jEdit.getProperty(name), but it can
	 * return something else for a standalone textarea.
	 * @param name the property name
	 * @return the property value
	 * @since 4.3pre13
	 */
	protected abstract String getProperty(String name);
	//}}}

	//{{{ Package-private members
	JEditActionContext context;

	//{{{ getActionNames() method
	void getActionNames(List<String> vec)
	{
		Enumeration<String> e = actions.keys();
		while(e.hasMoreElements())
			vec.add(e.nextElement());
	} //}}}

	//}}}

	//{{{ Private members
	protected Hashtable<String,Object> actions;
	protected URL uri;
	protected boolean loaded;

	protected static final Object placeholder = new Object();

	//}}}
}
