/*
 * VFSFileFilter.java - A file filter for the VFS Browser.
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Marcelo Vanzin
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

package org.gjt.sp.jedit.io;

/**
 * An interface similar to <code>java.io.FilenameFilter</code>, that
 * is used by {@link org.gjt.sp.jedit.browser.VFSBrowser} to define what
 * files to show in the directory view.
 *
 * @author	Marcelo Vanzin
 * @version	$Id$
 * @since	jEdit 4.3pre7
 */
public interface VFSFileFilter 
{
	public static final String SERVICE_NAME = VFSFileFilter.class.getName();

	/**
	 * Should return whether the entry represented by the given URL
	 * should be listed in the browser view. Can be a file or a directory. 
	 */
	public boolean accept(VFSFile file);

	/**
	 * Same thing as {@link #accept(VFSFile)} above, but operates on
	 * the raw URL instead of a VFSFile object.
	 */
	public boolean accept(String url);

	/**
	 * Returns the description of the filter, to be used in the
	 * VFSBrowser window.
	 */
	public String getDescription();

}

