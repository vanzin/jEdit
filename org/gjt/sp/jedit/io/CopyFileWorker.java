/*
 * CopyFileWorker.java - a worker that will copy a file
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */

package org.gjt.sp.jedit.io;

//{{{ Imports
import java.awt.Component;
import java.io.IOException;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkRequest;
//}}}

/**
 * This worker will copy a file. Be careful it override files if the target
 * already exists
 * @author Matthieu Casanova
 * @since jEdit 4.3pre13
 */
public class CopyFileWorker extends WorkRequest
{
	private final Component comp;
	private final String source;
	
	private final String target;

	
	//{{{ CopyFileWorker constructor
	/**
	 * @param comp the component that will be used as parent in case of error
	 * @param source the source VFS
	 * @param target the target VFS
	 */
	public CopyFileWorker(Component comp, String source, String target) 
	{
		if (source == null || target == null)
			throw new NullPointerException("The source and target cannot be null");
		this.comp = comp;
		this.source = source;
		this.target = target;
	} //}}}

	//{{{ run() method
	public void run() 
	{
		try
		{
			VFS.copy(this, source, target, comp, false);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR,this, e, e);
		}
	} //}}}
}
