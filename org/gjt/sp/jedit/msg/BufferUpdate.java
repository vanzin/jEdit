/*
 * BufferUpdate.java - Buffer update message
 * Copyright (C) 1999, 2001 Slava Pestov
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

import org.gjt.sp.jedit.*;

/**
 * Message sent when a buffer-related change occurs.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class BufferUpdate extends EBMessage.NonVetoable
{
	/**
	 * Buffer created.
	 */
	public static final Object CREATED = "CREATED";

	/**
	 * Buffer load started.
	 * @since jEdit 2.6pre1
	 */
	public static final Object LOAD_STARTED = "LOAD_STARTED";

	/**
	 * Buffer loaded.
	 */
	public static final Object LOADED = "LOADED";

	/**
	 * Buffer closed.
	 */
	public static final Object CLOSED = "CLOSED";

	/**
	 * Buffer dirty changed.
	 */
	public static final Object DIRTY_CHANGED = "DIRTY_CHANGED";

	/**
	 * Buffer markers changed.
	 */
	public static final Object MARKERS_CHANGED = "MARKERS_CHANGED";

	/**
	 * Buffer mode changed.
	 */
	public static final Object MODE_CHANGED = "MODE_CHANGED";

	/**
	 * Character ncoding changed.
	 * @since jEdit 3.2pre4
	 */
	public static final Object ENCODING_CHANGED = "ENCODING_CHANGED";

	/**
	 * Buffer saving.
	 */
	public static final Object SAVING = "SAVING";

	/**
	 * Creates a new buffer update message.
	 * @param buffer The buffer
	 * @param what What happened
	 */
	public BufferUpdate(Buffer buffer, View view, Object what)
	{
		super(buffer);

		this.view = view;

		if(what == null)
			throw new NullPointerException("What must be non-null");

		this.what = what;
	}

	/**
	 * Returns what caused this buffer update.
	 */
	public Object getWhat()
	{
		return what;
	}

	/**
	 * Returns the buffer involved.
	 */
	public Buffer getBuffer()
	{
		return (Buffer)getSource();
	}

	/**
	 * Returns the view involved, which may be null.
	 */
	public View getView()
	{
		return view;
	}

	public String paramString()
	{
		return super.paramString() + ",what=" + what
			+ ",view=" + view;
	}

	// private members
	private Object what;
	private View view;
}
