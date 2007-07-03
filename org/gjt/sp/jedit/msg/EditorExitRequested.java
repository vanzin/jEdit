/*
 * EditorExitRequested.java - Message sent before jEdit starts exiting
 * Copyright (C) 2000 Dirk Moebius
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.View;

/**
 * Message sent when jEdit starts the exit process. It is send before
 * the settings are saved and the buffers are closed. Listeners of this
 * message should be aware that jEdit might not exit truely, maybe because
 * of errors, or the user cancelled the "Save unsaved changed" dialog, or
 * jEdit is in background mode.
 *
 * @author Dirk Moebius
 * @version $Id$
 *
 * @since jEdit 3.1pre4
 */
public class EditorExitRequested extends EBMessage
{
	private boolean hasBeenExitCancelled;
	
	/**
	 * Creates a new editor exiting started message.
	 * @param view The view from which this exit was called
	 */
	public EditorExitRequested(View view)
	{
		super(view);
	}

	/**
	 * Returns the view involved.
	 */
	public View getView()
	{
		return (View)getSource();
	}
	
	/**
	 * Cancels the exit process. If a plugin calls this method, jEdit will not
	 * exit anymore
	 */
	public void cancelExit()
	{
		hasBeenExitCancelled = true;
	}
	
	/**
	 * Check if the exit process has been cancelled.
	 */ 
	 public boolean hasBeenExitCancelled()
	 {
		 return hasBeenExitCancelled;
	 }
}
