/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSOptionPane.java - options pane for Mac OS Plugin
 * Copyright (C) 2002 Kris Kopicki
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

package macos;

//{{{ Imports
import java.awt.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
//}}}

public class MacOSOptionPane extends AbstractOptionPane
{
	//{{{ Variables
	private JCheckBox menuBox;
	private JCheckBox preserveBox;
	private JCheckBox selectionBox;
	//}}}
	
	//{{{ Constructor
	public MacOSOptionPane()
	{
		super("MacOSPlugin");
	}//}}}

	//{{{ _init() method
	public void _init()
	{
		Dimension d = new Dimension(7,7);
		Dimension d_2 = new Dimension(20,20);
		
		menuBox = new JCheckBox(jEdit.getProperty("options.MacOSPlugin.menubar.label"));
		addComponent(menuBox);
		addComponent(new JLabel("(Requires restart for changes to take effect)"));
		
		addComponent(new Box.Filler(d,d,d));
		
		preserveBox = new JCheckBox(jEdit.getProperty("options.MacOSPlugin.preserve.label"));
		addComponent(preserveBox);
		
		addComponent(new Box.Filler(d,d,d));
		
		selectionBox = new JCheckBox(jEdit.getProperty("options.MacOSPlugin.useSelection.label"));
		addComponent(selectionBox);
		
		getSettings();
	}//}}}

	//{{{ _save() method
	public void _save()
	{
		jEdit.setBooleanProperty("MacOSPlugin.useScreenMenuBar", menuBox.isSelected());
		jEdit.setBooleanProperty("MacOSPlugin.preserveCodes", preserveBox.isSelected());
		jEdit.setBooleanProperty("MacOSPlugin.useSelection", selectionBox.isSelected());
	}//}}}

	//{{{ getSettings() method
	public void getSettings()
	{
		menuBox.setSelected(jEdit.getBooleanProperty("MacOSPlugin.useScreenMenuBar",
			jEdit.getBooleanProperty("MacOSPlugin.default.useScreenMenuBar")));
		preserveBox.setSelected(jEdit.getBooleanProperty("MacOSPlugin.preserveCodes",
			jEdit.getBooleanProperty("MacOSPlugin.default.preserveCodes")));
		selectionBox.setSelected(jEdit.getBooleanProperty("MacOSPlugin.useSelection",
			jEdit.getBooleanProperty("MacOSPlugin.default.useSelection")));
	}//}}}
}
