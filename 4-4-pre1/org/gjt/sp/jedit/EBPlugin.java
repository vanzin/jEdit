/*
 * EBPlugin.java - An EditBus plugin
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

package org.gjt.sp.jedit;

import org.gjt.sp.util.Log;

/**
 * Plugins extending this class are automatically added to the EditBus.
 * Otherwise, this class is identical to the {@link EditPlugin}
 * class.
 *
 * @see org.gjt.sp.jedit.EditBus
 * @see org.gjt.sp.jedit.EBComponent
 * @see org.gjt.sp.jedit.EBMessage
 *
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class EBPlugin extends EditPlugin implements EBComponent
{
	/**
	 * Handles a message sent on the EditBus.
	 */
	// next version: remove this
	public void handleMessage(EBMessage message)
	{
		EditBus.removeFromBus(this);
		if(seenWarning)
			return;
		seenWarning = true;
		Log.log(Log.WARNING,this,getClassName() + " should extend"
			+ " EditPlugin not EBPlugin since it has an empty"
			+ " handleMessage()");
	}

	// protected members
	protected EBPlugin() {}

	// private members
	private boolean seenWarning;
}
