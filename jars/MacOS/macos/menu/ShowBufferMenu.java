/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * ShowBufferMenu.java
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
import javax.swing.*;
import javax.swing.event.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.*;
import macos.*;
//}}}

public class ShowBufferMenu extends JMenu implements MenuListener
{
	//{{{ Constructor
	public ShowBufferMenu()
	{
		super(jEdit.getProperty("MacOSPlugin.menu.buffers.label"));
		addMenuListener(this);
	} //}}}
	
	//{{{ construct() method
	private void construct()
	{
		JMenuItem item;
		removeAll();
		
		Buffer[] buffs = jEdit.getBuffers();
		for (int i=0; i < buffs.length; i++)
		{
			if (!buffs[i].isUntitled())
			{
				item = add(new ShowBufferMenuItem(
					buffs[i].getName(),buffs[i].getPath()));
				item.setIcon(FileCellRenderer.fileIcon);
				add(item);
			}
		}
		
		if (getItemCount() == 0)
		{
			item = new JMenuItem(jEdit.getProperty("MacOSPlugin.menu.buffers.none"));
			item.setEnabled(false);
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
	} //}}}
	
	//{{{ menuCanceled() method
	public void menuCanceled(MenuEvent e)
	{
	} //}}}
	
	//{{{ ShowBufferMenuItem class
	class ShowBufferMenuItem extends JMenuItem
	{
		String path;
		
		public ShowBufferMenuItem(String name, String path)
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
