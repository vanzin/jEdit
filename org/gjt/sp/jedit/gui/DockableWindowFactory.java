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
import bsh.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.XMLUtilities;
//}}}

/**
 * This class loads <code>dockable.xml</code> files and manages creation
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
		dockableWindowFactories = new HashMap();
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
		String[] name, boolean[] actions)
	{
		for(int i = 0; i < name.length; i++)
		{
			Window factory = new Window(plugin,
				name[i],null,actions[i]);
			dockableWindowFactories.put(name[i],factory);
		}
	} //}}}

	//{{{ registerDockableWindow() method
	public void registerDockableWindow(PluginJAR plugin,
		String name, String code, boolean actions)
	{
		Window factory = (Window)dockableWindowFactories.get(name);
		if(factory != null)
		{
			factory.code = code;
			factory.loaded = true;
		}
		else
		{
			factory = new Window(plugin,name,code,actions);
			dockableWindowFactories.put(name,factory);
		}
	} //}}}

	//{{{ getRegisteredDockableWindows() method
	public String[] getRegisteredDockableWindows()
	{
		String[] retVal = new String[dockableWindowFactories.size()];
		Iterator entries = dockableWindowFactories.values().iterator();
		int i = 0;
		while(entries.hasNext())
		{
			Window factory = (Window)entries.next();
			retVal[i++] = factory.name;
		}

		return retVal;
	} //}}}

	//{{{ getDockableWindowIterator() method
	Iterator getDockableWindowIterator()
	{
		return dockableWindowFactories.values().iterator();
	} //}}}

	//{{{ DockableListHandler class
	class DockableListHandler extends DefaultHandler
	{
		//{{{ DockableListHandler constructor
		DockableListHandler(PluginJAR plugin, URL uri)
		{
			this.plugin = plugin;
			this.uri = uri;
			stateStack = new Stack();
			actions = true;

			code = new StringBuffer();
			cachedDockableNames = new LinkedList();
			cachedDockableActionFlags = new LinkedList();
		} //}}}

		//{{{ resolveEntity() method
		public InputSource resolveEntity(String publicId, String systemId)
		{
			return XMLUtilities.findEntity(systemId, "dockables.dtd", MiscUtilities.class);
		} //}}}

		//{{{ characters() method
		public void characters(char[] c, int off, int len)
		{
			String tag = peekElement();
			if (tag.equals("DOCKABLE"))
				code.append(c, off, len);
		} //}}}

		//{{{ startElement() method
		public void startElement(String uri, String localName,
					 String qName, Attributes attrs)
		{
			String tag = pushElement(qName);
			if (tag.equals("DOCKABLE"))
			{
				dockableName = attrs.getValue("NAME");
				actions = "FALSE".equals(attrs.getValue("NO_ACTIONS"));
			}
		} //}}}

		//{{{ endElement() method
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
						dockableName,code.toString(),actions);
					cachedDockableNames.add(dockableName);
					cachedDockableActionFlags.add(
						new Boolean(actions));
					// make default be true for the next
					// action
					actions = true;
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
		public void startDocument()
		{
			try
			{
				pushElement(null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		} //}}}

		//{{{ getCachedDockableNames() method
		public String[] getCachedDockableNames()
		{
			return (String[])cachedDockableNames.toArray(new String[cachedDockableNames.size()]);
		} //}}}

		//{{{ getCachedDockableActionFlags() method
		public boolean[] getCachedDockableActionFlags()
		{
			boolean[] returnValue = new boolean[
				cachedDockableActionFlags.size()];
			Iterator iter = cachedDockableActionFlags.iterator();
			int i = 0;
			while(iter.hasNext())
			{
				boolean flag = ((Boolean)iter.next())
					.booleanValue();
				returnValue[i++] = flag;
			}

			return returnValue;
		} //}}}

		//{{{ Private members

		//{{{ Instance variables
		private PluginJAR plugin;
		private URL uri;

		private java.util.List cachedDockableNames;
		private java.util.List cachedDockableActionFlags;

		private String dockableName;
		private StringBuffer code;
		private boolean actions;

		private Stack stateStack;
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
			return (String) stateStack.peek();
		} //}}}

		//{{{ popElement() method
		private String popElement()
		{
			return (String) stateStack.pop();
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

		//{{{ Window constructor
		Window(PluginJAR plugin, String name, String code,
			boolean actions)
		{
			this.plugin = plugin;
			this.name = name;
			this.code = code;

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
			load();

			if(!loaded)
			{
				Log.log(Log.WARNING,this,"Outdated cache");
				return null;
			}

			NameSpace nameSpace = new NameSpace(
				BeanShell.getNameSpace(),
				"DockableWindowManager.Factory"
				+ ".createDockableWindow()");
			try
			{
				nameSpace.setVariable(
					"position",position);
			}
			catch(UtilEvalError e)
			{
				Log.log(Log.ERROR,this,e);
			}
			JComponent win = (JComponent)BeanShell.eval(view,
				nameSpace,code);
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
			public String getCode()
			{
				return "view.getDockableWindowManager()"
					+ ".floatDockableWindow(\"" + dockable + "\");";
			} //}}}
		} //}}}
	} //}}}

	private static DockableWindowFactory instance;
	private HashMap dockableWindowFactories;
}
