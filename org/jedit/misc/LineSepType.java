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

package org.jedit.misc;

/**
 * @author Matthieu Casanova
 */
public enum LineSepType
{
	LF("\n"),
	CRLF("\r\n"),
	CR("\r");

	private final String separator;

	LineSepType(String separator)
	{
		this.separator = separator;
	}

	public String getSeparator()
	{
		return separator;
	}

	public static LineSepType fromSeparator(String separator)
	{
		switch (separator)
		{
			case "\n":
				return LF;
			case "\r\n":
				return CRLF;
			case "\r":
				return CR;
		}
		return LF;
	}
}
