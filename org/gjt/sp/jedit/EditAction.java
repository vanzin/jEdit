/*
 * EditAction.java - jEdit action listener
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.util.EventObject;
import org.gjt.sp.util.Log;

/**
 * Instead of subclassing EditAction directly, you should now write an
 * actions.xml file.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class EditAction
// no longer implements ActionListener
{
	/**
	 * @deprecated Create an actions.xml file instead of writing
	 * EditAction implementations!
	 */
	public EditAction(String name)
	{
		// The only people who use this constructor are
		// plugins written for the old action API, so
		// we can safely assume that 'plugin' should be
		// true.
		this(name,true);
	}

	/**
	 * Creates a new <code>EditAction</code>.
	 * @param name The name of the action
	 * @param plugin True if this is a plugin action
	 * @since jEdit 3.1pre1
	 */
	/* package-private */ EditAction(String name, boolean plugin)
	{
		this.name = name;
		this.plugin = plugin;
	}

	/**
	 * Returns the internal name of this action.
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Returns true if this action was loaded from a plugin, false
	 * if it was loaded from the core.
	 * @since jEdit 3.1pre1
	 */
	public boolean isPluginAction()
	{
		return plugin;
	}

	/**
	 * Invokes the action.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void invoke(View view)
	{
		// default implementation
		ActionEvent evt = new ActionEvent(view,
			ActionEvent.ACTION_PERFORMED,
			null);

		actionPerformed(evt);
	}

	/**
	 * @deprecated Create an actions.xml file instead of writing
	 * EditAction implementations!
	 */
	public void actionPerformed(ActionEvent evt) {}

	/**
	 * @deprecated No longer necessary.
	 */
	public static View getView(EventObject evt)
	{
		if(evt != null)
		{
			Object o = evt.getSource();
			if(o instanceof Component)
				return getView((Component)o);
		}
		// this shouldn't happen
		return null;
	}

	/**
	 * @deprecated No longer necessary.
	 */
	public static Buffer getBuffer(EventObject evt)
	{
		View view = getView(evt);
		if(view != null)
			return view.getBuffer();
		return null;
	}

	/**
	 * Finds the view parent of the specified component.
	 * @since jEdit 2.2pre4
	 */
	public static View getView(Component comp)
	{
		for(;;)
		{
			if(comp instanceof View)
				return (View)comp;
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else if(comp != null)
				comp = comp.getParent();
			else
				break;
		}
		return null;
	}

	/**
	 * Returns if this edit action should be displayed as a check box
	 * in menus.
	 * @since jEdit 2.2pre4
	 */
	public boolean isToggle()
	{
		return false;
	}

	/**
	 * If this edit action is a toggle, returns if it is selected or not.
	 * @param view The view
	 * @since jEdit 3.2pre5
	 */
	public boolean isSelected(View view)
	{
		return isSelected((Component)view);
	}

	/**
	 * @deprecated Override the form that accepts a view instead
	 */
	public boolean isSelected(Component comp)
	{
		return false;
	}

	/**
	 * Returns if this edit action should not be repeated. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRepeat()
	{
		return false;
	}

	/**
	 * Returns if this edit action should not be recorded. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRecord()
	{
		return false;
	}

	/**
	 * Returns the BeanShell code that will replay this action.
	 * @since jEdit 2.7pre2
	 */
	public String getCode()
	{
		return "view.getInputHandler().invokeAction("
			+ "jEdit.getAction(\"" + name + "\"))";
	}

	public String toString()
	{
		return name;
	}

	// private members
	private String name;
	private boolean plugin;

	/**
	 * 'Wrap' EditActions in this class to turn them into AWT
	 * ActionListeners, that can be attached to buttons, menu items, etc.
	 */
	public static class Wrapper implements ActionListener
	{
		public Wrapper(EditAction action)
		{
			this.action = action;
		}

		/**
		 * Called when the user selects this action from a menu.
		 * It passes the action through the
		 * <code>InputHandler.executeAction()</code> method,
		 * which performs any recording or repeating. It also
		 * loads the action if necessary.
		 *
		 * @param evt The action event
		 */
		public void actionPerformed(ActionEvent evt)
		{
			// Let input handler do recording, repeating, etc
			EditAction.getView(evt).getInputHandler()
				.invokeAction(action);
		}

		// private members
		private EditAction action;
	}
}
