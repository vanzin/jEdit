/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * ScriptsMenu.java
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
import com.apple.mrj.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileView;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.*;
import macos.*;
//}}}

public class ScriptsMenu extends JMenu implements MenuListener
{
	public static final File SCRIPT_DIR = new File(MiscUtilities.concatPath(
		jEdit.getJEditHome(),"scripts"));
	
	private JFileChooser fc = new JFileChooser();
	
	//{{{ Constructor
	public ScriptsMenu()
	{
		super(jEdit.getProperty("MacOSPlugin.menu.scripts.label"));
		addMenuListener(this);
	} //}}}
	
	//{{{ construct() method
	private void construct()
	{
		removeAll();
		
		if (!SCRIPT_DIR.exists())
			SCRIPT_DIR.mkdir();
		
		createMenu(SCRIPT_DIR,0,this);
		
		if (getItemCount() == 0)
		{
			JMenuItem item = new JMenuItem(
			jEdit.getProperty("MacOSPlugin.menu.scripts.none"));
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
	
	//{{{ createMenu() method
	private JMenu createMenu(File cd, int depth, JMenu parent)
	{
		JMenuItem item;
		JMenu submenu;
		int max = jEdit.getIntegerProperty("MacOSPlugin.menu.scripts.depth",1);
		
		System.err.println(depth);
		System.err.println(max);
		
		File[] scripts = cd.listFiles(new ScriptFilter());
		for (int i=0; i < scripts.length; i++)
		{
			if (scripts[i].isDirectory())
			{
				if (depth < max)
				{
					submenu = createMenu(scripts[i],depth+1,new JMenu(scripts[i].getName()));
					if (submenu.getItemCount() != 0)
					{
						submenu.setIcon(FileCellRenderer.dirIcon);
						parent.add(submenu);
					}
				}
			}
			else
			{
				item = new ScriptMenuItem(
					scripts[i].getName(),scripts[i].getPath());
				item.setIcon(fc.getIcon(scripts[i]));
				parent.add(item);
			}
		}
		return parent;
	} //}}}
	
	//{{{ ScriptMenuItem class
	class ScriptMenuItem extends JMenuItem
	{
		String path;
		
		public ScriptMenuItem(String name, String path)
		{
			super(name);
			this.path = path;
			addActionListener(new RunAction());
		}
		
		class RunAction implements ActionListener
		{
			public void actionPerformed(ActionEvent e)
			{
				MacOSActions.runScript(path);
			}
		}
	} //}}}
	
	//{{{ ScriptFilter class
	class ScriptFilter implements FileFilter
	{
		public boolean accept(File pathname)
		{
			if (pathname.isDirectory())
				return true;
			if (pathname.getName().endsWith(".scpt"))
				return true;
			if (pathname.getName().endsWith(".applescript"))
				return true;
			
			try
			{
				MRJOSType type = MRJFileUtils.getFileType(pathname);
				MRJOSType creator = MRJFileUtils.getFileCreator(pathname);
				if (type.equals(new MRJOSType("osas")))
					return true;
				else if (type.equals(new MRJOSType("APPL")) && creator.equals(new MRJOSType("dplt")))
					return true;
			}
			catch (Exception e)
			{
				return false;
			}
			
			return false;
		}
	} //}}}
}
