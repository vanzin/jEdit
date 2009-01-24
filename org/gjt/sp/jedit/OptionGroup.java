/*
 * OptionGroup.java - Option pane group
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000 mike dillon
 * Portions copyright (C) 2003 Slava Pestov
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
 * A set of option panes shown in one branch in the options dialog.<p>
 *
 * Plugins should not create instances of this class directly. See
 * {@link EditPlugin} for information on how jEdit obtains and constructs
 * option pane instances.
 *
 * @author Mike Dillon
 * @version $Id$
 */
public class OptionGroup
{
	
	// {{{ data members
	protected final String name;
	protected final String label;
	protected final Vector<Object> members;
	private boolean sort;
	// }}}
	
	//{{{ OptionGroup constructor
	/**
	 * Creates an option group.
	 * @param name The internal name of the option group, used to key a
	 * property <code>options.<i>name</i>.label</code> which is the
	 * label displayed in the options dialog.
	 * @see jEdit#getProperty(String)
	 */
	public OptionGroup(String name)
	{
		this.name = name;
		label = jEdit.getProperty("options." + name + ".label");
		members = new Vector<Object>();
	} //}}}

	//{{{ OptionGroup constructor
	/**
	 * Creates an option group.
	 * @param label The label
	 * @param options A whitespace-separated list of option pane names
	 * @since jEdit 4.2pre2
	 */
	public OptionGroup(String name, String label, String options)
	{
		this.name = name;
		this.label = label;
		members = new Vector<Object>();

		StringTokenizer st = new StringTokenizer(options);
		while(st.hasMoreTokens())
		{
			String pane = st.nextToken();
			addOptionPane(pane);
		}
	} //}}}

	//{{{ getName() method
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getLabel() method
	/**
	 * Returns the option group's human-readable label.
	 * @since jEdit 4.2pre1
	 */
	public String getLabel()
	{
		return label;
	} //}}}

	//{{{ addOptionGroup() method
	public void addOptionGroup(OptionGroup group)
	{
		insertionSort(group.getLabel(),group);
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(OptionPane pane)
	{
		String label = jEdit.getProperty("options."
			+ pane.getName() + ".label","NO LABEL PROPERTY: "
			+ pane.getName());

		insertionSort(label,pane);
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(String pane)
	{
		String label = jEdit.getProperty("options."
			+ pane + ".label","NO LABEL PROPERTY: "
			+ pane);

		insertionSort(label,pane);
	} //}}}

	//{{{ getMembers() method
	public Enumeration<Object> getMembers()
	{
		return members.elements();
	} //}}}

	//{{{ getMember() method
	public Object getMember(int index)
	{
		return (index >= 0 && index < members.size())
			? members.elementAt(index) : null;
	} //}}}

	//{{{ getMemberIndex() method
	public int getMemberIndex(Object member)
	{
		return members.indexOf(member);
	} //}}}

	//{{{ getMemberCount() method
	public int getMemberCount()
	{
		return members.size();
	} //}}}

	//{{{ setSort() method
	/**
	 * Sets if the members of this group should be sorted.
	 * @since jEdit 4.2pre3
	 */
	public void setSort(boolean sort)
	{
		this.sort = sort;
	} //}}}

	//{{{ Private members


	//{{{ insertionSort() method
	private void insertionSort(String newLabel, Object newObj)
	{
		if(sort)
		{
			for(int i = 0; i < members.size(); i++)
			{
				Object obj = members.elementAt(i);
				String label;
				if(obj instanceof OptionPane)
				{
					String name = ((OptionPane)obj).getName();
					label = jEdit.getProperty("options."
						+ name + ".label","NO LABEL PROPERTY: "
						+ name);
				}
				else if(obj instanceof String)
				{
					label = jEdit.getProperty("options."
						+ obj + ".label","NO LABEL PROPERTY: "
						+ obj);
				}
				else if(obj instanceof OptionGroup)
					label = ((OptionGroup)obj).getLabel();
				else
					throw new InternalError();

				if(newLabel.compareToIgnoreCase(label) < 0)
				{
					members.insertElementAt(newObj,i);
					return;
				}
			}
		}

		members.addElement(newObj);
	} //}}}

	//}}}
}
