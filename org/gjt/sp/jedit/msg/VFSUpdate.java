/*
 * VFSUpdate.java - A path has changed
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

import org.gjt.sp.jedit.*;

/**
 * Message sent when a file or directory changes.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.6pre4
 */
public class VFSUpdate extends EBMessage
{
	/**
	 * Creates a VFS update message.
	 * @param path The path in question
	 */
	public VFSUpdate(String path)
	{
		super(null);

		if(path == null)
			throw new NullPointerException("Path must be non-null");

		this.path = path;
	}

	/**
	 * Returns the path that changed.
	 */
	public String getPath()
	{
		return path;
	}

	public String paramString()
	{
		return "path=" + path + "," + super.paramString();
	}

	// private members
	private String path;
}
