/* 
 * :tabSize=4:indentSize=4:noTabs=false:
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

import java.awt.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;


public class MacOSOptionPane extends AbstractOptionPane
{

//{{{ Variables
	private JCheckBox menuBox;
	private JCheckBox preserveBox;
//}}}
	
	//{{{ Constructor
    public MacOSOptionPane()
	{
        super("MacOSPlugin");
    }//}}}

	//{{{ _init() method
    public void _init()
	{
        menuBox = new JCheckBox(jEdit.getProperty("options.MacOSPlugin.menubar.label"));
        addComponent(menuBox);
		addComponent(new JLabel("(Requires restart for changes to take effect)"));
		
		Dimension d = new Dimension(10,10);
		addComponent(new Box.Filler(d,d,d));
		
		preserveBox = new JCheckBox(jEdit.getProperty("options.MacOSPlugin.preserve.label"));
		addComponent(preserveBox);
		
        getSettings();
    }//}}}

	//{{{ _save() method
    public void _save()
	{
        jEdit.setProperty("MacOSPlugin.useScreenMenuBar", menuBox.isSelected() ? "true" : "false");
		jEdit.setProperty("MacOSPlugin.preserveCodes", preserveBox.isSelected() ? "true" : "false");
    }//}}}

	//{{{ getSettings() method
    public void getSettings()
	{
        menuBox.setSelected((jEdit.getProperty("MacOSPlugin.useScreenMenuBar").equals("true")) ? true : false);
		preserveBox.setSelected((jEdit.getProperty("MacOSPlugin.preserveCodes").equals("true")) ? true : false);
    }//}}}

}
