/*
 * ThreadUtilities.java - Utilities for threading
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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

import java.awt.*;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.4pre1
 */
public class ThreadUtilities
{
	/**
	 * Run the runnable in EventDispatch Thread.
	 * If the current thread is EventDispatch, it will run
	 * immediately otherwise it will be queued in the DispatchThread
	 * The difference with VFSManager.runInAWTThread() method is that
	 * this one will not wait for IO Request before being executed
	 *
	 * @param runnable the runnable to run
	 * @since jEdit 4.4pre1
	 */
	public static void runInDispatchThread(Runnable runnable)
	{
		if(EventQueue.isDispatchThread())
			runnable.run();
		else
			EventQueue.invokeLater(runnable);
	}

	private ThreadUtilities()
	{
	}
}
