/*
 * EnhancedButton.java - Tool bar button
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.statusbar.HoverSetStatusMouseHandler;
//}}}
/** A toolbar button */
public class EnhancedButton extends RolloverButton
{
	//{{{ EnhancedButton constructor
	public EnhancedButton(Icon icon, String toolTip, String action,
		ActionContext context)
	{
		super(icon);

		if(action != null)
		{
			// set the name of this button :
			// for instance, if the action is 'vfs.browser.previous'
			// the name will be 'previous'
			// this helps greatly in testing the UI with Fest-Swing
			int iSuffix = action.lastIndexOf('.');
			if(iSuffix<0 || iSuffix == action.length()-1)
			{
				setName(action);
			}
			else
			{
				setName(action.substring(iSuffix+1));
			}

			setEnabled(true);
			addActionListener(new EditAction.Wrapper(context,action));
			addMouseListener(new HoverSetStatusMouseHandler(action));
		}
		else
			setEnabled(false);

		setToolTipText(toolTip);
	} //}}}

	//{{{ isFocusable() method
	public boolean isFocusable()
	{
		return false;
	} //}}}

	//{{{ Private members
	//}}}

}
