/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Delegate.java - A delegate for NSApplication
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
import com.apple.eawt.*;
import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;
import java.util.Vector;
import java.io.File;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.options.GlobalOptions;
import org.gjt.sp.util.Log;
//}}}

public class Delegate extends ApplicationAdapter
{
	//{{{ Variables
	private final NSSelector showSel = new NSSelector("show", new Class[] {});
	
	private Buffer lastOpenFile;
	//}}}
	
	//{{{ Constructor
	public Delegate()
	{
		if (jEdit.getBooleanProperty("MacOSPlugin.useScreenMenuBar",
			jEdit.getBooleanProperty("MacOSPlugin.default.useScreenMenuBar"))
		)
			System.setProperty("apple.laf.useScreenMenuBar","true");
		else
			System.setProperty("apple.laf.useScreenMenuBar","false");
		
		if (jEdit.getBooleanProperty("MacOSPlugin.liveResize",
			jEdit.getBooleanProperty("MacOSPlugin.default.liveResize"))
		)
			System.setProperty("com.apple.mrj.application.live-resize","true");
		else
			System.setProperty("com.apple.mrj.application.live-resize","false");
	} //}}}
	
	//{{{ Handlers
	
	//{{{ handleAbout() method
	public void handleAbout(ApplicationEvent event)
	{
		new AboutDialog(jEdit.getActiveView());
	} //}}}

	//{{{ handleFileCodes() method
	public void handleFileCodes(BufferUpdate msg)
	{
		
	} //}}}
	
	//{{{ handleOpenFile() method
	public void handleOpenFile(ApplicationEvent event)
	{
		File file = new File(event.getFilename());
		View view = jEdit.getActiveView();
		Buffer buffer;
		
		if (view == null && MacOSPlugin.started)
		{
			if (jEdit.getBooleanProperty("restore.cli"))
				view = jEdit.newView(null,jEdit.restoreOpenFiles());
			else
				view = jEdit.newView(null,jEdit.newFile(null));
		}
		
		if ((buffer = jEdit.openFile(view,file.getPath())) != null)
			lastOpenFile = buffer;
		else
			Log.log(Log.ERROR,this,"Error opening file.");
	} //}}}

	//{{{ handleOpenFile() method
	public void handleOpenFile(ViewUpdate msg)
	{
		if(msg.getWhat() == ViewUpdate.CREATED)
		{
			if(lastOpenFile != null)
				msg.getView().setBuffer(lastOpenFile);
			MacOSPlugin.started = true;
		}
	} //}}}
	
	//{{{ handlePreferences() method
	public void handlePreferences(ApplicationEvent event)
	{
		new GlobalOptions(jEdit.getActiveView());
	} //}}}
	
	//{{{ handleQuit() method
	/**
	 * This never seems to be called when uses with a delegate
	 */
	public void handleQuit(ApplicationEvent event)
	{
		event.setHandled(false);
		jEdit.exit(jEdit.getActiveView(),true);
	} //}}}

	//}}}
	
	//{{{ Delegate methods
	
	//{{{ applicationDockMenu() method
	public NSMenu applicationDockMenu(NSApplication sender)
	{
		NSMenu dockMenu;
		BufferMenu bufMenu;
		RecentMenu recMenu;
		RecentDirMenu dirMenu;
		NSMenuItem showCurrItem;
		NSMenuItem showCurrDirItem;
		
		// Buffers
		NSMenuItem miBuff = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.buffers.label"),null,"");
		miBuff.setSubmenu(bufMenu = new BufferMenu());
		
		// Recent Buffers
		NSMenuItem miRec = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recent.label"),null,"");
		miRec.setSubmenu(recMenu = new RecentMenu());
		
		// Recent Directories
		NSMenuItem miDir = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recentDir.label"),null,"");
		miDir.setSubmenu(dirMenu = new RecentDirMenu());
		
		dockMenu = new NSMenu();
		showCurrItem = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.showCurrent"),showSel,"");
		dockMenu.addItem(showCurrItem);
		showCurrDirItem = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.showCurrentDir"),showSel,"");
		dockMenu.addItem(showCurrDirItem);
		dockMenu.addItem(new NSMenuItem().separatorItem());
		dockMenu.addItem(miBuff);
		dockMenu.addItem(miRec);
		dockMenu.addItem(miDir);
		
		bufMenu.updateMenu();
		recMenu.updateMenu();
		dirMenu.updateMenu();
		
		File buff = new File(jEdit.getActiveView().getBuffer().getPath());
		if (buff.exists())
		{
			showCurrItem.setTarget(new ShowFileAction(buff.getPath()));
			showCurrItem.setEnabled(true);
		}
		else
			showCurrItem.setEnabled(false);
		if (buff.exists())
		{
			showCurrDirItem.setTarget(new ShowFileAction(buff.getParent()));
			showCurrDirItem.setEnabled(true);
		}
		else
			showCurrDirItem.setEnabled(false);
		
		return dockMenu;
	} //}}}
	
