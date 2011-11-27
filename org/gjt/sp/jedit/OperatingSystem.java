/*
 * OperatingSystem.java - OS detection
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002, 2005 Slava Pestov
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.UIManager;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import org.gjt.sp.util.Log;

/**
 * Operating system detection routines.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre4
 */
public class OperatingSystem
{
	//{{{ getScreenBounds() method
	/**
	 * Returns the bounds of the default screen.
	 */
	public static Rectangle getScreenBounds()
	{
		int screenX = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		int screenY = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		int x, y, w, h;
		
		if (isMacOS())
		{
			x = 0;
			y = 22;
			w = screenX;
			h = screenY - y - 4;//shadow size
		}
		else if (isWindows())
		{
			x = -4;
			y = -4;
			w = screenX - 2*x;
			h = screenY - 2*y;
		}
		else
		{
			x = 0;
			y = 0;
			w = screenX;
			h = screenY;
		}
		
		return new Rectangle(x,y,w,h);
	} //}}}

	//{{{ getScreenBounds() method
	/**
	 * Returns the bounds of the (virtual) screen that the window should be in
	 * @param window The bounds of the window to get the screen for
	 */
	public static Rectangle getScreenBounds(Rectangle window)
	{
		GraphicsDevice[] gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		Set<GraphicsConfiguration> intersects = new HashSet<GraphicsConfiguration>();

		// Get available screens
		// O(n^3), this is nasty, but since we aren't dealling with
		// many items it should be fine
		for (int i=0; i < gd.length; i++)
		{
			GraphicsConfiguration gc = gd[i]
				.getDefaultConfiguration();
			// Don't add duplicates
			if (window.intersects(gc.getBounds()))
			{
				if (!intersects.contains(gc))
					intersects.add(gc);
			}
		}
		
		GraphicsConfiguration choice = null;
		if (!intersects.isEmpty())
		{
			// Pick screen with largest intersection
			for (GraphicsConfiguration gcc : intersects)
			{
				if (choice == null)
					choice = gcc;
				else
				{
					Rectangle int1 = choice.getBounds().intersection(window);
					Rectangle int2 = gcc.getBounds().intersection(window);
					int area1 = int1.width * int1.height;
					int area2 = int2.width * int2.height;
					if (area2 > area1)
						choice = gcc;
				}
			}
		}
		else
			choice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		
		// Make adjustments for some OS's
		int screenX = choice.getBounds().x;
		int screenY = choice.getBounds().y;
		int screenW = choice.getBounds().width;
		int screenH = choice.getBounds().height;
		int x, y, w, h;
		
		if (isMacOS())
		{
			x = screenX;
			y = screenY + 22;
			w = screenW;
			h = screenH - y - 4;//shadow size
		}
		else
		{
			x = screenX;
			y = screenY;
			w = screenW;
			h = screenH;
		}
		
		// Yay, we're finally there
		return new Rectangle(x,y,w,h);
	} //}}}

	//{{{ isDOSDerived() method
	/**
	 * Returns if we're running Windows 95/98/ME/NT/2000/XP/Vista/Win7, or OS/2.
	 */
	public static boolean isDOSDerived()
	{
		return isWindows() || isOS2();
	} //}}}

	//{{{ isWindows() method
	/**
	 * Returns if we're running Windows 95/98/ME/NT/2000/XP/Vista/Win7.
	 */
	public static boolean isWindows()
	{
		return os == WINDOWS_9x || os == WINDOWS_NT;
	} //}}}

	//{{{ isWindows9x() method
	/**
	 * Returns if we're running Windows 95/98/ME.
	 */
	public static boolean isWindows9x()
	{
		return os == WINDOWS_9x;
	} //}}}

	//{{{ isWindowsNT() method
	/**
	 * Returns if we're running Windows NT/2000/XP/Vista/Win7.
	 */
	public static boolean isWindowsNT()
	{
		return os == WINDOWS_NT;
	} //}}}

	//{{{ isOS2() method
	/**
	 * Returns if we're running OS/2.
	 */
	public static boolean isOS2()
	{
		return os == OS2;
	} //}}}

