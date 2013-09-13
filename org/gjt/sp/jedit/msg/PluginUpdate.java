/*
 * PluginUpdate.java - Plugin update message
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003 Slava Pestov
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

package org.gjt.sp.jedit.msg;

import java.io.File;
import org.gjt.sp.jedit.*;

/**
 * Message sent when plugins are loaded and unloaded.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 4.2pre1
 */
public class PluginUpdate extends EBMessage
{
	//{{{ Message types
	/**
	 * Plugin loaded. This is sent after a JAR file is added to the
	 * list and scanned.
	 * @since jEdit 4.2pre1
	 */
	public static final Object LOADED = "LOADED";

	/**
	 * Plugin activated. This is sent after the plugin core class
	 * is loaded and its <code>start()</code> method is called.
	 * @since jEdit 4.2pre1
	 */
	public static final Object ACTIVATED = "ACTIVATED";

	/**
	 * Plugin deactivated. This is sent after the plugin core class
	 * <code>stop()</code> method is called.
	 * @since jEdit 4.2pre2
	 */
	public static final Object DEACTIVATED = "DEACTIVATED";

	/**
	 * Plugin unloaded.
	 * @since jEdit 4.2pre1
	 */
	public static final Object UNLOADED = "UNLOADED";

	/**
	 * Plugin removed, as in deleted from disk.
	 * @since jEdit 5.2
	 */
	public static final Object REMOVED = "REMOVED";
	//}}}
	
	//{{{ PluginUpdate constructor
	/**
	 * Creates a new plugin update message.
	 * @param jar The plugin
	 * @param what What happened
	 * @param exit Is the editor exiting?
	 * @since jEdit 4.2pre3
	 */
	public PluginUpdate(PluginJAR jar, Object what, boolean exit)
	{
		super(jar);

		if (jar == null) {
			throw new IllegalArgumentException("PluginJAR may not be null.");	
		}
		this.jar = jar;
		
		if(what == null)
			throw new NullPointerException("What must be non-null");

		EditPlugin plugin = jar.getPlugin();
		if (plugin != null)
		{
			String clazz = plugin.getClassName();
			version = jEdit.getProperty("plugin."+clazz+".version");
		}
		this.what = what;
		this.exit = exit;
	} //}}}

	//{{{ PluginUpdate constructor
	/**
	 * Creates a new plugin update message. This constructor should be used
	 * when the plugin is being removed and the PluginJAR is no longer available.
	 * @param file The file representing the plugin
	 * @param what What happened
	 * @param exit Is the editor exiting?
	 * @since jEdit 4.2pre3
	 */
	public PluginUpdate(File file, Object what, boolean exit)
	{
		super(file);
		this.file = file;
		
		if(what == null)
			throw new NullPointerException("What must be non-null");

		this.what = what;
		this.exit = exit;
	} //}}}

	//{{{ getWhat() method
	/**
	 * Returns what caused this plugin update.
	 */
	public Object getWhat()
	{
		return what;
	} //}}}

	//{{{ isExiting() method
	/**
	 * Returns true if this plugin is being unloaded as part of the
	 * shutdown process, in which case some components like the help
	 * viewer and plugin manager ignore the event.
	 * @since jEdit 4.2pre3
	 */
	public boolean isExiting()
	{
		return exit;
	} //}}}

	//{{{ getPluginJAR() method
	/**
	 * Returns the plugin involved.
	 */
	public PluginJAR getPluginJAR()
	{
		return jar;
	} //}}}

	//{{{ getFile() method
	/**
	 * Returns the file representing the plugin involved.
	 */
	public File getFile()
	{
		return file;
	} //}}}

	//{{{ getPluginVersion() method
	/**
	 * Returns the plugin version.
	 *
	 * @return the plugin version. It may be null in some case like for the libraries
	 * @since 4.4pre1
	 */
	public String getPluginVersion()
	{
		return version;
	} //}}}

	//{{{ paramString() method
	public String paramString()
	{
		return "what=" + what + ",exit=" + exit + ",version=" + version + ","
			+ super.paramString();
	} //}}}

	//{{{ Private members
	private PluginJAR jar = null;
	private File file = null;
	private Object what;
	private boolean exit;
	private String version;
	//}}}
}
