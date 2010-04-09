/*
 * JEditRichText.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Matthieu Casanova
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
package org.gjt.sp.jedit.datatransfer;

import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.syntax.ModeProvider;

/**
 * @author Matthieu Casanova
 * @since jEdit 4.4.x
 */
public class JEditRichText
{
	private final String text;

	private final String mode;

	public JEditRichText(String text, String mode)
	{
		this.text = text;
		this.mode = mode;
	}

	public String getText()
	{
		return text;
	}

	public Mode getMode()
	{
		return ModeProvider.instance.getMode(mode);
	}

	@Override
	public String toString()
	{
		return text;
	}
}