	//{{{ applicationShouldHandleReopen() method
	public boolean applicationShouldHandleReopen(NSApplication theApplication, boolean flag)
	{
		if (jEdit.getViewCount() == 0)
			jEdit.newView(null,jEdit.restoreOpenFiles());
		
		return false;
	} //}}}
	
	//{{{ applicationShouldTerminate() method
	public boolean applicationShouldTerminate(NSApplication sender)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				jEdit.exit(jEdit.getActiveView(),true);
			}
		});
		return false;
	}
	//}}}
	
	//}}}
	
	//{{{ Services
	
	public String openFile(NSPasteboard pboard, String userData)
	{
		return null;
	}
	
	public String openSelection(NSPasteboard pboard, String userData)
	{
		jEdit.newFile(jEdit.getActiveView()).insert(0,userData);
		return null;
	}
	
	//}}}
	
	//{{{ Dock Menu
	
	//{{{ BufferMenu class
	class BufferMenu extends NSMenu
	{
		public BufferMenu()
		{
			super();
		}
		
		public void updateMenu()
		{
			NSMenuItem item;
			for (int i=0; i<numberOfItems(); i++)
				removeItemAtIndex(0);
			
			Buffer[] buffs = jEdit.getBuffers();
			for (int i=0; i < buffs.length; i++)
			{
				if (!buffs[i].isUntitled())
				{
					item = new NSMenuItem(buffs[i].getName(),showSel,"");
					item.setTarget(new ShowFileAction(buffs[i].getPath()));
					//item.setImage(NSWorkspace.sharedWorkspace().iconForFile(
					//	buffs[i].getPath()));
					if (!new File(buffs[i].getPath()).exists())
						item.setEnabled(false);
					addItem(item);
				}
			}
			
			if (numberOfItems() == 0)
			{
				item = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.buffers.none"),null,"");
				item.setEnabled(false);
				addItem(item);
			}
		}
	} //}}}
	
	//{{{ RecentMenu class
	class RecentMenu extends NSMenu
	{
		public RecentMenu()
		{
			super();
		}
		
		public void updateMenu()
		{
			Vector recent = BufferHistory.getBufferHistory();
			NSMenuItem item;
			File file;
			int max = recent.size();
			int min = max - 20;
			
			int length = numberOfItems();
			for (int i=0; i<length; i++)
				removeItemAtIndex(0);
			
			if (max == 0)
			{
				item = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recent.none"),null,"");
				item.setEnabled(false);
				addItem(item);
				return;
			}
			
			if (min < 0)
				min = 0;
			
			for (int i=max-1; i >= min ; i--)
			{
				file = new File(((BufferHistory.Entry)recent.get(i)).path);
				item = new NSMenuItem(file.getName(),showSel,"");
				item.setTarget(new ShowFileAction(file.getPath()));
				//item.setImage(NSWorkspace.sharedWorkspace().iconForFile(file.getPath()));
				if (!file.exists())
					item.setEnabled(false);
				addItem(item);
			}
		}
	} //}}}
	
	//{{{ RecentDirMenu class
	class RecentDirMenu extends NSMenu
	{
		public RecentDirMenu()
		{
			super();
		}
		
		public void updateMenu()
		{
			HistoryModel model = HistoryModel.getModel("vfs.browser.path");
			NSMenuItem item;
			File file;
			int max = model.getSize();
			
			int length = numberOfItems();
			for (int i=0; i<length; i++)
				removeItemAtIndex(0);
			
			if (max == 0)
			{
				item = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recentDir.none"),null,"");
				item.setEnabled(false);
				addItem(item);
				return;
			}
			
			for (int i=0; i < max ; i++)
			{
				file = new File(model.getItem(i));
				item = new NSMenuItem(file.getName(),showSel,"");
				item.setTarget(new ShowFileAction(file.getPath()));
				//item.setImage(NSWorkspace.sharedWorkspace().iconForFile(file.getPath()));
				if (!file.exists())
					item.setEnabled(false);
				addItem(item);
			}
		}
	} //}}}
	
	//{{{ ShowBufferMenuItem class
	class ShowFileAction
	{
		private String path;
		
		public ShowFileAction(String path)
		{
			this.path = path;
		}
		
		public void show()
		{
			MacOSActions.showInFinder(path);
		}
	} //}}}
	
	//}}}
}
