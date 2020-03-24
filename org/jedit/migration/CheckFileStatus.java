/* CheckFileStatus.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
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

package org.jedit.migration;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.options.GeneralOptionPane;


//{{{ CheckFileStatus class
/**
 * Migration step for the 'checkFileStatus' property whose meaning changed slightly
 * in jEdit. Default value was before 0 and is now 1.
 */
public class CheckFileStatus extends OneTimeMigrationService
{
	public CheckFileStatus()
	{
		super("checkFileStatus");
	}


	@Override
	public void migrate()
	{
		if ((jEdit.getIntegerProperty("checkFileStatus", GeneralOptionPane.checkFileStatus_focus) == 0))
			jEdit.setIntegerProperty("checkFileStatus", GeneralOptionPane.checkFileStatus_focus);
	}
}
