/*
 * DockableWindowManager.java - manages dockable windows
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2004 Slava Pestov
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
 import com.microstar.xml.*;
 import javax.swing.*;
 import java.awt.event.*;
 import java.awt.*;
 import java.io.*;
 import java.net.URL;
 import java.util.*;
 import org.gjt.sp.jedit.msg.*;
 import org.gjt.sp.jedit.*;
 import org.gjt.sp.util.Log;
//}}}

/**
 * The <code>DockableWindowManager</code> keeps track of dockable windows.
 * Each {@link org.gjt.sp.jedit.View} has an instance of this class.<p>
 *
 * <b>dockables.xml:</b><p>
 *
 * Dockable window definitions are read from <code>dockables.xml</code> files
 * contained inside plugin JARs. A dockable definition file has the following
 * form:
 *
 * <pre>&lt;?xml version="1.0"?&gt;
 *&lt;!DOCTYPE DOCKABLES SYSTEM "dockables.dtd"&gt;
 *&lt;DOCKABLES&gt;
 *    &lt;DOCKABLE NAME="name"&gt;
 *        // Code to create the dockable
 *    &lt;/DOCKABLE&gt;
 *&lt;/DOCKABLES&gt;</pre>
 *
 * More than one <code>&lt;DOCKABLE&gt;<code> tag may be present. The code that
 * creates the dockable can reference any BeanShell built-in variable
 * (see {@link org.gjt.sp.jedit.BeanShell}), along with a variable
 * <code>position</code> whose value is one of
 * {@link #FLOATING}, {@link #TOP}, {@link #LEFT}, {@link #BOTTOM},
 * and {@link #RIGHT}.<p>
 *
 * The following properties must be defined for each dockable window:
 *
 * <ul>
 * <li><code><i>name</i>.title</code> - the string to show in the title bar
 * of the dockable.</li>
 * <li><code><i>name</i>.label</code> - the dockable's menu item label.</li>
 * </ul>
 *
 * A number of actions are automatically created for each dockable window:
 *
 * <ul>
 * <li><code><i>name</i></code> - opens the dockable window.</li>
 * <li><code><i>name</i>-toggle</code> - toggles the dockable window's visibility.</li>
 * <li><code><i>name</i>-float</code> - opens the dockable window in a new
 * floating window.</li>
 * </ul>
 *
 * Note that only the first action needs a <code>label</code> property, the
 * rest have automatically-generated labels.
 *
 * <b>Implementation details:</b><p>
 *
 * When an instance of this class is initialized by the {@link org.gjt.sp.jedit.View}
 * class, it
 * iterates through the list of registered dockable windows (from jEdit itself,
 * and any loaded plugins) and
 * examines options supplied by the user in the <b>Global
 * Options</b> dialog box. Any plugins designated for one of the
 * four docking positions are displayed.<p>
 *
 * To create an instance of a dockable window, the <code>DockableWindowManager</code>
 * finds and executes the BeanShell code extracted from the appropriate
 * <code>dockables.xml</code> file. This code will typically consist of a call
 * to the constructor of the dockable window component. The result of the
 * BeanShell expression, typically a newly constructed component, is placed
 * in a window managed by this class.
 *
 * @see org.gjt.sp.jedit.View#getDockableWindowManager()
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public class DockableWindowManager extends JPanel implements EBComponent
{
	//{{{ Static part of class

	//{{{ Constants
	/**
	 * Floating position.
	 * @since jEdit 2.6pre3
	 */
	public static final String FLOATING = "floating";

	/**
	 * Top position.
	 * @since jEdit 2.6pre3
	 */
	public static final String TOP = "top";

	/**
	 * Left position.
	 * @since jEdit 2.6pre3
	 */
	public static final String LEFT = "left";

	/**
	 * Bottom position.
	 * @since jEdit 2.6pre3
	 */
	public static final String BOTTOM = "bottom";

	/**
	 * Right position.
	 * @since jEdit 2.6pre3
	 */
	public static final String RIGHT = "right";
	//}}}

	//{{{ loadDockableWindows() method
	/**
	 * Plugins shouldn't need to call this method.
	 * @since jEdit 4.2pre1
	 */
	public static void loadDockableWindows(PluginJAR plugin, URL uri,
		PluginJAR.PluginCacheEntry cache)
	{
		Reader in = null;

		try
		{
			Log.log(Log.DEBUG,DockableWindowManager.class,
				"Loading dockables from " + uri);

			DockableListHandler dh = new DockableListHandler(plugin,uri);
			in = new BufferedReader(
				new InputStreamReader(
				uri.openStream()));
			XmlParser parser = new XmlParser();
			parser.setHandler(dh);
			parser.parse(null, null, in);
			if(cache != null)
			{
				cache.cachedDockableNames = dh.getCachedDockableNames();
				cache.cachedDockableActionFlags = dh.getCachedDockableActionFlags();
			}
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,DockableWindowManager.class,uri + ":" + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,DockableWindowManager.class,e);
		}
		finally
		{
			try
			{
				if(in != null)
					in.close();
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,DockableWindowManager.class,io);
			}
		}
	} //}}}

	//{{{ unloadDockableWindows() method
	/**
	 * Plugins shouldn't need to call this method.
	 * @since jEdit 4.2pre1
	 */
	public static void unloadDockableWindows(PluginJAR plugin)
	{
		Iterator entries = dockableWindowFactories.entrySet().iterator();
		while(entries.hasNext())
		{
			Map.Entry entry = (Map.Entry)entries.next();
			Factory factory = (Factory)entry.getValue();
			if(factory.plugin == plugin)
				entries.remove();
		}
	} //}}}

	//{{{ cacheDockableWindows() method
	/**
	 * @since jEdit 4.2pre1
	 */
	public static void cacheDockableWindows(PluginJAR plugin,
		String[] name, boolean[] actions)
	{
		for(int i = 0; i < name.length; i++)
		{
			Factory factory = new Factory(plugin,
				name[i],null,actions[i]);
			dockableWindowFactories.put(name[i],factory);
		}
	} //}}}

	//{{{ registerDockableWindow() method
	public static void registerDockableWindow(PluginJAR plugin,
		String name, String code, boolean actions)
	{
		Factory factory = (Factory)dockableWindowFactories.get(name);
		if(factory != null)
		{
			factory.code = code;
			factory.loaded = true;
		}
		else
		{
			factory = new Factory(plugin,name,code,actions);
			dockableWindowFactories.put(name,factory);
		}
	} //}}}

	//{{{ getRegisteredDockableWindows() method
	public static String[] getRegisteredDockableWindows()
	{
		String[] retVal = new String[dockableWindowFactories.size()];
		Iterator entries = dockableWindowFactories.values().iterator();
		int i = 0;
		while(entries.hasNext())
		{
			Factory factory = (Factory)entries.next();
			retVal[i++] = factory.name;
		}

		return retVal;
	} //}}}

	//{{{ DockableListHandler class
	static class DockableListHandler extends HandlerBase
	{
		//{{{ DockableListHandler constructor
		DockableListHandler(PluginJAR plugin, URL uri)
		{
			this.plugin = plugin;
			this.uri = uri;
			stateStack = new Stack();
			actions = true;

			cachedDockableNames = new LinkedList();
			cachedDockableActionFlags = new LinkedList();
		} //}}}

		//{{{ resolveEntity() method
		public Object resolveEntity(String publicId, String systemId)
		{
			if("dockables.dtd".equals(systemId))
			{
				// this will result in a slight speed up, since we
				// don't need to read the DTD anyway, as AElfred is
				// non-validating
				return new StringReader("<!-- -->");

				/* try
				{
					return new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream
						("/org/gjt/sp/jedit/dockables.dtd")));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,"Error while opening"
						+ " dockables.dtd:");
					Log.log(Log.ERROR,this,e);
				} */
			}

			return null;
		} //}}}

		//{{{ attribute() method
		public void attribute(String aname, String value, boolean isSpecified)
		{
			aname = (aname == null) ? null : aname.intern();
			value = (value == null) ? null : value.intern();

			if(aname == "NAME")
				dockableName = value;
			else if(aname == "NO_ACTIONS")
				actions = (value == "FALSE");
		} //}}}

		//{{{ doctypeDecl() method
		public void doctypeDecl(String name, String publicId,
			String systemId) throws Exception
		{
			if("DOCKABLES".equals(name))
				return;

			Log.log(Log.ERROR,this,uri + ": DOCTYPE must be DOCKABLES");
		} //}}}

		//{{{ charData() method
		public void charData(char[] c, int off, int len)
		{
			String tag = peekElement();
			String text = new String(c, off, len);

			if (tag == "DOCKABLE")
			{
				code = text;
			}
		} //}}}

		//{{{ startElement() method
		public void startElement(String tag)
		{
			tag = pushElement(tag);
		} //}}}

		//{{{ endElement() method
		public void endElement(String name)
		{
			if(name == null)
				return;

			String tag = peekElement();

			if(name.equals(tag))
			{
				if(tag == "DOCKABLE")
				{
					registerDockableWindow(plugin,
						dockableName,code,actions);
					cachedDockableNames.add(dockableName);
					cachedDockableActionFlags.add(
						new Boolean(actions));
					// make default be true for the next
					// action
					actions = true;
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
		private String code;
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

	//{{{ Factory class
	static class Factory
	{
		PluginJAR plugin;
		String name;
		String code;
		boolean loaded;

		//{{{ Factory constructor
		Factory(PluginJAR plugin, String name, String code,
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
		static class OpenAction extends EditAction
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
		static class ToggleAction extends EditAction
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
		static class FloatAction extends EditAction
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

	private static HashMap dockableWindowFactories;

	//{{{ Static initializer
	static
	{
		dockableWindowFactories = new HashMap();
	} //}}}

	//}}}

	//{{{ Instance part of class

	//{{{ DockableWindowManager constructor
	/**
	 * Creates a new dockable window manager.
	 * @param view The view
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager(View view, View.ViewConfig config)
	{
		setLayout(new DockableLayout());
		this.view = view;
		windows = new Hashtable();
		clones = new ArrayList();

		top = new PanelWindowContainer(this,TOP,config.topPos);
		left = new PanelWindowContainer(this,LEFT,config.leftPos);
		bottom = new PanelWindowContainer(this,BOTTOM,config.bottomPos);
		right = new PanelWindowContainer(this,RIGHT,config.rightPos);

		add(DockableLayout.TOP_BUTTONS,top.buttonPanel);
		add(DockableLayout.LEFT_BUTTONS,left.buttonPanel);
		add(DockableLayout.BOTTOM_BUTTONS,bottom.buttonPanel);
		add(DockableLayout.RIGHT_BUTTONS,right.buttonPanel);

		add(TOP,top.dockablePanel);
		add(LEFT,left.dockablePanel);
		add(BOTTOM,bottom.dockablePanel);
		add(RIGHT,right.dockablePanel);
	} //}}}

	//{{{ init() method
	/**
	 * Initialises dockable window manager. Do not call this method directly.
	 */
	public void init()
	{
		EditBus.addToBus(this);

		Iterator entries = dockableWindowFactories.values().iterator();

		while(entries.hasNext())
			addEntry((Factory)entries.next());

		propertiesChanged();
	} //}}}

	//{{{ getView() method
	/**
	 * Returns this dockable window manager's view.
	 * @since jEdit 4.0pre2
	 */
	public View getView()
	{
		return view;
	} //}}}

	//{{{ floatDockableWindow() method
	/**
	 * Opens a new instance of the specified dockable window in a floating
	 * container.
	 * @param name The dockable window name
	 * @return The new dockable window instance
	 * @since jEdit 4.1pre2
	 */
	public JComponent floatDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return null;
		}

		// create a copy of this dockable window and float it
		Entry newEntry = new Entry(entry.factory,FLOATING);
		newEntry.win = newEntry.factory.createDockableWindow(view,FLOATING);
		if(newEntry.win != null)
		{
			newEntry.container = new FloatingWindowContainer(this,true);
			newEntry.container.register(newEntry);
			newEntry.container.show(newEntry);
		}

		clones.add(newEntry);
		return newEntry.win;
	} //}}}

	//{{{ showDockableWindow() method
	/**
	 * Opens the specified dockable window.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void showDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}

		if(entry.win == null)
		{
			entry.win = entry.factory.createDockableWindow(
				view,entry.position);
		}

		if(entry.win != null)
		{
			if(entry.position.equals(FLOATING)
				&& entry.container == null)
			{
				entry.container = new FloatingWindowContainer(
					this,view.isPlainView());
				entry.container.register(entry);
			}

			entry.container.show(entry);
		}
		else
			/* an error occurred */;
	} //}}}

	//{{{ addDockableWindow() method
	/**
	 * Opens the specified dockable window. As of jEdit 4.0pre1, has the
	 * same effect as calling showDockableWindow().
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void addDockableWindow(String name)
	{
		showDockableWindow(name);
	} //}}}

	//{{{ hideDockableWindow() method
	/**
	 * Hides the specified dockable window.
	 * @param name The dockable window name
	 * @since jEdit 2.6pre3
	 */
	public void hideDockableWindow(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
		{
			Log.log(Log.ERROR,this,"Unknown dockable window: " + name);
			return;
		}

		if(entry.win == null)
			return;

		entry.container.show(null);
	} //}}}

	//{{{ removeDockableWindow() method
	/**
	 * Hides the specified dockable window. As of jEdit 4.2pre1, has the
	 * same effect as calling hideDockableWindow().
	 * @param name The dockable window name
	 * @since jEdit 4.2pre1
	 */
	public void removeDockableWindow(String name)
	{
		hideDockableWindow(name);
	} //}}}

	//{{{ toggleDockableWindow() method
	/**
	 * Toggles the visibility of the specified dockable window.
	 * @param name The dockable window name
	 */
	public void toggleDockableWindow(String name)
	{
		if(isDockableWindowVisible(name))
			removeDockableWindow(name);
		else
			addDockableWindow(name);
	} //}}}

	//{{{ getDockableWindow() method
	/**
	 * Returns the specified dockable window.
	 *
	 * Note that this method
	 * will return null if the dockable has not been added yet.
	 * Make sure you call {@link #addDockableWindow(String)} first.
	 *
	 * @param name The name of the dockable window
	 * @since jEdit 4.1pre2
	 */
	public JComponent getDockableWindow(String name)
	{
		return getDockable(name);
	} //}}}

	//{{{ getDockable() method
	/**
	 * Returns the specified dockable window.
	 *
	 * Note that this method
	 * will return null if the dockable has not been added yet.
	 * Make sure you call {@link #addDockableWindow(String)} first.
	 *
	 * For historical reasons, this
	 * does the same thing as {@link #getDockableWindow(String)}.
	 *
	 * @param name The name of the dockable window
	 * @since jEdit 4.0pre1
	 */
	public JComponent getDockable(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return null;
		else
			return entry.win;
	} //}}}

	//{{{ getDockableTitle() method
	/**
	 * Returns the title of the specified dockable window.
	 * @param name The name of the dockable window.
	 * @since jEdit 4.1pre5
	 */
	public String getDockableTitle(String name)
	{
		String title = jEdit.getProperty(name + ".title");
		if(title == null)
			return "NO TITLE PROPERTY: " + name;
		else
			return title;
	} //}}}

	//{{{ isDockableWindowVisible() method
	/**
	 * Returns if the specified dockable window is visible.
	 * @param name The dockable window name
	 */
	public boolean isDockableWindowVisible(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null || entry.win == null)
			return false;
		else
			return entry.container.isVisible(entry);
	} //}}}

	//{{{ isDockableWindowDocked() method
	/**
	 * Returns if the specified dockable window is docked into the
	 * view.
	 * @param name The dockable's name
	 * @since jEdit 4.0pre2
	 */
	public boolean isDockableWindowDocked(String name)
	{
		Entry entry = (Entry)windows.get(name);
		if(entry == null)
			return false;
		else
			return !entry.position.equals(FLOATING);
	} //}}}

	//{{{ closeCurrentArea() method
	/**
	 * Closes the currently focused docking area.
	 * @since jEdit 4.1pre3
	 */
	public void closeCurrentArea()
	{
		// I don't know of any other way to fix this, since invoking this
		// command from a menu results in the focus owner being the menu
		// until the menu goes away.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Component comp = view.getFocusOwner();
				while(comp != null)
				{
					//System.err.println(comp.getClass());
					if(comp instanceof PanelWindowContainer
						.DockablePanel)
					{
						PanelWindowContainer container =
							((PanelWindowContainer.DockablePanel)
							comp).getWindowContainer();
						container.show(null);
						return;
					}

					comp = comp.getParent();
				}

				getToolkit().beep();
			}
		});
	} //}}}

	//{{{ close() method
	/**
	 * Called when the view is being closed.
	 * @since jEdit 2.6pre3
	 */
	public void close()
	{
		EditBus.removeFromBus(this);

		Iterator iter = windows.values().iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.win != null)
			{
				entry.container.unregister(entry);
			}
		}

		iter = clones.iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.win != null)
			{
				entry.container.unregister(entry);
			}
		}
	} //}}}

	//{{{ getTopDockingArea() method
	public PanelWindowContainer getTopDockingArea()
	{
		return top;
	} //}}}

	//{{{ getLeftDockingArea() method
	public PanelWindowContainer getLeftDockingArea()
	{
		return left;
	} //}}}

	//{{{ getBottomDockingArea() method
	public PanelWindowContainer getBottomDockingArea()
	{
		return bottom;
	} //}}}

	//{{{ getRightDockingArea() method
	public PanelWindowContainer getRightDockingArea()
	{
		return right;
	} //}}}

	//{{{ createPopupMenu() method
	public JPopupMenu createPopupMenu(
		final DockableWindowContainer container,
		final String dockable,
		final boolean clone)
	{
		JPopupMenu popup = new JPopupMenu();
		if(dockable == null && container instanceof PanelWindowContainer)
		{
			ActionListener listener = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					showDockableWindow(evt.getActionCommand());
				}
			};

			String[] dockables = ((PanelWindowContainer)
				container).getDockables();
			for(int i = 0; i < dockables.length; i++)
			{
				String name = dockables[i];
				JMenuItem item = new JMenuItem(getDockableTitle(name));
				item.setActionCommand(name);
				item.addActionListener(listener);
				popup.add(item);
			}
		}
		else
		{
			JMenuItem caption = new JMenuItem(getDockableTitle(dockable));
			caption.setEnabled(false);
			popup.add(caption);
			popup.addSeparator();
			String currentPos = jEdit.getProperty(dockable + ".dock-position",FLOATING);
			if(!clone)
			{
				String[] positions = { FLOATING, TOP, LEFT, BOTTOM, RIGHT };
				for(int i = 0; i < positions.length; i++)
				{
					final String pos = positions[i];
					if(pos.equals(currentPos))
						continue;

					JMenuItem moveMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-"
						+ pos));

					moveMenuItem.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent evt)
						{
							jEdit.setProperty(dockable + ".dock-position",pos);
							EditBus.send(new DockableWindowUpdate(
								DockableWindowManager.this,
								DockableWindowUpdate.PROPERTIES_CHANGED,
								null
							));
							showDockableWindow(dockable);
						}
					});
					popup.add(moveMenuItem);
				}

				popup.addSeparator();
			}

			JMenuItem cloneMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-clone"));

			cloneMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					floatDockableWindow(dockable);
				}
			});
			popup.add(cloneMenuItem);

			popup.addSeparator();

			JMenuItem closeMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-close"));

			closeMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					if(clone)
						((FloatingWindowContainer)container).dispose();
					else
						removeDockableWindow(dockable);
				}
			});
			popup.add(closeMenuItem);

			if(!(clone || currentPos.equals(FLOATING)))
			{
				JMenuItem undockMenuItem = new JMenuItem(jEdit.getProperty("view.docking.menu-undock"));

				undockMenuItem.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						jEdit.setProperty(dockable + ".dock-position",FLOATING);
						EditBus.send(new DockableWindowUpdate(
							DockableWindowManager.this,
							DockableWindowUpdate.PROPERTIES_CHANGED,
							null
						));
					}
				});
				popup.add(undockMenuItem);
			}
		}

		return popup;
	} //}}}

	//{{{ paintChildren() method
	public void paintChildren(Graphics g)
	{
		super.paintChildren(g);

		if(resizeRect != null)
		{
			g.setColor(Color.darkGray);
			g.fillRect(resizeRect.x,resizeRect.y,
				resizeRect.width,resizeRect.height);
		}
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof DockableWindowUpdate)
		{
			if(((DockableWindowUpdate)msg).getWhat()
				== DockableWindowUpdate.PROPERTIES_CHANGED)
				propertiesChanged();
		}
		else if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof PluginUpdate)
		{
			PluginUpdate pmsg = (PluginUpdate)msg;
			if(pmsg.getWhat() == PluginUpdate.LOADED)
			{
				Iterator iter = dockableWindowFactories
					.values().iterator();

				while(iter.hasNext())
				{
					Factory factory = (Factory)iter.next();
					if(factory.plugin == pmsg.getPluginJAR())
						addEntry(factory);
				}

				propertiesChanged();
			}
			else if(pmsg.isExiting())
			{
				// we don't care
			}
			else if(pmsg.getWhat() == PluginUpdate.DEACTIVATED)
			{
				Iterator iter = getAllPluginEntries(
					pmsg.getPluginJAR(),false);
				while(iter.hasNext())
				{
					Entry entry = (Entry)iter.next();
					entry.container.remove(entry);
				}
			}
			else if(pmsg.getWhat() == PluginUpdate.UNLOADED)
			{
				Iterator iter = getAllPluginEntries(
					pmsg.getPluginJAR(),true);
				while(iter.hasNext())
				{
					Entry entry = (Entry)iter.next();
					entry.container.unregister(entry);
					entry.win = null;
					entry.container = null;
				}
			}
		}
	} //}}}

	//{{{ Package-private members
	int resizePos;
	Rectangle resizeRect;

	//{{{ setResizePos() method
	void setResizePos(int resizePos, PanelWindowContainer resizing)
	{
		this.resizePos = resizePos;

		if(resizePos < 0)
			resizePos = 0;

		Rectangle newResizeRect = new Rectangle(0,0,
			PanelWindowContainer.SPLITTER_WIDTH - 2,
			PanelWindowContainer.SPLITTER_WIDTH - 2);
		if(resizing == top)
		{
			resizePos = Math.min(resizePos,getHeight()
				- top.buttonPanel.getHeight()
				- bottom.dockablePanel.getHeight()
				- bottom.buttonPanel.getHeight()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = top.dockablePanel.getX() + 1;
			newResizeRect.y = resizePos + top.buttonPanel.getHeight() + 1;
			newResizeRect.width = top.dockablePanel.getWidth() - 2;
		}
		else if(resizing == left)
		{
			resizePos = Math.min(resizePos,getWidth()
				- left.buttonPanel.getWidth()
				- right.dockablePanel.getWidth()
				- right.buttonPanel.getWidth()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = resizePos + left.buttonPanel.getWidth() + 1;
			newResizeRect.y = left.dockablePanel.getY() + 1;
			newResizeRect.height = left.dockablePanel.getHeight() - 2;
		}
		else if(resizing == bottom)
		{
			resizePos = Math.min(resizePos,getHeight()
				- bottom.buttonPanel.getHeight()
				- top.dockablePanel.getHeight()
				- top.buttonPanel.getHeight()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = bottom.dockablePanel.getX() + 1;
			newResizeRect.y = getHeight() - bottom.buttonPanel.getHeight() - resizePos
				- PanelWindowContainer.SPLITTER_WIDTH + 2;
			newResizeRect.width = bottom.dockablePanel.getWidth() - 2;
		}
		else if(resizing == right)
		{
			resizePos = Math.min(resizePos,getWidth()
				- right.buttonPanel.getWidth()
				- left.dockablePanel.getWidth()
				- left.buttonPanel.getWidth()
				- PanelWindowContainer.SPLITTER_WIDTH);
			newResizeRect.x = getWidth() - right.buttonPanel.getWidth() - resizePos
				- PanelWindowContainer.SPLITTER_WIDTH + 1;
			newResizeRect.y = right.dockablePanel.getY() + 1;
			newResizeRect.height = right.dockablePanel.getHeight() - 2;
		}

		Rectangle toRepaint;
		if(resizeRect == null)
			toRepaint = newResizeRect;
		else
			toRepaint = resizeRect.union(newResizeRect);
		resizeRect = newResizeRect;
		repaint(toRepaint);
	} //}}}

	//{{{ finishResizing() method
	void finishResizing()
	{
		resizeRect = null;
		repaint();
	} //}}}

	//}}}

	//{{{ Private members
	private View view;
	private Hashtable windows;
	private boolean alternateLayout;
	private PanelWindowContainer left;
	private PanelWindowContainer right;
	private PanelWindowContainer top;
	private PanelWindowContainer bottom;
	private ArrayList clones;

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		if(view.isPlainView())
			return;

		alternateLayout = jEdit.getBooleanProperty("view.docking.alternateLayout");

		String[] windowList = getRegisteredDockableWindows();

		for(int i = 0; i < windowList.length; i++)
		{
			String dockable = windowList[i];
			Entry entry = (Entry)windows.get(dockable);

			String newPosition = jEdit.getProperty(dockable
				+ ".dock-position",FLOATING);
			if(newPosition.equals(entry.position))
			{
				continue;
			}

			entry.position = newPosition;
			if(entry.container != null)
			{
				entry.container.unregister(entry);
				entry.container = null;
				entry.win = null;
			}

			if(newPosition.equals(FLOATING))
				/* do nothing */;
			else
			{
				if(newPosition.equals(TOP))
					entry.container = top;
				else if(newPosition.equals(LEFT))
					entry.container = left;
				else if(newPosition.equals(BOTTOM))
					entry.container = bottom;
				else if(newPosition.equals(RIGHT))
					entry.container = right;
				else
				{
					Log.log(Log.WARNING,this,
						"Unknown position: "
						+ newPosition);
					continue;
				}

				entry.container.register(entry);
			}
		}

		top.sortDockables();
		left.sortDockables();
		bottom.sortDockables();
		right.sortDockables();

		revalidate();
		repaint();
	} //}}}

	//{{{ addEntry() method
	private void addEntry(Factory factory)
	{
		Entry e;
		if(view.isPlainView())
		{
			// don't show menu items to dock into a plain view
			e = new Entry(factory,FLOATING);
		}
		else
		{
			e = new Entry(factory);
			if(e.position.equals(FLOATING))
				/* nothing to do */;
			else if(e.position.equals(TOP))
				e.container = top;
			else if(e.position.equals(LEFT))
				e.container = left;
			else if(e.position.equals(BOTTOM))
				e.container = bottom;
			else if(e.position.equals(RIGHT))
				e.container = right;
			else
			{
				Log.log(Log.WARNING,this,
					"Unknown position: "
					+ e.position);
			}

			if(e.container != null)
				e.container.register(e);
		}
		windows.put(factory.name,e);
	} //}}}

	//{{{ getAllPluginEntries() method
	/**
	 * If remove is false, only remove from clones list, otherwise remove
	 * from both entries and clones.
	 */
	private Iterator getAllPluginEntries(PluginJAR plugin, boolean remove)
	{
		java.util.List returnValue = new LinkedList();
		Iterator iter = windows.values().iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.factory.plugin == plugin)
			{
				returnValue.add(entry);
				if(remove)
					iter.remove();
			}
		}

		iter = clones.iterator();
		while(iter.hasNext())
		{
			Entry entry = (Entry)iter.next();
			if(entry.factory.plugin == plugin)
			{
				returnValue.add(entry);
				iter.remove();
			}
		}

		return returnValue.iterator();
	} //}}}

	//}}}

	//}}}

	//{{{ DockableLayout class
	public class DockableLayout implements LayoutManager2
	{
		// for backwards compatibility with plugins that fiddle with
		// jEdit's UI layout
		static final String CENTER = BorderLayout.CENTER;

		public static final String TOP_TOOLBARS = "top-toolbars";
		public static final String BOTTOM_TOOLBARS = "bottom-toolbars";

		static final String TOP_BUTTONS = "top-buttons";
		static final String LEFT_BUTTONS = "left-buttons";
		static final String BOTTOM_BUTTONS = "bottom-buttons";
		static final String RIGHT_BUTTONS = "right-buttons";

		Component topToolbars, bottomToolbars;
		Component center;
		Component top, left, bottom, right;
		Component topButtons, leftButtons, bottomButtons, rightButtons;

		//{{{ addLayoutComponent() method
		public void addLayoutComponent(String name, Component comp)
		{
			addLayoutComponent(comp,name);
		} //}}}

		//{{{ addLayoutComponent() method
		public void addLayoutComponent(Component comp, Object cons)
		{
			if(cons == null || CENTER.equals(cons))
				center = comp;
			else if(TOP_TOOLBARS.equals(cons))
				topToolbars = comp;
			else if(BOTTOM_TOOLBARS.equals(cons))
				bottomToolbars = comp;
			else if(TOP.equals(cons))
				top = comp;
			else if(LEFT.equals(cons))
				left = comp;
			else if(BOTTOM.equals(cons))
				bottom = comp;
			else if(RIGHT.equals(cons))
				right = comp;
			else if(TOP_BUTTONS.equals(cons))
				topButtons = comp;
			else if(LEFT_BUTTONS.equals(cons))
				leftButtons = comp;
			else if(BOTTOM_BUTTONS.equals(cons))
				bottomButtons = comp;
			else if(RIGHT_BUTTONS.equals(cons))
				rightButtons = comp;
		} //}}}

		//{{{ removeLayoutComponent() method
		public void removeLayoutComponent(Component comp)
		{
			if(center == comp)
				center = null;
			if(comp == topToolbars)
				topToolbars = null;
			if(comp == bottomToolbars)
				bottomToolbars = null;
			{
				// none of the others are ever meant to be
				// removed. retarded, eh? this needs to be
				// fixed eventually, for plugins might
				// want to do weird stuff to jEdit's UI
			}
		} //}}}

		//{{{ preferredLayoutSize() method
		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension prefSize = new Dimension(0,0);
			Dimension _top = top.getPreferredSize();
			Dimension _left = left.getPreferredSize();
			Dimension _bottom = bottom.getPreferredSize();
			Dimension _right = right.getPreferredSize();
			Dimension _topButtons = topButtons.getPreferredSize();
			Dimension _leftButtons = leftButtons.getPreferredSize();
			Dimension _bottomButtons = bottomButtons.getPreferredSize();
			Dimension _rightButtons = rightButtons.getPreferredSize();
			Dimension _center = (center == null
				? new Dimension(0,0)
				: center.getPreferredSize());
			Dimension _topToolbars = (topToolbars == null
				? new Dimension(0,0)
				: topToolbars.getPreferredSize());
			Dimension _bottomToolbars = (bottomToolbars == null
				? new Dimension(0,0)
				: bottomToolbars.getPreferredSize());

			prefSize.height = _top.height + _bottom.height + _center.height
				+ _topButtons.height + _bottomButtons.height
				+ _topToolbars.height + _bottomToolbars.height;
			prefSize.width = _left.width + _right.width
				+ Math.max(_center.width,
				Math.max(_topToolbars.width,_bottomToolbars.width))
				+ _leftButtons.width + _rightButtons.width;

			return prefSize;
		} //}}}

		//{{{ minimumLayoutSize() method
		public Dimension minimumLayoutSize(Container parent)
		{
			// I'm lazy
			return preferredLayoutSize(parent);
		} //}}}

		//{{{ maximumLayoutSize() method
		public Dimension maximumLayoutSize(Container parent)
		{
			return new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
		} //}}}

		//{{{ layoutContainer() method
		public void layoutContainer(Container parent)
		{
			Dimension size = parent.getSize();

			Dimension _topToolbars = (topToolbars == null
				? new Dimension(0,0)
				: topToolbars.getPreferredSize());
			Dimension _bottomToolbars = (bottomToolbars == null
				? new Dimension(0,0)
				: bottomToolbars.getPreferredSize());

			int topButtonHeight = -1;
			int bottomButtonHeight = -1;
			int leftButtonWidth = -1;
			int rightButtonWidth = -1;

			Dimension _top = top.getPreferredSize();
			Dimension _left = left.getPreferredSize();
			Dimension _bottom = bottom.getPreferredSize();
			Dimension _right = right.getPreferredSize();

			int topHeight = _top.height;
			int bottomHeight = _bottom.height;
			int leftWidth = _left.width;
			int rightWidth = _right.width;

			boolean topEmpty = ((Container)topButtons)
				.getComponentCount() <= 2;
			boolean leftEmpty = ((Container)leftButtons)
				.getComponentCount() <= 2;
			boolean bottomEmpty = ((Container)bottomButtons)
				.getComponentCount() <= 2;
			boolean rightEmpty = ((Container)rightButtons)
				.getComponentCount() <= 2;

			Dimension closeBoxSize;
			if(((Container)topButtons).getComponentCount() == 0)
				closeBoxSize = new Dimension(0,0);
			else
			{
				closeBoxSize = ((Container)topButtons)
					.getComponent(0).getPreferredSize();
			}

			int closeBoxWidth = Math.max(closeBoxSize.width,
				closeBoxSize.height) + 1;

			if(alternateLayout)
			{
				//{{{ Lay out independent buttons
				int _width = size.width;

				int padding = (leftEmpty&&rightEmpty)
					? 0 : closeBoxWidth;

				topButtonHeight = DockableWindowManager.this.
					top.getWrappedDimension(_width
					- closeBoxWidth * 2);
				topButtons.setBounds(
					padding,
					0,
					size.width - padding * 2,
					topButtonHeight);

				bottomButtonHeight = DockableWindowManager.this.
					bottom.getWrappedDimension(_width);
				bottomButtons.setBounds(
					padding,
					size.height - bottomButtonHeight,
					size.width - padding * 2,
					bottomButtonHeight);

				int _height = size.height
					- topButtonHeight
					- bottomButtonHeight;
				//}}}

				//{{{ Lay out dependent buttons
				leftButtonWidth = DockableWindowManager.this.
					left.getWrappedDimension(_height);
				leftButtons.setBounds(
					0,
					topHeight + topButtonHeight,
					leftButtonWidth,
					_height - topHeight - bottomHeight);

				rightButtonWidth = DockableWindowManager.this.
					right.getWrappedDimension(_height);
				rightButtons.setBounds(
					size.width - rightButtonWidth,
					topHeight + topButtonHeight,
					rightButtonWidth,
					_height - topHeight - bottomHeight);
				//}}}

				int[] dimensions = adjustDockingAreasToFit(
					size,
					topHeight,
					leftWidth,
					bottomHeight,
					rightWidth,
					topButtonHeight,
					leftButtonWidth,
					bottomButtonHeight,
					rightButtonWidth,
					_topToolbars,
					_bottomToolbars);

				topHeight = dimensions[0];
				leftWidth = dimensions[1];
				bottomHeight = dimensions[2];
				rightWidth = dimensions[3];

				//{{{ Lay out docking areas
				top.setBounds(
					0,
					topButtonHeight,
					size.width,
					topHeight);

				bottom.setBounds(
					0,
					size.height
					- bottomHeight
					- bottomButtonHeight,
					size.width,
					bottomHeight);

				left.setBounds(
					leftButtonWidth,
					topButtonHeight + topHeight,
					leftWidth,
					_height - topHeight - bottomHeight);

				right.setBounds(
					_width - rightButtonWidth - rightWidth,
					topButtonHeight + topHeight,
					rightWidth,
					_height - topHeight - bottomHeight); //}}}
			}
			else
			{
				//{{{ Lay out independent buttons
				int _height = size.height;

				int padding = (topEmpty && bottomEmpty
					? 0 : closeBoxWidth);

				leftButtonWidth = DockableWindowManager.this.
					left.getWrappedDimension(_height
					- closeBoxWidth * 2);
				leftButtons.setBounds(
					0,
					padding,
					leftButtonWidth,
					_height - padding * 2);

				rightButtonWidth = DockableWindowManager.this.
					right.getWrappedDimension(_height);
				rightButtons.setBounds(
					size.width - rightButtonWidth,
					padding,
					rightButtonWidth,
					_height - padding * 2);

				int _width = size.width
					- leftButtonWidth
					- rightButtonWidth;
				//}}}

				//{{{ Lay out dependent buttons
				topButtonHeight = DockableWindowManager.this.
					top.getWrappedDimension(_width);
				topButtons.setBounds(
					leftButtonWidth + leftWidth,
					0,
					_width - leftWidth - rightWidth,
					topButtonHeight);

				bottomButtonHeight = DockableWindowManager.this.
					bottom.getWrappedDimension(_width);
				bottomButtons.setBounds(
					leftButtonWidth + leftWidth,
					_height - bottomButtonHeight,
					_width - leftWidth - rightWidth,
					bottomButtonHeight); //}}}

				int[] dimensions = adjustDockingAreasToFit(
					size,
					topHeight,
					leftWidth,
					bottomHeight,
					rightWidth,
					topButtonHeight,
					leftButtonWidth,
					bottomButtonHeight,
					rightButtonWidth,
					_topToolbars,
					_bottomToolbars);

				topHeight = dimensions[0];
				leftWidth = dimensions[1];
				bottomHeight = dimensions[2];
				rightWidth = dimensions[3];

				//{{{ Lay out docking areas
				top.setBounds(
					leftButtonWidth + leftWidth,
					topButtonHeight,
					_width - leftWidth - rightWidth,
					topHeight);

				bottom.setBounds(
					leftButtonWidth + leftWidth,
					size.height - bottomHeight - bottomButtonHeight,
					_width - leftWidth - rightWidth,
					bottomHeight);

				left.setBounds(
					leftButtonWidth,
					0,
					leftWidth,
					_height);

				right.setBounds(
					size.width - rightWidth - rightButtonWidth,
					0,
					rightWidth,
					_height); //}}}
			}

			//{{{ Position tool bars if they are managed by us
			if(topToolbars != null)
			{
				topToolbars.setBounds(
					leftButtonWidth + leftWidth,
					topButtonHeight + topHeight,
					size.width - leftWidth - rightWidth
					- leftButtonWidth - rightButtonWidth,
					_topToolbars.height);
			}

			if(bottomToolbars != null)
			{
				bottomToolbars.setBounds(
					leftButtonWidth + leftWidth,
					size.height - bottomHeight
					- bottomButtonHeight
					- _bottomToolbars.height
					+ topButtonHeight
					+ topHeight,
					size.width - leftWidth - rightWidth
					- leftButtonWidth - rightButtonWidth,
					_bottomToolbars.height);
			} //}}}

			//{{{ Position center (edit pane, or split pane)
			if(center != null)
			{
				center.setBounds(
					leftButtonWidth + leftWidth,
					topButtonHeight + topHeight
					+ _topToolbars.height,
					size.width
					- leftWidth
					- rightWidth
					- leftButtonWidth
					- rightButtonWidth,
					size.height
					- topHeight
					- topButtonHeight
					- bottomHeight
					- bottomButtonHeight
					- _topToolbars.height
					- _bottomToolbars.height);
			} //}}}
		} //}}}

		//{{{ adjustDockingAreasToFit() method
		private int[] adjustDockingAreasToFit(
			Dimension size,
			int topHeight,
			int leftWidth,
			int bottomHeight,
			int rightWidth,
			int topButtonHeight,
			int leftButtonWidth,
			int bottomButtonHeight,
			int rightButtonWidth,
			Dimension _topToolbars,
			Dimension _bottomToolbars)
		{
			int maxTopHeight = size.height - bottomHeight
				- topButtonHeight - bottomButtonHeight
				- _topToolbars.height - _bottomToolbars.height;
			topHeight = Math.min(Math.max(0,maxTopHeight),
				topHeight);
			leftWidth = Math.min(Math.max(0,
				size.width - leftButtonWidth
				- rightButtonWidth - rightWidth),leftWidth);
			int maxBottomHeight = size.height - topHeight
				- topButtonHeight - bottomButtonHeight
				- _topToolbars.height - _bottomToolbars.height;
			bottomHeight = Math.min(Math.max(0,maxBottomHeight),
				bottomHeight);
			rightWidth = Math.min(Math.max(0,
				size.width - leftButtonWidth
				- rightButtonWidth - leftWidth),rightWidth);

			DockableWindowManager.this.top.setDimension(topHeight);
			DockableWindowManager.this.left.setDimension(leftWidth);
			DockableWindowManager.this.bottom.setDimension(bottomHeight);
			DockableWindowManager.this.right.setDimension(rightWidth);

			return new int[] {
				topHeight,
				leftWidth,
				bottomHeight,
				rightWidth
			};
		} //}}}

		//{{{ getLayoutAlignmentX() method
		public float getLayoutAlignmentX(Container target)
		{
			return 0.5f;
		} //}}}

		//{{{ getLayoutAlignmentY() method
		public float getLayoutAlignmentY(Container target)
		{
			return 0.5f;
		} //}}}

		//{{{ invalidateLayout() method
		public void invalidateLayout(Container target) {}
		//}}}
	} //}}}

	//{{{ Entry class
	class Entry
	{
		Factory factory;

		String title;
		String position;
		DockableWindowContainer container;

		// only set if open
		JComponent win;

		// only for docked
		AbstractButton btn;

		//{{{ Entry constructor
		Entry(Factory factory)
		{
			this(factory,jEdit.getProperty(factory.name
				+ ".dock-position",FLOATING));
		} //}}}

		//{{{ Entry constructor
		Entry(Factory factory, String position)
		{
			this.factory = factory;
			this.position = position;

			// get the title here, not in the factory constructor,
			// since the factory might be created before a plugin's
			// props are loaded
			title = getDockableTitle(factory.name);
		} //}}}
	} //}}}
}
