/*
 * GUIUtilities.java - Various GUI utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2004 Slava Pestov
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

package org.gjt.sp.jedit;

//{{{ Imports
import org.gjt.sp.jedit.browser.VFSFileChooserDialog;
import org.gjt.sp.jedit.gui.DynamicContextMenuService;
import org.gjt.sp.jedit.gui.EnhancedButton;
import org.gjt.sp.jedit.gui.FloatingWindowContainer;
import org.gjt.sp.jedit.gui.SplashScreen;
import org.gjt.sp.jedit.gui.VariableGridLayout;
import org.gjt.sp.jedit.keymap.Keymap;
import org.gjt.sp.jedit.menu.EnhancedCheckBoxMenuItem;
import org.gjt.sp.jedit.menu.EnhancedMenu;
import org.gjt.sp.jedit.menu.EnhancedMenuItem;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaMouseHandler;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.SyntaxUtilities;


import java.net.URL;
import java.util.*;
import java.util.List;
import java.lang.ref.SoftReference;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;


import java.awt.*;
import java.awt.event.*;
//}}}

/**
 * Various GUI functions.<p>
 *
 * The most frequently used members of this class are:
 *
 * <ul>
 * <li>{@link #loadIcon(String)}</li>
 * <li>{@link #confirm(Component,String,Object[],int,int)}</li>
 * <li>{@link #error(Component,String,Object[])}</li>
 * <li>{@link #message(Component,String,Object[])}</li>

 * <li>{@link #showVFSFileDialog(View,String,int,boolean)}</li>
 * <li>{@link #loadGeometry(Window,String)}</li>
 * <li>{@link #saveGeometry(Window,String)}</li>
 * <li>{@link #showPopupMenu(JPopupMenu,Component,int,int)}</li>
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class GUIUtilities
{
	//{{{ Icon methods

	//{{{ setIconPath() method
	/**
	 * Sets the path where jEdit looks for icons.
	 * @since jEdit 4.2pre5
	 */
	public static void setIconPath(String iconPath)
	{
		GUIUtilities.iconPath = iconPath;
		iconCache = null;
	} //}}}

	//{{{ loadIcon() method
	/**
	 * Loads an icon.
	 * @param iconName The icon name
	 * @since jEdit 2.6pre7
	 */
	public static Icon loadIcon(String iconName)
	{
		if(iconName == null)
			return null;

		// * Enable old icon naming scheme support
		if(deprecatedIcons != null && deprecatedIcons.containsKey(iconName))
			iconName = deprecatedIcons.get(iconName);

		// check if there is a cached version first
		Map<String, Icon> cache = null;
		if(iconCache != null)
		{
			cache = iconCache.get();
		}
		if(cache == null)
		{
			cache = new Hashtable<String, Icon>();
			iconCache = new SoftReference<Map<String, Icon>>(cache);
		}
		Icon icon = cache.get(iconName);
		if(icon != null)
			return icon;

		URL url;

		try
		{
			// get the icon
			if(MiscUtilities.isURL(iconName))
				url = new URL(iconName);
			else
				url = new URL(iconPath + iconName);
		}
		catch(Exception e)
		{
			try
			{
				url = new URL(defaultIconPath + iconName);
			}
			catch(Exception ex)
			{
				Log.log(Log.ERROR,GUIUtilities.class,
					"Icon not found: " + iconName);
				Log.log(Log.ERROR,GUIUtilities.class,ex);
				return null;
			}
		}

		icon = new ImageIcon(url);

		cache.put(iconName,icon);
		return icon;
	} //}}}

	//{{{ getEditorIcon() method
	/**
	 * Returns the default editor window image.
	 */
	public static Image getEditorIcon()
	{
		return ((ImageIcon)loadIcon(jEdit.getProperty("logo.icon.medium"))).getImage();
	} //}}}

	//{{{ getPluginIcon() method
	/**
	 * Returns the default plugin window image.
	 */
	public static Image getPluginIcon()
	{
		return getEditorIcon();
	} //}}}

	//}}}

	//{{{ Menus, tool bars

	//{{{ loadMenuBar() method
	/**
	 * Creates a menubar. Plugins should not need to call this method.
	 * @param name The menu bar name
	 * @since jEdit 3.2pre5
	 */
	public static JMenuBar loadMenuBar(String name)
	{
		return loadMenuBar(jEdit.getActionContext(),name);
	} //}}}

	//{{{ loadMenuBar() method
	/**
	 * Creates a menubar. Plugins should not need to call this method.
	 * @param context An action context
	 * @param name The menu bar name
	 * @since jEdit 4.2pre1
	 */
	public static JMenuBar loadMenuBar(ActionContext context, String name)
	{
		String menus = jEdit.getProperty(name);
		StringTokenizer st = new StringTokenizer(menus);

		JMenuBar mbar = new JMenuBar();

		while(st.hasMoreTokens())
		{
			String menuName = st.nextToken();
			mbar.add(loadMenu(context, menuName));
		}

		return mbar;
	} //}}}

	//{{{ loadMenu() method
	/**
	 * Creates a menu. The menu label is set from the
	 * <code><i>name</i>.label</code> property. The menu contents is taken
	 * from the <code><i>name</i></code> property, which is a whitespace
	 * separated list of action names. An action name of <code>-</code>
	 * inserts a separator in the menu.
	 * @param name The menu name
	 * @see #loadMenuItem(String)
	 * @since jEdit 2.6pre2
	 */
	public static JMenu loadMenu(String name)
	{
		return loadMenu(jEdit.getActionContext(),name);
	} //}}}

	//{{{ loadMenu() method
	/**
	 * Creates a menu. The menu label is set from the
	 * <code><i>name</i>.label</code> property. The menu contents is taken
	 * from the <code><i>name</i></code> property, which is a whitespace
	 * separated list of action names. An action name of <code>-</code>
	 * inserts a separator in the menu.
	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The menu name
	 * @see #loadMenuItem(String)
	 * @since jEdit 4.2pre1
	 */
	public static JMenu loadMenu(ActionContext context, String name)
	{
		return new EnhancedMenu(name,
			jEdit.getProperty(name.concat(".label")),
			context);
	} //}}}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.
	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JPopupMenu loadPopupMenu(String name, JEditTextArea textArea, MouseEvent evt)
	{
		return loadPopupMenu(jEdit.getActionContext(), name, textArea, evt);
	} //}}}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.

	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JPopupMenu loadPopupMenu(String name)
	{
		return loadPopupMenu(jEdit.getActionContext(),name);
	} //}}}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.

	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The menu name
	 * @since jEdit 4.2pre1
	 */
	public static JPopupMenu loadPopupMenu(ActionContext context, String name)
	{
		return loadPopupMenu(context, name, null, null);
	}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.
	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The menu name
	 * @param textArea the textArea wanting to show the popup.
	 * 	If not null, include context menu items defined by services.
	 * @param evt additional context info about where the mouse was when menu was requested
	 * @since jEdit 4.3pre15
	 */
	public static JPopupMenu loadPopupMenu(ActionContext context, String name, JEditTextArea textArea, MouseEvent evt)
	{
		JPopupMenu menu = new JPopupMenu();

		String menuItems = jEdit.getProperty(name);
		if(menuItems != null)
		{
			StringTokenizer st = new StringTokenizer(menuItems);
			while(st.hasMoreTokens())
			{
				String menuItemName = st.nextToken();
				if("-".equals(menuItemName))
					menu.addSeparator();
				else
					menu.add(loadMenuItem(context,menuItemName,false));
			}
		}
		// load menu items defined by services
		if (textArea != null)
		{
			List<JMenuItem> list = GUIUtilities.getServiceContextMenuItems(textArea, evt);
			if (!list.isEmpty())
			{
				menu.addSeparator();
			}
			for (JMenuItem mi : list)
			{
				menu.add(mi);
			}
		}

		return menu;
	} //}}}
	//{{{ addServiceContextMenuItems() method
	/**
	 * @return a list of menu items defined by services.
	 *
	 * @param textArea the TextArea desiring to display these menu items
	 * @since jEdit 4.3pre15
	 */
	public static List<JMenuItem> getServiceContextMenuItems(JEditTextArea textArea, MouseEvent evt)
	{
		List<JMenuItem> list = new ArrayList<JMenuItem>();
		String serviceClassName =  DynamicContextMenuService.class.getName();
		String[] menuServiceList = ServiceManager.getServiceNames(serviceClassName);
		for (String menuServiceName : menuServiceList)
		{
			if (menuServiceName != null && menuServiceName.trim().length() > 0)
			{
				DynamicContextMenuService dcms = (DynamicContextMenuService)
						ServiceManager.getService(serviceClassName, menuServiceName);
				if (dcms != null)
				{
					JMenuItem[] items = dcms.createMenu(textArea, evt);
					if (items != null)
					{
						list.addAll(Arrays.asList(items));
					}
				}
			}
		}
		return list;
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item. The menu item is bound to the action named by
	 * <code>name</code> with label taken from the return value of the
	 * {@link EditAction#getLabel()} method.
	 *
	 * @param name The menu item name
	 * @see #loadMenu(String)
	 * @since jEdit 2.6pre1
	 */
	public static JMenuItem loadMenuItem(String name)
	{
		return loadMenuItem(jEdit.getActionContext(),name,true);
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item.
	 * @param name The menu item name
	 * @param setMnemonic True if the menu item should have a mnemonic
	 * @since jEdit 3.1pre1
	 */
	public static JMenuItem loadMenuItem(String name, boolean setMnemonic)
	{
		return loadMenuItem(jEdit.getActionContext(),name,setMnemonic);
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item.
	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The menu item name
	 * @param setMnemonic True if the menu item should have a mnemonic
	 * @since jEdit 4.2pre1
	 */
	public static JMenuItem loadMenuItem(ActionContext context, String name,
		boolean setMnemonic)
	{
		if(name.charAt(0) == '%')
			return loadMenu(context,name.substring(1));

		return _loadMenuItem(name, context, setMnemonic);
	} //}}}

	//{{{ loadMenuItem(EditAction, boolean)
	public static JMenuItem loadMenuItem(EditAction editAction,
		boolean setMnemonic)
	{
		String name = editAction.getName();
		ActionContext context = jEdit.getActionContext();

		return _loadMenuItem(name, context, setMnemonic);
	} //}}}

	//{{{ loadToolBar() method
	/**
	 * Creates a toolbar.
	 * @param name The toolbar name
	 * @since jEdit 4.2pre2
	 */
	public static Container loadToolBar(String name)
	{
		return loadToolBar(jEdit.getActionContext(),name);
	} //}}}

	//{{{ loadToolBar() method
	/**
	 * Creates a toolbar.
	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The toolbar name
	 * @since jEdit 4.2pre2
	 */
	public static Container loadToolBar(ActionContext context, String name)
	{
		JToolBar toolB = new JToolBar();
		toolB.setName(name);
		toolB.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		toolB.setFloatable(jEdit.getBooleanProperty("view.toolbar.floatable"));

		String buttons = jEdit.getProperty(name);
		if(buttons != null)
		{
			StringTokenizer st = new StringTokenizer(buttons);
			while(st.hasMoreTokens())
			{
				String button = st.nextToken();
				if("-".equals(button))
				{
					toolB.addSeparator(new Dimension(12,12));
				}
				else
				{
					JButton b = loadToolButton(context,button);
					if(b != null)
						toolB.add(b);
				}
			}
		}
		toolB.addSeparator(new Dimension(12,12));
		return toolB;
	} //}}}

	//{{{ loadToolButton() method
	/**
	 * Loads a tool bar button. The tooltip is constructed from
	 * the <code><i>name</i>.label</code> and
	 * <code><i>name</i>.shortcut</code> properties and the icon is loaded
	 * from the resource named '/org/gjt/sp/jedit/icons/' suffixed
	 * with the value of the <code><i>name</i>.icon</code> property.
	 * @param name The name of the button
	 */
	public static EnhancedButton loadToolButton(String name)
	{
		return loadToolButton(jEdit.getActionContext(),name);
	} //}}}

	//{{{ loadToolButton() method
	/**
	 * Loads a tool bar button. The tooltip is constructed from
	 * the <code><i>name</i>.label</code> and
	 * <code><i>name</i>.shortcut</code> properties and the icon is loaded
	 * from the resource named '/org/gjt/sp/jedit/icons/' suffixed
	 * with the value of the <code><i>name</i>.icon</code> property.
	 * @param context An action context; either
	 * <code>jEdit.getActionContext()</code> or
	 * <code>VFSBrowser.getActionContext()</code>.
	 * @param name The name of the button
	 * @since jEdit 4.2pre1
	 */
	public static EnhancedButton loadToolButton(ActionContext context,
		String name)
	{
		String label = jEdit.getProperty(name + ".label");

		if(label == null)
			label = name;

		Icon icon;
		String iconName = jEdit.getProperty(name + ".icon");
		if(iconName == null)
			icon = loadIcon(jEdit.getProperty("broken-image.icon"));
		else
		{
			icon = loadIcon(iconName);
			if(icon == null)
				icon = loadIcon(jEdit.getProperty("broken-image.icon"));
		}

		String toolTip = prettifyMenuLabel(label);
		String shortcutLabel = getShortcutLabel(name);
		if(shortcutLabel != null)
		{
			toolTip = toolTip + " (" + shortcutLabel + ')';
		}

		EnhancedButton b = new EnhancedButton(icon,toolTip,name,context);
		b.setPreferredSize(new Dimension(32,32));
		return b;
	} //}}}

	//{{{ prettifyMenuLabel() method
	/**
	 * `Prettifies' a menu item label by removing the `$' sign. This
	 * can be used to process the contents of an <i>action</i>.label
	 * property.
	 */
	public static String prettifyMenuLabel(String label)
	{
		int index = label.indexOf('$');
		if(index != -1)
		{
			label = label.substring(0,index)
				.concat(label.substring(index + 1));
		}
		return label;
	} //}}}

	//{{{ getPlatformShortcutLabel() method
	/**
	* Translates a shortcut description string (e.g. "CS+SEMICOLON") to
	* a platform-localized description.  On OS X this puts in the pretty
	* unicode characters for Shift, Cmd, etc.
	*/
	public static String getPlatformShortcutLabel(String label)
	{
		if( !OperatingSystem.isMacOSLF() || label == null || label.length() == 0)
			return label;

		String[] strokes = label.split(" +");
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < strokes.length; i++)
		{
			if (i > 0)
				out.append(' ');
			out.append(getMacShortcutLabel(strokes[i]));
		}
		
		return out.toString();
        } //}}}

	//{{{ getShortcutLabel() method
	/**
	 * Returns a label string to show users what shortcut are
	 * assigned to the action.
	 */
	public static String getShortcutLabel(String action)
	{
		if(action == null)
			return null;
		else
		{
			Keymap keymap = jEdit.getKeymapManager().getKeymap();
			String shortcut1 = keymap.getShortcut(action + ".shortcut");
			String shortcut2 = keymap.getShortcut(action + ".shortcut2");

			shortcut1 = getPlatformShortcutLabel(shortcut1);
			shortcut2 = getPlatformShortcutLabel(shortcut2);

			if(shortcut1 == null || shortcut1.length() == 0)
			{
				if(shortcut2 == null || shortcut2.length() == 0)
					return null;
				else
					return shortcut2;
			}
			else
			{
				if(shortcut2 == null || shortcut2.length() == 0)
					return shortcut1;
				else
					return shortcut1 + " or " + shortcut2;
			}
		}
	} //}}}

	//}}}

	//}}}

	//{{{ Canned dialog boxes

	//{{{ message() method
	/**
	 * Displays a dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void message(final Component comp, final String name, final Object[] args)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();
	
			JOptionPane.showMessageDialog(comp,
				jEdit.getProperty(name.concat(".message"),args),
				jEdit.getProperty(name.concat(".title"),args),
				JOptionPane.INFORMATION_MESSAGE);
		}
                else 
                {
                        try
                        {
                            EventQueue.invokeAndWait(new Runnable()
                            {
                                    @Override
                                    public void run()
                                    {
                                            message(comp, name, args);
                                    }
                            });
                        }
                        catch (Exception e)
                        {
                            // ignored
                        }
                }
	} //}}}

	//{{{ error() method
	/**
	 * Displays an error dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void error(final Component comp, final String name, final Object[] args)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();
	
			JOptionPane.showMessageDialog(comp,
				jEdit.getProperty(name.concat(".message"),args),
				jEdit.getProperty(name.concat(".title"),args),
				JOptionPane.ERROR_MESSAGE);
		}
                else 
                {
                        try
                        {
                                EventQueue.invokeAndWait(new Runnable()
                                {
                                        @Override
                                        public void run()
                                        {
                                                error(comp, name, args);
                                        }
                                });
                        }
                        catch (Exception e)
                        {
                                // ignored
                        }
                }
	} //}}}

	//{{{ input() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The text to display by default in the input field
	 */
	public static String input(Component comp, String name, Object def)
	{
		return input(comp,name,null,def);
	} //}}}

	//{{{ inputProperty() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The property whose text to display in the input field
	 */
	public static String inputProperty(Component comp, String name,
		String def)
	{
		return inputProperty(comp,name,null,def);
	} //}}}

	//{{{ input() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The text to display by default in the input field
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @since jEdit 3.1pre3
	 */
	public static String input(final Component comp, final String name,
		final Object[] args, final Object def)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();
	
			return (String)JOptionPane.showInputDialog(comp,
				jEdit.getProperty(name.concat(".message"),args),
				jEdit.getProperty(name.concat(".title")),
				JOptionPane.QUESTION_MESSAGE,null,null,def);
		}
		final String[] retValue = new String[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = input(comp, name, args, def);
				}
			});
		}
		catch (Exception e)
		{
			return null;
		}
		return retValue[0];
		
	} //}}}

	//{{{ inputProperty() method
	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @param def The property whose text to display in the input field
	 * @since jEdit 3.1pre3
	 */
	public static String inputProperty(final Component comp, final String name,
		final Object[] args, final String def)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();
	
			String retVal = (String)JOptionPane.showInputDialog(comp,
				jEdit.getProperty(name.concat(".message"),args),
				jEdit.getProperty(name.concat(".title")),
				JOptionPane.QUESTION_MESSAGE,
				null,null,jEdit.getProperty(def));
			if(retVal != null)
				jEdit.setProperty(def,retVal);
			return retVal;
		}
		final String[] retValue = new String[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = inputProperty(comp, name, args, def);
				}
			});
		}
		catch (Exception e)
		{
			return null;
		}
		return retValue[0];
		
	} //}}}

	//{{{ confirm() method
	/**
	 * Displays a confirm dialog box and returns the button pushed by the
	 * user. The title of the dialog is fetched from the
	 * <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @param buttons The buttons to display - for example,
	 * JOptionPane.YES_NO_CANCEL_OPTION
	 * @param type The dialog type - for example,
	 * JOptionPane.WARNING_MESSAGE
	 * @since jEdit 3.1pre3
	 */
	public static int confirm(final Component comp, final String name,
		final Object[] args, final int buttons, final int type)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();
	
			return JOptionPane.showConfirmDialog(comp,
				jEdit.getProperty(name + ".message",args),
				jEdit.getProperty(name + ".title"),buttons,type);
		}
		final int [] retValue = new int[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = confirm(comp, name, args, buttons, type);
				}
			});
		}
		catch (Exception e)
		{
			return JOptionPane.CANCEL_OPTION;
		}
		return retValue[0];
	} //}}}

	//{{{ option() method
	 /**
	 * Displays an option dialog dialog box and returns the button pushed by the
	 * user. The title of the dialog is fetched from the
	 * <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 * @param type The dialog type - for example,
	 * JOptionPane.WARNING_MESSAGE
	 * @param options the buttons
	 * @param initialValue the initial value
	 * @since jEdit 4.5pre1
	 */
	public static int option(final Component comp, final String name,
		final Object[] args, final int type,
		final Object[] options, final Object initialValue)
	{
		if (EventQueue.isDispatchThread())
		{
			hideSplashScreen();

			return JOptionPane.showOptionDialog(comp,
				jEdit.getProperty(name + ".message",args),
				jEdit.getProperty(name + ".title"),JOptionPane.DEFAULT_OPTION, type,
				null, options, initialValue);
		}
		final int[] retValue = new int[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = option(comp, name, args, type, options, initialValue);
				}
			});
		}
		catch (Exception e)
		{
			return 0;
		}
		return retValue[0];
	} //}}}

	//{{{ listConfirm() method
	/**
	 * Displays a confirm dialog box and returns the button pushed by the
	 * user. The title of the dialog is fetched from the
	 * <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The dialog
	 * also shows a list of entries given by the <code>listModel</code>
	 * parameter.
	 * @param comp the parent component
	 * @param name the name of the confirm dialog
	 * @param args the for the message
	 * @param listModel the items in the list
	 * @return an integer indicating the option selected by the user
	 * @since jEdit 4.3pre1
	 */
	public static int listConfirm(final Component comp, final String name, final String[] args,
		final Object[] listModel)
	{
		if (EventQueue.isDispatchThread())
		{
			JList list = new JList(listModel);
			list.setVisibleRowCount(8);
	
			Object[] message = {
				jEdit.getProperty(name + ".message",args),
				new JScrollPane(list)
			};
	
			return JOptionPane.showConfirmDialog(comp,
				message,
				jEdit.getProperty(name + ".title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		}
		final int [] retValue = new int[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = listConfirm(comp, name, args, listModel);
				}
			});
		}
		catch (Exception e)
		{
			return JOptionPane.CANCEL_OPTION;
		}
		return retValue[0];
		
	} //}}}

	//{{{ listConfirm() method
	/**
	 * Displays a confirm dialog box and returns the button pushed by the
	 * user. The title of the dialog is fetched from the
	 * <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The dialog
	 * also shows a list of entries given by the <code>listModel</code>
	 * parameter.
	 * @param comp the parent component
	 * @param name the name of the confirm dialog
	 * @param args the for the message
	 * @param listModel the items in the list
	 * @param selectedItems give an empty list, it will contains in return the selected items
	 * @return an integer indicating the option selected by the user
	 * @since jEdit 4.3pre12
	 */
	public static int listConfirm(final Component comp, final String name, final String[] args,
		final Object[] listModel, final List selectedItems)
	{
		
		if (EventQueue.isDispatchThread())
		{
			JList list = new JList(listModel);
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			list.setVisibleRowCount(8);
			list.addSelectionInterval(0,listModel.length - 1);
	
			Object[] message = {
				jEdit.getProperty(name + ".message",args),
				new JScrollPane(list)
			};
	
			int ret = JOptionPane.showConfirmDialog(comp,
								message,
								jEdit.getProperty(name + ".title"),
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);
			Object[] selectedValues = list.getSelectedValues();
			selectedItems.addAll(Arrays.asList(selectedValues));
			return ret;
		}
		final int [] retValue = new int[1];
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					retValue[0] = listConfirm(comp, name, args, listModel, selectedItems);
				}
			});
		}
		catch (Exception e)
		{
			return JOptionPane.CANCEL_OPTION;
		}
		return retValue[0];
		
	} //}}}

	//{{{ showVFSFileDialog() methods
	/**
	 * Displays a VFS file selection dialog box.
	 * @param view The view, should be non-null
	 * @param path The initial directory to display. May be null
	 * @param type The dialog type. One of
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#OPEN_DIALOG},
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#SAVE_DIALOG}, or
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#CHOOSE_DIRECTORY_DIALOG}.
	 * @param multipleSelection True if multiple selection should be allowed
	 * @return The selected file(s)
	 * @since jEdit 2.6pre2
	 */
	public static String[] showVFSFileDialog(View view, String path,
		int type, boolean multipleSelection)
	{
		// the view should not be null, but some plugins might do this
		if(view == null)
		{
			Log.log(Log.WARNING,GUIUtilities.class,
			"showVFSFileDialog(): given null view, assuming jEdit.getActiveView()");
			view = jEdit.getActiveView();
		}

		hideSplashScreen();

		VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(
			view,path,type,multipleSelection);
		return fileChooser.getSelectedFiles();
	}

	/**
	 * Displays a VFS file selection dialog box.
	 * This version can specify a dialog as the parent instead
	 * of the view.
	 * @param view The view, should be non-null
	 * @param path The initial directory to display. May be null
	 * @param type The dialog type. One of
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#OPEN_DIALOG},
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#SAVE_DIALOG}, or
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#CHOOSE_DIRECTORY_DIALOG}.
	 * @param multipleSelection True if multiple selection should be allowed
	 * @return The selected file(s)
	 * @since jEdit 4.3pre10
	 */
	public static String[] showVFSFileDialog(Dialog parent, View view,
		String path, int type, boolean multipleSelection)
	{
		hideSplashScreen();

		VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(
			parent, view, path, type, multipleSelection, true);
		return fileChooser.getSelectedFiles();
	}

	/**
	 * Displays a VFS file selection dialog box.
	 * This version can specify a frame as the parent instead
	 * of the view.
	 * @param parent The parent frame
	 * @param view The view, should be non-null
	 * @param path The initial directory to display. May be null
	 * @param type The dialog type. One of
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#OPEN_DIALOG},
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#SAVE_DIALOG}, or
	 * {@link org.gjt.sp.jedit.browser.VFSBrowser#CHOOSE_DIRECTORY_DIALOG}.
	 * @param multipleSelection True if multiple selection should be allowed
	 * @return The selected file(s)
	 * @since jEdit 4.3pre10
	 */
	public static String[] showVFSFileDialog(Frame parent, View view,
		String path, int type, boolean multipleSelection)
	{
		hideSplashScreen();
		VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(
			parent, view, path, type, multipleSelection, true);
		return fileChooser.getSelectedFiles();
	} //}}}

	//}}}

	//{{{ Colors and styles

	//{{{ parseColor() method
	/**
	 * Converts a color name to a color object. The name must either be
	 * a known string, such as `red', `green', etc (complete list is in
	 * the <code>java.awt.Color</code> class) or a hex color value
	 * prefixed with `#', for example `#ff0088'.
	 * @param name The color name
	 */
	public static Color parseColor(String name)
	{
		return SyntaxUtilities.parseColor(name, Color.black);
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 3.2pre6
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size)
		throws IllegalArgumentException
	{
		return SyntaxUtilities.parseStyle(str,family,size,true);
	} //}}}

	//{{{ getStyleString() method
	/**
	 * Converts a style into it's string representation.
	 * @param style The style
	 */
	public static String getStyleString(SyntaxStyle style)
	{
		StringBuilder buf = new StringBuilder();

		if (style.getForegroundColor() != null)
		{
			buf.append("color:").append(SyntaxUtilities.getColorHexString(style.getForegroundColor()));
		}

		if (style.getBackgroundColor() != null)
		{
			buf.append(" bgColor:").append(SyntaxUtilities.getColorHexString(style.getBackgroundColor()));
		}

		Font font = style.getFont();
		if (!font.isPlain())
		{
			buf.append(" style:");
			if (font.isItalic())
				buf.append('i');
			if (font.isBold())
				buf.append('b');
		}

		return buf.toString();
	} //}}}

	//}}}

	//{{{ Loading, saving window geometry

	//{{{ loadGeometry() method
	/**
	 * Loads a windows's geometry from the properties.
	 * The geometry is loaded from the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 *
	 * @param win The window to load geometry from
	 * @param parent The parent frame to be relative to.
	 * @param name The name of the window
	 */
	public static void loadGeometry(Window win, Container parent, String name )
	{
		Dimension size = win.getSize();
		int width = jEdit.getIntegerProperty(name + ".width", size.width);
		int height = jEdit.getIntegerProperty(name + ".height", size.height);
		int x = jEdit.getIntegerProperty(name + ".x",50);
		int y = jEdit.getIntegerProperty(name + ".y",50);
		if(parent != null)
		{
			Point location = parent.getLocation();
			x = location.x + x;
			y = location.y + y;
		}

		int extState = jEdit.getIntegerProperty(name + ".extendedState", Frame.NORMAL);

		Rectangle desired = new Rectangle(x,y,width,height);
		try
		{
			if(!Debug.DISABLE_MULTIHEAD)
				adjustForScreenBounds(desired);
		}
		catch(Exception e)
		{
			/* Workaround for OS X bug. */
			Log.log(Log.ERROR,GUIUtilities.class,e);
		}

		if(OperatingSystem.isX11() && Debug.GEOMETRY_WORKAROUND)
			new UnixWorkaround(win,name,desired,extState);
		else
		{
			win.setBounds(desired);
			if(win instanceof Frame)
				((Frame)win).setExtendedState(extState);
		}

	} //}}}

	//{{{ loadGeometry() method
	/**
	 * Loads a windows's geometry from the properties.
	 * The geometry is loaded from the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 *
	 * @param win The window to load geometry from
	 * @param name The name of the window
	 */
	public static void loadGeometry(Window win, String name)
	{
		loadGeometry(win, win.getParent(), name);
	} //}}}

	//{{{ adjustForScreenBounds() method
	/**
	 * Gives a rectangle the specified bounds, ensuring it is within the
	 * screen bounds.
	 * @since jEdit 4.2pre3
	 */
	public static void adjustForScreenBounds(Rectangle desired)
	{
		// Make sure the window is displayed in visible region
		Rectangle osbounds = OperatingSystem.getScreenBounds(desired);

		if (desired.width > osbounds.width)
		{
			desired.width = osbounds.width;
		}
		if (desired.x < osbounds.x)
		{
			desired.x = osbounds.x;
		}
		if (desired.x + desired.width > osbounds.x + osbounds.width)
		{
			desired.x = osbounds.x + osbounds.width - desired.width;
		}
		if (desired.height > osbounds.height)
		{
			desired.height = osbounds.height;
		}
		if (desired.y < osbounds.y)
		{
			desired.y = osbounds.y;
		}
		if (desired.y + desired.height > osbounds.y + osbounds.height)
		{
			desired.y = osbounds.y + osbounds.height - desired.height;
		}
	} //}}}

	//{{{ UnixWorkaround class
	public static class UnixWorkaround
	{
		Window win;
		String name;
		Rectangle desired;
		Rectangle required;
		long start;
		boolean windowOpened;

		//{{{ UnixWorkaround constructor
		public UnixWorkaround(Window win, String name, Rectangle desired,
			int extState)
		{
			this.win = win;
			this.name = name;
			this.desired = desired;

			int adjust_x = jEdit.getIntegerProperty(name + ".dx",0);
			int adjust_y = jEdit.getIntegerProperty(name + ".dy",0);
			int adjust_width = jEdit.getIntegerProperty(name + ".d-width",0);
			int adjust_height = jEdit.getIntegerProperty(name + ".d-height",0);

			required = new Rectangle(
				desired.x - adjust_x,
				desired.y - adjust_y,
				desired.width - adjust_width,
				desired.height - adjust_height);

			Log.log(Log.DEBUG,GUIUtilities.class,"Window " + name
				+ ": desired geometry is " + desired);
			Log.log(Log.DEBUG,GUIUtilities.class,"Window " + name
				+ ": setting geometry to " + required);

			start = System.currentTimeMillis();

			win.setBounds(required);
			if(win instanceof Frame)
				((Frame)win).setExtendedState(extState);

			win.addComponentListener(new ComponentHandler());
			win.addWindowListener(new WindowHandler());
		} //}}}

		//{{{ ComponentHandler class
		private class ComponentHandler extends ComponentAdapter
		{
			//{{{ componentMoved() method
			@Override
			public void componentMoved(ComponentEvent evt)
			{
				if(System.currentTimeMillis() - start < 1000L)
				{
					Rectangle r = win.getBounds();
					if(!windowOpened && r.equals(required))
						return;

					if(!r.equals(desired))
					{
						Log.log(Log.DEBUG,GUIUtilities.class,
							"Window resize blocked: " + win.getBounds());
						win.setBounds(desired);
					}
				}

				win.removeComponentListener(this);
			} //}}}

			//{{{ componentResized() method
			@Override
			public void componentResized(ComponentEvent evt)
			{
				if(System.currentTimeMillis() - start < 1000L)
				{
					Rectangle r = win.getBounds();
					if(!windowOpened && r.equals(required))
						return;

					if(!r.equals(desired))
					{
						Log.log(Log.DEBUG,GUIUtilities.class,
							"Window resize blocked: " + win.getBounds());
						win.setBounds(desired);
					}
				}

				win.removeComponentListener(this);
			} //}}}
		} //}}}

		//{{{ WindowHandler class
		private class WindowHandler extends WindowAdapter
		{
			//{{{ windowOpened() method
			@Override
			public void windowOpened(WindowEvent evt)
			{
				windowOpened = true;

				Rectangle r = win.getBounds();
				Log.log(Log.DEBUG,GUIUtilities.class,"Window "
					+ name + ": bounds after opening: " + r);

				jEdit.setIntegerProperty(name + ".dx",
					r.x - required.x);
				jEdit.setIntegerProperty(name + ".dy",
					r.y - required.y);
				jEdit.setIntegerProperty(name + ".d-width",
					r.width - required.width);
				jEdit.setIntegerProperty(name + ".d-height",
					r.height - required.height);

				win.removeWindowListener(this);
			} //}}}
		} //}}}
	} //}}}

	//{{{ saveGeometry() method
	/**
	 * Saves a window's geometry to the properties.
	 * The geometry is saved to the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.<br />
	 * For Frame's and descendents use {@link #addSizeSaver(Frame,String)} to save the sizes
	 * correct even if the Frame is in maximized or iconified state.
	 * @param win The window to load geometry from
	 * @param name The name of the window
	 * @see #addSizeSaver(Frame,String)
	 */
	public static void saveGeometry(Window win, String name)
	{
		saveGeometry (win, win.getParent(), name);
	} //}}}

	//{{{ saveGeometry() method
	/**
	 * Saves a window's geometry to the properties.
	 * The geometry is saved to the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.<br />
	 * For Frame's and descendents use {@link #addSizeSaver(Frame,Container,String)} to save the sizes
	 * correct even if the Frame is in maximized or iconified state.
	 * @param win The window to load geometry from
	 * @param parent The parent frame to be relative to.
	 * @param name The name of the window
	 * @see #addSizeSaver(Frame,Container,String)
	 */
	public static void saveGeometry(Window win, Container parent, String name)
	{
		if(win instanceof Frame)
		{
			jEdit.setIntegerProperty(name + ".extendedState",
				((Frame)win).getExtendedState());
		}

		Rectangle bounds = win.getBounds();
		int x = bounds.x;
		int y = bounds.y;
		if (parent != null)
		{
			Rectangle parentBounds = parent.getBounds();
			x -= parentBounds.x;
			y -= parentBounds.y;
		}
		jEdit.setIntegerProperty(name + ".x",x);
		jEdit.setIntegerProperty(name + ".y",y);
		jEdit.setIntegerProperty(name + ".width", bounds.width);
		jEdit.setIntegerProperty(name + ".height", bounds.height);
	} //}}}

	//}}}

	//{{{ hideSplashScreen() method
	/**
	 * Ensures that the splash screen is not visible. This should be
	 * called before displaying any dialog boxes or windows at
	 * startup.
	 */
	public static void hideSplashScreen()
	{
		if(splash != null)
		{
			splash.dispose();
			splash = null;
		}
	} //}}}

	//{{{ applyTextAreaColors() method
	/**
	 * experimental - applies the text area colors on a Component 
	 * (such as a dockable window) and its children. 
	 * @since jEdit 5.0pre1
	 * @author ezust
	 * 
	 */
	public static void applyTextAreaColors(Container win) 
	{		
		for (Component child: win.getComponents()) 
		{
			child.setBackground(jEdit.getColorProperty("view.bgColor", Color.WHITE));
			child.setForeground(jEdit.getColorProperty("view.fgColor", Color.BLACK));
			if (child instanceof JTextPane)  
				((JTextPane)child).setUI(new javax.swing.plaf.basic.BasicEditorPaneUI());
			if (child instanceof Container)
				applyTextAreaColors((Container)child);
		}
	} //}}}
	
	//{{{ createMultilineLabel() method
	/**
	 * Creates a component that displays a multiple line message. This
	 * is implemented by assembling a number of <code>JLabels</code> in
	 * a <code>JPanel</code>.
	 * @param str The string, with lines delimited by newline
	 * (<code>\n</code>) characters.
	 * @since jEdit 4.1pre3
	 */
	public static JComponent createMultilineLabel(String str)
	{
		JPanel panel = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,1,1,1));
		int lastOffset = 0;
		while(true)
		{
			int index = str.indexOf('\n',lastOffset);
			if(index == -1)
				break;
			else
			{
				panel.add(new JLabel(str.substring(lastOffset,index)));
				lastOffset = index + 1;
			}
		}

		if(lastOffset != str.length())
			panel.add(new JLabel(str.substring(lastOffset)));

		return panel;
	} //}}}

	//{{{ requestFocus() method
	/**
	 * Focuses on the specified component as soon as the window becomes
	 * active.
	 * @param win The window
	 * @param comp The component
	 */
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowFocusListener(new WindowAdapter()
		{
			@Override
			public void windowGainedFocus(WindowEvent evt)
			{
				EventQueue.invokeLater(new Runnable()
				{
						@Override
						public void run()
						{
							comp.requestFocusInWindow();
						}
				});
				win.removeWindowFocusListener(this);
			}
		});
	} //}}}

	//{{{ isPopupTrigger() method
	/**
	 * Returns if the specified event is the popup trigger event.
	 * This implements precisely defined behavior, as opposed to
	 * MouseEvent.isPopupTrigger().
	 * @param evt The event
	 * @since jEdit 3.2pre8
	 */
	public static boolean isPopupTrigger(MouseEvent evt)
	{
		return TextAreaMouseHandler.isRightButton(evt.getModifiers());
	} //}}}

	//{{{ isMiddleButton() method
	/**
	 * @param modifiers The modifiers flag from a mouse event
	 * @since jEdit 4.1pre9
	 */
	public static boolean isMiddleButton(int modifiers)
	{
		return TextAreaMouseHandler.isMiddleButton(modifiers);
	} //}}}

	//{{{ isRightButton() method
	/**
	 * @param modifiers The modifiers flag from a mouse event
	 * @since jEdit 4.1pre9
	 */
	public static boolean isRightButton(int modifiers)
	{
		return TextAreaMouseHandler.isRightButton(modifiers);
	} //}}}

	//{{{ getScreenBounds() method
	/**
	 * Returns the screen bounds, taking into account multi-screen
	 * environments.
	 * @since jEdit 4.3pre18
	 */
	public static Rectangle getScreenBounds()
	{
		Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().
			getMaximumWindowBounds();
		GraphicsDevice [] devices = GraphicsEnvironment.
			getLocalGraphicsEnvironment().getScreenDevices();
		if (devices.length > 1)
		{
			for (GraphicsDevice device: devices)
			{
				for (GraphicsConfiguration config: device.getConfigurations())
					bounds = bounds.union(config.getBounds());
			}
		}
		return bounds;
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @since jEdit 4.0pre1
	 * @see javax.swing.JComponent#setComponentPopupMenu(javax.swing.JPopupMenu) setComponentPopupMenu
	 * which works better and is simpler to use: you don't have to write the code to
	 * show/hide popups in response to mouse events anymore.
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y)
	{
		showPopupMenu(popup,comp,x,y,true);
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the specified popup menu, ensuring it is displayed within
	 * the bounds of the screen.
	 * @param popup The popup menu
	 * @param comp The component to show it for
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param point If true, then the popup originates from a single point;
	 * otherwise it will originate from the component itself. This affects
	 * positioning in the case where the popup does not fit onscreen.
	 *
	 * @since jEdit 4.1pre1
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y, boolean point)
	{
		int offsetX = 0;
		int offsetY = 0;

		int extraOffset = point ? 1 : 0;

		Component win = comp;
		while(!(win instanceof Window || win == null))
		{
			offsetX += win.getX();
			offsetY += win.getY();
			win = win.getParent();
		}

		if(win != null)
		{
			Dimension size = popup.getPreferredSize();

			Rectangle screenSize = getScreenBounds();

			if(x + offsetX + size.width + win.getX() > screenSize.width
				&& x + offsetX + win.getX() >= size.width)
			{
				//System.err.println("x overflow");
				if(point)
					x -= size.width + extraOffset;
				else
					x = win.getWidth() - size.width - offsetX + extraOffset;
			}
			else
			{
				x += extraOffset;
			}

			//System.err.println("y=" + y + ",offsetY=" + offsetY
			//	+ ",size.height=" + size.height
			//	+ ",win.height=" + win.getHeight());
			if(y + offsetY + size.height + win.getY() > screenSize.height
				&& y + offsetY + win.getY() >= size.height)
			{
				if(point)
					y = win.getHeight() - size.height - offsetY + extraOffset;
				else
					y = -size.height - 1;
			}
			else
			{
				y += extraOffset;
			}

			popup.show(comp,x,y);
		}
		else
			popup.show(comp,x + extraOffset,y + extraOffset);

	} //}}}

	//{{{ isAncestorOf() method
	/**
	 * Returns if the first component is an ancestor of the
	 * second by traversing up the component hierarchy.
	 *
	 * @param comp1 The ancestor
	 * @param comp2 The component to check
	 * @since jEdit 4.1pre5
	 */
	public static boolean isAncestorOf(Component comp1, Component comp2)
	{
		while(comp2 != null)
		{
			if(comp1 == comp2)
				return true;
			else
				comp2 = comp2.getParent();
		}

		return false;
	} //}}}

	//{{{ getParentDialog() method
	/**
	 * Traverses the given component's parent tree looking for an
	 * instance of JDialog, and return it. If not found, return null.
	 * @param c The component
	 */
	public static JDialog getParentDialog(Component c)
	{
		return (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, c);
	} //}}}

	//{{{ getComponentParent() method
	/**
	 * Finds a parent of the specified component.
	 * @param comp The component
	 * @param clazz Looks for a parent with this class (exact match, not
	 * derived).
	 * @since jEdit 4.2pre1
	 */
	public static Component getComponentParent(Component comp, Class clazz)
	{
		while(true)
		{
			if(comp == null)
				break;

			if(comp instanceof JComponent)
			{
				Component real = (Component)((JComponent)comp)
					.getClientProperty("KORTE_REAL_FRAME");
				if(real != null)
					comp = real;
			}

			if(comp.getClass().equals(clazz))
				return comp;
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else if(comp instanceof FloatingWindowContainer)
			{
				comp = ((FloatingWindowContainer)comp)
					.getDockableWindowManager();
			}
			else
				comp = comp.getParent();
		}
		return null;
	} //}}}

	//{{{ setEnabledRecursively() method
	/**
	 * Call setEnabled() recursively on the container and its descendants.
	 * @param c The container
	 * @param enabled The enabled state to set
	 * @since jEdit 4.3pre17
	 */
	public static void setEnabledRecursively(Container c, boolean enabled)
	{
		for (Component child: c.getComponents())
		{
			if (child instanceof Container)
				setEnabledRecursively((Container)child, enabled);
			else
				child.setEnabled(enabled);
		}
		c.setEnabled(enabled);
	} //}}}

	//{{{ getView() method
	/**
	 * Finds the view parent of the specified component.
	 * @param comp the component from which you want to get the parent view
	 * @return the parent view, or null if the component was not in a View.
	 * @since jEdit 4.0pre2
	 */
	public static View getView(Component comp)
	{
		return (View)getComponentParent(comp,View.class);
	} //}}}

	//{{{ addSizeSaver() method
	/**
	* Adds a SizeSaver to the specified Frame. For non-Frame's use {@link #saveGeometry(Window,String)}
	 *
	 * @param frame The Frame for which to save the size
	 * @param name The prefix for the settings
	 * @since jEdit 4.3pre6
	 * @see #saveGeometry(Window,String)
	 */
	public static void addSizeSaver(Frame frame, String name)
	{
		addSizeSaver(frame,frame.getParent(),name);
	} //}}}

	//{{{ addSizeSaver() method
	/**
	 * Adds a SizeSaver to the specified Frame. For non-Frame's use {@link #saveGeometry(Window,Container,String)}
	 *
	 * @param frame The Frame for which to save the size
	 * @param parent The parent to be relative to
	 * @param name The prefix for the settings
	 * @since jEdit 4.3pre7
	 * @see #saveGeometry(Window,Container,String)
	 */
	public static void addSizeSaver(Frame frame, Container parent, String name)
	{
		SizeSaver ss = new SizeSaver(frame,parent,name);
		frame.addWindowStateListener(ss);
		frame.addComponentListener(ss);
	} //}}}

	//{{{ initContinuousLayout() method
	/**
	 * This method do nothing.
	 *
	 * @param split the split. It must never be null
	 * @since jEdit 4.3pre9
	 * @deprecated since jEdit 5.0 using or not continuous layout is not anymore an option.
	 */
	@Deprecated
	public static void initContinuousLayout(JSplitPane split)
	{
	} //}}}

	//{{{ Package-private members

	//{{{ initializeDeprecatedIcons() method
	/**
	 * Initializes a list of mappings between old icon names and new names
	 */
	private static void initializeDeprecatedIcons()
	{
		deprecatedIcons.put("File.png",       "16x16/mimetypes/text-x-generic.png");
		deprecatedIcons.put("Folder.png",     "16x16/places/folder.png");
		deprecatedIcons.put("OpenFolder.png", "16x16/status/folder-open.png");
		deprecatedIcons.put("OpenFile.png",   "16x16/actions/edit-select-all.png");
		deprecatedIcons.put("ReloadSmall.png","16x16/actions/view-refresh.png");
		deprecatedIcons.put("DriveSmall.png", "16x16/devices/drive-harddisk.png");
		deprecatedIcons.put("New.png",        "22x22/actions/document-new.png");
		deprecatedIcons.put("NewDir.png",     "22x22/actions/folder-new.png");
		deprecatedIcons.put("Reload.png",     "22x22/actions/view-refresh.png");
		deprecatedIcons.put("Load.png",       "22x22/places/plugins.png");
		deprecatedIcons.put("Save.png",       "22x22/actions/document-save.png");
		deprecatedIcons.put("SaveAs.png",     "22x22/actions/document-save-as.png");
		deprecatedIcons.put("SaveAll.png",    "22x22/actions/document-save-all.png");
		deprecatedIcons.put("Open.png",       "22x22/actions/document-open.png");
		deprecatedIcons.put("Print.png",      "22x22/actions/document-print.png");
		deprecatedIcons.put("Drive.png",      "22x22/devices/drive-harddisk.png");
		deprecatedIcons.put("Clear.png",      "22x22/actions/edit-clear.png");
		deprecatedIcons.put("Run.png",        "22x22/actions/application-run.png");
		deprecatedIcons.put("RunAgain.png",   "22x22/actions/application-run-again.png");
		deprecatedIcons.put("RunToBuffer.png",  "22x22/actions/run-to-buffer.png");
		deprecatedIcons.put("CopyToBuffer.png", "22x22/actions/copy-to-buffer.png");
		deprecatedIcons.put("Plus.png",       "22x22/actions/list-add.png");
		deprecatedIcons.put("Minus.png",      "22x22/actions/list-remove.png");
		deprecatedIcons.put("Find.png",       "22x22/actions/edit-find.png");
		deprecatedIcons.put("FindAgain.png",  "22x22/actions/edit-find-next.png");
		deprecatedIcons.put("FindInDir.png",  "22x22/actions/edit-find-in-folder.png");
		deprecatedIcons.put("Parse.png",      "22x22/actions/document-reload2.png");
		deprecatedIcons.put("Delete.png",     "22x22/actions/edit-delete.png");
		deprecatedIcons.put("Paste.png",      "22x22/actions/edit-paste.png");
		deprecatedIcons.put("Cut.png",        "22x22/actions/edit-cut.png");
		deprecatedIcons.put("Copy.png",       "22x22/actions/edit-copy.png");
		deprecatedIcons.put("Undo.png",       "22x22/actions/edit-undo.png");
		deprecatedIcons.put("Redo.png",       "22x22/actions/edit-redo.png");
		deprecatedIcons.put("CurrentDir.png", "22x22/status/folder-visiting.png");
		deprecatedIcons.put("ParentDir.png",  "22x22/actions/go-parent.png");
		deprecatedIcons.put("PageSetup.png",  "22x22/actions/printer-setup.png");
		deprecatedIcons.put("Plugins.png",    "22x22/apps/system-installer.png");
		deprecatedIcons.put("Floppy.png",     "22x22/devices/media-floppy.png");
		deprecatedIcons.put("Stop.png",       "22x22/actions/process-stop.png");
		deprecatedIcons.put("Cancel.png",     "22x22/actions/process-stop.png");
		deprecatedIcons.put("Home.png",       "22x22/actions/go-home.png");
		deprecatedIcons.put("Help.png",       "22x22/apps/help-browser.png");
		deprecatedIcons.put("Properties.png", "22x22/actions/document-properties.png");
		deprecatedIcons.put("Preferences.png","22x22/categories/preferences-system.png");
		deprecatedIcons.put("ZoomIn.png",     "22x22/actions/zoom-in.png");
		deprecatedIcons.put("ZoomOut.png",    "22x22/actions/zoom-out.png");
		deprecatedIcons.put("BrokenImage.png","22x22/status/image-missing.png");
		deprecatedIcons.put("AdjustWidth.png","22x22/actions/resize-horisontal.png");
		deprecatedIcons.put("ToolbarMenu.gif","ToolbarMenu.gif");

		deprecatedIcons.put("Play.png","22x22/actions/media-playback-start.png");
		deprecatedIcons.put("Pause.png","22x22/actions/media-playback-pause.png");

		deprecatedIcons.put("MultipleResults.png", "22x22/actions/edit-find-multiple.png");
		deprecatedIcons.put("SingleResult.png",    "22x22/actions/edit-find-single.png");

		deprecatedIcons.put("NextFile.png",    "22x22/go-last.png");
		deprecatedIcons.put("PreviousFile.png","22x22/go-first.png");

		deprecatedIcons.put("closebox.gif",   "10x10/actions/close.png");
		deprecatedIcons.put("normal.gif",   "10x10/status/document-unmodified.png");
		deprecatedIcons.put("readonly.gif",   "10x10/emblem/emblem-readonly.png");
		deprecatedIcons.put("dirty.gif",    "10x10/status/document-modified.png");
		deprecatedIcons.put("new.gif",    "10x10/status/document-new.png");

		deprecatedIcons.put("ArrowU.png", "22x22/actions/go-up.png");
		deprecatedIcons.put("ArrowR.png", "22x22/actions/go-next.png");
		deprecatedIcons.put("ArrowD.png", "22x22/actions/go-down.png");
		deprecatedIcons.put("ArrowL.png", "22x22/actions/go-previous.png");
		deprecatedIcons.put("arrow1.png", "16x16/actions/group-expand.png");
		deprecatedIcons.put("arrow2.png", "16x16/actions/group-collapse.png");

		deprecatedIcons.put("NewView.png", "22x22/actions/window-new.png");
		deprecatedIcons.put("UnSplit.png", "22x22/actions/window-unsplit.png");
		deprecatedIcons.put("SplitVertical.png", "22x22/actions/window-split-vertical.png");
		deprecatedIcons.put("SplitHorizontal.png", "22x22/actions/window-split-horizontal.png");

		deprecatedIcons.put("ButtonProperties.png", "22x22/actions/document-properties.png");

	}
	//}}}

	//{{{ init() method
	static void init()
	{
		initializeDeprecatedIcons();

		// Load the icon theme but fallback on the old icons
		String theme = jEdit.getProperty("icon-theme", "tango");
		Log.log(Log.DEBUG, GUIUtilities.class, "Icon theme set to: "+theme);
		setIconPath("jeditresource:/org/gjt/sp/jedit/icons/themes/" + theme + '/');
		Log.log(Log.DEBUG, GUIUtilities.class, "Loading icon theme from: "+iconPath);
	} //}}}

	//{{{ showSplashScreen() method
	static void showSplashScreen()
	{
		splash = new SplashScreen();
	} //}}}

	//{{{ advanceSplashProgress() method
	static void advanceSplashProgress()
	{
		if(splash != null)
			splash.advance();
	} //}}}

	//{{{ advanceSplashProgress() method
	static void advanceSplashProgress(String label)
	{
		if(splash != null)
			splash.advance(label);
	} //}}}

	//}}}

	//{{{ Private members
	private static SplashScreen splash;
	private static SoftReference<Map<String, Icon>> iconCache;
	private static String iconPath = "jeditresource:/org/gjt/sp/jedit/icons/themes/";
	private static final String defaultIconPath = "jeditresource:/org/gjt/sp/jedit/icons/themes/";
	private static final HashMap<String, String> deprecatedIcons = new HashMap<String, String>();

	//{{{ _loadMenuItem() method
	private static JMenuItem _loadMenuItem(String name, ActionContext context, boolean setMnemonic)
	{

		String label = jEdit.getProperty(name + ".label", name);
		char mnemonic;
		int index = label.indexOf('$');
		if (index != -1 && label.length() - index > 1)
		{
			mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0, index).concat(label.substring(++index));
		}
		else
		{
			mnemonic = '\0';
		}
		JMenuItem mi;
		if (jEdit.getBooleanProperty(name + ".toggle"))
		{
			mi = new EnhancedCheckBoxMenuItem(label, name, context);
		}
		else
		{
			mi = new EnhancedMenuItem(label, name, context);
		}
		if (!OperatingSystem.isMacOS() && setMnemonic && mnemonic != '\0')
		{
			mi.setMnemonic(mnemonic);
		}
		Icon itemIcon = loadIcon(jEdit.getProperty(name + ".icon.small"));
		if(itemIcon != null)
		{
			mi.setIcon(itemIcon);
		}

		return mi;
	} //}}}

	private static HashMap<String, String> macKeySymbols = null;
	
	/*
	 * Create a list of unicode characters to be used in displaying keyboard shortcuts
	 * on Mac OS X.
	 */
	static
	{
		macKeySymbols = new HashMap<String, String>();
		
		// These are the unicode code points used in cocoa apps for displaying
		// shortcuts.
		macKeySymbols.put("ENTER",         "\u21A9");
		macKeySymbols.put("HOME",          "\u2196");
		macKeySymbols.put("END",           "\u2198");
		macKeySymbols.put("BACK_SPACE",    "\u232B");
		macKeySymbols.put("DELETE",        "\u2326");
		macKeySymbols.put("PAGE_UP",       "\u21DE");
		macKeySymbols.put("PAGE_DOWN",     "\u21DF");
		macKeySymbols.put("LEFT",          "\u2190");
		macKeySymbols.put("UP",            "\u2191");
		macKeySymbols.put("RIGHT",         "\u2192");
		macKeySymbols.put("DOWN",          "\u2193");
		macKeySymbols.put("ESCAPE",        "\u238B");
		macKeySymbols.put("TAB",           "\u21E5");
		macKeySymbols.put("SPACE",         "\u2423");
	}

	//{{{ getMacShortcutLabel() method
	/**
	 * Convert a shortcut label to a Mac-friendly version by changing written-out
	 * names and modifiers (e.g. C+PERIOD) to symbols.
	 */
	private static String getMacShortcutLabel(String label)
	{	
		StringBuilder out = new StringBuilder();
		
		int endOfModifiers = label.indexOf('+');
		for (int i = 0; i < endOfModifiers; i++)
		{
			char c = Character.toUpperCase(label.charAt(i));
			switch (c)
			{
			case 'A':
				out.append('\u2303');  // ctrl
				break;
			case 'C':
				out.append('\u2318');  // cmd
				break;
			case 'M':
				out.append('\u2325');  // alt
				break;
			case 'S':
				out.append('\u21E7');  // shift
				break;
			}
		}
		
		// We've done the modifiers, now do the key
		String key = label.substring(endOfModifiers + 1);
		
		// Some keys have Mac-specific symbols
		String text = macKeySymbols.get(key);
		
		// Others don't
		if (text == null)
		{
			// Everything else: periods, commas, etc. should be shown as the actual
			// character
			try
			{
				// e.g., convert the string "PERIOD" to the int KeyEvent.VK_PERIOD
				int keyCode = KeyEvent.class.getField("VK_".concat(key)).getInt(null);
				
				// And then convert KeyEvent.VK_PERIOD to the string "."
				text = KeyEvent.getKeyText(keyCode).toUpperCase();
			}
			catch(Exception e)
			{
				// This is probably an error, but it will be logged in
				// KeyEventTranslator.parseKey anyway, so just ignore it here.
				text = key.toUpperCase();
			}
		}
		out.append(text);
		
		return out.toString();
	} //}}}
	
	private GUIUtilities() {}
	//}}}

	//{{{ Inner classes

	//{{{ SizeSaver class
	/**
	 * A combined ComponentListener and WindowStateListener to continually save a Frames size.<br />
	 * For non-Frame's use {@link GUIUtilities#saveGeometry(Window,String)}
	 *
	 * @author Björn Kautler
	 * @version $Id$
	 * @since jEdit 4.3pre6
	 * @see GUIUtilities#saveGeometry(Window,Container,String)
	 */
	private static class SizeSaver extends ComponentAdapter implements WindowStateListener
	{
		private final Frame frame;
		private final Container parent;
		private final String name;

		//{{{ SizeSaver constructors
		/**
		 * Constructs a new SizeSaver.
		 *
		 * @param frame The Frame for which to save the size
		 * @param parent The parent to be relative to.
		 * @param name The prefix for the settings
		 */
		SizeSaver(Frame frame, Container parent, String name)
		{
			if (frame == null || name == null)
			{
				throw new NullPointerException();
			}
			this.frame = frame;
			this.parent = parent;
			this.name = name;
		} //}}}

		//{{{ windowStateChanged() method
		@Override
		public void windowStateChanged(WindowEvent wse)
		{
			int extendedState = wse.getNewState();
			jEdit.setIntegerProperty(name + ".extendedState",extendedState);
			Rectangle bounds = frame.getBounds();
			save(extendedState, bounds);
		} //}}}

		//{{{ save() method
		private void save(int extendedState, Rectangle bounds)
		{
			switch (extendedState)
			{
				case Frame.MAXIMIZED_VERT:
					jEdit.setIntegerProperty(name + ".x",bounds.x);
					jEdit.setIntegerProperty(name + ".width",bounds.width);
					break;

				case Frame.MAXIMIZED_HORIZ:
					jEdit.setIntegerProperty(name + ".y",bounds.y);
					jEdit.setIntegerProperty(name + ".height",bounds.height);
					break;

				case Frame.NORMAL:
					saveGeometry(frame,parent,name );
					break;
			}
		} //}}}

		//{{{ componentResized() method
		@Override
		public void componentResized(ComponentEvent ce)
		{
			componentMoved(ce);
		} //}}}

		//{{{ componentMoved() method
		@Override
		public void componentMoved(ComponentEvent ce)
		{
			final Rectangle bounds = frame.getBounds();
			final Runnable sizeSaver = new Runnable()
			{
				@Override
				public void run()
				{
					int extendedState = frame.getExtendedState();
					save(extendedState, bounds);
				}
			};
			new Thread("Sizesavingdelay")
			{
				@Override
				public void run()
				{
					try
					{
						Thread.sleep(500L);
					}
					catch (InterruptedException ie)
					{
					}
					EventQueue.invokeLater(sizeSaver);
				}
			}.start();
		} //}}}
	} //}}}

	//}}}
}
