/*
 * ToolBarManager.java - Handles tool bars for the View
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 mike dillon
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;
//}}}

public class ToolBarManager
{
	//{{{ ToolBarManager constructor
	public ToolBarManager(Container top, Container bottom)
	{
		this.top = top;
		this.bottom = bottom;
	} //}}}

	//{{{ addToolBar() method
	public void addToolBar(int group, int layer, Component toolbar)
	{
		Entry entry = new Entry(layer, toolbar);

		if (group == View.TOP_GROUP)
			addToolBar(top, topToolBars, entry);
		else if (group == View.BOTTOM_GROUP)
			addToolBar(bottom, bottomToolBars, entry);
		else
			throw new InternalError("Invalid tool bar group");
	} //}}}

	//{{{ removeToolBar() method
	public void removeToolBar(Component toolbar)
	{
		removeToolBar(top, topToolBars, toolbar);
		removeToolBar(bottom, bottomToolBars, toolbar);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Container top;
	private Container bottom;

	private ArrayList topToolBars = new ArrayList();
	private ArrayList bottomToolBars = new ArrayList();
	//}}}

	//{{{ addToolBar() method
	private void addToolBar(Container group, ArrayList toolbars,
		Entry entry)
	{
		// See if we should place this toolbar before any others
		for(int i = 0; i < toolbars.size(); i++)
		{
			if(entry.layer > ((Entry)toolbars.get(i)).layer)
			{
				toolbars.add(i,entry);
				group.add(entry.toolbar,i);
				return;
			}
		}

		// Place the toolbar at the bottom of the group
		toolbars.add(entry);
		group.add(entry.toolbar);
	} //}}}

	//{{{ removeToolBar() method
	private void removeToolBar(Container group, ArrayList toolbars,
		Component toolbar)
	{
		for(int i = 0; i < toolbars.size(); i++)
		{
			if(toolbar == ((Entry)toolbars.get(i)).toolbar)
			{
				group.remove(toolbar);
				toolbars.remove(i);

				return;
			}
		}
	} //}}}

	//}}}

	//{{{ Entry class
	static class Entry
	{
		int layer;
		Component toolbar;

		Entry(int layer, Component toolbar)
		{
			this.layer = layer;
			this.toolbar = toolbar;
		}
	} //}}}
}
