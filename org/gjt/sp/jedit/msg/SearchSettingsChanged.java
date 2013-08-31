/*
 * SearchSettingsChanged.java - Search and replace settings changed message
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
 * Message sent when search and replace settings change.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.3pre1
 */
public class SearchSettingsChanged extends EBMessage
{
	/**
	 * Creates a new search and replace settings changed message.
	 * @param source The message source
	 */
	public SearchSettingsChanged(EBComponent source)
	{
		super(source);
	}
}
