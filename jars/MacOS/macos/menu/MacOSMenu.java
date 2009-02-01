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
import org.gjt.sp.jedit.menu.*;
import org.gjt.sp.util.Log;
import macos.*;
//}}}

public class MacOSMenu implements DynamicMenuProvider
{	
	//{{{ Constructor
	public MacOSMenu()
	{
		//super();
	} //}}}
	
	//{{{ updateEveryTime() method
	public boolean updateEveryTime()
	{
		return true;
	} //}}}
	
	//{{{ update() method
	public void update(JMenu menu)
	{
		File buff = new File(jEdit.getActiveView().getBuffer().getPath());
		
		JMenuItem showCurrent = new JMenuItem(
			jEdit.getProperty("MacOSPlugin.menu.showCurrent"));
		showCurrent.addActionListener(new ShowFileAction(buff.getPath()));
		showCurrent.setEnabled(buff.exists());
		JMenuItem showCurrentDir = new JMenuItem(
			jEdit.getProperty("MacOSPlugin.menu.showCurrentDir"));
		showCurrentDir.addActionListener(new ShowDirAction(buff.getParent()));
		showCurrent.setEnabled(buff.getParentFile().exists());
		menu.add(showCurrent);
		menu.add(showCurrentDir);
		menu.addSeparator();
		menu.add(new ShowBufferMenu());
		menu.add(new ShowRecentMenu());
		menu.add(new ShowRecentDirMenu());
	} //}}}
	
	//{{{ ShowFileAction class
	class ShowFileAction implements ActionListener
	{
		private String path;
		
		public ShowFileAction(String path)
		{
			this.path = path;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MacOSActions.showInFinder(path);
		}
	} //}}}
	
	//{{{ ShowDirAction class
	class ShowDirAction implements ActionListener
	{
		private String path;
		
		public ShowDirAction(String path)
		{
			this.path = path;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			MacOSActions.showInFinder(path);
		}
	} //}}}
}
