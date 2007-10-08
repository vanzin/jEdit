/*
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Delegate.java - A delegate for NSApplication
 * Copyright (C) 2003 Kris Kopicki
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
import com.apple.eio.*;
import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;
import java.util.*;
import java.io.File;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.options.GlobalOptions;
import org.gjt.sp.util.Log;
//}}}

public class Delegate extends ApplicationAdapter
{
	//{{{ Variables
	private final NSSelector actionSel = new NSSelector("doAction", new Class[] {});

	private List filenames = new LinkedList();
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
	} //}}}

	//{{{ Handlers

	//{{{ handleAbout() method
	public void handleAbout(ApplicationEvent event)
	{
		event.setHandled(true);
		new AboutDialog(jEdit.getActiveView());
	} //}}}

	//{{{ handleFileCodes() method
	public void handleFileCodes(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();

		if (jEdit.getBooleanProperty("MacOSPlugin.setTypeCreator",
			jEdit.getBooleanProperty("MacOSPlugin.default.setTypeCreator")))
		{
			// Set type/creator on save
			if (!buffer.isDirty() && msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
			{
				try {
					int type = FileManager.getFileType(buffer.getPath());
					int creator = FileManager.getFileCreator(buffer.getPath());

					if (type != 0)
						buffer.setIntegerProperty("MacOSPlugin.type",type);
					if (creator != 0)
						buffer.setIntegerProperty("MacOSPlugin.creator",creator);
					FileManager.setFileTypeAndCreator(buffer.getPath(),
						buffer.getIntegerProperty("MacOSPlugin.type",
							jEdit.getIntegerProperty("MacOSPlugin.default.type",0)),
						buffer.getIntegerProperty("MacOSPlugin.creator",
							jEdit.getIntegerProperty("MacOSPlugin.default.creator",0)));
				} catch (Exception e) {
					// This will happen when a new file is created
					// Fail silently, since we may be using UFS
				}
			}
			// Add type/creator to local buffer property list on open
			else if (msg.getWhat() == BufferUpdate.CREATED)
			{
				if (jEdit.getBooleanProperty("MacOSPlugin.preserveCodes",
					jEdit.getBooleanProperty("MacOSPlugin.default.preserveCodes")))
				{
					try {
						int type = FileManager.getFileType(buffer.getPath());
						int creator = FileManager.getFileCreator(buffer.getPath());

						if (type != 0)
							buffer.setIntegerProperty("MacOSPlugin.type",type);
						if (creator != 0)
							buffer.setIntegerProperty("MacOSPlugin.creator",creator);
					} catch (Exception e) {
						// This will happen when a new file is created
					}
				}
			}
		}

	} //}}}

	//{{{ handleOpenFile() method
	public void handleOpenFile(ApplicationEvent event)
	{
		filenames.add(event.getFilename());
		event.setHandled(true);
	} //}}}

	//{{{ handleOpenFile() method
	public void handleOpenFile(ViewUpdate msg)
	{
		if(msg.getWhat() == ViewUpdate.CREATED)
		{
			Iterator i = filenames.iterator();
			while (i.hasNext())
				jEdit.openFile(msg.getView(),(String)i.next());
			MacOSPlugin.started = true;
			NSApplication app = NSApplication.sharedApplication();
			app.setServicesProvider(new Delegate());
		}
	} //}}}

	//{{{ handlePreferences() method
	public void handlePreferences(ApplicationEvent event)
	{
		event.setHandled(true);
		new GlobalOptions(jEdit.getActiveView());
	} //}}}

	//{{{ handleQuit() method
	/**
	 * This never seems to be called when used with a delegate
	 */
	//public void handleQuit(ApplicationEvent event)
	//{
	//	event.setHandled(false);
	//	jEdit.exit(jEdit.getActiveView(),true);
	//} //}}}

	//}}}

	//{{{ Delegate methods

	//{{{ applicationDockMenu() method
	public NSMenu applicationDockMenu(NSApplication sender)
	{
		NSMenu dockMenu;
		BufferMenu bufMenu;
		MacrosMenu macMenu;
		RecentMenu recMenu;
		RecentDirMenu dirMenu;
		NSMenuItem showCurrItem;
		NSMenuItem showCurrDirItem;
		NSMenuItem newViewItem;

		// Buffers
		NSMenuItem miBuff = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.buffers.label"),null,"");
		miBuff.setSubmenu(bufMenu = new BufferMenu());

		// Recent Buffers
		NSMenuItem miRec = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recent.label"),null,"");
		miRec.setSubmenu(recMenu = new RecentMenu());

		// Recent Directories
		NSMenuItem miDir = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.recentDir.label"),null,"");
		miDir.setSubmenu(dirMenu = new RecentDirMenu());

		// Macros
		NSMenuItem miMac = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.macros.label"),null,"");
		miMac.setSubmenu(macMenu = new MacrosMenu());

		dockMenu = new NSMenu();
		newViewItem = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.newView"),actionSel,"");
		newViewItem.setTarget(new NewViewAction());
		dockMenu.addItem(newViewItem);
		dockMenu.addItem(new NSMenuItem().separatorItem());
		showCurrItem = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.showCurrent"),actionSel,"");
		dockMenu.addItem(showCurrItem);
		showCurrDirItem = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.showCurrentDir"),actionSel,"");
		dockMenu.addItem(showCurrDirItem);
		dockMenu.addItem(new NSMenuItem().separatorItem());
		dockMenu.addItem(miBuff);
		dockMenu.addItem(miRec);
		dockMenu.addItem(miDir);
		//dockMenu.addItem(new NSMenuItem().separatorItem());
		//dockMenu.addItem(miMac);
		if (jEdit.getViewCount() == 0)
			miMac.setEnabled(false);

		bufMenu.updateMenu();
		recMenu.updateMenu();
		dirMenu.updateMenu();
		macMenu.updateMenu();

		View view = jEdit.getActiveView();
		if (view != null)
		{
			File buff = new File(view.getBuffer().getPath());
			if (buff.exists())
			{
				showCurrItem.setTarget(new ShowFileAction(buff.getPath()));
				showCurrDirItem.setTarget(new ShowFileAction(buff.getParent()));
			}
		}
		else
		{
			showCurrItem.setEnabled(false);
			showCurrDirItem.setEnabled(false);
		}

		return dockMenu;
	} //}}}

	//{{{ applicationOpenFiles() method
	public void applicationOpenFiles(NSApplication sender, NSArray filenames)
	{
		// Must be declared final to be used by inner class
		final Collection files = new ArrayList();
		int count = filenames.count();
		for (int i=0; i<count; i++)
			files.add(new File((String)filenames.objectAtIndex(i)));
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				for(Iterator iter = files.iterator(); iter.hasNext(); )
				{
					File file = (File) iter.next();
					View view = jEdit.getActiveView();
					if(view == null)
						view = PerspectiveManager.loadPerspective(true);
					
					if (file.isDirectory())
					{
						VFSBrowser.browseDirectory(jEdit.getActiveView(),file.getPath());
						return;
					}
					
					if (jEdit.openFile(view,file.getPath()) == null)
						Log.log(Log.ERROR,this,"Error opening file.");
				}
			}
		});
	} //}}}

	//{{{ applicationShouldHandleReopen() method
	public boolean applicationShouldHandleReopen(NSApplication theApplication, boolean flag)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (jEdit.getViewCount() == 0)
					new NewViewAction().doAction();
			}
		});

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

	//{{{ openFile() method
	public String openFile(NSPasteboard pboard, String userData)
	{
		if (jEdit.getViewCount() == 0)
			return null;

		NSData data = pboard.dataForType("NSFilenamesPboardType");
		String[] error = new String[1];
		int[] format = new int[1];
		NSArray filenames = (NSArray)NSPropertyListSerialization.propertyListFromData(data,
			NSPropertyListSerialization.PropertyListImmutable,
			format,
			error);
		int count = filenames.count();
		for (int i=0; i<count; i++)
		{
			File file = new File((String)filenames.objectAtIndex(i));
			if (file.isDirectory())
				VFSBrowser.browseDirectory(jEdit.getActiveView(),file.getPath());
			else
				jEdit.openFile(jEdit.getActiveView(),file.getPath());
		}

		return null;
	} //}}}

	//{{{ insertSelection() method
	public String insertSelection(NSPasteboard pboard, String userData)
	{
		String string = pboard.stringForType("NSStringPboardType");
		if (jEdit.getViewCount() > 0)
		{
			View view = jEdit.getActiveView();
			view.getBuffer().insert(view.getTextArea().getCaretPosition(),string);
		}
		return null;
	} //}}}

	//{{{ openSelection() method
	public String openSelection(NSPasteboard pboard, String userData)
	{
		String string = pboard.stringForType("NSStringPboardType");
		if (jEdit.getViewCount() == 0)
			new NewViewAction().doAction();
		jEdit.newFile(jEdit.getActiveView()).insert(0,pboard.stringForType("NSStringPboardType"));
		return null;
	} //}}}

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
					item = new NSMenuItem(buffs[i].getName(),actionSel,"");
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

	//{{{ MacrosMenu class
	class MacrosMenu extends NSMenu
	{
		public MacrosMenu()
		{
			super();
		}

		public void updateMenu()
		{
			Vector macroVector = Macros.getMacroHierarchy();
			NSMenuItem item;
			File file;
			int max = macroVector.size();

			int length = numberOfItems();
			for (int i=0; i<length; i++)
				removeItemAtIndex(0);

			if (max == 0)
			{
				item = new NSMenuItem(jEdit.getProperty("MacOSPlugin.menu.macros.none"),null,"");
				item.setEnabled(false);
				addItem(item);
				return;
			}

			createMenu(this,macroVector);
		}

		public void createMenu(NSMenu menu, Vector vector)
		{
			for(int i=0; i < vector.size(); i++)
			{
				Object obj = vector.elementAt(i);
				if(obj instanceof Macros.Macro)
				{
					Macros.Macro macro = (Macros.Macro)obj;
					NSMenuItem item = new NSMenuItem(macro.getLabel(),actionSel,"");
					item.setTarget(new MacroAction(macro));
					menu.addItem(item);
				}
				else if(obj instanceof Vector)
				{
					Vector subvector = (Vector)obj;
					String name = (String)subvector.elementAt(0);
					NSMenu submenu = new NSMenu();
					createMenu(submenu,subvector);
					if(submenu.numberOfItems() > 0)
					{
						NSMenuItem submenuitem = new NSMenuItem(name,null,"");
						submenuitem.setSubmenu(submenu);
						menu.addItem(submenuitem);
					}
				}
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
			List recent = BufferHistory.getHistory();
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
				item = new NSMenuItem(file.getName(),actionSel,"");
				item.setTarget(new ShowFileAction(file.getPath()));
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
				item = new NSMenuItem(file.getName(),actionSel,"");
				item.setTarget(new ShowFileAction(file.getPath()));
				if (!file.exists())
					item.setEnabled(false);
				addItem(item);
			}
		}
	} //}}}

	//{{{ MacroAction class
	class MacroAction
	{
		private Macros.Macro macro;

		public MacroAction(Macros.Macro macro)
		{
			this.macro = macro;
		}

		public void doAction()
		{
			macro.invoke(jEdit.getActiveView());
		}
	} //}}}

	//{{{ NewViewAction class
	class NewViewAction
	{
		public void doAction()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (jEdit.getViewCount() == 0)
						PerspectiveManager.loadPerspective(true);
					else
						jEdit.newView(jEdit.getActiveView());
				}
			});
		}
	} //}}}

	//{{{ ShowFileAction class
	class ShowFileAction
	{
		private String path;

		public ShowFileAction(String path)
		{
			this.path = path;
		}

		public void doAction()
		{
			MacOSActions.showInFinder(path);
		}
	} //}}}

	//}}}
}
