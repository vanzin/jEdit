/*
 * VFSFileFilter.java - VFSFileFilter that uses Unix-style globs.
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

import java.util.regex.Pattern;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.StandardUtilities;

/**
 * Implementation of {@link VFSFileFilter} that uses Unix-style globs
 * to filter files. This doesn't filter directories - just files.
 *
 * @author	Marcelo Vanzin
 * @version	$Id$
 * @since	jEdit 4.3pre7
 */
public class GlobVFSFileFilter implements VFSFileFilter
{

	public GlobVFSFileFilter(String glob)
	{
		this.glob = glob;
	}

	public boolean accept(VFSFile file)
	{
		if (file.getType() == VFSFile.DIRECTORY
				|| file.getType() == VFSFile.FILESYSTEM)
		{
			return true;
		}
		else
		{
			return accept(file.getName());
		}
	}

	public boolean accept(String url)
	{
		if (pattern == null)
		{
			pattern = Pattern.compile(StandardUtilities.globToRE(glob),
						  Pattern.CASE_INSENSITIVE);
		}
		return pattern.matcher(url).matches();
	}

	public String getDescription()
	{
		return jEdit.getProperty("vfs.browser.file_filter.glob");
	}

	public String toString()
	{
		return glob;
	}

	public void setGlob(String glob)
	{
		this.glob = glob;
		pattern = null;
	}

	public String getGlob()
	{
		return glob;
	}

	private String glob;
	private Pattern pattern;

}

