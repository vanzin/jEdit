/*
 * EditAction.java - jEdit action listener
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import org.gjt.sp.util.Log;
//}}}

/**
 * Instead of subclassing EditAction directly, you should now write an
 * actions.xml file.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class EditAction
{
	//{{{ EditAction constructor
	/**
	 * Creates a new edit action with the specified name.
	 * @param name The action name
	 */
	public EditAction(String name)
	{
		this.name = name;
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the internal name of this action.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getLabel() method
	/**
	 * Returns the action's label. The default implementation returns the
	 * value of the property named by the action's internal name suffixed
	 * with <code>.label</code>.
	 */
	public String getLabel()
	{
		return jEdit.getProperty(name + ".label");
	} //}}}

	//{{{ getMouseOverText() method
	/**
	 * Returns the text that should be shown when the mouse is placed over
	 * this action's menu item or tool bar button. Currently only used by
	 * the macro system.
	 * @since jEdit 4.0pre5
	 */
	public String getMouseOverText()
	{
		return null;
	} //}}}

	//{{{ invoke() method
	/**
	 * Invokes the action.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public abstract void invoke(View view);
	//}}}

	//{{{ getView() method
	/**
	 * @deprecated Call <code>GUIUtilities.getView()</code> instead.
	 */
	public static View getView(Component comp)
	{
		// moved to GUIUtilities as it makes no sense being here.
		return GUIUtilities.getView(comp);
	} //}}}

	//{{{ isToggle() method
	/**
	 * Returns if this edit action should be displayed as a check box
	 * in menus.
	 * @since jEdit 2.2pre4
	 */
	public boolean isToggle()
	{
		return false;
	} //}}}

	//{{{ isSelected() method
	/**
	 * If this edit action is a toggle, returns if it is selected or not.
	 * @param view The view
	 * @since jEdit 3.2pre5
	 */
	public boolean isSelected(View view)
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

	//{{{ getCode() method
	/**
	 * Returns the BeanShell code that will replay this action.
	 * @since jEdit 2.7pre2
	 */
	public abstract String getCode();
	//}}}

	//{{{ toString() method
	public String toString()
	{
		return name;
	} //}}}

	//{{{ Private members
	private String name;
	//}}}

	//{{{ Wrapper class
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
			jEdit.getActiveView().getInputHandler().invokeAction(action);
		}

		private EditAction action;
	} //}}}
}
