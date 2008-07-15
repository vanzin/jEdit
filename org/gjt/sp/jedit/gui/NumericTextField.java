/*
 * NumericTextField.java - A TextField that accepts only numeric values
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2008 Matthieu Casanova
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
package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author Matthieu Casanova
 * @version $Id: KeyEventWorkaround.java 12889 2008-06-23 20:14:00Z kpouer $
 * @since jEdit 4.3pre15
 */
public class NumericTextField extends JTextField
{
	private final boolean positiveOnly;

	public NumericTextField(String text)
	{
		this(text, false);
	}

	public NumericTextField(String text, boolean positiveOnly)
	{
		super(text);
		this.positiveOnly = positiveOnly;
	}

	@Override
	protected void processKeyEvent(KeyEvent e)
	{
		if (e.getID() == KeyEvent.KEY_TYPED)
		{
			if (!Character.isDigit(e.getKeyChar()) && !(!positiveOnly && e.getKeyChar() == '-'))
			{
				e.consume();
			}
		}
		super.processKeyEvent(e);
	}
}
