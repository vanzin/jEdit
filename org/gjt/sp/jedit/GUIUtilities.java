/*
 * GUIUtilities.java - Various GUI utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import gnu.regexp.REException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.util.Log;
//}}}

/**
 * Class with several useful GUI functions.<p>
 *
 * It provides methods for:
 * <ul>
 * <li>Loading menu bars, menus and menu items from the properties
 * <li>Loading popup menus from the properties
 * <li>Loading tool bars and tool bar buttons from the properties
 * <li>Displaying various common dialog boxes
 * <li>Converting string representations of colors to color objects
 * <li>Loading and saving window geometry from the properties
 * <li>Displaying file open and save dialog boxes
 * <li>Loading images and caching them
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class GUIUtilities
{
	//{{{ Some predefined icons
	public static final Icon NEW_BUFFER_ICON = loadIcon("new.gif");
	public static final Icon DIRTY_BUFFER_ICON = loadIcon("dirty.gif");
	public static final Icon READ_ONLY_BUFFER_ICON = loadIcon("readonly.gif");
	public static final Icon NORMAL_BUFFER_ICON = loadIcon("normal.gif");
	public static final Icon WINDOW_ICON = loadIcon("jedit_icon.jpg");
	//}}}

	//{{{ Icon methods

	//{{{ loadIcon() method
	/**
	 * Loads an icon.
	 * @param iconName The icon name
	 * @since jEdit 2.6pre7
	 */
	public static Icon loadIcon(String iconName)
	{
		if(icons == null)
			icons = new Hashtable();

		// check if there is a cached version first
		Icon icon = (Icon)icons.get(iconName);
		if(icon != null)
			return icon;

		// get the icon
		if(iconName.startsWith("file:"))
		{
			icon = new ImageIcon(iconName.substring(5));
		}
		else
		{
			URL url = GUIUtilities.class.getResource(
				"/org/gjt/sp/jedit/icons/" + iconName);
			if(url == null)
			{
				Log.log(Log.ERROR,GUIUtilities.class,
					"Icon not found: " + iconName);
				return null;
			}

			icon = new ImageIcon(url);
		}

		icons.put(iconName,icon);
		return icon;
	} //}}}

	//{{{ getEditorIcon() method
	/**
	 * Returns the default editor window image.
	 */
	public static Image getEditorIcon()
	{
		return ((ImageIcon)WINDOW_ICON).getImage();
	} //}}}

	//{{{ getPluginIcon() method
	/**
	 * Returns the default plugin window image.
	 */
	public static Image getPluginIcon()
	{
		return ((ImageIcon)WINDOW_ICON).getImage();
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
		String menus = jEdit.getProperty(name);
		StringTokenizer st = new StringTokenizer(menus);

		JMenuBar mbar = new JMenuBar();

		while(st.hasMoreTokens())
			mbar.add(loadMenu(st.nextToken()));

		return mbar;
	} //}}}

	//{{{ loadMenu() method
	/**
	 * @deprecated Use loadMenu(name) instead
	 */
	public static JMenu loadMenu(View view, String name)
	{
		return loadMenu(name);
	} //}}}

	//{{{ loadMenu() method
	/**
	 * Creates a menu. This form of loadMenu() does not need to be used
	 * by plugins; use the other form instead.
	 * @param view The view to load the menu for
	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JMenu loadMenu(String name)
	{
		if(name.equals("open-encoding"))
			return new OpenWithEncodingMenu();
		else if(name.equals("recent-files"))
			return new RecentFilesMenu();
		else if(name.equals("recent-directories"))
			return new RecentDirectoriesMenu();
		else if(name.equals("current-directory"))
			return new CurrentDirectoryMenu();
		else if(name.equals("markers"))
			return new MarkersMenu();
		else if(name.equals("macros"))
			return new MacrosMenu();
		else if(name.equals("plugins"))
			return new PluginsMenu();
		else
			return new EnhancedMenu(name);
	} //}}}

	//{{{ loadPopupMenu() method
	/**
	 * Creates a popup menu.
	 * @param name The menu name
	 * @since jEdit 2.6pre2
	 */
	public static JPopupMenu loadPopupMenu(String name)
	{
		JPopupMenu menu = new JPopupMenu();

		String menuItems = jEdit.getProperty(name);
		if(menuItems != null)
		{
			StringTokenizer st = new StringTokenizer(menuItems);
			while(st.hasMoreTokens())
			{
				String menuItemName = st.nextToken();
				if(menuItemName.equals("-"))
					menu.addSeparator();
				else
				{
					if(menuItemName.startsWith("%"))
						menu.add(loadMenu(menuItemName.substring(1)));
					else
						menu.add(loadMenuItem(menuItemName,false));
				}
			}
		}

		return menu;
	} //}}}

	//{{{ loadMenuItem() method
	/**
	 * Creates a menu item.
	 * @param name The menu item name
	 * @since jEdit 2.6pre1
	 */
	public static JMenuItem loadMenuItem(String name)
	{
		return loadMenuItem(name,true);
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
		EditAction action = jEdit.getAction(name);
		String label = (action == null ?
			jEdit.getProperty(name + ".label")
			: action.getLabel());
		if(label == null)
			label = name;

		char mnemonic;
		int index = label.indexOf('$');
		if(index != -1 && label.length() - index > 1)
		{
			mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0,index).concat(label.substring(++index));
		}
		else
			mnemonic = '\0';

		JMenuItem mi;
		if(action != null && action.isToggle())
			mi = new EnhancedCheckBoxMenuItem(label,action);
		else
			mi = new EnhancedMenuItem(label,action);

		if(setMnemonic && mnemonic != '\0')
			mi.setMnemonic(mnemonic);

		return mi;
	} //}}}

	//{{{ loadToolBar() method
	/**
	 * Creates a toolbar.
	 * @param name The toolbar name
	 */
	public static JToolBar loadToolBar(String name)
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.putClientProperty("JToolBar.isRollover",Boolean.TRUE);

		String buttons = jEdit.getProperty(name);
		if(buttons != null)
		{
			StringTokenizer st = new StringTokenizer(buttons);
			while(st.hasMoreTokens())
			{
				String button = st.nextToken();
				if(button.equals("-"))
					toolBar.addSeparator();
				else
				{
					JButton b = loadToolButton(button);
					if(b != null)
						toolBar.add(b);
				}
			}
		}

		return toolBar;
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
		EditAction action = jEdit.getAction(name);
		String label = (action == null
			? jEdit.getProperty(name + ".label")
			: action.getLabel());

		Icon icon;
		String iconName = jEdit.getProperty(name + ".icon");
		if(iconName == null)
			return null;
		else
		{
			icon = loadIcon(iconName);
			if(icon == null)
				return null;
		}

		String toolTip = prettifyMenuLabel(label);
		String shortcut1 = jEdit.getProperty(name + ".shortcut");
		String shortcut2 = jEdit.getProperty(name + ".shortcut2");
		if(shortcut1 != null || shortcut2 != null)
		{
			toolTip = toolTip + " ("
				+ (shortcut1 != null
				? shortcut1 + " or "
				: "")
				+ (shortcut2 != null
				? shortcut2
				: "") + ")";
		}

		return new EnhancedButton(icon,toolTip,action);
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
	public static void message(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.INFORMATION_MESSAGE);
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
	public static void error(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.ERROR_MESSAGE);
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
	public static String input(Component comp, String name,
		Object[] args, Object def)
	{
		hideSplashScreen();

		String retVal = (String)JOptionPane.showInputDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title")),
			JOptionPane.QUESTION_MESSAGE,null,null,def);
		return retVal;
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
	public static String inputProperty(Component comp, String name,
		Object[] args, String def)
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
	public static int confirm(Component comp, String name,
		Object[] args, int buttons, int type)
	{
		hideSplashScreen();

		return JOptionPane.showConfirmDialog(comp,
			jEdit.getProperty(name + ".message",args),
			jEdit.getProperty(name + ".title"),buttons,type);
	} //}}}

	//{{{ showVFSFileDialog() method
	/**
	 * Displays a VFS file selection dialog box.
	 * @param view The view
	 * @param path The initial directory to display. May be null
	 * @param type The dialog type
	 * @param multipleSelection True if multiple selection should be allowed
	 * @return The selected file(s)
	 * @since jEdit 2.6pre2
	 */
	public static String[] showVFSFileDialog(View view, String path,
		int type, boolean multipleSelection)
	{
		hideSplashScreen();

		VFSFileChooserDialog fileChooser = new VFSFileChooserDialog(
			view,path,type,multipleSelection);
		String[] selectedFiles = fileChooser.getSelectedFiles();
		if(selectedFiles == null)
			return null;

		return selectedFiles;
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
		return parseColor(name, Color.black);
	} //}}}

	//{{{ parseColor() method
	public static Color parseColor(String name, Color defaultColor)
	{
		if(name == null)
			return defaultColor;
		else if(name.startsWith("#"))
		{
			try
			{
				return Color.decode(name);
			}
			catch(NumberFormatException nf)
			{
				return defaultColor;
			}
		}
		else if("red".equals(name))
			return Color.red;
		else if("green".equals(name))
			return Color.green;
		else if("blue".equals(name))
			return Color.blue;
		else if("yellow".equals(name))
			return Color.yellow;
		else if("orange".equals(name))
			return Color.orange;
		else if("white".equals(name))
			return Color.white;
		else if("lightGray".equals(name))
			return Color.lightGray;
		else if("gray".equals(name))
			return Color.gray;
		else if("darkGray".equals(name))
			return Color.darkGray;
		else if("black".equals(name))
			return Color.black;
		else if("cyan".equals(name))
			return Color.cyan;
		else if("magenta".equals(name))
			return Color.magenta;
		else if("pink".equals(name))
			return Color.pink;
		else
			return defaultColor;
	} //}}}

	//{{{ getColorHexString() method
	/**
	 * Converts a color object to its hex value. The hex value
	 * prefixed is with `#', for example `#ff0088'.
	 * @param c The color object
	 */
	public static String getColorHexString(Color c)
	{
		String colString = Integer.toHexString(c.getRGB() & 0xffffff);
		return "#000000".substring(0,7 - colString.length()).concat(colString);
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
		return parseStyle(str,family,size,true);
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @param color If false, the styles will be monochrome
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 4.0pre4
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size,
		boolean color)
		throws IllegalArgumentException
	{
		Color fgColor = Color.black;
		Color bgColor = null;
		boolean italic = false;
		boolean bold = false;
		StringTokenizer st = new StringTokenizer(str);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			if(s.startsWith("color:"))
			{
				if(color)
					fgColor = GUIUtilities.parseColor(s.substring(6), Color.black);
			}
			else if(s.startsWith("bgColor:"))
			{
				if(color)
					bgColor = GUIUtilities.parseColor(s.substring(8), null);
			}
			else if(s.startsWith("style:"))
			{
				for(int i = 6; i < s.length(); i++)
				{
					if(s.charAt(i) == 'i')
						italic = true;
					else if(s.charAt(i) == 'b')
						bold = true;
					else
						throw new IllegalArgumentException(
							"Invalid style: " + s);
				}
			}
			else
				throw new IllegalArgumentException(
					"Invalid directive: " + s);
		}
		return new SyntaxStyle(fgColor,bgColor,
			new Font(family,
			(italic ? Font.ITALIC : 0) | (bold ? Font.BOLD : 0),
			size));
	} //}}}

	//{{{ getStyleString() method
	/**
	 * Converts a style into it's string representation.
	 * @param style The style
	 */
	public static String getStyleString(SyntaxStyle style)
	{
		StringBuffer buf = new StringBuffer();

		buf.append("color:" + getColorHexString(style.getForegroundColor()));
		if(style.getBackgroundColor() != null) 
		{
			buf.append(" bgColor:" + getColorHexString(style.getBackgroundColor()));
		}
		if(!style.getFont().isPlain())
		{
			buf.append(" style:" + (style.getFont().isItalic() ? "i" : "")
				+ (style.getFont().isBold() ? "b" : ""));
		}

		return buf.toString();
	} //}}}

	//{{{ loadStyles() method
	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @since jEdit 3.2pre6
	 */
	public static SyntaxStyle[] loadStyles(String family, int size)
	{
		return loadStyles(family,size,true);
	} //}}}

	//{{{ loadStyles() method
	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @param color If false, the styles will be monochrome
	 * @since jEdit 4.0pre4
	 */
	public static SyntaxStyle[] loadStyles(String family, int size, boolean color)
	{
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		try
		{
			styles[Token.COMMENT1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment1"),
				family,size,color);
			styles[Token.COMMENT2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment2"),
				family, size,color);
			styles[Token.LITERAL1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal1"),
				family,size,color);
			styles[Token.LITERAL2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal2"),
				family,size,color);
			styles[Token.LABEL] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.label"),
				family,size,color);
			styles[Token.KEYWORD1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword1"),
				family,size,color);
			styles[Token.KEYWORD2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword2"),
				family,size,color);
			styles[Token.KEYWORD3] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword3"),
				family,size,color);
			styles[Token.FUNCTION] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.function"),
				family,size,color);
			styles[Token.MARKUP] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.markup"),
				family,size,color);
			styles[Token.OPERATOR] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.operator"),
				family,size,color);
			styles[Token.DIGIT] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.digit"),
				family,size,color);
			styles[Token.INVALID] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"),
				family,size,color);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,GUIUtilities.class,e);
		}

		return styles;
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
	 * @param win The window
	 * @param name The window name
	 */
	public static void loadGeometry(Window win, String name)
	{
		// all this adjust_* crap is there to work around buggy
		// Unix Java versions which don't put windows where you
		// tell them to
		int x, y, width, height, adjust_x, adjust_y, adjust_width,
			adjust_height;

		Dimension size = win.getSize();

		width = jEdit.getIntegerProperty(name + ".width",size.width);
		height = jEdit.getIntegerProperty(name + ".height",size.height);

		Component parent = win.getParent();
		if(parent == null)
		{
			Dimension screen = win.getToolkit().getScreenSize();
			x = (screen.width - width) / 2;
			y = (screen.height - height) / 2;
		}
		else
		{
			Rectangle bounds = parent.getBounds();
			x = bounds.x + (bounds.width - width) / 2;
			y = bounds.y + (bounds.height - height) / 2;
		}

		x = jEdit.getIntegerProperty(name + ".x",x);
		y = jEdit.getIntegerProperty(name + ".y",y);

		adjust_x = jEdit.getIntegerProperty(name + ".dx",0);
		adjust_y = jEdit.getIntegerProperty(name + ".dy",0);
		adjust_width = jEdit.getIntegerProperty(name + ".d-width",0);
		adjust_height = jEdit.getIntegerProperty(name + ".d-height",0);

		Rectangle desired = new Rectangle(x,y,width,height);
		Rectangle required = new Rectangle(x - adjust_x,
			y - adjust_y,width - adjust_width,
			height - adjust_height);
// 		Log.log(Log.DEBUG,GUIUtilities.class,"Window " + name
// 			+ ": desired geometry is " + desired);
// 		Log.log(Log.DEBUG,GUIUtilities.class,"Window " + name
// 			+ ": setting geometry to " + required);
		win.setBounds(required);

		if(File.separatorChar == '/'
			&& MiscUtilities.compareStrings(
			System.getProperty("java.version"),
			"1.2",false) < 0)
		{
			win.setBounds(required);
			new UnixWorkaround(win,name,desired,required);
		}
		else
			win.setBounds(desired);
	} //}}}

	//{{{ UnixWorkaround class
	static class UnixWorkaround
	{
		Window win;
		String name;
		Rectangle desired;
		Rectangle required;
		long start;
		boolean windowOpened;

		//{{{ UnixWorkaround constructor
		UnixWorkaround(Window win, String name, Rectangle desired,
			Rectangle required)
		{
			this.win = win;
			this.name = name;
			this.desired = desired;
			this.required = required;

			start = System.currentTimeMillis();

			win.addComponentListener(new ComponentHandler());
			win.addWindowListener(new WindowHandler());
		} //}}}

		//{{{ ComponentHandler class
		class ComponentHandler extends ComponentAdapter
		{
			//{{{ componentMoved() method
			public void componentMoved(ComponentEvent evt)
			{
				if(System.currentTimeMillis() - start < 1000)
				{
					Rectangle r = win.getBounds();
					if(!windowOpened && r.equals(required))
						return;

					if(!r.equals(desired))
					{
//						Log.log(Log.DEBUG,GUIUtilities.class,
//							"Window resize blocked: " + win.getBounds());
						win.setBounds(desired);
					}
				}
				else
					win.removeComponentListener(this);
			} //}}}

			//{{{ componentResized() method
			public void componentResized(ComponentEvent evt)
			{
				if(System.currentTimeMillis() - start < 1000)
				{
					Rectangle r = win.getBounds();
					if(!windowOpened && r.equals(required))
						return;

					if(!r.equals(desired))
					{
// 						Log.log(Log.DEBUG,GUIUtilities.class,
// 							"Window resize blocked: " + win.getBounds());
						win.setBounds(desired);
					}
				}
				else
					win.removeComponentListener(this);
			} //}}}
		} //}}}

		//{{{ WindowHandler class
		class WindowHandler extends WindowAdapter
		{
			//{{{ windowOpened() method
			public void windowOpened(WindowEvent evt)
			{
				windowOpened = true;

				Rectangle r = win.getBounds();
// 				Log.log(Log.DEBUG,GUIUtilities.class,"Window "
// 					+ name + ": bounds after opening: " + r);

				if(r.x != desired.x || r.y != desired.y
					|| r.width != desired.width
					|| r.height != desired.height)
				{
					jEdit.setIntegerProperty(name + ".dx",
						r.x - required.x);
					jEdit.setIntegerProperty(name + ".dy",
						r.y - required.y);
					jEdit.setIntegerProperty(name + ".d-width",
						r.width - required.width);
					jEdit.setIntegerProperty(name + ".d-height",
						r.height - required.height);
				}

				win.removeWindowListener(this);
			} //}}}
		} //}}}
	} //}}}

	//{{{ saveGeometry() method
	/**
	 * Saves a window's geometry to the properties.
	 * The geometry is saved to the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 * @param win The window
	 * @param name The window name
	 */
	public static void saveGeometry(Window win, String name)
	{
		Rectangle bounds = win.getBounds();
		jEdit.setIntegerProperty(name + ".x",bounds.x);
		jEdit.setIntegerProperty(name + ".y",bounds.y);
		jEdit.setIntegerProperty(name + ".width",bounds.width);
		jEdit.setIntegerProperty(name + ".height",bounds.height);
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

	//{{{ requestFocus() method
	/**
	 * Focuses on the specified component as soon as the window becomes
	 * active.
	 * @param win The window
	 * @param comp The component
	 */
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			public void windowActivated(WindowEvent evt)
			{
				comp.requestFocus();
				win.removeWindowListener(this);
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
		if(macOS)
			return evt.isControlDown();
		else
			return ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0);
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
	 */
	public static void showPopupMenu(JPopupMenu popup, Component comp,
		int x, int y)
	{
		Point p = new Point(x,y);
		SwingUtilities.convertPointToScreen(p,comp);

		Dimension size = popup.getPreferredSize();

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		boolean horiz = false;
		boolean vert = false;

		// might need later
		int origX = x;

		if(p.x + size.width > screen.width
			&& size.width < screen.width)
		{
			x += (screen.width - p.x - size.width);
			horiz = true;
		}

		if(p.y + size.height > screen.height
			&& size.height < screen.height)
		{
			y += (screen.height - p.y - size.height);
			vert = true;
		}

		// If popup needed to be moved both horizontally and
		// vertically, the mouse pointer might end up over a
		// menu item, which will be invoked when the mouse is
		// released. This is bad, so move popup to a different
		// location.
		if(horiz && vert)
		{
			x = origX - size.width - 2;
		}

		popup.show(comp,x,y);
	} //}}}

	//{{{ getView() method
	/**
	 * Finds the view parent of the specified component.
	 * @since jEdit 4.0pre2
	 */
	public static View getView(Component comp)
	{
		for(;;)
		{
			if(comp instanceof View)
				return (View)comp;
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else if(comp != null)
				comp = comp.getParent();
			else
				break;
		}
		return null;
	} //}}}

	//{{{ Package-private members

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

	//}}}

	//{{{ Private members
	private static SplashScreen splash;
	private static boolean macOS;
	private static Hashtable icons;

	private GUIUtilities() {}

	static
	{
		macOS = (System.getProperty("os.name").indexOf("Mac") != -1);
	} //}}}
}
