/*
 * UndoPane.java - Mode-specific options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2002 Slava Pestov
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

package org.gjt.sp.jedit.options;

//{{{ Imports
import javax.swing.*;

import java.awt.event.*;
import java.nio.file.*;

import org.gjt.sp.jedit.*;
//}}}

/**
 * Set options for undo here.
 * @author Dale Anson
 * @version $Id: UndoPane.java 24012 2015-08-12 08:48:07Z kpouer $
 */
public class UndoPane extends AbstractOptionPane
{
	//{{{ UndoPane constructor
	public UndoPane()
	{
		super("undo");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{

		undoCount = new JTextField(jEdit.getProperty("buffer.undoCount"));
		addComponent(jEdit.getProperty("options.editing.undoCount"),undoCount);

		// Reset Undo Manager On Save
		resetUndoOnSave = new JCheckBox(jEdit.getProperty("options.general.resetUndo"));
		resetUndoOnSave.setSelected(jEdit.getBooleanProperty("resetUndoOnSave"));
		addComponent(resetUndoOnSave);
	} //}}}
	
	//{{{ _save() method
	@Override
	protected void _save()
	{
		jEdit.setProperty("buffer.undoCount",undoCount.getText());
		jEdit.setBooleanProperty("resetUndoOnSave", resetUndoOnSave.isSelected());
	} //}}}

	//{{{ Instance variables
	private JTextField undoCount;
	private JCheckBox resetUndoOnSave;
	//}}}

}
