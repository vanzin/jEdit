/*
 * EnhancedButton.java - Tool bar button
 * :tabSize=8:indentSize=8:noTabs=false:
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
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;
//}}}

public class EnhancedButton extends RolloverButton
{
	//{{{ EnhancedButton constructor
	public EnhancedButton(Icon icon, String toolTip, String action,
		ActionContext context)
	{
		super(icon);

		this.action = action;

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
			addMouseListener(new MouseHandler());
		}
		else
			setEnabled(false);

		setToolTipText(toolTip);
	} //}}}

	//{{{ isFocusTraversable() method
	public boolean isFocusTraversable()
	{
		return false;
	} //}}}

	//{{{ Private members
	private String action;
	//}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		boolean msgSet = false;

		public void mouseReleased(MouseEvent evt)
		{
			if(msgSet)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(null);
				msgSet = false;
			}
		}

		public void mouseEntered(MouseEvent evt)
		{
			String msg = jEdit.getProperty(action + ".mouse-over");
			if(msg != null)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(msg);
				msgSet = true;
			}
		}

		public void mouseExited(MouseEvent evt)
		{
			if(msgSet)
			{
				GUIUtilities.getView((Component)evt.getSource())
					.getStatus().setMessage(null);
				msgSet = false;
			}
		}
	} //}}}
}
