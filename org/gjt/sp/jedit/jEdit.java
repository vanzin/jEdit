/*
 * jEdit.java - Main class of the jEdit editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2005 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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
import org.gjt.sp.jedit.datatransfer.JEditTransferableService;
import org.gjt.sp.jedit.visitors.JEditVisitor;

import java.awt.*;

import org.gjt.sp.jedit.View.ViewConfig;
import org.gjt.sp.jedit.bsh.UtilEvalError;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

import org.xml.sax.SAXParseException;

import org.gjt.sp.jedit.bufferio.BufferIORequest;
import org.gjt.sp.jedit.buffer.KillRing;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.buffer.FoldHandler;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.notification.ErrorEntry;
import org.gjt.sp.jedit.notification.NotificationManager;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.help.HelpViewer;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.pluginmgr.PluginManager;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.syntax.Chunk;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.syntax.XModeHandler;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.visitors.SaveCaretInfoVisitor;
import org.gjt.sp.jedit.bufferset.BufferSetManager;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.XMLUtilities;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.SyntaxUtilities;
//}}}

/**
 * The main class of the jEdit text editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class jEdit
{
	//{{{ getVersion() method
	/**
	 * Returns the jEdit version as a human-readable string.
	 */
	public static String getVersion()
	{
		return MiscUtilities.buildToVersion(getBuild());
	} //}}}

	//{{{ getBuild() method
	/**
	 * Returns the internal version. MiscUtilities.compareStrings() can be used
	 * to compare different internal versions.
	 */
	public static String getBuild()
	{
		// (major).(minor).(<99 = preX, 99 = "final").(bug fix)
		return "04.05.01.00";
	} //}}}

	//{{{ main() method
	/**
	 * The main method of the jEdit application.
	 * This should never be invoked directly.
	 * @param args The command line arguments
	 */
	public static void main(String[] args)
	{
		//{{{ Check for Java 1.6 or later
		String javaVersion = System.getProperty("java.version");
		if(javaVersion.compareTo("1.6") < 0)
		{
			System.err.println("You are running Java version "
				+ javaVersion + '.');
			System.err.println("jEdit requires Java 1.6 or later.");
			System.exit(1);
		} //}}}

		startupDone.add(false);

		// later on we need to know if certain code is called from
		// the main thread
		mainThread = Thread.currentThread();

		settingsDirectory = ".jedit";
		// On mac, different rules (should) apply
		if(OperatingSystem.isMacOS())
			settingsDirectory = "Library/jEdit";

		// MacOS users expect the app to keep running after all windows
		// are closed
		background = OperatingSystem.isMacOS();

		//{{{ Parse command line
		boolean endOpts = false;
		int level = Log.WARNING;
		String portFile = "server";
		boolean restore = true;
		boolean newView = true;
		boolean newPlainView = false;
		boolean gui = true; // open initial view?
		boolean loadPlugins = true;
		boolean runStartupScripts = true;
		boolean quit = false;
		boolean wait = false;
		boolean shouldRelocateSettings = true;
		String userDir = System.getProperty("user.dir");
		boolean splash = true;

		// script to run
		String scriptFile = null;

		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if(arg == null)
				continue;
			else if(arg.length() == 0)
				args[i] = null;
			else if(arg.startsWith("-") && !endOpts)
			{
				if(arg.equals("--"))
					endOpts = true;
				else if(arg.equals("-usage"))
				{
					version();
					System.err.println();
					usage();
					System.exit(1);
				}
				else if(arg.equals("-version"))
				{
					version();
					System.exit(1);
				}
				else if(arg.startsWith("-log="))
				{
					try
					{
						level = Integer.parseInt(arg.substring("-log=".length()));
					}
					catch(NumberFormatException nf)
					{
						System.err.println("Malformed option: " + arg);
					}
				}
				else if(arg.equals("-nosettings"))
					settingsDirectory = null;
				else if(arg.startsWith("-settings="))
				{
					settingsDirectory = arg.substring(10);
					shouldRelocateSettings = false;
				}
				else if(arg.startsWith("-noserver"))
					portFile = null;
				else if(arg.equals("-server"))
					portFile = "server";
				else if(arg.startsWith("-server="))
					portFile = arg.substring(8);
				else if(arg.startsWith("-background"))
					background = true;
				else if(arg.startsWith("-nobackground"))
					background = false;
				else if(arg.equals("-gui"))
					gui = true;
				else if(arg.equals("-nogui"))
					gui = false;
				else if(arg.equals("-newview"))
					newView = true;
				else if(arg.equals("-newplainview"))
					newPlainView = true;
				else if(arg.equals("-reuseview"))
					newPlainView = newView = false;
				else if(arg.equals("-restore"))
					restore = true;
				else if(arg.equals("-norestore"))
					restore = false;
				else if(arg.equals("-plugins"))
					loadPlugins = true;
				else if(arg.equals("-noplugins"))
					loadPlugins = false;
				else if(arg.equals("-startupscripts"))
					runStartupScripts = true;
				else if(arg.equals("-nostartupscripts"))
					runStartupScripts = false;
				else if(arg.startsWith("-run="))
					scriptFile = arg.substring(5);
				else if(arg.equals("-wait"))
					wait = true;
				else if(arg.equals("-quit"))
					quit = true;
				else if(arg.equals("-nosplash"))
					splash = false;
				else
				{
					System.err.println("Unknown option: "
						+ arg);
					usage();
					System.exit(1);
				}
				args[i] = null;
			}
		} //}}}

		//{{{ We need these initializations very early on
		if(settingsDirectory != null)
		{
			settingsDirectory = MiscUtilities.constructPath(
				System.getProperty("user.home"),
				settingsDirectory);
			settingsDirectory = MiscUtilities.resolveSymlinks(
				settingsDirectory);
		}

		if(settingsDirectory != null && portFile != null)
			portFile = MiscUtilities.constructPath(settingsDirectory,portFile);
		else
			portFile = null;

		Log.init(true,level);
		//}}}

		//{{{ Try connecting to another running jEdit instance
		if(portFile != null && new File(portFile).exists())
		{
			try
			{
				BufferedReader in = new BufferedReader(new FileReader(portFile));
				String check = in.readLine();
				if(!"b".equals(check))
					throw new Exception("Wrong port file format");

				int port = Integer.parseInt(in.readLine());
				int key = Integer.parseInt(in.readLine());

				Socket socket = new Socket(InetAddress.getByName("127.0.0.1"),port);
				DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
				out.writeInt(key);

				String script;
				if(quit)
				{
					script = "socket.close();\n"
						+ "jEdit.exit(null,true);\n";
				}
				else
				{
					script = makeServerScript(wait,restore,
						newView,newPlainView,args,
						scriptFile);
				}

				out.writeUTF(script);

				Log.log(Log.DEBUG,jEdit.class,"Waiting for server");
				// block until its closed
				try
				{
					socket.getInputStream().read();
				}
				catch(Exception e)
				{
				}

				in.close();
				out.close();

				System.exit(0);
			}
			catch(Exception e)
			{
				// ok, this one seems to confuse newbies
				// endlessly, so log it as NOTICE, not
				// ERROR
				Log.log(Log.NOTICE,jEdit.class,"An error occurred"
					+ " while connecting to the jEdit server instance.");
				Log.log(Log.NOTICE,jEdit.class,"This probably means that"
					+ " jEdit crashed and/or exited abnormally");
				Log.log(Log.NOTICE,jEdit.class,"the last time it was run.");
				Log.log(Log.NOTICE,jEdit.class,"If you don't"
					+ " know what this means, don't worry.");
				Log.log(Log.NOTICE,jEdit.class,e);
			}
		}

		if(quit)
		{
			// if no server running and user runs jedit -quit,
			// just exit
			System.exit(0);
		} //}}}

		// don't show splash screen if there is a file named
		// 'nosplash' in the settings directory
		if(splash && (!new File(settingsDirectory,"nosplash").exists()))
			GUIUtilities.showSplashScreen();

		//{{{ Mac settings migration code. Should eventually be removed
		if(OperatingSystem.isMacOS() && shouldRelocateSettings && settingsDirectory != null)
		{
			relocateSettings();
		}
		// }}}

		//{{{ Initialize settings directory
		Writer stream;
		if(settingsDirectory != null)
		{
			File _settingsDirectory = new File(settingsDirectory);
			if(!_settingsDirectory.exists())
				_settingsDirectory.mkdirs();
			File _macrosDirectory = new File(settingsDirectory,"macros");
			if(!_macrosDirectory.exists())
				_macrosDirectory.mkdir();

			String logPath = MiscUtilities.constructPath(
				settingsDirectory,"activity.log");

			backupSettingsFile(new File(logPath));

			try
			{
				stream = new BufferedWriter(new FileWriter(logPath));

				// Write a warning message:
				String lineSep = System.getProperty("line.separator");
				stream.write("Log file created on " + new Date());
				stream.write(lineSep);
				stream.write("IMPORTANT:");
				stream.write(lineSep);
				stream.write("Because updating this file after "
					+ "every log message would kill");
				stream.write(lineSep);
				stream.write("performance, it will be *incomplete* "
					+ "unless you invoke the");
				stream.write(lineSep);
				stream.write("Utilities->Troubleshooting->Update "
					+ "Activity Log on Disk command!");
				stream.write(lineSep);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				stream = null;
			}
		}
		else
		{
			stream = null;
		} //}}}

		Log.setLogWriter(stream);

		Log.log(Log.NOTICE,jEdit.class,"jEdit version " + getVersion());
		Log.log(Log.MESSAGE,jEdit.class,"Settings directory is "
			+ settingsDirectory);

		//{{{ Get things rolling
		GUIUtilities.advanceSplashProgress("init");
		initMisc();
		GUIUtilities.advanceSplashProgress("init system properties");
		initSystemProperties();

		GUIUtilities.advanceSplashProgress("init beanshell");
		BeanShell.init();

		GUIUtilities.advanceSplashProgress("loading site properties");
		if(jEditHome != null)
			initSiteProperties();

		GUIUtilities.advanceSplashProgress("loading user properties");
		initUserProperties();

		GUIUtilities.advanceSplashProgress("init GUI");
		GUIUtilities.init();

		bufferSetManager = new BufferSetManager();
		//}}}

		//{{{ Initialize server
		if(portFile != null)
		{
			GUIUtilities.advanceSplashProgress("init server");
			server = new EditServer(portFile);
			if(!server.isOK())
				server = null;
		}
		else
		{
			GUIUtilities.advanceSplashProgress();
			if(background)
			{
				background = false;
				Log.log(Log.WARNING,jEdit.class,"You cannot specify both the"
					+ " -background and -noserver switches");
			}
		} //}}}

		//{{{ Do more stuff
		GUIUtilities.advanceSplashProgress("init look and feel");
		initPLAF();
		GUIUtilities.advanceSplashProgress("init VFS Manager");
		VFSManager.init();
		GUIUtilities.advanceSplashProgress("init resources");
		initResources();
		SearchAndReplace.load();

		if(loadPlugins)
		{
			GUIUtilities.advanceSplashProgress("init plugins");
			initPlugins();
		}
		else
			GUIUtilities.advanceSplashProgress();

		Registers.setSaver(new JEditRegisterSaver());
		Registers.setListener(new JEditRegistersListener());
		GUIUtilities.advanceSplashProgress("init history model");
		HistoryModel.setSaver(new JEditHistoryModelSaver());
		HistoryModel.loadHistory();
		GUIUtilities.advanceSplashProgress("init buffer history");
		BufferHistory.load();
		GUIUtilities.advanceSplashProgress("init killring");
		KillRing.setInstance(new JEditKillRing());
		KillRing.getInstance().load();
		GUIUtilities.advanceSplashProgress("init various properties");
		propertiesChanged();

		GUIUtilities.advanceSplashProgress("init modes");

		// Buffer sort
		sortBuffers = getBooleanProperty("sortBuffers");
		sortByName = getBooleanProperty("sortByName");

		reloadModes();

		GUIUtilities.advanceSplashProgress("activate plugins");
		//}}}

		//{{{ Activate plugins that must be activated at startup
		for(int i = 0; i < jars.size(); i++)
		{
			jars.elementAt(i).activatePluginIfNecessary();
		} //}}}

		String[] serviceNames = ServiceManager.getServiceNames(JEditTransferableService.class);
		for (String serviceName : serviceNames)
		{
			JEditTransferableService service = ServiceManager.getService(JEditTransferableService.class, serviceName);
			org.gjt.sp.jedit.datatransfer.TransferHandler.getInstance().registerTransferableService(service);
		}

		//{{{ Load macros and run startup scripts, after plugins and settings are loaded
		GUIUtilities.advanceSplashProgress("init macros");
		Macros.loadMacros();
		Macros.getMacroActionSet().initKeyBindings();

		if(runStartupScripts && jEditHome != null)
		{
			String path = MiscUtilities.constructPath(jEditHome,"startup");
			File file = new File(path);
			if(file.exists())
			{
				runStartupScripts(file);
			}
			else
				GUIUtilities.advanceSplashProgress();
		}
		else
			GUIUtilities.advanceSplashProgress("run startup scripts");

		if(runStartupScripts && settingsDirectory != null)
		{
			String path = MiscUtilities.constructPath(settingsDirectory,"startup");
			File file = new File(path);
			if (file.exists())
			{
				GUIUtilities.advanceSplashProgress("run startup scripts");
				runStartupScripts(file);
			}
			else
			{
				GUIUtilities.advanceSplashProgress();
				file.mkdirs();
			}
		}
		else
		{
			GUIUtilities.advanceSplashProgress();
		} //}}}

		//{{{ Run script specified with -run= parameter
		if(scriptFile != null)
		{
			GUIUtilities.advanceSplashProgress("run script file");
			scriptFile = MiscUtilities.constructPath(userDir,scriptFile);
			try
			{
				BeanShell.getNameSpace().setVariable("args",args);
			}
			catch(UtilEvalError e)
			{
				Log.log(Log.ERROR,jEdit.class,e);
			}
			BeanShell.runScript(null,scriptFile,null,false);
		}
		else
		{
			GUIUtilities.advanceSplashProgress();
		}
		//}}}

		GUIUtilities.advanceSplashProgress();

		// Create dynamic actions for switching to saved layouts.
		// The list of saved layouts is retrieved from the docking framework,
		// which can be provided by a plugin, so this must be called only after
		// the plugins are loaded.
		DockingLayoutManager.init();

		// Open files, create the view and hide the splash screen.
		SyntaxUtilities.propertyManager = jEdit.propertyManager;
		finishStartup(gui,restore,newPlainView,userDir,args);
	} //}}}

	//{{{ Property methods

	//{{{ getProperties() method
	/**
	 * Returns the properties object which contains all known
	 * jEdit properties. Note that as of jEdit 4.2pre10, this returns a
	 * new collection, not the existing properties instance.
	 * @since jEdit 3.1pre4
	 */
	public static Properties getProperties()
	{
		return propMgr.getProperties();
	} //}}}

	//{{{ getProperty() method
	/**
	 * Fetches a property, returning null if it's not defined.
	 * @param name The property
	 */
	public static String getProperty(String name)
	{
		return propMgr.getProperty(name);
	} //}}}

	//{{{ getProperty() method
	/**
	 * Fetches a property, returning the default value if it's not
	 * defined.
	 * @param name The property
	 * @param def The default value
	 */
	public static String getProperty(String name, String def)
	{
		String value = propMgr.getProperty(name);
		if(value == null)
			return def;
		else
			return value;
	} //}}}

	//{{{ getProperty() method
	/**
	 * Returns the property with the specified name.<p>
	 *
	 * The elements of the <code>args</code> array are substituted
	 * into the value of the property in place of strings of the
	 * form <code>{<i>n</i>}</code>, where <code><i>n</i></code> is an index
	 * in the array.<p>
	 *
	 * You can find out more about this feature by reading the
	 * documentation for the <code>format</code> method of the
	 * <code>java.text.MessageFormat</code> class.
	 *
	 * @param name The property
	 * @param args The positional parameters
	 */
	public static String getProperty(String name, Object[] args)
	{
		if(name == null)
			return null;
		if(args == null)
			return getProperty(name);
		else
		{
			String value = getProperty(name);
			if(value == null)
				return null;
			else
				return MessageFormat.format(value,args);
		}
	} //}}}

	//{{{ getBooleanProperty() method
	/**
	 * Returns the value of a boolean property.
	 * @param name The property
	 */
	public static boolean getBooleanProperty(String name)
	{
		return getBooleanProperty(name,false);
	} //}}}

	//{{{ getBooleanProperty() method
	/**
	 * Returns the value of a boolean property.
	 * @param name The property
	 * @param def The default value
	 */
	public static boolean getBooleanProperty(String name, boolean def)
	{
		String value = getProperty(name);
		return StandardUtilities.getBoolean(value, def);
	} //}}}

	//{{{ getIntegerProperty() method
	/**
	 * Returns the value of an integer property.
	 * @param name The property
	 */
	public static int getIntegerProperty(String name)
	{
		return getIntegerProperty(name,0);
	} //}}}

	//{{{ getIntegerProperty() method
	/**
	 * Returns the value of an integer property.
	 * @param name The property
	 * @param def The default value
	 * @since jEdit 4.0pre1
	 */
	public static int getIntegerProperty(String name, int def)
	{
		String value = getProperty(name);
		if(value == null)
			return def;
		else
		{
			try
			{
				return Integer.parseInt(value.trim());
			}
			catch(NumberFormatException nf)
			{
				return def;
			}
		}
	} //}}}

	//{{{ getDoubleProperty() method
	public static double getDoubleProperty(String name, double def)
	{
		String value = getProperty(name);
		if(value == null)
			return def;
		else
		{
			try
			{
				return Double.parseDouble(value.trim());
			}
			catch(NumberFormatException nf)
			{
				return def;
			}
		}
	}
	//}}}

	//{{{ getFontProperty() method
	/**
	 * Returns the value of a font property. The family is stored
	 * in the <code><i>name</i></code> property, the font size is stored
	 * in the <code><i>name</i>size</code> property, and the font style is
	 * stored in <code><i>name</i>style</code>. For example, if
	 * <code><i>name</i></code> is <code>view.gutter.font</code>, the
	 * properties will be named <code>view.gutter.font</code>,
	 * <code>view.gutter.fontsize</code>, and
	 * <code>view.gutter.fontstyle</code>.
	 *
	 * @param name The property
	 * @since jEdit 4.0pre1
	 */
	public static Font getFontProperty(String name)
	{
		return getFontProperty(name,null);
	} //}}}

	//{{{ getFontProperty() method
	/**
	 * Returns the value of a font property. The family is stored
	 * in the <code><i>name</i></code> property, the font size is stored
	 * in the <code><i>name</i>size</code> property, and the font style is
	 * stored in <code><i>name</i>style</code>. For example, if
	 * <code><i>name</i></code> is <code>view.gutter.font</code>, the
	 * properties will be named <code>view.gutter.font</code>,
	 * <code>view.gutter.fontsize</code>, and
	 * <code>view.gutter.fontstyle</code>.
	 *
	 * @param name The property
	 * @param def The default value
	 * @since jEdit 4.0pre1
	 */
	public static Font getFontProperty(String name, Font def)
	{
		String family = getProperty(name);
		String sizeString = getProperty(name + "size");
		String styleString = getProperty(name + "style");

		if(family == null || sizeString == null || styleString == null)
			return def;
		else
		{
			int size, style;

			try
			{
				size = Integer.parseInt(sizeString);
			}
			catch(NumberFormatException nf)
			{
				return def;
			}

			try
			{
				style = Integer.parseInt(styleString);
			}
			catch(NumberFormatException nf)
			{
				return def;
			}

			return new Font(family,style,size);
		}
	} //}}}

	//{{{ getColorProperty() method
	/**
	 * Returns the value of a color property.
	 * @param name The property name
	 * @since jEdit 4.0pre1
	 */
	public static Color getColorProperty(String name)
	{
		return getColorProperty(name,Color.black);
	} //}}}

	//{{{ getColorProperty() method
	/**
	 * Returns the value of a color property.
	 * @param name The property name
	 * @param def The default value
	 * @since jEdit 4.0pre1
	 */
	public static Color getColorProperty(String name, Color def)
	{
		String value = getProperty(name);
		if(value == null)
			return def;
		else
			return SyntaxUtilities.parseColor(value, def);
	} //}}}

	//{{{ setColorProperty() method
	/**
	 * Sets the value of a color property.
	 * @param name The property name
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public static void setColorProperty(String name, Color value)
	{
		setProperty(name, SyntaxUtilities.getColorHexString(value));
	} //}}}

	//{{{ setProperty() method
	/**
	 * Sets a property to a new value.
	 * @param name The property
	 * @param value The new value
	 */
	public static void setProperty(String name, String value)
	{
		propMgr.setProperty(name,value);
	} //}}}

	//{{{ setTemporaryProperty() method
	/**
	 * Sets a property to a new value. Properties set using this
	 * method are not saved to the user properties list.
	 * @param name The property
	 * @param value The new value
	 * @since jEdit 2.3final
	 */
	public static void setTemporaryProperty(String name, String value)
	{
		propMgr.setTemporaryProperty(name,value);
	} //}}}

	//{{{ setBooleanProperty() method
	/**
	 * Sets a boolean property.
	 * @param name The property
	 * @param value The value
	 */
	public static void setBooleanProperty(String name, boolean value)
	{
		setProperty(name,value ? "true" : "false");
	} //}}}

	//{{{ setIntegerProperty() method
	/**
	 * Sets the value of an integer property.
	 * @param name The property
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public static void setIntegerProperty(String name, int value)
	{
		setProperty(name,String.valueOf(value));
	} //}}}

	//{{{ setDoubleProperty() method
	public static void setDoubleProperty(String name, double value)
	{
		setProperty(name,String.valueOf(value));
	}
	//}}}

	//{{{ setFontProperty() method
	/**
	 * Sets the value of a font property. The family is stored
	 * in the <code><i>name</i></code> property, the font size is stored
	 * in the <code><i>name</i>size</code> property, and the font style is
	 * stored in <code><i>name</i>style</code>. For example, if
	 * <code><i>name</i></code> is <code>view.gutter.font</code>, the
	 * properties will be named <code>view.gutter.font</code>,
	 * <code>view.gutter.fontsize</code>, and
	 * <code>view.gutter.fontstyle</code>.
	 *
	 * @param name The property
	 * @param value The value
	 * @since jEdit 4.0pre1
	 */
	public static void setFontProperty(String name, Font value)
	{
		setProperty(name,value.getFamily());
		setIntegerProperty(name + "size",value.getSize());
		setIntegerProperty(name + "style",value.getStyle());
	} //}}}

	//{{{ unsetProperty() method
	/**
	 * Unsets (clears) a property.
	 * @param name The property
	 */
	public static void unsetProperty(String name)
	{
		propMgr.unsetProperty(name);
	} //}}}

	//{{{ resetProperty() method
	/**
	 * Resets a property to its default value.
	 * @param name The property
	 *
	 * @since jEdit 2.5pre3
	 */
	public static void resetProperty(String name)
	{
		propMgr.resetProperty(name);
	} //}}}

	//{{{ propertiesChanged() method
	/**
	 * Reloads various settings from the properties.
	 */
	public static void propertiesChanged()
	{
		initPLAF();

		initKeyBindings();

		Autosave.setInterval(getIntegerProperty("autosave",30));

		saveCaret = getBooleanProperty("saveCaret");

		UIDefaults defaults = UIManager.getDefaults();

		// give all text areas the same font
		Font font = getFontProperty("view.font");

		//defaults.put("TextField.font",font);
		defaults.put("TextArea.font",font);
		defaults.put("TextPane.font",font);

		// Enable/Disable tooltips
		ToolTipManager.sharedInstance().setEnabled(
			jEdit.getBooleanProperty("showTooltips"));

		initProxy();

		// we do this here instead of adding buffers to the bus.
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			buffer.resetCachedProperties();
			buffer.propertiesChanged();
			buffer = buffer.next;
		}

		HistoryModel.setDefaultMax(getIntegerProperty("history",25));
		KillRing.getInstance().propertiesChanged(getIntegerProperty("history",25));
		Chunk.propertiesChanged(propertyManager);

		EditBus.send(new PropertiesChanged(null));
	} //}}}

	//}}}

	//{{{ Plugin management methods

	//{{{ getNotLoadedPluginJARs() method
	/**
	 * Returns a list of plugin JARs pathnames that are not currently loaded
	 * by examining the user and system plugin directories.
	 * @since jEdit 3.2pre1
	 */
	public static String[] getNotLoadedPluginJARs()
	{
		List<String> returnValue = new ArrayList<String>();

		if(jEditHome != null)
		{
			String systemPluginDir = MiscUtilities
				.constructPath(jEditHome,"jars");

			String[] list = new File(systemPluginDir).list();
			if(list != null)
				getNotLoadedPluginJARs(returnValue,systemPluginDir,list);
		}

		if(settingsDirectory != null)
		{
			String userPluginDir = MiscUtilities
				.constructPath(settingsDirectory,"jars");
			String[] list = new File(userPluginDir).list();
			if(list != null)
			{
				getNotLoadedPluginJARs(returnValue,
					userPluginDir,list);
			}
		}

		String[] _returnValue = new String[returnValue.size()];
		returnValue.toArray(_returnValue);
		return _returnValue;
	} //}}}

	//{{{ getPlugin() method
	/**
	 * Returns the plugin with the specified class name.
	 * Only works for plugins that were loaded.
	 */
	public static EditPlugin getPlugin(String name)
	{
		return getPlugin(name, false);
	} //}}}

	//{{{ getPlugin(String, boolean) method
	/**
	 * Returns the plugin with the specified class name.
	 * If * <code>loadIfNecessary</code> is true, the plugin will be searched for,
	 * loaded, and activated in case it has not yet been loaded.
	 *
	 * @param name the classname of the main Plugin class.
	 * @param loadIfNecessary - loads plugin + dependencies if it is not loaded yet.
	 * @since jEdit 4.2pre4
	 */
	public static EditPlugin getPlugin(String name, boolean loadIfNecessary)
	{
		EditPlugin[] plugins = getPlugins();
		EditPlugin plugin = null;
		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClassName().equals(name))
				plugin = plugins[i];
			if(loadIfNecessary)
			{
				if(plugin instanceof EditPlugin.Deferred)
				{
					plugin.getPluginJAR().activatePlugin();
					plugin = plugin.getPluginJAR().getPlugin();
					break;
				}
			}
		}
		if (!loadIfNecessary) return plugin;
		String jarPath = PluginJAR.findPlugin(name);
		PluginJAR pjar = PluginJAR.load(jarPath, true);
		return pjar.getPlugin();
	} //}}}

	//{{{ getPlugins() method
	/**
	 * Returns an array of installed plugins.
	 */
	public static EditPlugin[] getPlugins()
	{
		Collection<EditPlugin> pluginList = new ArrayList<EditPlugin>();
		for(int i = 0; i < jars.size(); i++)
		{
			EditPlugin plugin = jars.elementAt(i).getPlugin();
			if(plugin != null)
				pluginList.add(plugin);
		}

		EditPlugin[] array = new EditPlugin[pluginList.size()];
		pluginList.toArray(array);
		return array;
	} //}}}

	//{{{ getPluginJARs() method
	/**
	 * Returns an array of installed plugins.
	 * @since jEdit 4.2pre1
	 */
	public static PluginJAR[] getPluginJARs()
	{
		PluginJAR[] array = new PluginJAR[jars.size()];
		jars.copyInto(array);
		return array;
	} //}}}

	//{{{ getPluginJAR() method
	/**
	 * Returns the JAR with the specified path name.
	 * @param path The path name
	 * @since jEdit 4.2pre1
	 */
	public static PluginJAR getPluginJAR(String path)
	{
		for(int i = 0; i < jars.size(); i++)
		{
			PluginJAR jar = jars.elementAt(i);
			if(jar.getPath().equals(path))
				return jar;
		}

		return null;
	} //}}}

	//{{{ addPluginJAR() method
	/**
	 * Loads the plugin JAR with the specified path. Some notes about this
	 * method:
	 *
	 * <ul>
	 * <li>Calling this at a time other than jEdit startup can have
	 * unpredictable results if the plugin has not been updated for the
	 * jEdit 4.2 plugin API.
	 * <li>You must make sure yourself the plugin is not already loaded.
	 * <li>After loading, you just make sure all the plugin's dependencies
	 * are satisified before activating the plugin, using the
	 * {@link PluginJAR#checkDependencies()} method.
	 * </ul>
	 *
	 * @param path The JAR file path
	 * @since jEdit 4.2pre1
	 */
	public static void addPluginJAR(String path)
	{
		PluginJAR jar = new PluginJAR(new File(path));
		jars.addElement(jar);
		jar.init();
		jEdit.unsetProperty("plugin-blacklist."+MiscUtilities.getFileName(path));
		EditBus.send(new PluginUpdate(jar,PluginUpdate.LOADED,false));
		if(!isMainThread())
		{
			EditBus.send(new DynamicMenuChanged("plugins"));
			initKeyBindings();
		}
	} //}}}

	//{{{ addPluginJARsFromDirectory() method
	/**
	 * Loads all plugins in a directory.
	 * @param directory The directory
	 * @since jEdit 4.2pre1
	 */
	private static void addPluginJARsFromDirectory(String directory)
	{
		Log.log(Log.NOTICE,jEdit.class,"Loading plugins from "
			+ directory);

		File file = new File(directory);
		if(!(file.exists() && file.isDirectory()))
			return;
		String[] plugins = file.list();
		if(plugins == null)
			return;

		for(int i = 0; i < plugins.length; i++)
		{
			String plugin = plugins[i];
			if(!plugin.toLowerCase().endsWith(".jar"))
				continue;

			String path = MiscUtilities.constructPath(directory,plugin);
			if (jEdit.getBooleanProperty("plugin-blacklist."+plugin))
				continue;
			
			addPluginJAR(path);
		}
	} //}}}

	//{{{ removePluginJAR() method
	/**
	 * Unloads the given plugin JAR with the specified path. Note that
	 * calling this at a time other than jEdit shutdown can have
	 * unpredictable results if the plugin has not been updated for the
	 * jEdit 4.2 plugin API.
	 *
	 * @param jar The <code>PluginJAR</code> instance
	 * @param exit Set to true if jEdit is exiting; enables some
	 * shortcuts so the editor can close faster.
	 * @since jEdit 4.2pre1
	 */
	public static void removePluginJAR(PluginJAR jar, boolean exit)
	{
		if(exit)
		{
			jar.uninit(true);
		}
		else
		{
			jar.uninit(false);
			jars.removeElement(jar);
			initKeyBindings();
		}

		EditBus.send(new PluginUpdate(jar,PluginUpdate.UNLOADED,exit));
		if(!isMainThread() && !exit)
			EditBus.send(new DynamicMenuChanged("plugins"));
	} //}}}

	//}}}

	//{{{ Action methods

	//{{{ getActionContext() method
	/**
	 * Returns the action context used to store editor actions.
	 * @since jEdit 4.2pre1
	 */
	public static ActionContext getActionContext()
	{
		return actionContext;
	} //}}}

	//{{{ addActionSet() method
	/**
	 * Adds a new action set to jEdit's list of ActionSets (viewable from the shortcuts
	 * option pane). By default, each plugin has one ActionSet,
	 * but some plugins may create dynamic action sets, such as ProjectViewer and Console.
	 * These plugins must call removeActionSet() when the plugin is unloaded.
	 *
	 * @since jEdit 4.0pre1
	 * @see #removeActionSet(ActionSet)
	 */
	public static void addActionSet(ActionSet actionSet)
	{
		actionContext.addActionSet(actionSet);
	} //}}}

	//{{{ removeActionSet() method
	/**
	 * Removes an action set from jEdit's list.
	 * Plugins that add a dynamic action set must call this method at plugin
	 * unload time.
	 * @since jEdit 4.2pre1
	 */
	public static void removeActionSet(ActionSet actionSet)
	{
		actionContext.removeActionSet(actionSet);
	} //}}}

	//{{{ getBuiltInActionSet() method
	/**
	 * Returns the set of commands built into jEdit.
	 * @since jEdit 4.2pre1
	 */
	public static ActionSet getBuiltInActionSet()
	{
		return builtInActionSet;
	} //}}}

	// {{{ getActionSets() method
	/**
	 * Returns all registered action sets.
	 *
	 * @return the ActionSet(s)
	 * @since jEdit 4.0pre1
	 */
	public static ActionSet[] getActionSets()
	{
		return actionContext.getActionSets();
	} // }}}

	//{{{ getAction() method
	/**
	 * Returns the specified action.
	 * @param name The action name
	 */
	public static EditAction getAction(String name)
	{
		return actionContext.getAction(name);
	} //}}}

	//{{{ getActionSetForAction() method
	/**
	 * Returns the action set that contains the specified action.
	 *
	 * @param action The action
	 * @since jEdit 4.2pre1
	 */
	public static ActionSet getActionSetForAction(String action)
	{
		return actionContext.getActionSetForAction(action);
	} //}}}

	//{{{ getActionNames() method
	/**
	 * Returns all registered action names.
	 */
	public static String[] getActionNames()
	{
		return actionContext.getActionNames();
	} //}}}

	//}}}

	//{{{ Edit mode methods

	//{{{ reloadModes() method
	/**
	 * Reloads all edit modes.  User defined edit modes are loaded after
	 * global modes so that user modes supercede global modes.
	 * @since jEdit 3.2pre2
	 */
	public static void reloadModes()
	{
		ModeProvider.instance.removeAll();

		//{{{ Load the global catalog first
		if(jEditHome == null)
			loadModeCatalog("/modes/catalog",true);
		else
		{
			loadModeCatalog(MiscUtilities.constructPath(jEditHome,
				"modes","catalog"),false);
		} //}}}

		//{{{ Load user catalog second so user modes override global modes.
		if(settingsDirectory != null)
		{
			File userModeDir = new File(MiscUtilities.constructPath(
				settingsDirectory,"modes"));
			if(!userModeDir.exists())
				userModeDir.mkdirs();

			File userCatalog = new File(MiscUtilities.constructPath(
				settingsDirectory,"modes","catalog"));
			if(!userCatalog.exists())
			{
				// create dummy catalog
				FileWriter out = null;
				try
				{
					out = new FileWriter(userCatalog);
					out.write(jEdit.getProperty("defaultCatalog"));
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,jEdit.class,io);
				}
				finally
				{
					IOUtilities.closeQuietly(out);
				}
			}

			loadModeCatalog(userCatalog.getPath(),false);
		} //}}}

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			// This reloads the token marker and sends a message
			// which causes edit panes to repaint their text areas
			buffer.setMode();

			buffer = buffer.next;
		}
	} //}}}

	//{{{ getMode() method
	/**
	 * Returns the edit mode with the specified name.
	 * @param name The edit mode
	 */
	public static Mode getMode(String name)
	{
		return ModeProvider.instance.getMode(name);
	} //}}}

	//{{{ getModes() method
	/**
	 * Returns an array of installed edit modes.
	 */
	public static Mode[] getModes()
	{
		return ModeProvider.instance.getModes();
	} //}}}

	//}}}

	//{{{ Buffer creation methods

	//{{{ openFiles() method
	/**
	 * Opens the file names specified in the argument array. This
	 * handles +line and +marker arguments just like the command
	 * line parser.
	 * @param parent The parent directory
	 * @param args The file names to open
	 * @since jEdit 3.2pre4
	 */
	public static Buffer openFiles(View view, String parent, String[] args)
	{
		Buffer retVal = null;
		Buffer lastBuffer = null;

		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if(arg == null)
				continue;
			else if(arg.startsWith("+line:") || arg.startsWith("+marker:"))
			{
				if(lastBuffer != null)
					gotoMarker(view,lastBuffer,arg);
				continue;
			}

			lastBuffer = openFile((View)null,parent,arg,false,null);

			if(retVal == null && lastBuffer != null)
				retVal = lastBuffer;
		}

		if(view != null && retVal != null)
			view.setBuffer(retVal,true);

		return retVal;
	} //}}}

	//{{{ openFile() methods
	/**
	 * Opens a file. Note that as of jEdit 2.5pre1, this may return
	 * null if the buffer could not be opened.
	 * @param view The view to open the file in
	 * @param path The file path
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 2.4pre1
	 */
	public static Buffer openFile(View view, String path)
	{
		return openFile(view,null,path,false,new Hashtable());
	}

	/**
	 * Opens a file. This may return null if the buffer could not be
	 * opened for some reason.
	 * @param view The view to open the file in. If it is null, the file
	 * will be opened and added to the bufferSet of the current edit pane,
	 * but not selected
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param newFile True if the file should not be loaded from disk
	 * be prompted if it should be reloaded
	 * @param props Buffer-local properties to set in the buffer
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 3.2pre10
	 */
	public static Buffer openFile(View view, String parent,
		String path, boolean newFile, Hashtable props)
	{
		return openFile(view == null ? null : view.getEditPane(), parent, path, newFile, props);
	}

	/**
	 * Opens a file. Note that as of jEdit 2.5pre1, this may return
	 * null if the buffer could not be opened.
	 * @param editPane the EditPane to open the file in.
	 * @param path The file path
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 4.3pre17
	 */
	public static Buffer openFile(EditPane editPane, String path)
	{
		return openFile(editPane,null,path,false,new Hashtable());
	}

	/**
	 * Opens a file. This may return null if the buffer could not be
	 * opened for some reason.
	 * @param editPane the EditPane to open the file in.
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param newFile True if the file should not be loaded from disk
	 * be prompted if it should be reloaded
	 * @param props Buffer-local properties to set in the buffer
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 4.3pre17
	 */
	public static Buffer openFile(EditPane editPane, String parent,
		String path, boolean newFile, Hashtable props)
	{
		PerspectiveManager.setPerspectiveDirty(true);

		if(editPane != null && parent == null && editPane.getBuffer() != null)
			parent = editPane.getBuffer().getDirectory();

		try
		{
			URL u = new URL(path);
			if ("file".equals(u.getProtocol()))
				path = URLDecoder.decode(u.getPath());
		}
		catch (MalformedURLException mue)
		{
			path = MiscUtilities.constructPath(parent,path);
		}


		if(props == null)
			props = new Hashtable();
		composeBufferPropsFromHistory(props, path);

		Buffer newBuffer;

		synchronized (editBusOrderingLock)
		{
			View view = editPane == null ? null : editPane.getView();
			synchronized(bufferListLock)
			{
				Buffer buffer = getBuffer(path);
				if(buffer != null)
				{
					if(editPane != null)
						editPane.setBuffer(buffer,true);

					return buffer;
				}

				newBuffer = new Buffer(path,newFile,false,props);

				if(!newBuffer.load(view,false))
					return null;
				addBufferToList(newBuffer);
				if (editPane != null)
					bufferSetManager.addBuffer(editPane, newBuffer);
				else
					bufferSetManager.addBuffer(jEdit.getActiveView(), newBuffer);
			}

			EditBus.send(new BufferUpdate(newBuffer,view,BufferUpdate.CREATED));
		}

		if(editPane != null)
			editPane.setBuffer(newBuffer,true);

		return newBuffer;
	} //}}}

	//{{{ openTemporary() methods
	/**
	 * Opens a temporary buffer. A temporary buffer is like a normal
	 * buffer, except that an event is not fired, the the buffer is
	 * not added to the buffers list.
	 *
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param newFile True if the file should not be loaded from disk
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 3.2pre10
	 */
	public static Buffer openTemporary(View view, String parent,
		String path, boolean newFile)
	{
		return openTemporary(view, parent, path, newFile, null);
	}
	/**
	 * Opens a temporary buffer. A temporary buffer is like a normal
	 * buffer, except that an event is not fired, the the buffer is
	 * not added to the buffers list.
	 *
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param newFile True if the file should not be loaded from disk
	 * @param props Buffer-local properties to set in the buffer
	 *
	 * @return the buffer, or null if jEdit was unable to load it
	 *
	 * @since jEdit 4.3pre10
	 */
	public static Buffer openTemporary(View view, String parent,
		String path, boolean newFile, Hashtable props)
	{
		if(view != null && parent == null)
			parent = view.getBuffer().getDirectory();

		if(MiscUtilities.isURL(path))
		{
			if("file".equals(MiscUtilities.getProtocolOfURL(path)))
				path = path.substring(5);
		}

		path = MiscUtilities.constructPath(parent,path);

		if(props == null)
			props = new Hashtable();
		composeBufferPropsFromHistory(props, path);

		synchronized(bufferListLock)
		{
			Buffer buffer = getBuffer(path);
			if(buffer != null)
				return buffer;

			buffer = new Buffer(path,newFile,true,props);
			buffer.setBooleanProperty(Buffer.ENCODING_AUTODETECT, true);
			if(!buffer.load(view,false))
				return null;
			else
				return buffer;
		}
	} //}}}

	//{{{ commitTemporary() method
	/**
	 * Adds a temporary buffer to the buffer list. This must be done
	 * before allowing the user to interact with the buffer in any
	 * way.
	 * @param buffer The buffer
	 */
	public static void commitTemporary(Buffer buffer)
	{
		if(!buffer.isTemporary())
			return;

		PerspectiveManager.setPerspectiveDirty(true);

		addBufferToList(buffer);
		buffer.commitTemporary();

		// send full range of events to avoid breaking plugins
		EditBus.send(new BufferUpdate(buffer,null,BufferUpdate.CREATED));
		EditBus.send(new BufferUpdate(buffer,null,BufferUpdate.LOAD_STARTED));
		EditBus.send(new BufferUpdate(buffer,null,BufferUpdate.LOADED));
	} //}}}

	//{{{ newFile() methods
	/**
	 * Creates a new `untitled' file.
	 *
	 * @param view The view to create the file in
	 *
	 * @return the new buffer
	 */
	public static Buffer newFile(View view)
	{
		return newFile(view == null ? null : view.getEditPane());
	}

	/**
	 * Creates a new `untitled' file.
	 * @param view The view to create the file in
	 * @param dir The directory to create the file in
	 *
	 * @return the new buffer
	 *
	 * @since jEdit 3.1pre2
	 */
	public static Buffer newFile(View view, String dir)
	{
		EditPane editPane = null;
		if (view != null)
		{
			editPane = view.getEditPane();
		}
		else
		{
			View v = getActiveView();
			if (v != null)
			{
				editPane = v.getEditPane();
			}
		}
		return newFile(editPane, dir);
	}

	/**
	 * Creates a new `untitled' file.
	 *
	 * @param editPane The editPane to create the file in
	 *
	 * @return the new buffer
	 * @since jEdit 4.3pre17
	 */
	public static Buffer newFile(EditPane editPane)
	{
		String path;

		if(editPane != null && editPane.getBuffer() != null)
		{
			path = editPane.getBuffer().getDirectory();
			VFS vfs = VFSManager.getVFSForPath(path);
			// don't want 'New File' to create a read only buffer
			// if current file is on SQL VFS or something
			if((vfs.getCapabilities() & VFS.WRITE_CAP) == 0)
				path = System.getProperty("user.home");
		}
		else
			path = null;

		return newFile(editPane,path);
	}

	/**
	 * Creates a new `untitled' file.
	 *
	 * @param editPane The editPane to create the file in
	 * @param dir The directory to create the file in
	 *
	 * @return the new buffer
	 *
	 * @since jEdit 4.3pre17
	 */
	public static Buffer newFile(EditPane editPane, String dir)
	{
		if (editPane != null)
		{
			BufferSet bufferSet = editPane.getBufferSet();
			Buffer[] buffers = bufferSet.getAllBuffers();
			for (Buffer buf:buffers)
			{
				if (buf.isUntitled() && !buf.isDirty())
				{

					if (!MiscUtilities.getParentOfPath(buf.getPath()).equals(dir))
					{
						// Find the highest Untitled-n file
						int untitledCount = getNextUntitledBufferId();

						Buffer newBuffer = openFile(editPane,dir,"Untitled-" + untitledCount,true,null);
						jEdit.closeBuffer(editPane, buf);
						return newBuffer;
					}
					/*  if  "never mark untitled buffers dirty"
					 *  is selected, we might have contents in non-dirty
					 *  untitled buffers. We must clear those contents
					 *  if user requested new file.
					 */
					int l = buf.getLength();
					if (l > 0)
						buf.remove(0, l);
					editPane.setBuffer(buf);
					return buf;
				}
			}
		}

		// Find the highest Untitled-n file
		int untitledCount = getNextUntitledBufferId();

		return openFile(editPane,dir,"Untitled-" + untitledCount,true,null);
	} //}}}

	//}}}

	//{{{ Buffer management methods

	//{{{ closeBuffer() method
	/**
	 * Closes a buffer. If there are unsaved changes, the user is
	 * prompted if they should be saved first.
	 * @param view The view
	 * @param buffer The buffer
	 * @return True if the buffer was really closed, false otherwise
	 */
	public static boolean closeBuffer(View view, Buffer buffer)
	{
		// Wait for pending I/O requests
		if(buffer.isPerformingIO())
		{
			VFSManager.waitForRequests();
			if(NotificationManager.errorOccurred())
				return false;
		}

		if(buffer.isDirty())
		{
			Object[] args = { buffer.getName() };
			int result = GUIUtilities.confirm(view,"notsaved",args,
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result == JOptionPane.YES_OPTION)
			{
				if(!buffer.save(view,null,true))
					return false;

				VFSManager.waitForRequests();
				if(buffer.getBooleanProperty(BufferIORequest
					.ERROR_OCCURRED))
				{
					return false;
				}
			}
			else if(result != JOptionPane.NO_OPTION)
				return false;
		}

		_closeBuffer(view,buffer);

		return true;
	} //}}}

	//{{{ closeBuffer() method
	/**
	 * Close a buffer.
	 * The buffer is first removed from the EditPane's bufferSet.
	 * If the buffer is not in any bufferSet after that, it is closed
	 * @param editPane the edit pane (it cannot be null)
	 * @param buffer the buffer (it cannot be null)
	 * @since jEdit 4.3pre15
	 */
	public static void closeBuffer(EditPane editPane, Buffer buffer)
	{
		switch (bufferSetManager.getScope())
		{
			case global:
				closeBuffer(editPane.getView(), buffer);
				break;
			case view:
				View[] views = jEdit.getViews();
				int viewOwner = 0;
				for (View view : views)
				{
					BufferSet bufferSet = view.getEditPane().getBufferSet();
					// no need to check every bufferSet since it's view scope
					if (bufferSet.indexOf(buffer) != -1)
					{
						viewOwner++;
						if (viewOwner > 1)
							break;
					}
				}
				if (viewOwner > 1)
				{
					// the buffer is in several view, we can remove it from bufferSet
					bufferSetManager.removeBuffer(editPane, buffer);
				}
				else
				{
					closeBuffer(editPane.getView(), buffer);
				}
				break;
			case editpane:
				int bufferSetsCount = bufferSetManager.countBufferSets(buffer);
				if (bufferSetsCount < 2)
				{
					closeBuffer(editPane.getView(), buffer);
				}
				else
				{
					bufferSetManager.removeBuffer(editPane, buffer);
				}
				break;
		}
	} //}}}

	//{{{ _closeBuffer() method
	/**
	 * Closes the buffer, even if it has unsaved changes.
	 * @param view The view, may be null
	 * @param buffer The buffer
	 *
	 * @exception NullPointerException if the buffer is null
	 *
	 * @since jEdit 2.2pre1
	 */
	public static void _closeBuffer(View view, Buffer buffer)
	{
		if(buffer.isClosed())
		{
			// can happen if the user presses C+w twice real
			// quick and the buffer has unsaved changes
			return;
		}

		PerspectiveManager.setPerspectiveDirty(true);

		if(!buffer.isNewFile())
		{
			if(view != null)
				view.getEditPane().saveCaretInfo();
			Integer _caret = (Integer)buffer.getProperty(Buffer.CARET);
			int caret = _caret == null ? 0 : _caret.intValue();

			BufferHistory.setEntry(buffer.getPath(),caret,
				(Selection[])buffer.getProperty(Buffer.SELECTION),
				buffer.getStringProperty(JEditBuffer.ENCODING),
				buffer.getMode().getName());
		}

		String path = buffer.getSymlinkPath();
		if((VFSManager.getVFSForPath(path).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			path = path.toLowerCase();
		}
		EditBus.send(new BufferUpdate(buffer,view,BufferUpdate.CLOSING));
		bufferHash.remove(path);
		removeBufferFromList(buffer);
		buffer.close();
		DisplayManager.bufferClosed(buffer);
		bufferSetManager.removeBuffer(buffer);
		EditBus.send(new BufferUpdate(buffer,view,BufferUpdate.CLOSED));
		if(jEdit.getBooleanProperty("persistentMarkers"))
			buffer.updateMarkersFile(view);
	} //}}}

	//{{{ closeAllBuffers() methods
	/**
	 * Closes all open buffers.
	 * @param view The view
	 *
	 * @return true if all buffers were closed, false otherwise
	 */
	public static boolean closeAllBuffers(View view)
	{
		return closeAllBuffers(view,false);
	}
	/**
	 * Closes all open buffers.
	 * @param view The view
	 * @param isExiting This must be false unless this method is
	 * being called by the exit() method
	 *
	 * @return true if all buffers were closed, false otherwise
	 */
	public static boolean closeAllBuffers(View view, boolean isExiting)
	{
		if(view != null)
			view.getEditPane().saveCaretInfo();

		boolean dirty = false;

		boolean saveRecent = !(isExiting && jEdit.getBooleanProperty("restore"));

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.isDirty())
			{
				dirty = true;
				break;
			}
			buffer = buffer.next;
		}

		if(dirty)
		{
			boolean ok = new CloseDialog(view).isOK();
			if(!ok)
				return false;
		}

		// Wait for pending I/O requests
		VFSManager.waitForRequests();
		if(NotificationManager.errorOccurred())
			return false;

		// close remaining buffers (the close dialog only deals with
		// dirty ones)

		buffer = buffersFirst;

		// zero it here so that BufferTabs doesn't have any problems
		buffersFirst = buffersLast = null;
		bufferHash.clear();
		bufferCount = 0;

		while(buffer != null)
		{
			if(!buffer.isNewFile() && saveRecent)
			{
				Integer _caret = (Integer)buffer.getProperty(Buffer.CARET);
				int caret = _caret == null ? 0 : _caret.intValue();
				BufferHistory.setEntry(buffer.getPath(),caret,
					(Selection[])buffer.getProperty(Buffer.SELECTION),
					buffer.getStringProperty(JEditBuffer.ENCODING),
					buffer.getMode().getName());
			}

			buffer.close();
			DisplayManager.bufferClosed(buffer);
			if(!isExiting)
			{
				bufferSetManager.removeBuffer(buffer);
				EditBus.send(new BufferUpdate(buffer,view,
					BufferUpdate.CLOSED));
			}
			if(jEdit.getBooleanProperty("persistentMarkers"))
				buffer.updateMarkersFile(view);
			buffer = buffer.next;
		}

		PerspectiveManager.setPerspectiveDirty(true);

		return true;
	} //}}}

	//{{{ saveAllBuffers() method
	/**
	 * Saves all open buffers.
	 * @param view The view
	 * @since jEdit 4.2pre1
	 */
	public static void saveAllBuffers(View view)
	{
		saveAllBuffers(view,jEdit.getBooleanProperty("confirmSaveAll"));
	} //}}}

	//{{{ saveAllBuffers() method
	/**
	 * Saves all open buffers.
	 * @param view The view
	 * @param confirm If true, a confirmation dialog will be shown first
	 * @since jEdit 2.7pre2
	 */
	public static void saveAllBuffers(View view, boolean confirm)
	{
		if(confirm)
		{
			int result = GUIUtilities.confirm(view,"saveall",null,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		Buffer current = view.getBuffer();

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.isDirty())
			{
				if(buffer.isNewFile())
					view.setBuffer(buffer,true);
				buffer.save(view,null,true,true);
			}

			buffer = buffer.next;
		}

		view.setBuffer(current,true);
	} //}}}

	//{{{ reloadAllBuffers() method
	/**
	 * Reloads all open buffers.
	 * @param view The view
	 * @param confirm If true, a confirmation dialog will be shown first
	 *	if any buffers are dirty
	 * @since jEdit 2.7pre2
	 */
	public static void reloadAllBuffers(View view, boolean confirm)
	{
		boolean hasDirty = false;
		Buffer[] buffers = jEdit.getBuffers();

		for(int i = 0; i < buffers.length && !hasDirty; i++)
			hasDirty = !buffers[i].isUntitled() && buffers[i].isDirty();

		if(confirm && hasDirty)
		{
			int result = GUIUtilities.confirm(view,"reload-all",null,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		// save caret info. Buffer.load() will load it.
		visit(new SaveCaretInfoVisitor());


		for(int i = 0; i < buffers.length; i++)
		{
			Buffer buffer = buffers[i];
			if (buffer.isUntitled())
				continue;
			buffer.load(view,true);
		}
	} //}}}

	//{{{ _getBuffer() method
	/**
	 * Returns the buffer with the specified path name. The path name
	 * must be an absolute, canonical, path.
	 *
	 * @param path The path name
	 *
	 * @return the searched buffer, or null if it is not already open
	 *
	 * @see MiscUtilities#constructPath(String,String)
	 * @see MiscUtilities#resolveSymlinks(String)
	 * @see #getBuffer(String)
	 *
	 * @since jEdit 4.2pre7
	 */
	public static Buffer _getBuffer(String path)
	{
		// paths on case-insensitive filesystems are stored as lower
		// case in the hash.
		if((VFSManager.getVFSForPath(path).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			path = path.toLowerCase();
		}

		synchronized(bufferListLock)
		{
			return bufferHash.get(path);
		}
	} //}}}

	//{{{ getBuffer() method
	/**
	 * Returns the buffer with the specified path name. The path name
	 * must be an absolute path. This method automatically resolves
	 * symbolic links. If performance is critical, cache the canonical
	 * path and call {@link #_getBuffer(String)} instead.
	 *
	 * @param path The path name
	 *
	 * @return the searched buffer, or null if it is not already open
	 *
	 * @see MiscUtilities#constructPath(String,String)
	 * @see MiscUtilities#resolveSymlinks(String)
	 */
	public static Buffer getBuffer(String path)
	{
		return _getBuffer(MiscUtilities.resolveSymlinks(path));
	} //}}}

	//{{{ getBuffers() method
	/**
	 * Returns an array of open buffers.
	 * @return  an array of all open buffers
	 */
	public static Buffer[] getBuffers()
	{
		synchronized(bufferListLock)
		{
			Buffer[] buffers = new Buffer[bufferCount];
			Buffer buffer = buffersFirst;
			for(int i = 0; i < bufferCount; i++)
			{
				buffers[i] = buffer;
				buffer = buffer.next;
			}
			return buffers;
		}
	} //}}}

	//{{{ getBufferCount() method
	/**
	 * Returns the number of open buffers.
	 */
	public static int getBufferCount()
	{
		return bufferCount;
	} //}}}

	//{{{ getFirstBuffer() method
	/**
	 * Returns the first buffer.
	 */
	public static Buffer getFirstBuffer()
	{
		return buffersFirst;
	} //}}}

	//{{{ getLastBuffer() method
	/**
	 * Returns the last buffer.
	 * @return the last buffer
	 */
	public static Buffer getLastBuffer()
	{
		return buffersLast;
	} //}}}

	//{{{ moveBuffer() method
	/**
	 * Moves a buffer from a old position to a new position in the
	 * BufferSet used in an EditPane.
	 * @param editPane The EditPane in which a buffer is moved
	 * @param oldPosition The position before the move
	 * @param newPosition The position after the move
	 */
	public static void moveBuffer(EditPane editPane,
		int oldPosition, int newPosition)
	{
		bufferSetManager.moveBuffer(editPane, oldPosition, newPosition);
	} //}}}

	//{{{ getBufferSetManager() method
	/**
	 * Returns the bufferSet manager.
	 * @return the bufferSetManager
	 * @since jEdit 4.3pre15
	 */
	public static BufferSetManager getBufferSetManager()
	{
		return bufferSetManager;
	} //}}}

	//{{{ getPropertyManager() method
	/**
	 * @return the propertyManager
	 * @since jEdit 4.3pre15
	 */
	public static JEditPropertyManager getPropertyManager()
	{
		return propertyManager;
	} //}}}

	//{{{ checkBufferStatus() methods
	/**
	 * Checks each buffer's status on disk and shows the dialog box
	 * informing the user that buffers changed on disk, if necessary.
	 * @param view The view
	 * @since jEdit 4.2pre1
	 */
	public static void checkBufferStatus(View view)
	{
		checkBufferStatus(view,false);
	}

	/**
	 * Checks buffer status on disk and shows the dialog box
	 * informing the user that buffers changed on disk, if necessary.
	 * @param view The view
	 * @param currentBuffer indicates whether to check only the current buffer
	 * @since jEdit 4.2pre1
	 */
	public static void checkBufferStatus(View view, boolean currentBuffer)
	{
		// still need to call the status check even if the option is
		// off, so that the write protection is updated if it changes
		// on disk

		// auto reload changed buffers?
		boolean autoReload = getBooleanProperty("autoReload");

		// the problem with this is that if we have two edit panes
		// looking at the same buffer and the file is reloaded both
		// will jump to the same location
		visit(new SaveCaretInfoVisitor());

		Buffer buffer = buffersFirst;

		int[] states = new int[bufferCount];
		int i = 0;
		boolean notifyFileChanged = false;
		while(buffer != null)
		{
			if(currentBuffer && buffer != view.getBuffer())
			{
				buffer = buffer.next;
				i++;
				continue;
			}

			states[i] = buffer.checkFileStatus(view);

			switch(states[i])
			{
			case Buffer.FILE_CHANGED:
				if(buffer.getAutoReload())
				{
					if(buffer.isDirty())
						notifyFileChanged = true;
					else
						buffer.load(view,true);
				}
				else	// no automatic reload even if general setting is true
					autoReload = false;
				// don't notify user if "do nothing" was chosen
				if(buffer.getAutoReloadDialog())
					notifyFileChanged = true;
				break;
			case Buffer.FILE_DELETED:
				notifyFileChanged = true;
				break;
			}

			buffer = buffer.next;
			i++;
		}

		if(notifyFileChanged)
			new FilesChangedDialog(view,states,autoReload);
	} //}}}

	//}}}

	//{{{ View methods

	//{{{ getInputHandler() method
	/**
	 * Returns the current input handler (key binding to action mapping)
	 * @see org.gjt.sp.jedit.gui.InputHandler
	 */
	public static InputHandler getInputHandler()
	{
		return inputHandler;
	} //}}}

	/* public static void newViewTest()
	{
		long time = System.currentTimeMillis();
		for(int i = 0; i < 30; i++)
		{
			Buffer b = newFile(null);
			b.insert(0,"x");
			new View(b,null,false);
		}
		System.err.println(System.currentTimeMillis() - time);
	} */

	//{{{ newView() methods
	/**
	 * Creates a new view.
	 * @param view An existing view
	 * @since jEdit 3.2pre2
	 */
	public static View newView(View view)
	{
		return newView(view,null,false);
	}
	/**
	 * Creates a new view of a buffer.
	 * @param view An existing view
	 * @param buffer The buffer
	 */
	public static View newView(View view, Buffer buffer)
	{
		return newView(view,buffer,false);
	}
	/**
	 * Creates a new view of a buffer.
	 * @param view An existing view
	 * @param buffer The buffer
	 * @param plainView If true, the view will not have dockable windows or
	 * tool bars.
	 *
	 * @since 4.1pre2
	 */
	public static View newView(View view, Buffer buffer, boolean plainView)
	{
		View.ViewConfig config;
		if(view != null && (plainView == view.isPlainView()))
		{
			config = view.getViewConfig();
			config.x -= 20;
			config.y += 20;
		}
		else
		{
			config = new View.ViewConfig(plainView);
		}
		return newView(view,buffer,config);
	}

	/**
	 * Creates a new view.
	 * @param view An existing view
	 * @param buffer A buffer to display, or null
	 * @param config Encapsulates the view geometry, split configuration
	 * and if the view is a plain view
	 * @since jEdit 4.2pre1
	 */
	public static View newView(View view, Buffer buffer, View.ViewConfig config)
	{
		// Mark the perspective as dirty, unless the new view is created
		// during jEdit startup, by the loading of the perspective.
		if (isStartupDone())
			PerspectiveManager.setPerspectiveDirty(true);

		try
		{
			if(view != null)
			{
				view.showWaitCursor();
				view.getEditPane().saveCaretInfo();
			}

			View newView = new View(buffer,config);
			addViewToList(newView);

			newView.pack();
			newView.adjust(view, config);

			EditBus.send(new ViewUpdate(newView,ViewUpdate.CREATED));

			newView.setVisible(true);

			if(!config.plainView)
			{
				int index;
				synchronized (startupDone)
				{
					index = startupDone.size();
					startupDone.add(false);
				}
				EventQueue.invokeLater(new DockingLayoutSetter(
					newView, config, index));
			}

			// show tip of the day
			if(newView == viewsFirst)
			{
				newView.getTextArea().requestFocus();

				// Don't show the welcome message if jEdit was started
				// with the -nosettings switch
				if(settingsDirectory != null && getBooleanProperty("firstTime"))
					new HelpViewer("welcome.html");
				else if(jEdit.getBooleanProperty("tip.show"))
					new TipOfTheDay(newView);

				setBooleanProperty("firstTime",false);
			}
			else
				GUIUtilities.requestFocus(newView,newView.getTextArea());

			return newView;
		}
		finally
		{
			if(view != null)
				view.hideWaitCursor();
		}
	} //}}}

	//{{{ closeView() method
	/**
	 * Closes a view.
	 *
	 * jEdit will exit if this was the last open view.
	 */
	public static void closeView(View view)
	{
		closeView(view,true);
	} //}}}

	//{{{ getViews() method
	/**
	 * Returns an array of all open views.
	 */
	public static View[] getViews()
	{
		View[] views = new View[viewCount];
		View view = viewsFirst;
		for(int i = 0; i < viewCount; i++)
		{
			views[i] = view;
			view = view.next;
		}
		return views;
	} //}}}

	//{{{ getViewCount() method
	/**
	 * Returns the number of open views.
	 */
	public static int getViewCount()
	{
		return viewCount;
	} //}}}

	//{{{ getFirstView() method
	/**
	 * Returns the first view.
	 */
	public static View getFirstView()
	{
		return viewsFirst;
	} //}}}

	//{{{ getLastView() method
	/**
	 * Returns the last view.
	 */
	public static View getLastView()
	{
		return viewsLast;
	} //}}}

	//{{{ getActiveView() method
	/**
	 * Returns the currently focused view.
	 * @since jEdit 4.1pre1
	 */
	public static View getActiveView()
	{
		if(activeView == null)
		{
			// eg user just closed a view and didn't focus another
			return viewsFirst;
		}
		else
			return activeView;
	} //}}}

	//}}}

	//{{{ Miscellaneous methods

	//{{{ relocateSettings() method
	public static void relocateSettings()
	{
		String oldSettingsPath = MiscUtilities.constructPath(
				System.getProperty("user.home"),
				".jedit");
		File oldSettingsDir = new File(oldSettingsPath);
		File newSettingsDir = new File(settingsDirectory);
		if(oldSettingsDir.exists() && !newSettingsDir.exists())
		{
			Log.log(Log.NOTICE,jEdit.class,"Old settings directory found (HOME/.jedit). Moving to new location ("+newSettingsDir+ ')');
			try
			{
				oldSettingsDir.renameTo(newSettingsDir);
			}
			catch(SecurityException se)
			{
				Log.log(Log.ERROR,jEdit.class,se);
			}
		}
	}
	//}}}

	//{{{ isStartupDone() method
	/**
	 * Whether jEdit startup is over.
	 * @since jEdit 4.3pre17
	 */
	public static boolean isStartupDone()
	{
		return (! startupDone.contains(false));
	} //}}}

	//{{{ isMainThread() method
	/**
	 * Returns true if the currently running thread is the main thread.
	 * @since jEdit 4.2pre1
	 */
	public static boolean isMainThread()
	{
		return Thread.currentThread() == mainThread;
	} //}}}

	//{{{ isBackgroundMode() method
	/**
	 * Returns true if jEdit was started with the <code>-background</code>
	 * command-line switch.
	 * @since jEdit 4.0pre4
	 */
	public static boolean isBackgroundModeEnabled()
	{
		return background;
	} //}}}

	//{{{ showMemoryDialog() method
	/**
	 * Performs garbage collection and displays a dialog box showing
	 * memory status.
	 * @param view The view
	 * @since jEdit 4.0pre1
	 */
	public static void showMemoryDialog(View view)
	{
		Runtime rt = Runtime.getRuntime();
		long usedBefore = rt.totalMemory() - rt.freeMemory();
		System.gc();
		long free = rt.freeMemory();
		long total = rt.totalMemory();
		long used = total - free;

		int totalKb = (int) (total / 1024);
		int usedKb = (int) (used / 1024);
		JProgressBar progress = new JProgressBar(0,totalKb);
		progress.setValue(usedKb);
		progress.setStringPainted(true);
		progress.setString(jEdit.getProperty("memory-status.use",
			new Object[] { usedKb, totalKb }));

		Object[] message = new Object[4];
		message[0] = getProperty("memory-status.gc",
			new Object[] { (usedBefore - used) / 1024 });
		message[1] = Box.createVerticalStrut(12);
		message[2] = progress;
		message[3] = Box.createVerticalStrut(6);

		JOptionPane.showMessageDialog(view,message,
			jEdit.getProperty("memory-status.title"),
			JOptionPane.INFORMATION_MESSAGE);
	} //}}}

	//{{{ getJEditHome() method
	/**
	 * Returns the jEdit install directory.
	 */
	public static String getJEditHome()
	{
		return jEditHome;
	} //}}}

	//{{{ getSettingsDirectory() method
	/**
	 * Returns the path of the directory where user-specific settings
	 * are stored. This will be <code>null</code> if jEdit was
	 * started with the <code>-nosettings</code> command-line switch; do not
	 * blindly use this method without checking for a <code>null</code>
	 * return value first.
	 */
	public static String getSettingsDirectory()
	{
		return settingsDirectory;
	} //}}}

	//{{{ getJARCacheDirectory() method
	/**
	 * Returns the directory where plugin cache files are stored.
	 * @since jEdit 4.2pre1
	 */
	public static String getJARCacheDirectory()
	{
		return jarCacheDirectory;
	} //}}}

	//{{{ backupSettingsFile() method
	/**
	 * Backs up the specified file in the settings directory.
	 * You should call this on any settings files your plugin
	 * writes.
	 * @param file The file
	 * @since jEdit 4.0pre1
	 */
	public static void backupSettingsFile(File file)
	{
		if(settingsDirectory == null || !file.exists())
			return;

		String backupDir = MiscUtilities.constructPath(
			settingsDirectory,"settings-backup");
		File dir = new File(backupDir);
		if(!dir.exists())
			dir.mkdirs();

		// ... sweet. saveBackup() will create backupDir if it
		// doesn't exist.

		MiscUtilities.saveBackup(file,5,null,"~",backupDir);
	} //}}}

	//{{{ saveSettings() method
	/**
	 * Saves all user preferences to disk.
	 */
	public static void saveSettings()
	{
		if(settingsDirectory == null)
			return;

		Abbrevs.save();
		FavoritesVFS.saveFavorites();
		HistoryModel.saveHistory();
		Registers.saveRegisters();
		SearchAndReplace.save();
		BufferHistory.save();
		KillRing.getInstance().save();

		File file1 = new File(MiscUtilities.constructPath(
			settingsDirectory,"#properties#save#"));
		File file2 = new File(MiscUtilities.constructPath(
			settingsDirectory,"properties"));
		if(file2.exists() && file2.lastModified() != propsModTime)
		{
			Log.log(Log.WARNING,jEdit.class,file2 + " changed"
				+ " on disk; will not save user properties");
		}
		else
		{
			backupSettingsFile(file2);
			OutputStream out = null;
			try
			{
				out = new FileOutputStream(file1);
				propMgr.saveUserProps(out);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,jEdit.class,io);
			}
			finally
			{
				IOUtilities.closeQuietly(out);
			}
			file2.delete();
			if (! file1.renameTo(file2))
			{
				Log.log(Log.ERROR,jEdit.class,"Failed to rename \"" + file1 +
					"\" to the user properties file \"" + file2 + "\".");
			}
			propsModTime = file2.lastModified();
		}
	} //}}}

	//{{{ exit() method
	/**
	 * Exits cleanly from jEdit, prompting the user if any unsaved files
	 * should be saved first.
	 * @param view The view from which this exit was called
	 * @param reallyExit If background mode is enabled and this parameter
	 * is true, then jEdit will close all open views instead of exiting
	 * entirely.
	 */
	public static void exit(View view, boolean reallyExit)
	{
		// Close dialog, view.close() call need a view...
		if(view == null)
			view = activeView;

		// Wait for pending I/O requests
		VFSManager.waitForRequests();

		// Create a new EditorExitRequested
		EditorExitRequested eer = new EditorExitRequested(view);

		// Send EditorExitRequested
		EditBus.send(eer);

		// Check if the ExitRequest has been cancelled
		// if so, do not proceed anymore in the exiting
		if (eer.hasBeenExitCancelled())
		{
			Log.log(Log.MESSAGE, jEdit.class, "Exit has been cancelled");
			return;
		}

		// Even if reallyExit is false, we still exit properly
		// if background mode is off
		reallyExit |= !background;

		PerspectiveManager.savePerspective(false);

		try
		{
			PerspectiveManager.setPerspectiveEnabled(false);

			// Close all buffers
			if(!closeAllBuffers(view,reallyExit))
				return;
		}
		finally
		{
			PerspectiveManager.setPerspectiveEnabled(true);
		}

		// If we are running in background mode and
		// reallyExit was not specified, then return here.
		if(!reallyExit)
		{
			// in this case, we can't directly call
			// view.close(); we have to call closeView()
			// for all open views
			view = viewsFirst;
			while(view != null)
			{
				closeView(view,false);
				view = view.next;
			}

			// Save settings in case user kills the backgrounded
			// jEdit process
			saveSettings();
		}
		else
		{

			// Send EditorExiting
			EditBus.send(new EditorExiting(null));

			// Save view properties here
			if(view != null)
			{
				view.close();
				removeViewFromList(view);
			}

			// Stop autosave timer
			Autosave.stop();

			// Stop server
			if(server != null)
				server.stopServer();

			// Stop all plugins
			PluginJAR[] plugins = getPluginJARs();
			for(int i = 0; i < plugins.length; i++)
			{
				removePluginJAR(plugins[i],true);
			}


			// Save settings
			saveSettings();

			// Close activity log stream
			Log.closeStream();

			// Byebye...
			System.exit(0);
		}
	} //}}}

	//{{{ getEditServer() method
	/**
	 * Returns the edit server instance. You can use this to find out the
	 * port number jEdit is listening on.
	 * @since jEdit 4.2pre10
	 */
	public static EditServer getEditServer()
	{
		return server;
	} //}}}

	//{{{ visit() method
	/**
	 * Visit the views, editpanes and textareas
	 * @param visitor the visitor
	 * @since jEdit 4.3pre13
	 */
	public static void visit(JEditVisitor visitor)
	{
		View view = jEdit.getFirstView();
		while (view != null)
		{
			visitor.visit(view);
			view.visit(visitor);
			view = view.getNext();
		}
	} //}}}

	//{{{ getRegisterStatusPrompt() method
	/**
	 * Returns the status prompt for the given register action. Only
	 * intended to be called from <code>actions.xml</code>.
	 * @since jEdit 4.3pre16
	 */
	public static String getRegisterStatusPrompt(String action)
	{
		String registerNameString = Registers.getRegisterNameString();
		return jEdit.getProperty("view.status." + action,
			new String[] {registerNameString == null ?
				      jEdit.getProperty("view.status.no-registers") :
				      registerNameString});
	} //}}}

	//}}}

	//{{{ Package-private members

	//{{{ updatePosition() method
	/**
	 * If buffer sorting is enabled, this repositions the buffer.
	 */
	static void updatePosition(String oldPath, Buffer buffer)
	{
		if((VFSManager.getVFSForPath(oldPath).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			oldPath = oldPath.toLowerCase();
		}

		bufferHash.remove(oldPath);

		String path = buffer.getSymlinkPath();
		if((VFSManager.getVFSForPath(path).getCapabilities()
			& VFS.CASE_INSENSITIVE_CAP) != 0)
		{
			path = path.toLowerCase();
		}

		bufferHash.put(path,buffer);

		if(sortBuffers)
		{
			removeBufferFromList(buffer);
			addBufferToList(buffer);
		}
	} //}}}

	//{{{ loadMode() method
	/**
	 * Loads an XML-defined edit mode from the specified reader.
	 * @param mode The edit mode
	 */
	/* package-private */ static void loadMode(Mode mode)
	{
		final String fileName = (String)mode.getProperty("file");
		XModeHandler xmh = new XModeHandler(mode.getName())
		{
			@Override
			public void error(String what, Object subst)
			{
				String msg;

				Object line = "<unknown>";
				if(subst == null)
					msg = jEdit.getProperty("xmode-error." + what);
				else
				{
					msg = jEdit.getProperty("xmode-error." + what,
						new String[] { subst.toString() });
					if(subst instanceof Throwable)
						Log.log(Log.ERROR,this,subst);
					if (subst instanceof SAXParseException)
					{
						line = ((SAXParseException)subst).getLineNumber();
					}
				}

				Object[] args = { fileName, line, null, msg };
				GUIUtilities.error(null,"xmode-error",args);
			}

			@Override
			public TokenMarker getTokenMarker(String modeName)
			{
				Mode mode = getMode(modeName);
				if(mode == null)
					return null;
				else
					return mode.getTokenMarker();
			}
		};
		ModeProvider.instance.loadMode(mode, xmh);
	} //}}}

	//{{{ addPluginProps() method
	static void addPluginProps(Properties map)
	{
		propMgr.addPluginProps(map);
	} //}}}

	//{{{ removePluginProps() method
	static void removePluginProps(Properties map)
	{
		propMgr.removePluginProps(map);
	} //}}}

	//{{{ pluginError() method
	/**
	 * @param path
	 * @param messageProp - a property of a message to print
	 * @param args a list of arguments which correspond to {0} and {1} in the string to print.
	 */
	static void pluginError(String path, String messageProp,
		Object[] args)
	{
		synchronized(pluginErrorLock)
		{
			if(pluginErrors == null)
				pluginErrors = new Vector<ErrorEntry>();

			ErrorEntry newEntry =
				new ErrorEntry(
				path,messageProp,args);

			for (ErrorEntry pluginError : pluginErrors)
			{
				if (pluginError.equals(newEntry))
					return;
			}
			pluginErrors.addElement(newEntry);

			if(isStartupDone())
			{
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						showPluginErrorDialog();
					}
				});
			}
		}
	} //}}}

	//{{{ setActiveView() method
	static void setActiveView(View view)
	{
		jEdit.activeView = view;
	} //}}}

	//{{{ getActiveViewInternal() method
	/**
	 * Returns the internal active view, which might be null.
	 *
	 * @since 4.3pre10
	 */
	public static View getActiveViewInternal()
	{
		return activeView;
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Static variables
	private static String jEditHome;
	private static String settingsDirectory;
	private static String jarCacheDirectory;
	private static long propsModTime;
	private static PropertyManager propMgr;
	private static EditServer server;
	private static boolean background;
	private static ActionContext actionContext;
	private static ActionSet builtInActionSet;
	private static Vector<ErrorEntry> pluginErrors;
	private static final Object pluginErrorLock = new Object();
	private static Vector<PluginJAR> jars;

	private static boolean saveCaret;
	private static InputHandler inputHandler;

	private static BufferSetManager bufferSetManager;

	// buffer link list
	private static boolean sortBuffers;
	private static boolean sortByName;
	private static int bufferCount;
	private static Buffer buffersFirst;
	private static Buffer buffersLast;
	private static Map<String, Buffer> bufferHash;

	// makes openTemporary() thread-safe
	private static final Object bufferListLock = new Object();

	private static final Object editBusOrderingLock	= new Object();

	// view link list
	private static int viewCount;
	private static View viewsFirst;
	private static View viewsLast;
	private static View activeView;

	private static final List<Boolean> startupDone = new Vector<Boolean>();

	private static Thread mainThread;
	//}}}

	private jEdit() {}

	//{{{ usage() method
	private static void usage()
	{
		System.out.println("Usage: jedit [<options>] [<files>]");

		System.out.println("	<file> +marker:<marker>: Positions caret"
			+ " at marker <marker>");
		System.out.println("	<file> +line:<line>: Positions caret"
			+ " at line number <line>");
		System.out.println("	<file> +line:<line>,<column>: Positions caret"
			+ " at line number <line> and column number <column>");
		System.out.println("	--: End of options");
		System.out.println("	-nosplash: Don't show splash screen");
		System.out.println("	-background: Run in background mode");
		System.out.println("	-nobackground: Disable background mode (default)");
		System.out.println("	-gui: Only if running in background mode; open initial view (default)");
		System.out.println("	-nogui: Only if running in background mode; don't open initial view");
		System.out.println("	-log=<level>: Log messages with level equal to or higher than this to");
		System.out.println("	 standard error. <level> must be between 1 and 9. Default is 7.");
		System.out.println("	-newplainview: Client instance opens a new plain view");
		System.out.println("	-newview: Client instance opens a new view (default)");
		System.out.println("	-plugins: Load plugins (default)");
		System.out.println("	-noplugins: Don't load any plugins");
		System.out.println("	-restore: Restore previously open files (default)");
		System.out.println("	-norestore: Don't restore previously open files");
		System.out.println("	-reuseview: Client instance reuses existing view");
		System.out.println("	-quit: Quit a running instance");
		System.out.println("	-run=<script>: Run the specified BeanShell script");
		System.out.println("	-server: Read/write server info from/to $HOME/.jedit/server (default)");
		System.out.println("	-server=<name>: Read/write server info from/to $HOME/.jedit/<name>");
		System.out.println("	-noserver: Don't start edit server");
		System.out.println("	-settings=<path>: Load user-specific settings from <path>");
		System.out.println("	-nosettings: Don't load user-specific settings");
		System.out.println("	-startupscripts: Run startup scripts (default)");
		System.out.println("	-nostartupscripts: Don't run startup scripts");
		System.out.println("	-usage: Print this message and exit");
		System.out.println("	-version: Print jEdit version and exit");
		System.out.println("	-wait: Wait until the user closes the specified buffer in the server");
		System.out.println("	 instance. Does nothing if passed to the initial jEdit instance.");
		System.out.println();
		System.out.println("Report bugs to http://sourceforge.net/tracker/?group_id=588&atid=100588");
	} //}}}

	//{{{ version() method
	private static void version()
	{
		System.out.println("jEdit " + getVersion());
	} //}}}

	//{{{ makeServerScript() method
	/**
	 * Creates a BeanShell script that can be sent to a running edit server.
	 */
	private static String makeServerScript(boolean wait,
		boolean restore, boolean newView,
		boolean newPlainView, String[] args,
		String scriptFile)
	{
		StringBuilder script = new StringBuilder();

		String userDir = System.getProperty("user.dir");

		script.append("parent = \"");
		script.append(StandardUtilities.charsToEscapes(userDir));
		script.append("\";\n");

		script.append("args = new String[");
		script.append(args.length);
		script.append("];\n");

		for(int i = 0; i < args.length; i++)
		{
			script.append("args[");
			script.append(i);
			script.append("] = ");

			if(args[i] == null)
				script.append("null");
			else
			{
				script.append('"');
				script.append(StandardUtilities.charsToEscapes(args[i]));
				script.append('"');
			}

			script.append(";\n");
		}

		script.append("view = jEdit.getLastView();\n");
		script.append("buffer = EditServer.handleClient(");
		script.append(restore).append(',').append(newView).append(',').append(newPlainView);
		script.append(",parent,args);\n");
		script.append("if(buffer != null && ").append(wait).append(") {\n");
		script.append("\tbuffer.setWaitSocket(socket);\n");
		script.append("\tdoNotCloseSocket = true;\n");
		script.append("}\n");
		script.append("if(view != jEdit.getLastView() && ").append(wait).append(") {\n");
		script.append("\tjEdit.getLastView().setWaitSocket(socket);\n");
		script.append("\tdoNotCloseSocket = true;\n");
		script.append("}\n");
		script.append("if(doNotCloseSocket == void)\n");
		script.append("\tsocket.close();\n");

		if(scriptFile != null)
		{
			scriptFile = MiscUtilities.constructPath(userDir,scriptFile);
			script.append("BeanShell.runScript(view,\"")
				.append(StandardUtilities.charsToEscapes(scriptFile))
				.append("\",null,this.namespace);\n");
		}

		return script.toString();
	} //}}}

	//{{{ initMisc() method
	/**
	 * Initialise various objects, register protocol handlers.
	 */
	private static void initMisc()
	{
		ModeProvider.instance = new ModeProvider()
		{
			@Override
			protected void error(String fileName, Throwable e)
			{
				Log.log(Log.ERROR, this, e);
				if (e instanceof SAXParseException)
				{
					String message = e.getMessage();
					int line = ((SAXParseException)e).getLineNumber();
					int col = ((SAXParseException)e).getColumnNumber();

					Object[] args = { fileName, line, col, message };
					GUIUtilities.error(null,"xmode-error",args);
				}
			}
		};
		jars = new Vector<PluginJAR>();
		FoldHandler.foldHandlerProvider = new ServiceManager.ServiceFoldHandlerProvider();
		actionContext = new ActionContext()
		{
			@Override
			public void invokeAction(EventObject evt,
				EditAction action)
			{
				View view = GUIUtilities.getView(
					(Component)evt.getSource());

				boolean actionBarVisible;
				if(view.getActionBar() == null
					|| !view.getActionBar().isShowing())
					actionBarVisible = false;
				else
				{
					actionBarVisible = view.getActionBar()
						.isVisible();
				}

				view.getInputHandler().invokeAction(action);

				if(actionBarVisible)
				{
					// XXX: action bar might not be 'temp'
					ActionBar actionBar = view
						.getActionBar();
					if(actionBar != null)
						view.removeToolBar(actionBar);
				}
			}
		};

		bufferHash = new HashMap<String, Buffer>();

		inputHandler = new DefaultInputHandler(null);
		// Add our protocols to java.net.URL's list
		System.getProperties().put("java.protocol.handler.pkgs",
			"org.gjt.sp.jedit.proto|" +
			System.getProperty("java.protocol.handler.pkgs",""));

		// Set the User-Agent string used by the java.net HTTP handler
		String userAgent = "jEdit/" + getVersion()
			+ " (Java " + System.getProperty("java.version")
			+ ". " + System.getProperty("java.vendor")
			+ "; " + System.getProperty("os.arch") + ')';
		System.getProperties().put("http.agent",userAgent);

		/* Determine installation directory.
		 * If the jedit.home property is set, use that.
		 * Then, look for jedit.jar in the classpath.
		 * If that fails, assume this is the web start version. */
		jEditHome = System.getProperty("jedit.home");
		if(jEditHome == null)
		{
			String classpath = System
				.getProperty("java.class.path");
			int index = classpath.toLowerCase()
				.indexOf("jedit.jar");
			int start = classpath.lastIndexOf(File
				.pathSeparator,index) + 1;
			// if started with java -jar jedit.jar
			if(start == index)
			{
				jEditHome = System.getProperty("user.dir");
			}
			else if(index > start)
			{
				jEditHome = classpath.substring(start,
					index - 1);
			}
			else
			{
				// check if web start
				/* if(jEdit.class.getResource("/modes/catalog") != null)
				{
					// modes bundled in; hence web start
					jEditHome = null;
				}
				else */
				{
					// use user.dir as last resort
					jEditHome = System.getProperty("user.dir");

					Log.log(Log.WARNING,jEdit.class,"jedit.jar not in class path!");
					Log.log(Log.WARNING,jEdit.class,"Assuming jEdit is installed in "
						+ jEditHome + '.');
					Log.log(Log.WARNING,jEdit.class,"Override with jedit.home "
						+ "system property.");
				}
			}
		}

		jEditHome = MiscUtilities.resolveSymlinks(jEditHome);

		Log.log(Log.MESSAGE,jEdit.class,"jEdit home directory is " + jEditHome);

		if(settingsDirectory != null)
		{
			jarCacheDirectory = MiscUtilities.constructPath(
				settingsDirectory,"jars-cache");
			new File(jarCacheDirectory).mkdirs();
		}

		//if(jEditHome == null)
		//	Log.log(Log.DEBUG,jEdit.class,"Web start mode");

		// Add an EditBus component that will reload edit modes and
		// macros if they are changed from within the editor
		EditBus.addToBus(new SettingsReloader());

		// Perhaps if Xerces wasn't slightly brain-damaged, we would
		// not need this
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Thread.currentThread().setContextClassLoader(
					new JARClassLoader());
			}
		});
		// Also set the ContextClassLoader for the main jEdit thread.
		// This way, the ContextClassLoader will be a JARClassLoader
		// even at plugin activation.
		Thread.currentThread().setContextClassLoader(new JARClassLoader());
	} //}}}

	//{{{ initSystemProperties() method
	/**
	 * Load system properties.
	 */
	private static void initSystemProperties()
	{
		propMgr = new PropertyManager();

		try
		{
			propMgr.loadSystemProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit.props"));
			propMgr.loadSystemProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit_gui.props"));
			propMgr.loadSystemProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit_keys.props"));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,
				"Error while loading system properties!");
			Log.log(Log.ERROR,jEdit.class,
				"One of the following property files could not be loaded:\n"
				+ "- jedit.props\n"
				+ "- jedit_gui.props\n"
				+ "- jedit_keys.props\n"
				+ "jedit.jar is probably corrupt.");
			Log.log(Log.ERROR,jEdit.class,e);
			System.exit(1);
		}
	} //}}}

	//{{{ initSiteProperties() method
	/**
	 * Load site properties.
	 */
	private static void initSiteProperties()
	{
		// site properties are loaded as default properties, overwriting
		// jEdit's system properties

		String siteSettingsDirectory = MiscUtilities.constructPath(
			jEditHome, "properties");
		File siteSettings = new File(siteSettingsDirectory);

		if (!(siteSettings.exists() && siteSettings.isDirectory()))
			return;

		String[] snippets = siteSettings.list();
		if (snippets == null)
			return;

		Arrays.sort(snippets,
			new StandardUtilities.StringCompare<String>(true));

		for (int i = 0; i < snippets.length; ++i)
		{
			String snippet = snippets[i];
			if(!snippet.toLowerCase().endsWith(".props"))
				continue;

			try
			{
				String path = MiscUtilities.constructPath(
					siteSettingsDirectory,snippet);
				Log.log(Log.DEBUG,jEdit.class,
					"Loading site snippet: " + path);

				propMgr.loadSiteProps(new FileInputStream(new File(path)));
			}
			catch(FileNotFoundException fnf)
			{
				Log.log(Log.DEBUG,jEdit.class,fnf);
			}
			catch(IOException e)
			{
				Log.log(Log.ERROR,jEdit.class,"Cannot load site snippet "
					+ snippet);
				Log.log(Log.ERROR,jEdit.class,e);
			}
		}
	} //}}}

	//{{{ initResources() method
	private static void initResources()
	{
		builtInActionSet = new ActionSet(null,null,null,
			jEdit.class.getResource("actions.xml"));
		builtInActionSet.setLabel(getProperty("action-set.jEdit"));
		builtInActionSet.load();

		actionContext.addActionSet(builtInActionSet);

		DockableWindowFactory.getInstance()
			.loadDockableWindows(null,
			jEdit.class.getResource("dockables.xml"),
			null);

		ServiceManager.loadServices(null,
			jEdit.class.getResource("services.xml"),
			null);
	} //}}}

	//{{{ initPlugins() method
	/**
	 * Loads plugins.
	 */
	private static void initPlugins()
	{
		if(jEditHome != null)
		{
			addPluginJARsFromDirectory(MiscUtilities.constructPath(
				jEditHome,"jars"));
		}

		if(settingsDirectory != null)
		{
			File jarsDirectory = new File(settingsDirectory,"jars");
			if(!jarsDirectory.exists())
				jarsDirectory.mkdir();
			addPluginJARsFromDirectory(jarsDirectory.getPath());
		}

		PluginJAR[] jars = getPluginJARs();
		for(int i = 0; i < jars.length; i++)
		{
			jars[i].checkDependencies();
		}
	} //}}}

	//{{{ initUserProperties() method
	/**
	 * Loads user properties.
	 */
	private static void initUserProperties()
	{
		if(settingsDirectory != null)
		{
			File file = new File(MiscUtilities.constructPath(
				settingsDirectory,"properties"));
			propsModTime = file.lastModified();

			try
			{
				propMgr.loadUserProps(
					new FileInputStream(file));
			}
			catch(FileNotFoundException fnf)
			{
				//Log.log(Log.DEBUG,jEdit.class,fnf);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,jEdit.class,e);
			}
		}
	} //}}}

	//{{{ fontStyleToString() method
	private static String fontStyleToString(int style)
	{
		if(style == 0)
			return "PLAIN";
		else if(style == Font.BOLD)
			return "BOLD";
		else if(style == Font.ITALIC)
			return "ITALIC";
		else if(style == (Font.BOLD | Font.ITALIC))
			return "BOLDITALIC";
		else
			throw new RuntimeException("Invalid style: " + style);
	} //}}}

	//{{{ fontToString() method
	private static String fontToString(Font font)
	{
		return font.getFamily()
			+ '-'
			+ fontStyleToString(font.getStyle())
			+ '-'
			+ font.getSize();
	} //}}}

	//{{{ initPLAF() method
	/**
	 * Sets the Swing look and feel.
	 */
	private static void initPLAF()
	{
		Font primaryFont = jEdit.getFontProperty(
			"metal.primary.font");
		if(primaryFont != null)
		{
			String primaryFontString =
				fontToString(primaryFont);

			System.getProperties().put(
				"swing.plaf.metal.controlFont",
				primaryFontString);
			System.getProperties().put(
				"swing.plaf.metal.menuFont",
				primaryFontString);
		}

		Font secondaryFont = jEdit.getFontProperty(
			"metal.secondary.font");
		if(secondaryFont != null)
		{
			String secondaryFontString =
				fontToString(secondaryFont);

			System.getProperties().put(
				"swing.plaf.metal.systemFont",
				secondaryFontString);
			System.getProperties().put(
				"swing.plaf.metal.userFont",
				secondaryFontString);
		}

		// Though the cause is not known, this must precede
		// UIManager.setLookAndFeel(), so that menu bar
		// interaction by ALT key interacts with swing.JMenuBar
		// (which uses L&F) instead of awt.MenuBar which we
		// don't use (and doesn't use L&F).
		// The difference of the behavior was seen on Sun JRE
		// 6u16 on Windows XP and Windows L&F.
		KeyboardFocusManager.setCurrentKeyboardFocusManager(
			new MyFocusManager());

		try
		{
			String lf = getProperty("lookAndFeel");
			if(lf != null && lf.length() != 0)
				UIManager.setLookAndFeel(lf);
			else if(OperatingSystem.isMacOS())
			{
				UIManager.setLookAndFeel(UIManager
					.getSystemLookAndFeelClassName());
			}
			else
			{
				UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,e);
		}

		UIDefaults defaults = UIManager.getDefaults();

		// give all Swing components our colors
		if(jEdit.getBooleanProperty("textColors"))
		{
			Color background = new javax.swing.plaf.ColorUIResource(
				jEdit.getColorProperty("view.bgColor"));
			Color foreground = new javax.swing.plaf.ColorUIResource(
				jEdit.getColorProperty("view.fgColor"));
			Color caretColor = new javax.swing.plaf.ColorUIResource(
				jEdit.getColorProperty("view.caretColor"));
			Color selectionColor = new javax.swing.plaf.ColorUIResource(
				jEdit.getColorProperty("view.selectionColor"));

			String[] prefixes = { "PasswordField", "TextField", "TextArea", "List", "Table" };
			for(int i = 0; i < prefixes.length; i++)
			{
				String prefix = prefixes[i];
				defaults.put(prefix + ".foreground",foreground);
				defaults.put(prefix + ".background",background);
				defaults.put(prefix + ".disabledForeground",foreground);
				defaults.put(prefix + ".disabledBackground",background);
				defaults.put(prefix + ".caretForeground",caretColor);
				defaults.put(prefix + ".selectionForeground",foreground);
				defaults.put(prefix + ".selectionBackground",selectionColor);
			}

			defaults.put("ComboBox.foreground",foreground);
			defaults.put("ComboBox.background",background);
			defaults.put("ComboBox.disabledForeground",foreground);
			defaults.put("ComboBox.disabledBackground",background);
			defaults.put("ComboBox.selectedForeground",foreground);
			defaults.put("ComboBox.selectedBackground",selectionColor);

			defaults.put("Tree.background",background);
			defaults.put("Tree.foreground",foreground);
			defaults.put("Tree.textBackground",background);
			defaults.put("Tree.textForeground",foreground);
			defaults.put("Tree.selectionForeground",foreground);
			defaults.put("Tree.selectionBackground",selectionColor);
		}

		defaults.remove("SplitPane.border");
		defaults.remove("SplitPaneDivider.border");

		JFrame.setDefaultLookAndFeelDecorated(
			getBooleanProperty("decorate.frames"));
		JDialog.setDefaultLookAndFeelDecorated(
			getBooleanProperty("decorate.dialogs"));

		if (isStartupDone())
		{
			for (Window window : Window.getWindows())
				SwingUtilities.updateComponentTreeUI(window);
		}
	} //}}}

	//{{{ getNextUntitledBufferId() method
	public static int getNextUntitledBufferId()
	{
		int untitledCount = 0;
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.getName().startsWith("Untitled-"))
			{
				try
				{
					untitledCount = Math.max(untitledCount,
						Integer.parseInt(buffer.getName()
						.substring(9)));
				}
				catch(NumberFormatException nf)
				{
				}
			}
			buffer = buffer.next;
		}
		return untitledCount + 1;
	} //}}}

	//{{{ runStartupScripts() method
	/**
	 * Runs scripts in a directory.
	 */
	private static void runStartupScripts(File directory)
	{
		if (!directory.isDirectory())
			return;

		File[] snippets = directory.listFiles();
		if (snippets == null)
			return;

		Arrays.sort(snippets,
			new StandardUtilities.StringCompare<File>(true));

		/*
		 * Force the default encoding to UTF-8 temporarily.
		 * The shipped scripts use that encoding, so we need
		 * to make sure we can load them correctly. If users
		 * want to write script with a different encoding,
		 * they can use buffer-local properties on the
		 * script to set it.
		 */
		String defaultEncoding = getProperty("buffer.encoding");
		setProperty("buffer.encoding", "UTF-8");

		for(int i = 0; i < snippets.length; ++i)
		{
			File snippet = snippets[i];

			Macros.Handler handler = Macros.getHandlerForPathName(
				snippet.getPath());
			if(handler == null)
				continue;

			try
			{
				Macros.Macro newMacro = handler.createMacro(
					snippet.getName(),
					snippet.getPath());
				handler.runMacro(null,newMacro,false);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,jEdit.class,e);
			}
		}

		setProperty("buffer.encoding", defaultEncoding);
	} //}}}

	//{{{ initProxy() method
	private static void initProxy()
	{
		boolean socksEnabled = jEdit.getBooleanProperty("firewall.socks.enabled");
		if(!socksEnabled)
		{
			Log.log(Log.DEBUG,jEdit.class,"SOCKS proxy disabled");
			System.getProperties().remove("socksProxyHost");
			System.getProperties().remove("socksProxyPort");
		}
		else
		{
			String socksHost = jEdit.getProperty("firewall.socks.host");
			if( socksHost != null )
			{
				System.setProperty("socksProxyHost", socksHost);
				Log.log(Log.DEBUG, jEdit.class,
					"SOCKS proxy enabled: " + socksHost);
			}

			String socksPort = jEdit.getProperty("firewall.socks.port");
			if(socksPort != null)
				System.setProperty("socksProxyPort", socksPort);
		}

		boolean httpEnabled = jEdit.getBooleanProperty("firewall.enabled");
		if (!httpEnabled)
		{
			Log.log(Log.DEBUG, jEdit.class, "HTTP proxy disabled");
			System.getProperties().remove("proxySet");
			System.getProperties().remove("proxyHost");
			System.getProperties().remove("proxyPort");
			System.getProperties().remove("http.proxyHost");
			System.getProperties().remove("http.proxyPort");
			System.getProperties().remove("http.nonProxyHosts");
			Authenticator.setDefault(null);
		}
		else
		{
			// set proxy host
			String host = jEdit.getProperty("firewall.host");
			if (host == null)
				return;

			System.setProperty("http.proxyHost", host);
			Log.log(Log.DEBUG, jEdit.class, "HTTP proxy enabled: " + host);
			// set proxy port
			String port = jEdit.getProperty("firewall.port");
			if (port != null)
				System.setProperty("http.proxyPort", port);

			// set non proxy hosts list
			String nonProxyHosts = jEdit.getProperty("firewall.nonProxyHosts");
			if (nonProxyHosts != null)
				System.setProperty("http.nonProxyHosts", nonProxyHosts);

			// set proxy authentication
			String username = jEdit.getProperty("firewall.user");
			String password = jEdit.getProperty("firewall.password");

			// null not supported?
			if(password == null)
				password = "";

			if(username == null || username.length()==0)
			{
				Log.log(Log.DEBUG, jEdit.class, "HTTP proxy without user");
				Authenticator.setDefault(new FirewallAuthenticator(null));
			}
			else
			{
				Log.log(Log.DEBUG, jEdit.class, "HTTP proxy user: " + username);
				PasswordAuthentication pw = new PasswordAuthentication(
					username,password.toCharArray()
				);
				Authenticator.setDefault(new FirewallAuthenticator(pw));
			}
		}
	} //}}}

	//{{{ FirewallAuthenticator class
	static class FirewallAuthenticator extends Authenticator
	{
		PasswordAuthentication pw;

		FirewallAuthenticator(PasswordAuthentication pw)
		{
			this.pw = pw;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication()
		{
			return pw;
		}
	} //}}}

	//{{{ finishStartup() method
	private static void finishStartup(final boolean gui, final boolean restore,
		final boolean newPlainView, final String userDir, final String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				int count = getBufferCount();

				boolean restoreFiles = restore
					&& jEdit.getBooleanProperty("restore")
					&& (count == 0 ||
					jEdit.getBooleanProperty("restore.cli"));

				if(gui || count != 0)
				{
					View view;
					if (newPlainView)
						view = newView(null,null,true);
					else
						view = PerspectiveManager.loadPerspective(restoreFiles);

					if(view == null)
						view = newView(null,null);

					Buffer buffer = openFiles(null,userDir,args);
					if(buffer != null)
						view.setBuffer(buffer,true);
				}
				else
				{
					openFiles(null,userDir,args);
				}

				// Start I/O threads
				EditBus.send(new EditorStarted(null));

				VFSManager.start();

				// Start edit server
				if(server != null)
					server.start();

				GUIUtilities.hideSplashScreen();

				Log.log(Log.MESSAGE,jEdit.class,"Startup "
					+ "complete");

				//{{{ Report any plugin errors
				if(pluginErrors != null)
				{
					showPluginErrorDialog();
				} //}}}

				startupDone.set(0, true);

				// in one case not a single AWT class will
				// have been touched (splash screen off +
				// -nogui -nobackground switches on command
				// line)
				Toolkit.getDefaultToolkit();
			}
		});
	} //}}}

	//{{{ showPluginErrorDialog() method
	private static void showPluginErrorDialog()
	{
		if(pluginErrors == null)
			return;

		String caption = getProperty(
			"plugin-error.caption" + (pluginErrors.size() == 1
			? "-1" : ""));

		Frame frame = (PluginManager.getInstance() == null
			? viewsFirst
			: PluginManager.getInstance());

		new ErrorListDialog(frame,
			getProperty("plugin-error.title"),
			caption,pluginErrors,true);
		pluginErrors = null;
	} //}}}

	//{{{ getNotLoadedPluginJARs() method
	private static void getNotLoadedPluginJARs(List<String> returnValue,
		String dir, String[] list)
	{
loop:		for(int i = 0; i < list.length; i++)
		{
			String name = list[i];
			if(!name.toLowerCase().endsWith(".jar"))
				continue loop;

			String path = MiscUtilities.constructPath(dir,name);

			for(int j = 0; j < jars.size(); j++)
			{
				PluginJAR jar = jars.elementAt(j);
				String jarPath = jar.getPath();
				String jarName = MiscUtilities.getFileName(jarPath);

				if(path.equals(jarPath))
					continue loop;
				else if(!new File(jarPath).exists()
					&& name.equals(jarName))
					continue loop;
			}

			returnValue.add(path);
		}
	} //}}}

	//{{{ gotoMarker() method
	private static void gotoMarker(final View view, final Buffer buffer,
		final String marker)
	{
		VFSManager.runInAWTThread(new Runnable()
		{
			@Override
			public void run()
			{
				int pos;

				// Handle line number
				if(marker.startsWith("+line:"))
				{
					try
					{
						String arg = marker.substring(6);
						String[] lineCol = arg.split(",");
						int line, col;
						if(lineCol.length > 1)
						{
							line = Integer.parseInt(lineCol[0]);
							col = Integer.parseInt(lineCol[1]);
						}
						else
						{
							line = Integer.parseInt(marker.substring(6));
							col = 1;
						}
						pos = buffer.getLineStartOffset(line - 1) + (col - 1);
					}
					catch(Exception e)
					{
						return;
					}
				}
				// Handle marker
				else if(marker.startsWith("+marker:"))
				{
					if(marker.length() != 9)
						return;

					Marker m = buffer.getMarker(marker.charAt(8));
					if(m == null)
						return;
					pos = m.getPosition();
				}
				// Can't happen
				else
					throw new InternalError();

				if(view != null && view.getBuffer() == buffer)
				{
					view.getTextArea().setCaretPosition(pos);
					buffer.setIntegerProperty(Buffer.CARET,pos);
					buffer.setBooleanProperty(Buffer.CARET_POSITIONED,true);
				}
				else
				{
					buffer.setIntegerProperty(Buffer.CARET,pos);
					buffer.setBooleanProperty(Buffer.CARET_POSITIONED,true);
					buffer.unsetProperty(Buffer.SCROLL_VERT);
				}
			}
		});
	} //}}}

	//{{{ addBufferToList() method
	private static void addBufferToList(Buffer buffer)
	{
		synchronized(bufferListLock)
		{
			String symlinkPath = buffer.getSymlinkPath();
			if((VFSManager.getVFSForPath(symlinkPath).getCapabilities()
				& VFS.CASE_INSENSITIVE_CAP) != 0)
			{
				symlinkPath = symlinkPath.toLowerCase();
			}

			bufferCount++;

			bufferHash.put(symlinkPath,buffer);

			if(buffersFirst == null)
			{
				buffersFirst = buffersLast = buffer;
				return;
			}
			//{{{ Sort buffer list
			else if(sortBuffers)
			{
				String str11, str12;
				if(sortByName)
				{
					str11 = buffer.getName();
					str12 = buffer.getDirectory();
				}
				else
				{
					str11 = buffer.getDirectory();
					str12 = buffer.getName();
				}

				Buffer _buffer = buffersFirst;
				while(_buffer != null)
				{
					String str21, str22;
					if(sortByName)
					{
						str21 = _buffer.getName();
						str22 = _buffer.getDirectory();
					}
					else
					{
						str21 = _buffer.getDirectory();
						str22 = _buffer.getName();
					}

					int comp = StandardUtilities.compareStrings(str11,str21,true);
					if(comp < 0 || (comp == 0 && StandardUtilities.compareStrings(str12,str22,true) < 0))
					{
						buffer.next = _buffer;
						buffer.prev = _buffer.prev;
						_buffer.prev = buffer;
						if(_buffer != buffersFirst)
							buffer.prev.next = buffer;
						else
							buffersFirst = buffer;
						return;
					}

					_buffer = _buffer.next;
				}
			} //}}}

			buffer.prev = buffersLast;
			// fixes the hang that can occur if we 'save as' to a
			// new filename which requires re-sorting
			buffer.next = null;
			buffersLast.next = buffer;
			buffersLast = buffer;
		}
	} //}}}

	//{{{ removeBufferFromList() method
	private static void removeBufferFromList(Buffer buffer)
	{
		synchronized(bufferListLock)
		{
			bufferCount--;

			String path = buffer.getPath();
			if(OperatingSystem.isCaseInsensitiveFS())
				path = path.toLowerCase();

			bufferHash.remove(path);

			if(buffer == buffersFirst && buffer == buffersLast)
			{
				buffersFirst = buffersLast = null;
				return;
			}

			if(buffer == buffersFirst)
			{
				buffersFirst = buffer.next;
				buffer.next.prev = null;
			}
			else
			{
				if (buffer.prev != null)
					buffer.prev.next = buffer.next;
			}

			if(buffer == buffersLast)
			{
				buffersLast = buffersLast.prev;
				buffer.prev.next = null;
			}
			else
			{
				if (buffer.next != null)
					buffer.next.prev = buffer.prev;
			}

			// fixes the hang that can occur if we 'save as' to a new
			// filename which requires re-sorting
			buffer.next = buffer.prev = null;
		}
	} //}}}

	//{{{ addViewToList() method
	private static void addViewToList(View view)
	{
		viewCount++;

		if(viewsFirst == null)
			viewsFirst = viewsLast = view;
		else
		{
			view.prev = viewsLast;
			viewsLast.next = view;
			viewsLast = view;
		}
	} //}}}

	//{{{ removeViewFromList() method
	private static void removeViewFromList(View view)
	{
		viewCount--;

		if(viewsFirst == viewsLast)
		{
			viewsFirst = viewsLast = null;
			return;
		}

		if(view == viewsFirst)
		{
			viewsFirst = view.next;
			view.next.prev = null;
		}
		else
		{
			view.prev.next = view.next;
		}

		if(view == viewsLast)
		{
			viewsLast = viewsLast.prev;
			view.prev.next = null;
		}
		else
		{
			view.next.prev = view.prev;
		}
	} //}}}

	//{{{ closeView() method
	/**
	 * closeView() used by exit().
	 */
	private static boolean closeView(View view, boolean callExit)
	{
		PerspectiveManager.setPerspectiveDirty(true);

		if(viewsFirst == viewsLast && callExit)
		{
			exit(view,false); /* exit does editor event & save */
			// Coming here means the request has been canceled.
			return false;
		}
		else
		{
			if (!view.confirmToCloseDirty())
				return false;

			view.close();
			view.dispose();
			removeViewFromList(view);

			if(view == activeView)
				activeView = null;

			return true;
		}
	} //}}}

	//{{{ loadModeCatalog() method
	/**
	 * Loads a mode catalog file.
	 * @since jEdit 3.2pre2
	 */
	private static void loadModeCatalog(String path, boolean resource)
	{
		Log.log(Log.MESSAGE,jEdit.class,"Loading mode catalog file " + path);

		ModeCatalogHandler handler = new ModeCatalogHandler(
			MiscUtilities.getParentOfPath(path),resource)
		{
			@Override
			protected Mode instantiateMode(String modeName)
			{
				return new JEditMode(modeName);
			}
		};
		try
		{
			InputStream _in;
			if(resource)
				_in = jEdit.class.getResourceAsStream(path);
			else
				_in = new FileInputStream(path);
			XMLUtilities.parseXML(_in, handler);
		}
		catch(IOException e)
		{
			Log.log(Log.ERROR,jEdit.class,e);
		}
	} //}}}

	//{{{ initKeyBindings() method
	/**
	 * Loads all key bindings from the properties.
	 *
	 * @since 3.1pre1
	 */
	private static void initKeyBindings()
	{
		inputHandler.removeAllKeyBindings();

		ActionSet[] actionSets = getActionSets();
		for (int i = 0; i < actionSets.length; i++)
		{
			actionSets[i].initKeyBindings();
		}
	} //}}}

	//{{{ composeBufferPropsFromHistory() method
	/**
	 * Compose buffer-local properties which can be got from history.
	 * @since 4.3pre10
	 */
	private static void composeBufferPropsFromHistory(Map props, String path)
	{
		BufferHistory.Entry entry = BufferHistory.getEntry(path);

		if(entry != null && saveCaret && props.get(Buffer.CARET) == null)
		{
			props.put(Buffer.CARET, entry.caret);
			 if(entry.selection != null)
			{
				// getSelection() converts from string to
				// Selection[]
				props.put(Buffer.SELECTION,entry.getSelection());
			}
		}

		if(entry != null && props.get(JEditBuffer.ENCODING) == null)
		{
			if(entry.encoding != null)
				props.put(JEditBuffer.ENCODING,entry.encoding);
		}

		if (entry != null && props.get("mode") == null)
		{
			if (entry.mode != null)
				props.put("mode", entry.mode);
		}
	} //}}}

	//}}}

	//{{{ MyFocusManager class
	private static class MyFocusManager extends DefaultKeyboardFocusManager
	{
		MyFocusManager()
		{
			setDefaultFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
		}

		@Override
		public boolean postProcessKeyEvent(KeyEvent evt)
		{
			if(!evt.isConsumed())
			{
				Component comp = (Component)evt.getSource();
				if(!comp.isShowing())
					return true;

				for(;;)
				{
					if(comp instanceof View)
					{
						((View)comp).getInputHandler().processKeyEvent(evt,
							View.VIEW, false);
						return true;
					}
					else if(comp == null || comp instanceof Window
						|| comp instanceof JEditTextArea)
					{
						if (comp instanceof PluginManager)
						{
							evt.setSource(comp);
							((PluginManager)comp).processKeyEvents(evt);
						}
						break;
					}
					else
						comp = comp.getParent();
				}
			}

			return super.postProcessKeyEvent(evt);
		}
	} //}}}

	//{{{ JEditPropertyManager class
	public static class JEditPropertyManager implements IPropertyManager
	{
		@Override
		public String getProperty(String name)
		{
			return jEdit.getProperty(name);
		}
	} //}}}

	//{{{ DockingLayoutSetter class
	private static class DockingLayoutSetter implements Runnable
	{
		private final View view;
		private final ViewConfig config;
		private final int startupDoneIndex;

		DockingLayoutSetter(View view, ViewConfig config, int startupDoneIndex)
		{
			this.view = view;
			this.config = config;
			this.startupDoneIndex = startupDoneIndex;
		}

		@Override
		public void run()
		{
			DockableWindowManager wm = view.getDockableWindowManager();
			wm.setDockingLayout(config.docking);
			startupDone.set(startupDoneIndex, true);
		}
	} //}}}

	private static final JEditPropertyManager propertyManager = new JEditPropertyManager();
}
