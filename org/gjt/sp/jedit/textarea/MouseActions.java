/*
 * MouseActions.java - Simplifies mouse handling
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

import java.awt.event.MouseEvent;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.jEdit;

public class MouseActions
{
	//{{{ MouseActions constructor
	MouseActions(String name)
	{
		this.name = name;
	} //}}}

	//{{{ getActionForEvent() method
	String getActionForEvent(MouseEvent evt, String variant)
	{
		String modStr = DefaultInputHandler.getModifierString(evt);
		if(modStr == null)
		{
			return jEdit.getProperty("view." + name + "."
				+ variant + "Click");
		}
		else
		{
			return jEdit.getProperty("view." + name + "."
				+ DefaultInputHandler.getModifierString(evt)
				+ variant + "Click");
		}
	} //}}}

	private String name;
}
