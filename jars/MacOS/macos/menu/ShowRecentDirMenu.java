/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * ShowRecentDirMenu.java
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
import java.awt.event.*;
import java.io.File;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
import macos.*;
//}}}

public class ShowRecentDirMenu extends JMenu implements MenuListener
{
	//{{{ Constructor
	public ShowRecentDirMenu()
	{
		super(jEdit.getProperty("MacOSPlugin.menu.recentDir.label"));
		addMenuListener(this);
	} //}}}
	
	//{{{ construct() method
	private void construct()
	{
		HistoryModel model = HistoryModel.getModel("vfs.browser.path");
		JMenuItem item;
		File file;
		int max = model.getSize();
		
		if (max == 0)
		{
			item = new JMenuItem(jEdit.getProperty("MacOSPlugin.menu.recentDir.none"));
			item.setEnabled(false);
			add(item);
			return;
		}
		
		for (int i=0; i < max ; i++)
		{
			file = new File(model.getItem(i));
			item = new ShowRecentDirMenuItem(file.getName(),file.getPath());
			item.setIcon(FileCellRenderer.dirIcon);
			add(item);
		}
	} //}}}
	
	//{{{ menuSelected() method
	public void menuSelected(MenuEvent e)
	{
		construct();
	} //}}}
	
	//{{{ menuDeselected() method
	public void menuDeselected(MenuEvent e)
	{
		removeAll();
	} //}}}
	
	//{{{ menuCanceled() method
	public void menuCanceled(MenuEvent e)
	{
	} //}}}
	
	//{{{ ShowRecentDirMenuItem class
	class ShowRecentDirMenuItem extends JMenuItem
	{
		String path;
		
		public ShowRecentDirMenuItem(String name, String path)
		{
			super(name);
			this.path = path;
			addActionListener(new ShowFileAction());
		}
		
		class ShowFileAction implements ActionListener
		{
			public void actionPerformed(ActionEvent e)
			{
				MacOSActions.showInFinder(path);
			}
		}
	} //}}}
}
