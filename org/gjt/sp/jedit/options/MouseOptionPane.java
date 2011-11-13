/*
 * MouseOptionPane.java - Editor window options
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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
import org.gjt.sp.jedit.*;
//}}}

public class MouseOptionPane extends AbstractOptionPane
{
	//{{{ MouseOptionPane constructor
	public MouseOptionPane()
	{
		super("mouse");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		/* Text drag and drop */
		dragAndDrop = new JCheckBox(jEdit.getProperty(
			"options.mouse.dragAndDrop"));
		dragAndDrop.setSelected(jEdit.getBooleanProperty(
			"view.dragAndDrop"));
		addComponent(dragAndDrop);

		/* Non word character selection behavior */
		joinNonWordChars = new JCheckBox(jEdit.getProperty(
			"options.mouse.joinNonWordChars"));
		joinNonWordChars.setSelected(jEdit.getBooleanProperty(
			"view.joinNonWordChars"));
		addComponent(joinNonWordChars);

		/* Middle mouse button click pastes % register */
		middleMousePaste = new JCheckBox(jEdit.getProperty("options.mouse"
			+ ".middleMousePaste"));
		middleMousePaste.setSelected(jEdit.getBooleanProperty(
			"view.middleMousePaste"));
		addComponent(middleMousePaste);

		/*
		 * Pressing Ctrl while mouse actions makes them as if
		 * selection mode were rectangular mode
		 */
		ctrlForRectangularSelection = new JCheckBox(jEdit.getProperty(
			"options.mouse.ctrlForRectangularSelection"));
		ctrlForRectangularSelection.setSelected(jEdit.getBooleanProperty(
			"view.ctrlForRectangularSelection"));
		addComponent(ctrlForRectangularSelection);

		/* Gutter mouse actions */
		int c = clickActionKeys.length;
		String[] clickActionNames = new String[c];
		for(int i = 0; i < c; i++)
		{
			clickActionNames[i] = jEdit.getProperty(
				"options.mouse.gutter."+clickActionKeys[i]);
		}

		c = clickModifierKeys.length;
		String[] clickModifierNames = new String[c];
		for(int i = 0; i < c; i++)
		{
			clickModifierNames[i] = jEdit.getProperty(
				"options.mouse.gutter."+clickModifierKeys[i]);
		}

		gutterClickActions = new JComboBox[c];

		for(int i = 0; i < c; i++)
		{
			JComboBox cb = new JComboBox(clickActionNames);

			gutterClickActions[i] = cb;

			String val = jEdit.getProperty("view.gutter."+clickModifierKeys[i]);
			for(int j = 0; j < clickActionKeys.length; j++)
			{
				if(val.equals(clickActionKeys[j]))
				{
					cb.setSelectedIndex(j);
				}
			}

			addComponent(clickModifierNames[i],cb);
		}
	} //}}}

	//{{{ _save() method
	public void _save()
	{
		jEdit.setBooleanProperty("view.dragAndDrop",
					 dragAndDrop.isSelected());
		jEdit.setBooleanProperty("view.joinNonWordChars",
					 joinNonWordChars.isSelected());
		jEdit.setBooleanProperty("view.middleMousePaste",
					 middleMousePaste.isSelected());
		jEdit.setBooleanProperty("view.ctrlForRectangularSelection",
					 ctrlForRectangularSelection.isSelected());

		int c = clickModifierKeys.length;
		for(int i = 0; i < c; i++)
		{
			int idx = gutterClickActions[i].getSelectedIndex();
			jEdit.setProperty("view.gutter."+clickModifierKeys[i],
				clickActionKeys[idx]);
		}
	} //}}}

	//{{{ Private members
	private JCheckBox dragAndDrop;
	private JCheckBox joinNonWordChars;
	private JCheckBox middleMousePaste;
	private JCheckBox ctrlForRectangularSelection;

	private JComboBox[] gutterClickActions;

	// simplified these settings a little...
	private static final String[] clickActionKeys = new String[] {
		"toggle-fold",
		"toggle-fold-fully"
	};

	private static final String[] clickModifierKeys = new String[] {
		"foldClick",
		"SfoldClick"
	}; //}}}
}
