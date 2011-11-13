/*
 * DockableWindowFactory.java - loads dockables.xml, etc
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.bsh.NameSpace;
import org.gjt.sp.jedit.bsh.UtilEvalError;
//}}}

/**
 * Loads <code>dockable.xml</code> files and manages creation
 * of new dockable windows.
 *
 * @see DockableWindowManager
 *
 * @since jEdit 4.3pre2
 */
public class DockableWindowFactory
{
	//{{{ getInstance() method
	public static synchronized DockableWindowFactory getInstance()
	{
		if(instance == null)
			instance = new DockableWindowFactory();
		return instance;
	} //}}}

	//{{{ DockableWindowFactory constructor
	public DockableWindowFactory()
	{
		dockableWindowFactories = new HashMap<String, Window>();
	} //}}}

	//{{{ loadDockableWindows() method
	/**
	 * Plugins shouldn't need to call this method.
	 * @since jEdit 4.2pre1
	 */
	public void loadDockableWindows(PluginJAR plugin, URL uri,
		PluginJAR.PluginCacheEntry cache)
	{
		try
		{
			Log.log(Log.DEBUG,DockableWindowManager.class,
				"Loading dockables from " + uri);
			DockableListHandler dh = new DockableListHandler(plugin,uri);
			boolean failure = XMLUtilities.parseXML(uri.openStream(), dh);

			if (!failure && cache != null)
			{
				cache.cachedDockableNames = dh.getCachedDockableNames();
				cache.cachedDockableActionFlags = dh.getCachedDockableActionFlags();
				cache.cachedDockableMovableFlags = dh.getCachedDockableMovableFlags();
			}
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,DockableWindowManager.class,e);
		}
	} //}}}

	//{{{ unloadDockableWindows() method
	/**
	 * Plugins shouldn't need to call this method.
	 * @since jEdit 4.2pre1
	 */
	public void unloadDockableWindows(PluginJAR plugin)
	{
		Iterator entries = dockableWindowFactories.entrySet().iterator();
		while(entries.hasNext())
		{
			Map.Entry entry = (Map.Entry)entries.next();
			Window factory = (Window)entry.getValue();
			if(factory.plugin == plugin)
				entries.remove();
		}
	} //}}}

	//{{{ cacheDockableWindows() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public void cacheDockableWindows(PluginJAR plugin,
		String[] name, boolean[] actions, boolean[] movable)
	{
		for(int i = 0; i < name.length; i++)
		{
			Window factory = new Window(plugin,
				name[i],null,actions[i],movable[i]);
			dockableWindowFactories.put(name[i],factory);
		}
	} //}}}

	//{{{ registerDockableWindow() method
	public void registerDockableWindow(PluginJAR plugin,
		String name, String code, boolean actions, boolean movable)
	{
		Window factory = dockableWindowFactories.get(name);
		if(factory != null)
		{
			factory.code = code;
			factory.loaded = true;
		}
		else
		{
			factory = new Window(plugin,name,code,actions, movable);
			dockableWindowFactories.put(name,factory);
		}
	} //}}}

	//{{{ getRegisteredDockableWindows() method
	public String[] getRegisteredDockableWindows()
	{
		String[] retVal = new String[dockableWindowFactories.size()];
		Iterator<Window> entries = dockableWindowFactories.values().iterator();
		int i = 0;
		while(entries.hasNext())
		{
			Window factory = entries.next();
			retVal[i++] = factory.name;
		}

		return retVal;
	} //}}}

	public Window getDockableWindowFactory(String name)
	{
		return dockableWindowFactories.get(name);
	}
	
	public String getDockableWindowPluginClass(String name)
	{
		Window w = getDockableWindowFactory(name);
		if (w == null || w.plugin == null || w.plugin.getPlugin() == null)
			return null;
		return w.plugin.getPlugin().getClassName();
	}
	
	//{{{ getDockableWindowIterator() method
	Iterator<Window> getDockableWindowIterator()
	{
		return dockableWindowFactories.values().iterator();
	} //}}}

	//{{{ DockableListHandler class
	class DockableListHandler extends DefaultHandler
	{
		//{{{ DockableListHandler constructor
		/**
		 * @param plugin - the pluginJAR for which we are loading the dockables.xml
		 * @param uri - the uri of the dockables.xml file?
		 */
		DockableListHandler(PluginJAR plugin, URL uri)
		{
			this.plugin = plugin;
			this.uri = uri;
			stateStack = new Stack<String>();
			actions = true;
			movable = MOVABLE_DEFAULT;

			code = new StringBuilder();
			cachedDockableNames = new LinkedList<String>();
			cachedDockableActionFlags = new LinkedList<Boolean>();
			cachedDockableMovableFlags = new LinkedList<Boolean>();
		} //}}}

		//{{{ resolveEntity() method
		@Override
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "dockables.dtd", MiscUtilities.class);
		} //}}}

		//{{{ characters() method
		@Override
		public void characters(char[] c, int off, int len)
		{
			String tag = peekElement();
			if (tag.equals("DOCKABLE"))
				code.append(c, off, len);
		} //}}}

		//{{{ startElement() method
		@Override
		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			String tag = pushElement(qName);
			if (tag.equals("DOCKABLE"))
			{
				dockableName = attrs.getValue("NAME");
				actions = "FALSE".equals(attrs.getValue("NO_ACTIONS"));
				String movableAttr = attrs.getValue("MOVABLE");
				if (movableAttr != null)
					movable = movableAttr.equalsIgnoreCase("TRUE");
			}
		} //}}}

		//{{{ endElement() method
		@Override
		public void endElement(String uri, String localName, String name)
		{
			if(name == null)
				return;

			String tag = peekElement();

			if(name.equals(tag))
			{
				if(tag.equals("DOCKABLE"))
				{
					registerDockableWindow(plugin,
						dockableName,code.toString(),actions, movable);
					cachedDockableNames.add(dockableName);
					cachedDockableActionFlags.add(
						Boolean.valueOf(actions));
					cachedDockableMovableFlags.add(
							Boolean.valueOf(movable));
					// make default be true for the next
					// action
					actions = true;
					movable = MOVABLE_DEFAULT;
					code.setLength(0);
				}

				popElement();
			}
			else
			{
				// can't happen
				throw new InternalError();
			}
		} //}}}

		//{{{ startDocument() method
		@Override
		public void startDocument()
		{
			try
			{
				pushElement(null);
			}
			catch (Exception e)
			{
				Log.log(Log.ERROR, this, e);
			}
		} //}}}

		//{{{ getCachedDockableNames() method
		public String[] getCachedDockableNames()
		{
			return cachedDockableNames.toArray(new String[cachedDockableNames.size()]);
		} //}}}

		//{{{ getCachedDockableActionFlags() method
		public boolean[] getCachedDockableActionFlags()
		{
			return booleanListToArray(cachedDockableActionFlags);
		} //}}}

		//{{{ getCachedDockableMovableFlags() method
		public boolean[] getCachedDockableMovableFlags()
		{
			return booleanListToArray(cachedDockableMovableFlags);
		} //}}}
		
		//{{{ booleanListToArray() method
		/**
		 * This method transforms a List<Boolean> into the corresponding
		 * boolean[] array
		 * @param list the List<Boolean> you want to convert
		 * @return a boolean[] array
		 */
		private boolean[] booleanListToArray(java.util.List<Boolean> list)
		{
			boolean[] returnValue = new boolean[list.size()];
			int i = 0;
			for (Boolean value : list)
			{
				returnValue[i++] = value.booleanValue();
			}

			return returnValue;
		} //}}}

		//{{{ Private members

		//{{{ Instance variables
		private PluginJAR plugin;
		// What is the purpose of this?
		private URL uri;

		private java.util.List<String> cachedDockableNames;
		private java.util.List<Boolean> cachedDockableActionFlags;
		private java.util.List<Boolean> cachedDockableMovableFlags;
		
		private String dockableName;
		private StringBuilder code;
		private boolean actions;
		private boolean movable;
		static final boolean MOVABLE_DEFAULT = false;
		
		private Stack<String> stateStack;
		//}}}

		//{{{ pushElement() method
		private String pushElement(String name)
		{
			name = (name == null) ? null : name.intern();

			stateStack.push(name);

			return name;
		} //}}}

		//{{{ peekElement() method
		private String peekElement()
		{
			return stateStack.peek();
		} //}}}

		//{{{ popElement() method
		private String popElement()
		{
			return stateStack.pop();
		} //}}}

		//}}}
	} //}}}

	//{{{ Window class
	class Window
	{
		PluginJAR plugin;
		String name;
		String code;
		boolean loaded;
		boolean movable;
		boolean isBeingCreated = false;

		//{{{ Window constructor
		Window(PluginJAR plugin, String name, String code,
			boolean actions, boolean movable)
		{
			this.plugin = plugin;
			this.name = name;
			this.code = code;
			this.movable = movable;

			if(code != null)
				loaded = true;

			if(actions)
			{
				ActionSet actionSet = (plugin == null
					? jEdit.getBuiltInActionSet()
					: plugin.getActionSet());
				actionSet.addAction(new OpenAction(name));
				actionSet.addAction(new ToggleAction(name));
				actionSet.addAction(new FloatAction(name));

				String label = jEdit.getProperty(name
					+ ".label");
				if(label == null)
					label = "NO LABEL PROPERTY: " + name;

				String[] args = { label };
				jEdit.setTemporaryProperty(name + ".label",
					label);
				jEdit.setTemporaryProperty(name
					+ "-toggle.label",
					jEdit.getProperty(
					"view.docking.toggle.label",args));
				jEdit.setTemporaryProperty(name
					+ "-toggle.toggle","true");
				jEdit.setTemporaryProperty(name
					+ "-float.label",
					jEdit.getProperty(
					"view.docking.float.label",args));
			}
		} //}}}

		//{{{ load() method
		void load()
		{
			if(loaded)
				return;

			loadDockableWindows(plugin,plugin.getDockablesURI(),null);
		} //}}}

		//{{{ createDockableWindow() method
		JComponent createDockableWindow(View view, String position)
		{
			// Avoid infinite recursion
			synchronized(this)
			{
				if (isBeingCreated)
					return null;
				isBeingCreated = true;
			}

			load();

			if(!loaded)
			{
				Log.log(Log.WARNING,this,"Outdated cache");
				return null;
			}

			NameSpace nameSpace = new NameSpace(BeanShell.getNameSpace(),
				"DockableWindowManager.Factory.createDockableWindow()");
			try
			{
				nameSpace.setVariable("position",position);
			}
			catch(UtilEvalError e)
			{
				Log.log(Log.ERROR,this,e);
			}
			JComponent win = (JComponent)BeanShell.eval(view,nameSpace,code);

			if (jEdit.getBooleanProperty("textColors")) {							
				LookAndFeel laf = UIManager.getLookAndFeel();
				if (!laf.getID().equals("Metal")) {
					GUIUtilities.applyTextAreaColors(win);
				}
			}
			
			synchronized(this)
			{
				isBeingCreated = false;
			}
			return win;
		} //}}}

		//{{{ OpenAction class
		class OpenAction extends EditAction
		{
			private String dockable;

			//{{{ OpenAction constructor
			OpenAction(String name)
			{
				super(name);
				this.dockable = name;
			} //}}}

			//{{{ invoke() method
			public void invoke(View view)
			{
				view.getDockableWindowManager()
					.showDockableWindow(dockable);
			} //}}}

			//{{{ getCode() method
			@Override
			public String getCode()
			{
				return "view.getDockableWindowManager()"
					+ ".showDockableWindow(\"" + dockable + "\");";
			} //}}}
		} //}}}

		//{{{ ToggleAction class
		class ToggleAction extends EditAction
		{
			private String dockable;

			//{{{ ToggleAction constructor
			ToggleAction(String name)
			{
				super(name + "-toggle");
				this.dockable = name;
			} //}}}

			//{{{ invoke() method
			public void invoke(View view)
			{
				view.getDockableWindowManager()
					.toggleDockableWindow(dockable);
			} //}}}

			//{{{ isSelected() method
			public boolean isSelected(View view)
			{
				return view.getDockableWindowManager()
					.isDockableWindowVisible(dockable);
			} //}}}

			//{{{ getCode() method
			@Override
			public String getCode()
			{
				return "view.getDockableWindowManager()"
					+ ".toggleDockableWindow(\"" + dockable + "\");";
			} //}}}
		} //}}}

		//{{{ FloatAction class
		class FloatAction extends EditAction
		{
			private String dockable;

			//{{{ FloatAction constructor
			FloatAction(String name)
			{
				super(name + "-float");
				this.dockable = name;
			} //}}}

			//{{{ invoke() method
			public void invoke(View view)
			{
				view.getDockableWindowManager()
					.floatDockableWindow(dockable);
			} //}}}

			//{{{ getCode() method
			@Override
			public String getCode()
			{
				return "view.getDockableWindowManager()"
					+ ".floatDockableWindow(\"" + dockable + "\");";
			} //}}}
		} //}}}
	} //}}}

	private static DockableWindowFactory instance;
	private final Map<String, Window> dockableWindowFactories;
}
