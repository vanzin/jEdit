/*
 * JEditEmbeddedTextArea.java - A TextArea that can be embedded in applications
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
package org.gjt.sp.jedit.textarea;

//{{{ Imports
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.jedit.buffer.JEditBuffer;
//}}}

/** An embeddable TextArea for jEdit plugins to use.
 *
 * @author Matthieu Casanova
 */
public class JEditEmbeddedTextArea extends TextArea
{
	//{{{ TextArea constructor
	/**
	 * Instantiate a TextArea.
	 */
	public JEditEmbeddedTextArea()
	{
		super(jEdit.getPropertyManager(), null);
		initInputHandler();
		EditPane.initPainter(getPainter());
		JEditBuffer buffer = new JEditBuffer();
		buffer.setMode(ModeProvider.instance.getMode("text"));
		setBuffer(buffer);
	} //}}}
}
