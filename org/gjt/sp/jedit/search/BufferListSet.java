/*
 * BufferListSet.java - Buffer list matcher
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit.search;

import javax.swing.SwingUtilities;
import java.util.Vector;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * A file set for searching a user-specified list of buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferListSet implements SearchFileSet
{
	/**
	 * Creates a new buffer list search set. This constructor is
	 * only used by the multifile settings dialog box.
	 * @param files The path names to search
	 */
	public BufferListSet(Object[] files)
	{
		this.files = new Vector(files.length);
		for(int i = 0; i < files.length; i++)
		{
			this.files.addElement(((Buffer)files[i]).getPath());
		}
	}

	/**
	 * Creates a new buffer list search set.
	 * @param files The path names to search
	 */
	public BufferListSet(Vector files)
	{
		this.files = files;
	}

	/**
	 * Returns the first buffer to search.
	 * @param view The view performing the search
	 */
	public Buffer getFirstBuffer(View view)
	{
		return getBuffer((String)files.elementAt(0));
	}

	/**
	 * Returns the next buffer to search.
	 * @param view The view performing the search
	 * @param buffer The last buffer searched
	 */
	public Buffer getNextBuffer(View view, Buffer buffer)
	{
		if(buffer == null)
		{
			buffer = view.getBuffer();

			for(int i = 0; i < files.size(); i++)
			{
				if(files.elementAt(i).equals(buffer.getPath()))
					return buffer;
			}

			return getFirstBuffer(view);
		}
		else
		{
			// -1 so that the last isn't checked
			for(int i = 0; i < files.size() - 1; i++)
			{
				if(files.elementAt(i).equals(buffer.getPath()))
					return getBuffer((String)files.elementAt(i+1));
			}

			return null;
		}
	}

	/**
	 * Called if the specified buffer was found to have a match.
	 * @param buffer The buffer
	 */
	public void matchFound(final Buffer buffer)
	{
		// HyperSearch runs stuff in another thread
		if(!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						jEdit.commitTemporary(buffer);
					}
				});
			}
			catch(Exception e)
			{
			}
		}
		else
			jEdit.commitTemporary(buffer);
	}

	/**
	 * Returns the number of buffers in this file set.
	 */
	public int getBufferCount()
	{
		return files.size();
	}

	/**
	 * Returns if this fileset is valid (ie, has one or more buffers
	 * in it.
	 */
	public boolean isValid()
	{
		return files.size() != 0;
	}

	/**
	 * Returns the BeanShell code to recreate this fileset.
	 */
	public String getCode()
	{
		// not supported for arbitriary filesets
		return null;
	}

	// private members
	private Vector files;

	private Buffer getBuffer(final String path)
	{
		// HyperSearch runs stuff in another thread
		if(!SwingUtilities.isEventDispatchThread())
		{
			final Buffer[] retVal = new Buffer[1];

			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						retVal[0] = jEdit.openTemporary(null,null,
							path,false);
					}
				});
				return retVal[0];
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
				return null;
			}
		}
		else
			return jEdit.openTemporary(null,null,path,false);
	}
}
