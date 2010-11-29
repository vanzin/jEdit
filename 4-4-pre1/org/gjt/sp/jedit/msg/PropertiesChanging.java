/*
 * PropertiesChanging.java - Properties changing message
 * Copyright (C) 2007 Marcelo Vanzin
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

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;

/**
 * Message sent right before the global options dialog is shown. This
 * allows plugins to flush any state before the options pane is loaded
 * and the properties are read by the panes.
 *
 * @author Marcelo Vanzin
 * @version $Id$
 *
 * @since jEdit 4.3pre9
 */
public class PropertiesChanging extends EBMessage
{

	public enum State {
		LOADING,
		CANCELED
	}

	/**
	 * Creates a new properties changing message.
	 * @param source 	The message source
	 * @param state		An enum describing what is happening.
	 */
	public PropertiesChanging(EBComponent source, State state)
	{
		super(source);
		assert (state != null) : "state shouldn't be null";
		this.state = state;
	}

	public State getState()
	{
		return state;
	}

	private final State state;

}
