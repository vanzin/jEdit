/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2021 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.jedit.util;

import java.lang.ref.Cleaner;

/**
 * The CleanerService aim to replace the use of finalize() method
 *  which is deprecated since java 9
 *  Of course you could instantiate your own, but it will create a thread,
 *  this class aim to prevent creating threads everytime
 *
 * @author Matthieu Casanova
 */
public class CleanerService
{
	public static final CleanerService instance = new CleanerService();

	private final Cleaner cleaner;

	public CleanerService()
	{
		cleaner = Cleaner.create();
	}

	/**
	 * Register a runnable that will be executed when the object is being garbage collected
	 *
	 * @param object the object
	 * @param runnable the task to execute
	 */
	public void register(Object object, Runnable runnable)
	{
		cleaner.register(object, runnable);
	}
}
