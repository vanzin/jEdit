/*
 * MouseHandler.java
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
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

package org.gjt.sp.jedit.textarea;

//{{{ Imports
import java.awt.event.*;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.msg.PositionChanging;

import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.event.MouseEvent.BUTTON2;
import static java.awt.event.MouseEvent.BUTTON3;
//}}}

/**
 * The mouseHandler used for jEdit.
 */
public class MouseHandler extends TextAreaMouseHandler
{
	//{{{ MouseHandler constructor
	public MouseHandler(JEditTextArea textArea)
	{
		super(textArea);
	} //}}}

	//{{{ mousePressed() method
	@Override
	public void mousePressed(MouseEvent evt)
	{
		int btn = evt.getButton();
		if (btn != BUTTON1 && btn != BUTTON2 && btn != BUTTON3)
		{
			// Suppress presses with unknown button, to avoid
			// problems due to horizontal scrolling.
			return;
		}

		if(textArea.getBuffer().isLoading())
			return;

		EditBus.send(new PositionChanging(textArea));
		super.mousePressed(evt);
	} //}}}
}
