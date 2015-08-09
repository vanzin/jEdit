/*
 * EditingOptionPane.java - Mode-specific options panel
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

import org.gjt.sp.jedit.*;
//}}}

/**
 * The EditingOptionPane has been split up so the large files option pane is 
 * now a separate pane.
 * @author Slava Pestov
 * @version $Id: EditingOptionPane.java 23381 2013-12-09 12:43:14Z kpouer $
 */
public class LargeFilesOptionPane extends AbstractOptionPane
{
	//{{{ EditingOptionPane constructor
	public LargeFilesOptionPane()
	{
		super("large-files");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		//{{{ Large file mode
		addSeparator(jEdit.getProperty("options.editing.largefilemode.title"));

		addComponent(new JLabel(jEdit.getProperty("options.editing.largefilemode")));
		addComponent(askLargeFileMode = new JRadioButton(jEdit.getProperty("options.editing.largefilemode.option.ask")));
		addComponent(fullSyntaxLargeFileMode = new JRadioButton(jEdit.getProperty("options.editing.largefilemode.option.full")));
		addComponent(limitedSyntaxLargeFileMode = new JRadioButton(jEdit.getProperty("options.editing.largefilemode.option.limited")));
		addComponent(noHighlightLargeFileMode = new JRadioButton(jEdit.getProperty("options.editing.largefilemode.option.nohighlight")));
		String option = jEdit.getProperty("largefilemode", "ask");
		if ("full".equals(option))
		{
			fullSyntaxLargeFileMode.setSelected(true);
		}
		else if ("limited".equals(option))
		{
			limitedSyntaxLargeFileMode.setSelected(true);
		}
		else if ("nohighlight".equals(option))
		{
			noHighlightLargeFileMode.setSelected(true);
		}
		else
		{
			askLargeFileMode.setSelected(true);
		}
		ButtonGroup largeFileModeButtonGroup = new ButtonGroup();
		largeFileModeButtonGroup.add(askLargeFileMode);
		largeFileModeButtonGroup.add(fullSyntaxLargeFileMode);
		largeFileModeButtonGroup.add(limitedSyntaxLargeFileMode);
		largeFileModeButtonGroup.add(noHighlightLargeFileMode);
		//}}}
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		if (fullSyntaxLargeFileMode.isSelected())
		{
			jEdit.setProperty("largefilemode", "full");
		}
		else if (limitedSyntaxLargeFileMode.isSelected())
		{
			jEdit.setProperty("largefilemode", "limited");
		}
		else if (noHighlightLargeFileMode.isSelected())
		{
			jEdit.setProperty("largefilemode", "nohighlight");
		}
		else
		{
			jEdit.setProperty("largefilemode", "ask");
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private JRadioButton askLargeFileMode;
	private JRadioButton noHighlightLargeFileMode;
	private JRadioButton limitedSyntaxLargeFileMode;
	private JRadioButton fullSyntaxLargeFileMode;
	//}}}

	//}}}

}
