/*
 * HoverSetStatusMouseHandler.java - set status with help text on hover
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Copyright (C) 2016 Eric Le Lay (extracted from EnhancedMenuItem)
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

package org.gjt.sp.jedit.gui.statusbar;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.StatusBar;

/**
 * Sets the status text to this action.mouse-over on hover.
 **/
public class HoverSetStatusMouseHandler extends MouseAdapter
{
	private boolean msgSet = false;
	private String msg;
	private String msgKey;
	
	public HoverSetStatusMouseHandler(String action){
		msgKey = action + ".mouse-over";
	}

	public void mouseReleased(MouseEvent evt)
	{
		cleanupStatusBar(evt);
	}

	public void mouseEntered(MouseEvent evt)
	{
		msg = jEdit.getProperty(msgKey);
		if(msg != null)
		{
			GUIUtilities.getView((Component)evt.getSource())
				.getStatus().setMessage(msg);
			msgSet = true;
		}
	}

	public void mouseExited(MouseEvent evt)
	{
		cleanupStatusBar(evt);
	}

	private void cleanupStatusBar(MouseEvent evt)
	{
		if(msgSet)
		{
			StatusBar statusBar = GUIUtilities.getView((Component) evt.getSource())
				.getStatus();
			if (msg == statusBar.getMessage())
			{
				statusBar.setMessage(null);
			}
			msgSet = false;
			msg = null;
		}
	}
}