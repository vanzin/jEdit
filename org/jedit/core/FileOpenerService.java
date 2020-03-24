/* FileOpenerService.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=sidekick:collapseFolds=1:
 *
 * Copyright © 2012 Alan Ezust
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

package org.jedit.core;

import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

/**  File Opener Service.
  *
  *  FastOpen 2.5 and SmartOpen 1.1 offer this as a service to
  *  other plugins such as ErrorList 2.0 that can use it to open
  *  files when, for example, the error message only provides
  *  a filename and not an absolute path.
  *
  *  A response to SF.net ticket #3481157
  *
  *  @since jEdit 5.0pre1
  *  @author Alan Ezust
  */
public abstract class FileOpenerService
{
	/** Opens a file in jEdit, given only a filename and no path.
	  *   May cause a dialog to popup asking the user for a choice.
	  *   @param fileName the file name to search for
	  *   @param view the parent View
        */
	public abstract void openFile(String fileName, View view);

	/** Searches available FileOpenerServices and uses the first, or the
	*   preferred one based on the "fileopener.service" property.
	*
	*   You can set a preferred FileOpener from the Console beanshell like this:
	*   <pre>
	*   jEdit.setProperty("fileopener.service", "FastOpen");  // or "SmartOpen"
	*   </pre>
	*   This setting is ignored if there is only one FileOpenerService available.
	*
	*   @param fileName the file name to search for
	*   @param view the parent View
        */
	public static void open(String fileName, View view)
	{
		String[] finders = ServiceManager.getServiceNames(FileOpenerService.class);

		// No installed finders, do nothing
		if (finders.length == 0) return;

		String myFinder = finders[0];
		// See if user set a preferred service
		if (finders.length > 1)
			myFinder = jEdit.getProperty("fileopener.service", myFinder);

		// try to get the service
		FileOpenerService obj = ServiceManager.getService(FileOpenerService.class, myFinder);

		// Preferred service is not found, use the only one available instead
		if ((obj == null) && (!myFinder.equals(finders[0])))
			obj = ServiceManager.getService(FileOpenerService.class, finders[0]);
		// Open the file!
		obj.openFile(fileName, view);
	}
}
