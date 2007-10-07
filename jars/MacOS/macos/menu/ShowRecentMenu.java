/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * ShowRecentMenu.java
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
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.*;
import macos.*;
//}}}

public class ShowRecentMenu extends JMenu implements MenuListener
{
	//{{{ Constructor
	public ShowRecentMenu()
	{
		super(jEdit.getProperty("MacOSPlugin.menu.recent.label"));
		addMenuListener(this);
	} //}}}
	
	//{{{ construct() method
	private void construct()
	{
		BufferHistory.readLock();
		List recent = BufferHistory.getHistory();
		JMenuItem item;
		File file;
		int max = recent.size();
		int min = max - 20;
		
		if (max == 0)
		{
			item = new JMenuItem(jEdit.getProperty("MacOSPlugin.menu.recent.none"));
			item.setEnabled(false);
			add(item);
			return;
		}
		
		if (min < 0)
			min = 0;
		
		for (int i=max-1; i >= min ; i--)
		{
			file = new File(((BufferHistory.Entry)recent.get(i)).path);
			item = new ShowRecentMenuItem(file.getName(),file.getPath());
			item.setIcon(FileCellRenderer.fileIcon);
			add(item);
		}
		BufferHistory.readUnlock();
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
	
	//{{{ ShowRecentMenuItem class
	class ShowRecentMenuItem extends JMenuItem
	{
		String path;
		
		public ShowRecentMenuItem(String name, String path)
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
