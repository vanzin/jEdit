/*
 * BrowserListener.java - VFS browser listener
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

package org.gjt.sp.jedit.browser;

import java.util.EventListener;

import org.gjt.sp.jedit.io.VFS;

/**
 * A browser event listener.
 * @author Slava Pestov
 * @version $Id$
 */
public interface BrowserListener extends EventListener
{
	/**
	 * The user has selected a set of files.
	 * @param browser The VFS browser
	 * @param files The selected files
	 */
	void filesSelected(VFSBrowser browser, VFS.DirectoryEntry[] files);

	/**
	 * The user has double-clicked a set of files.
	 * @param browser The VFS browser
	 * @param files The selected files
	 */
	void filesActivated(VFSBrowser browser, VFS.DirectoryEntry[] files);
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:19  spestov
 * Initial revision
 *
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
