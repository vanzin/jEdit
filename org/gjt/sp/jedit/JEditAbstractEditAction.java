/*
 * AbstractEditAction.java - Base class for EditAction
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
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

/**
 * An action that can be bound to a menu item, tool bar button or keystroke.
 *
 * @see jEdit#getAction(String)
 * @see jEdit#getActionNames()
 * @see ActionSet
 *
 * @author S. Pestov, M. Casanova, K. Satoda
 * @version $Id: EditAction.java 11177 2007-12-01 09:50:50Z k_satoda $
 * @since 4.3pre13
 */
public abstract class JEditAbstractEditAction<E>
{
	//{{{ Private members
	protected String name;

	protected Object[] args;

	//}}}

	//{{{ EditAction constructors
	/**
	 * Creates a new edit action with the specified name.
	 * @param name The action name
	 */
	protected JEditAbstractEditAction(String name)
	{
		this.name = name;
	}

	protected JEditAbstractEditAction(String name, Object[] newArgs)
	{
		this.name = name;
		this.args = newArgs;
	} //}}}

	//{{{ getName() method
	/**
	 * Returns the internal name of this action.
	 * @return the action name
	 */
	public String getName()
	{
		return name;
	} //}}}

	// {{{ setName() method
	/**
	 * Changes the name of an action
	 * @param newName the new name of the action
	 * @since jEdit 4.3pre4
	 */
	public void setName(String newName)
	{
		name = newName;
	}// }}}

	//{{{ invoke() method
	/**
	 * Invokes the action. This is an implementation of the Command pattern,
	 * and concrete actions should override this.
	 *
	 * @param arg the argument
	 */
	public abstract void invoke(E arg);

	/**
	 * @param arg the arguments of the action
	 * @param newArgs new argument list
	 */
	public final void invoke(E arg, Object[] newArgs)
	{
		args = newArgs;
		invoke(arg);
	} //}}}

	//{{{ toString() method
	@Override
	public String toString()
	{
		return name;
	} //}}}
}
