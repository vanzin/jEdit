/*
 * CreateDockableWindow.java - Message requesting a dockable window
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.gui.DockableWindow;
import org.gjt.sp.jedit.*;

/**
 * @deprecated Use the new dockable window API (with the <code>dockables.xml</code>
 * file) instead of listening for this message.
 */
public class CreateDockableWindow extends EBMessage
{
	/**
	 * Creates a dockable window request message.
	 * @param view The view
	 * @param name The dockable window name
	 * @param position The dockable window position
	 */
	public CreateDockableWindow(View view, String name, String position)
	{
		super(view);

		if(name == null)
			throw new NullPointerException("Name must be non-null");

		this.name = name;
		this.position = position;
	}

	/**
	 * Returns the view involved.
	 */
	public View getView()
	{
		return (View)getSource();
	}

	/**
	 * Returns the name of the dockable window to create.
	 */
	public String getDockableWindowName()
	{
		return name;
	}

	/**
	 * Sets the dockable window name.
	 */
	public void setDockableWindow(DockableWindow win)
	{
		this.win = win;
		veto();
	}

	/**
	 * Returns the dockable window, or null if nobody responded to the
	 * message.
	 */
	public DockableWindow getDockableWindow()
	{
		return win;
	}

	/**
	 * Returns the dockable window position.
	 */
	public String getPosition()
	{
		return position;
	}

	public String paramString()
	{
		return super.paramString() + ",name=" + name + ",position="
			+ position;
	}

	// private members
	private String name;
	private String position;
	private DockableWindow win;
}
