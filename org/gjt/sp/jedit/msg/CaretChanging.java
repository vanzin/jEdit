/*
 * CaretChanging.java - caret changing (specialized text area update message)
 * :tabSize=8:indentSize=8:noTabs=false:
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

import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
   A message currently emitted not by jEdit's TextArea, but by various plugins, 
   including CodeBrowser, Tags, CscopeFinder, and SideKick, 
   whenever the plugin takes an action to change the caret to another location.

   @deprecated use @ref BufferChanging instead. It serves the same purpose, alerting plugins
   that the navigation position for the EditPane is about to change. The name of this class is misleading
   since a TextArea does not emit a caretChanging message each time the caret changes.
   
   @author Alan Ezust
   @since jEdit 4.3pre3
*/

public class CaretChanging extends TextAreaUpdate
{
	JEditTextArea jta;
	int caret;
	public CaretChanging(JEditTextArea jta)
	{
		super(jta, TextAreaUpdate.CARET_CHANGING);
		this.jta = jta;
		caret = jta.getCaretPosition();
	}

}

