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
{
	/**
	 * Creates a new edit action with the specified name.
	 * @param name The action name
	 */
	public EditAction(String name)
	{
		this.name = name;
	}

	/**
	 * Returns the internal name of this action.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the action's label. The default implementation returns the
	 * value of the property named by the action's internal name suffixed
	 * with <code>.label</code>.
	 */
	public String getLabel()
	{
		return jEdit.getProperty(name + ".label");
	}

	/**
	 * Invokes the action.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public abstract void invoke(View view);

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
	private String cachedCode;

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
			EditAction.getView((Component)evt.getSource())
				.getInputHandler().invokeAction(action);
		}

		// private members
		private EditAction action;
	}
}
