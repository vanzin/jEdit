/*
 * EBComponent.java - An EditBus component
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

/**
 * A component on the EditBus. Every plugin class that uses the EditBus for
 * receiving messages must implement this interface.
 *
 * @see org.gjt.sp.jedit.EBMessage
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public interface EBComponent
{
	/**
	 * Handles a message sent on the EditBus.
	 *
	 * This method must specify the type of responses the plugin will have
	 * for various subclasses of the {@link EBMessage} class. Typically
	 * this is done with one or more <code>if</code> blocks that test
	 * whether the message is an instance of a derived message class in
	 * which the component has an interest. For example:
	 *
	 * <pre> if(msg instanceof BufferUpdate) {
	 *     // a buffer's state has changed!
	 * }
	 * else if(msg instanceof ViewUpdate) {
	 *     // a view's state has changed!
	 * }
	 * // ... and so on</pre>
	 *
	 * @param message The message
	 */
	void handleMessage(EBMessage message);
}
