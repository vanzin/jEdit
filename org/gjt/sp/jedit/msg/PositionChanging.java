/*
 * PositionChanging.java - Cursor position changing (specialized Edit Pane update message)
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Alan Ezust
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

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;


/**
 * An EBMessage associated with an EditPane that is sent just before its caret 
 * position changes in a "major way" to another location in the same Buffer.
 * These messages are tracked by the Navigator plugin, 
 * and other interested plugins.
 * 
 * jEdit plugins such as SideKick, Tags, Jump, CscopeFinder, etc, should 
 * emit this message whenever the user wants to jump from one position
 * to another in the same buffer.
 * 
 * For jumps to a different buffer entirely, it is not necessary for plugins
 * to send any message, since BufferChanging is sent by jEdit whenever 
 * EditPane.setBuffer() is called, and it serves as a PositionChanging message
 * also.
 * 
 *
 * @see org.gjt.sp.jedit.msg.BufferChanging
 * @author ezust
 * @since jEdit 4.3pre15
 *
 */
public class PositionChanging extends EditPaneUpdate
{
	
	protected PositionChanging(EditPane editPane, Object whatt)
	{
		super(editPane, whatt);
	}

	public PositionChanging(TextArea textArea)
	{
		super(EditPane.get(textArea), EditPaneUpdate.POSITION_CHANGING);
	}
	
	public PositionChanging(EditPane editPane)
	{
		super (editPane, EditPaneUpdate.POSITION_CHANGING);
	}
}
