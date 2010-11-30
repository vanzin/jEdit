/*
 * WorkRequest.java - Runnable subclass
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

package org.gjt.sp.util;

/**
 * A subclass of the Runnable interface.
 * @since jEdit 2.6pre1
 * @deprecated
 * @see org.gjt.sp.util.Task 
 * @version $Id$
 */
@Deprecated
public abstract class WorkRequest implements Runnable, ProgressObserver
{
	/**
	 * If the max value is greater that <code>Integer.MAX_VALUE</code> this 
	 * will be true and all values will be divided by 1024.
	 * @since jEdit 4.3pre3
	 */
	private boolean largeValues;

	/**
	 * Sets if the request can be aborted.
	 */
	public void setAbortable(boolean abortable)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).setAbortable(abortable);
	}

	/**
	 * Sets the status text.
	 * @param status The status text
	 */
	public void setStatus(String status)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).setStatus(status);
	}

	/**
	 * Sets the progress value.
	 * @param value The progress value.
	 * @deprecated use {@link #setValue(long)}
	 */
	@Deprecated
	public void setProgressValue(int value)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).setProgressValue(value);
	}

	/**
	 * Sets the maximum progress value.
	 * @param value The progress value.
	 * @deprecated use {@link #setMaximum(long)}
	 */
	@Deprecated
	public void setProgressMaximum(int value)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
			((WorkThread)thread).setProgressMaximum(value);
	}

	//{{{ setValue() method
	/**
	 * Update the progress value.
	 *
	 * @param value the new value
	 * @since jEdit 4.3pre3
	 */
	public void setValue(long value)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
		{
			if (largeValues)
			{
				((WorkThread)thread).setProgressValue((int) (value >> 10));
			}
			else
			{
				((WorkThread)thread).setProgressValue((int) value);
			}
		}
	} //}}}

	//{{{ setValue() method
	/**
	 * Update the maximum value.
	 *
	 * @param value the new maximum value
	 * @since jEdit 4.3pre3
	 */
	public void setMaximum(long value)
	{
		Thread thread = Thread.currentThread();
		if(thread instanceof WorkThread)
		{
			if (value > Integer.MAX_VALUE)
			{
				largeValues = true;
				((WorkThread)thread).setProgressMaximum((int) (value >> 10));
			}
			else
			{
				largeValues = false;
				((WorkThread)thread).setProgressMaximum((int) value);
			}
		}
	} //}}}
}
