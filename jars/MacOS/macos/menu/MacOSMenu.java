/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSMenu.java - Menu providing features for Mac OS
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

package macos.menu;

//{{{ Imports
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import macos.*;
//}}}

public class MacOSMenu extends JMenu implements ActionListener, MenuListener
{
	private MacMenuItem showCurrent;
	private MacMenuItem showCurrentDir;
	
	//{{{ Constructor
	public MacOSMenu()
	{
		super(jEdit.getProperty("MacOSPlugin.menu.label"));
		
		showCurrent = new MacMenuItem(
			jEdit.getProperty("MacOSPlugin.menu.showCurrent"));
		showCurrent.addActionListener(this);
		showCurrentDir = new MacMenuItem(
			jEdit.getProperty("MacOSPlugin.menu.showCurrentDir"));
		showCurrentDir.addActionListener(this);
		add(showCurrent);
		add(showCurrentDir);
		addSeparator();
		add(new ShowBufferMenu());
		add(new ShowRecentMenu());
		add(new ShowRecentDirMenu());
		
		addMenuListener(this);
	} //}}}
	
	//{{{ construct() method
	private void construct()
	{
		File buff = new File(jEdit.getActiveView().getBuffer().getPath());
		showCurrent.setPath(buff.getPath());
		showCurrent.setEnabled(buff.exists());
		showCurrentDir.setPath(buff.getParent());
	} //}}}
	
	//{{{ menuSelected() method
	public void menuSelected(MenuEvent e)
	{
		construct();
	} //}}}
	
	//{{{ menuDeselected() method
	public void menuDeselected(MenuEvent e)
	{
	} //}}}
	
	//{{{ menuCanceled() method
	public void menuCanceled(MenuEvent e)
	{
	} //}}}
	
	//{{{ actionPerformed() method
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		if (src == showCurrent)
			MacOSActions.showInFinder(showCurrent.getPath());
		else if (src == showCurrentDir)
			MacOSActions.showInFinder(showCurrentDir.getPath());
	} //}}}
	
	//{{{ MacMenuItem class
	class MacMenuItem extends JMenuItem
	{
		String path;
		
		public MacMenuItem(String name)
		{
			super(name);
		}
		
		public String getPath()
		{
			return path;
		}
		
		public void setPath(String path)
		{
			this.path = path;
		}
	} //}}}
}
