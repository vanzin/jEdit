/*
 * EditPaneUpdate.java - Edit pane update message
 * Copyright (C) 1999, 2000 Slava Pestov
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

import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

/**
 * Message sent when an edit pane-related change occurs.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.5pre1
 */
public class EditPaneUpdate extends EBMessage
{
	/**
	 * Edit pane created.
	 */
	public static final Object CREATED = "CREATED";

	/**
	 * Edit pane destroyed.
	 */
	public static final Object DESTROYED = "DESTROYED";

	/**
	 * Edit pane buffer changed.
	 */
	public static final Object BUFFER_CHANGED = "BUFFER_CHANGED";

	/**
	 * Creates a new edit pane update message.
	 * @param editPane The edit pane
	 * @param what What happened
	 */
	public EditPaneUpdate(EditPane editPane, Object what)
	{
		super(editPane);

		if(what == null)
			throw new NullPointerException("What must be non-null");

		this.what = what;
	}

	/**
	 * Returns what caused this edit pane update.
	 */
	public Object getWhat()
	{
		return what;
	}

	/**
	 * Returns the edit pane involved.
	 */
	public EditPane getEditPane()
	{
		return (EditPane)getSource();
	}

	public String paramString()
	{
		return "what=" + what + "," + super.paramString();
	}

	// private members
	private Object what;
}
