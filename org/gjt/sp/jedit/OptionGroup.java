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
import org.gjt.sp.util.Log;

/**
 * A set of option panes shown in one branch in the options dialog.<p>
 *
 * Plugins should not create instances of this class anymore. See
 * {@link EditPlugin} for information on how jEdit obtains and constructs
 * option pane instances.
 *
 * @author Mike Dillon
 * @version $Id$
 */
public class OptionGroup
{
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
		members = new Vector();
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
		members = new Vector();

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
		members.addElement(group);
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(OptionPane pane)
	{
		members.addElement(pane);
	} //}}}

	//{{{ addOptionPane() method
	public void addOptionPane(String pane)
	{
		members.addElement(pane);
	} //}}}

	//{{{ getMembers() method
	public Enumeration getMembers()
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

	//{{{ Private members
	private String name;
	private String label;
	private Vector members;
	//}}}
}
