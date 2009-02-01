/*
 * PropertiesChanged.java - Properties changed message
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;

/**
 * Message sent when properties are changed using the global options or
 * plugin options dialog box.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class PropertiesChanged extends EBMessage
{
	/**
	 * Creates a new properties changed message.
	 * @param source The message source
	 */
	public PropertiesChanged(EBComponent source)
	{
		super(source);
	}
}