	//{{{ isUnix() method
	/**
	 * Returns if we're running Unix (this includes MacOS X).
	 */
	public static boolean isUnix()
	{
		return os == UNIX || os == MAC_OS_X;
	} //}}}

	//{{{ isMacOS() method
	/**
	 * Returns if we're running MacOS X.
	 */
	public static boolean isMacOS()
	{
		return os == MAC_OS_X;
	} //}}}

	//{{{ isX11() method
	/**
	 * Returns if this OS is likely to be using X11 as the graphics
	 * system.
	 * @since jEdit 4.2pre3
	 */
	public static boolean isX11()
	{
		return os == UNIX;
	} //}}}

	//{{{ isVMS() method
	/**
	 * Returns if we're running VMS.
	 */
	public static boolean isVMS()
	{
		return os == VMS;
	} //}}}

	//{{{ isMacOSLF() method
	/**
	* Returns if we're running MacOS X and using the native look and feel.
	*/
	public static boolean isMacOSLF()
	{
		return isMacOS() && UIManager.getLookAndFeel().isNativeLookAndFeel();
	} //}}}

	//{{{ hasScreenMenuBar() method
	/**
	 * Returns whether the screen menu bar on Mac OS X is in use.
	 * @since jEdit 4.2pre1
	*/
	public static boolean hasScreenMenuBar()
	{
		if(!isMacOS())
			return false;
		else if(hasScreenMenuBar == -1)
		{
			String result = System.getProperty("apple.laf.useScreenMenuBar");
			if (result == null)
				result = System.getProperty("com.apple.macos.useScreenMenuBar");
			hasScreenMenuBar = "true".equals(result) ? 1 : 0;
		}

		return hasScreenMenuBar == 1;
	} //}}}

	//{{{ hasJava16() method
	/**
	 * Returns if Java 2 version 1.6 is in use.
	 * @since jEdit 4.3pre17
	 * @deprecated obsolete, since we depend on Java 1.6 now
	 */
	public static boolean hasJava16()
	{
		return true;
	} //}}}

		//{{{ hasJava17() method
	/**
	 * Returns if Java 2 version 1.7 is in use.
	 * @since jEdit 5.0pre1
	 */
	public static boolean hasJava17()
	{
		return java17;
	} //}}}

	
	//{{{ isCaseInsensitiveFS() method
	/**
	 * @since jEdit 4.3pre2
	 */
	public static boolean isCaseInsensitiveFS()
	{
		return isDOSDerived() || isMacOS();
	} //}}}
	
	//{{{ Private members
	private static final int UNIX = 0x31337;
	private static final int WINDOWS_9x = 0x640;
	private static final int WINDOWS_NT = 0x666;
	private static final int OS2 = 0xDEAD;
	private static final int MAC_OS_X = 0xABC;
	private static final int VMS = 0xDEAD2;
	private static final int UNKNOWN = 0xBAD;

	private static int os;
	private static boolean java17;
	private static int hasScreenMenuBar = -1;

	//{{{ Class initializer
	static
	{
		if(System.getProperty("mrj.version") != null)
		{
			os = MAC_OS_X;
		}
		else
		{
			String osName = System.getProperty("os.name");
			if(osName.contains("Windows 9")
				|| osName.contains("Windows M"))
			{
				os = WINDOWS_9x;
			}
			else if(osName.contains("Windows"))
			{
				os = WINDOWS_NT;
			}
			else if(osName.contains("OS/2"))
			{
				os = OS2;
			}
			else if(osName.contains("VMS"))
			{
				os = VMS;
			}
			else if(File.separatorChar == '/')
			{
				os = UNIX;
			}
			else
			{
				os = UNKNOWN;
				Log.log(Log.WARNING,OperatingSystem.class,
					"Unknown operating system: " + osName);
			}
		}

		// for debugging, make jEdit think its using a different
		// version of Java than it really is.
		String javaVersion = System.getProperty("jedit.force.java.version");
		if(javaVersion == null || javaVersion.length() == 0)
			javaVersion = System.getProperty("java.version");
		if(javaVersion == null || javaVersion.length() == 0)
			javaVersion = System.getProperty("java.runtime.version");
		java17 = javaVersion.compareTo("1.7") >= 0;
	} //}}}

	//}}}
}
