/*
 * OptionGroup.java - Option pane group
 * Copyright (C) 2000 mike dillon
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

import java.util.Enumeration;
import java.util.Vector;

import org.gjt.sp.util.Log;

/**
 * A set of option panes shown in one branch in the options dialog.<p>
 *
 * In those cases where a single option pane is inadequate to present all
 * of a plugin's configuration options, this class can be used to create a
 * group of options panes. The group will appear as a single node in the
 * options dialog tree. The member option panes will appear as
 * leaf nodes under the group's node.
 *
 * @author Mike Dillon
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class OptionGroup
{
	/**
	 * Creates an option group.
	 * @param name The internal name of the option group, used to key a
	 * property <code>options.<i>name</i>.label</code> which is the
	 * label displayed in the options dialog.
	 */
	public OptionGroup(String name)
	{
		this.name = name;
		members = new Vector();
	}

	public String getName()
	{
		return name;
	}

	public void addOptionGroup(OptionGroup group)
	{
		if (members.indexOf(group) != -1) return;

		members.addElement(group);
	}

	public void addOptionPane(OptionPane pane)
	{
		if (members.indexOf(pane) != -1) return;

		members.addElement(pane);
	}

	public Enumeration getMembers()
	{
		return members.elements();
	}

	public Object getMember(int index)
	{
		return (index >= 0 && index < members.size())
			? members.elementAt(index) : null;
	}

	public int getMemberIndex(Object member)
	{
		return members.indexOf(member);
	}

	public int getMemberCount()
	{
		return members.size();
	}

	public void save()
	{
		Enumeration enum = members.elements();

		while (enum.hasMoreElements())
		{
			Object elem = enum.nextElement();
			try
			{
				if (elem instanceof OptionPane)
				{
					((OptionPane)elem).save();
				}
				else if (elem instanceof OptionGroup)
				{
					((OptionGroup)elem).save();
				}
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR, elem,
					"Error saving option pane");
				Log.log(Log.ERROR, elem, t);
			}
		}
	}

	private String name;
	private Vector members;
}
