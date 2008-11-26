/*
 * MenuItemTextComparator.java - Compares the text values of two JMenuItems
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Eric Berry
 * Portions copyright (C) 1999, 2005 Slava Pestov
 * Portions copyright (C) 2000 Richard S. Hall
 * Portions copyright (C) 2001 Dirk Moebius
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
package org.gjt.sp.jedit.menu;

//{{{ Imports
import java.util.Comparator;

import javax.swing.JMenuItem;

import org.gjt.sp.util.StandardUtilities;

//}}}
/**
 * MenuItemTextComparator implements java.util.Comparator, and compares the text
 * value of JMenuItems using the case-insensitive smart comparison of
 * StandardUtilities.compareStrings. If one of the JMenuItems is an
 * EnhancedMenuItem it is given a higher comparison value.
 */
public class MenuItemTextComparator implements Comparator<JMenuItem>
{

	// {{{ Compare Method.
	public int compare(JMenuItem obj1, JMenuItem obj2)
	{
		int compareValue = 0;
		boolean obj1E = obj1 instanceof EnhancedMenuItem;
		boolean obj2E = obj2 instanceof EnhancedMenuItem;
		if (obj1E && !obj2E)
		{
			compareValue = 1;
		}
		else if (obj2E && !obj1E)
		{
			compareValue = -1;
		}
		else
		{
			compareValue = StandardUtilities.compareStrings(obj1.getText(), obj2.getText(), true);
		}
		return compareValue;
	} // }}}

}
