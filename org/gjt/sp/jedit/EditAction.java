/*
 * EditAction.java - jEdit action listener
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

//{{{ Imports
import org.gjt.sp.util.Log;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//}}}

/**
 * An action that can be bound to a menu item, tool bar button or keystroke.
 *
 * @see jEdit#getAction(String)
 * @see jEdit#getActionNames()
 * @see ActionSet
 *
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class EditAction extends JEditAbstractEditAction<View>
{
	//{{{ EditAction constructors
	/**
	 * Creates a new edit action with the specified name.
	 * @param name The action name
	 */
	public EditAction(String name)
	{
		super(name);
	}
	
	public EditAction(String name, Object[] newArgs) 
	{
		super(name, newArgs);
	} //}}}
			
	//{{{ getLabel() method
	/**
	 * Returns the action's label. This returns the
	 * value of the property named by {@link #getName()} suffixed
	 * with <code>.label</code>.
	 * 
	 */
	public String getLabel()
	{
		if (args != null)
		{
			return jEdit.getProperty(name + ".label", args);
		}
		return jEdit.getProperty(name + ".label");
	} //}}}

	//{{{ getMouseOverText() method
	/**
	 * Returns the action's mouse over message. This returns the
	 * value of the property named by {@link #getName()} suffixed
	 * with <code>.mouse-over</code>.
	 */
	public final String getMouseOverText()
	{
		return jEdit.getProperty(name + ".mouse-over");
	} //}}}

	//{{{ invoke() method
	/**
	 * Invokes the action. This is an implementation of the Command pattern,
	 * and concrete actions should override this.
	 * 
	 * @param view The view
	 * @since jEdit 2.7pre2
	 * abstract since jEdit 4.3pre7
	 */
	abstract public void invoke(View view);

	//{{{ isToggle() method
	/**
	 * Returns if this edit action should be displayed as a check box
	 * in menus. This returns the
	 * value of the property named by {@link #getName()} suffixed
	 * with <code>.toggle</code>.
	 *
	 * @since jEdit 2.2pre4
	 */
	public final boolean isToggle()
	{
		return jEdit.getBooleanProperty(name + ".toggle");
	} //}}}

	//{{{ isSelected() method
	/**
	 * If this edit action is a toggle, returns if it is selected or not.
	 * @param comp The component
	 * @since jEdit 4.2pre1
	 */
	public boolean isSelected(Component comp)
	{
		return false;
	} //}}}

	//{{{ noRepeat() method
	/**
	 * Returns if this edit action should not be repeated. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRepeat()
	{
		return false;
	} //}}}

	//{{{ noRecord() method
	/**
	 * Returns if this edit action should not be recorded. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRecord()
	{
		return false;
	} //}}}

	//{{{ noRememberLast() method
	/**
	 * Returns if this edit action should not be remembered as the most
	 * recently invoked action.
	 * @since jEdit 4.2pre1
	 */
	public boolean noRememberLast()
	{
		return false;
	} //}}}

	//{{{ getCode() method
	/**
	 * Returns the BeanShell code that will replay this action.
	 * BeanShellAction.getCode() returns something more interesting for Actions that were loaded
	 * from the actions.xml file. 
	 * You do not need to override this method if your action name is unique,
	 * this EditAction was added to an ActionSet and that to an ActionContext of jEdit.
	 * 
	 * concrete since jEdit 4.3pre7
	 * @since jEdit 2.7pre2
	 * 
	 */
	public String getCode() 
	{
		return "jEdit.getAction(" + name + ").invoke(view); ";
	}
	//}}}
	
	//{{{ Wrapper class
	/**
	 * 'Wrap' EditActions in this class to turn them into AWT
	 * ActionListeners, that can be attached to buttons, menu items, etc.
	 */
	public static class Wrapper implements ActionListener
	{

		private final ActionContext context;
		private final String actionName;
		
		/**
		 * Creates a new action listener wrapper.
		 * @since jEdit 4.2pre1
		 */
		public Wrapper(ActionContext context, String actionName)
		{
			this.context = context;
			this.actionName = actionName;
		}

		/**
		 * Called when the user selects this action from a menu.
		 * It passes the action through the
		 * {@link org.gjt.sp.jedit.gui.InputHandler#invokeAction(EditAction)}
		 * method, which performs any recording or repeating.
		 *
		 * @param evt The action event
		 */
		public void actionPerformed(ActionEvent evt)
		{
			EditAction action = context.getAction(actionName);
			if(action == null)
			{
				Log.log(Log.WARNING,this,"Unknown action: "
					+ actionName);
			}
			else
				context.invokeAction(evt,action);
		}
	} //}}}
}
