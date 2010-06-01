/*
 * PluginList.java - Plugin list downloaded from server
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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

package org.gjt.sp.jedit.pluginmgr;

//{{{ Imports
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.gjt.sp.util.*;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.gjt.sp.jedit.*;
//}}}


/**
 * Plugin list downloaded from server.
 * @since jEdit 3.2pre2
 * @version $Id$
 */
class PluginList
{
	/**
	 * Magic numbers used for auto-detecting GZIP files.
	 */
	public static final int GZIP_MAGIC_1 = 0x1f;
	public static final int GZIP_MAGIC_2 = 0x8b;
	public static final long MILLISECONDS_PER_MINUTE = 60L * 1000;

	final List<Plugin> plugins = new ArrayList<Plugin>();
	final Map<String, Plugin> pluginHash = new HashMap<String, Plugin>();
	final List<PluginSet> pluginSets = new ArrayList<PluginSet>();

	/**
	 * The mirror id.
	 * @since jEdit 4.3pre3
	 */
	private final String id;
	private String cachedURL;
	private final Task task;
	String gzipURL;
	//{{{ PluginList constructor
	PluginList(Task task)
	{
		id = jEdit.getProperty("plugin-manager.mirror.id");
		this.task = task;
		readPluginList(true);
	}
	
	void readPluginList(boolean allowRetry)
	{
		gzipURL = jEdit.getProperty("plugin-manager.export-url");	
		if (!id.equals(MirrorList.Mirror.NONE))
			gzipURL += "?mirror="+id;		
		String path = null;
		if (jEdit.getSettingsDirectory() == null)
		{
			cachedURL = gzipURL;
		}
		else
		{
			path = jEdit.getSettingsDirectory() + File.separator + "pluginMgr-Cached.xml.gz";
			cachedURL = "file:///" + path;
		}
		boolean downloadIt = !id.equals(jEdit.getProperty("plugin-manager.mirror.cached-id"));
		if (path != null)
		{
			try
			{

				File f = new File(path);
				if (!f.canRead()) downloadIt = true;
				long currentTime = System.currentTimeMillis();
				long age = currentTime - f.lastModified();
				/* By default only download plugin lists every 5 minutes */
				long interval = jEdit.getIntegerProperty("plugin-manager.list-cache.minutes", 5) * MILLISECONDS_PER_MINUTE;
				if (age > interval)
				{
					Log.log(Log.MESSAGE, this, "PluginList cached copy too old. Downloading from mirror. ");
					downloadIt = true;
				}
			}
			catch (Exception e)
			{
				Log.log(Log.MESSAGE, this, "No cached copy. Downloading from mirror. ");
				downloadIt = true;
			}
		}
		if (downloadIt && cachedURL != gzipURL)
		{
			downloadPluginList();
		}
		InputStream in = null, inputStream = null;
		try
		{
			if (cachedURL != gzipURL) 
				Log.log(Log.MESSAGE, this, "Using cached pluginlist");
			inputStream = new URL(cachedURL).openStream();
			XMLReader parser = XMLReaderFactory.createXMLReader();
			PluginListHandler handler = new PluginListHandler(this, cachedURL);
			in = new BufferedInputStream(inputStream);
			if(in.markSupported())
			{
				in.mark(2);
				int b1 = in.read();
				int b2 = in.read();
				in.reset();

				if(b1 == GZIP_MAGIC_1 && b2 == GZIP_MAGIC_2)
					in = new GZIPInputStream(in);
			}
			InputSource isrc = new InputSource(new InputStreamReader(in,"UTF8"));
			isrc.setSystemId("jedit.jar");
			parser.setContentHandler(handler);
			parser.setDTDHandler(handler);
			parser.setEntityResolver(handler);
			parser.setErrorHandler(handler);
			parser.parse(isrc);
				
		}
		catch (Exception e)
		{
			Log.log(Log.ERROR, this, "readpluginlist: error", e);
			if (cachedURL.startsWith("file:///"))
			{
				Log.log(Log.DEBUG, this, "Unable to read plugin list, deleting cached file and try again");
				new File(cachedURL.substring(8)).delete();
				if (allowRetry)
				{
					plugins.clear();
					pluginHash.clear();
					pluginSets.clear();
					readPluginList(false);
				}
			}
		}
		finally
		{
			IOUtilities.closeQuietly(in);
			IOUtilities.closeQuietly(inputStream);
		}
		
	}
	
	/** Caches it locally */
	void downloadPluginList()
	{
		BufferedInputStream is = null;
		BufferedOutputStream out = null;
		try
		{
			
			task.setStatus(jEdit.getProperty("plugin-manager.list-download"));
			InputStream inputStream = new URL(gzipURL).openStream();
			String fileName = cachedURL.replaceFirst("file:///", "");
			out = new BufferedOutputStream(new FileOutputStream(fileName));
			long start = System.currentTimeMillis();
			is = new BufferedInputStream(inputStream);
			IOUtilities.copyStream(4096, null, is, out, false);
			jEdit.setProperty("plugin-manager.mirror.cached-id", id);
			Log.log(Log.MESSAGE, this, "Updated cached pluginlist " + (System.currentTimeMillis() - start));
		}
		catch (Exception e)
		{
			Log.log (Log.ERROR, this, "CacheRemotePluginList: error", e);
		}
		finally
		{
			IOUtilities.closeQuietly(out);
			IOUtilities.closeQuietly(is);
		}
	}
	
	//{{{ addPlugin() method
	void addPlugin(Plugin plugin)
	{
		plugin.checkIfInstalled();
		plugins.add(plugin);
		pluginHash.put(plugin.name,plugin);
	} //}}}

	//{{{ addPluginSet() method
	void addPluginSet(PluginSet set)
	{
		pluginSets.add(set);
	} //}}}

	//{{{ finished() method
	void finished()
	{
		// after the entire list is loaded, fill out plugin field
		// in dependencies
		for(int i = 0; i < plugins.size(); i++)
		{
			Plugin plugin = plugins.get(i);
			for(int j = 0; j < plugin.branches.size(); j++)
			{
				Branch branch = plugin.branches.get(j);
				for(int k = 0; k < branch.deps.size(); k++)
				{
					Dependency dep = branch.deps.get(k);
					if(dep.what.equals("plugin"))
						dep.plugin = pluginHash.get(dep.pluginName);
				}
			}
		}
	} //}}}

	//{{{ dump() method
	void dump()
	{
		for(int i = 0; i < plugins.size(); i++)
		{
			System.err.println(plugins.get(i));
			System.err.println();
		}
	} //}}}

	//{{{ getMirrorId() method
	/**
	 * Returns the mirror ID.
	 *
	 * @return the mirror ID
	 * @since jEdit 4.3pre3
	 */
	String getMirrorId()
	{
		return id;
	} //}}}

	//{{{ PluginSet class
	static class PluginSet
	{
		String name;
		final List<String> plugins = new ArrayList<String>();

		public String toString()
		{
			return plugins.toString();
		}
	} //}}}

	//{{{ Plugin class
	public static class Plugin
	{
		String jar;
		String name;
		String description;
		String author;
		final List<Branch> branches = new ArrayList<Branch>();
		//String installed;
		//String installedVersion;

		void checkIfInstalled()
		{
			/* // check if the plugin is already installed.
			// this is a bit of hack
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();
				if(!new File(path).exists())
					continue;

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;

					EditPlugin plugin = jars[i].getPlugin();
					if(plugin != null)
					{
						installedVersion = jEdit.getProperty(
							"plugin." + plugin.getClassName()
							+ ".version");
					}
					break;
				}
			}

			String[] notLoaded = jEdit.getNotLoadedPluginJARs();
			for(int i = 0; i < notLoaded.length; i++)
			{
				String path = notLoaded[i];

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;
					break;
				}
			} */
		}

		String getInstalledVersion()
		{
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					EditPlugin plugin = jars[i].getPlugin();
					if(plugin != null)
					{
						return jEdit.getProperty(
							"plugin." + plugin.getClassName()
							+ ".version");
					}
					else
						return null;
				}
			}

			return null;
		}

		String getInstalledPath()
		{
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();

				if(MiscUtilities.getFileName(path).equals(jar))
					return path;
			}

			return null;
		}

		/**
		 * Find the first branch compatible with the running jEdit release.
		 */
		Branch getCompatibleBranch()
		{
			for(int i = 0; i < branches.size(); i++)
			{
				Branch branch = branches.get(i);
				if(branch.canSatisfyDependencies())
					return branch;
			}

			return null;
		}

		boolean canBeInstalled()
		{
			Branch branch = getCompatibleBranch();
			return branch != null && !branch.obsolete
				&& branch.canSatisfyDependencies();
		}

		void install(Roster roster, String installDirectory, boolean downloadSource)
		{
			String installed = getInstalledPath();

			Branch branch = getCompatibleBranch();
			if(branch.obsolete)
			{
				if(installed != null)
					roster.addRemove(installed);
				return;
			}

			//branch.satisfyDependencies(roster,installDirectory,
			//	downloadSource);

			if(installed != null)
			{
				installDirectory = MiscUtilities.getParentOfPath(
					installed);
			}

			roster.addInstall(
				installed,
				downloadSource ? branch.downloadSource : branch.download,
				installDirectory,
				downloadSource ? branch.downloadSourceSize : branch.downloadSize);

		}

		public String toString()
		{
			return name;
		}
	} //}}}

	//{{{ Branch class
	static class Branch
	{
		String version;
		String date;
		int downloadSize;
		String download;
		int downloadSourceSize;
		String downloadSource;
		boolean obsolete;
		final List<Dependency> deps = new ArrayList<Dependency>();

		boolean canSatisfyDependencies()
		{
			for(int i = 0; i < deps.size(); i++)
			{
				Dependency dep = deps.get(i);
				if(!dep.canSatisfy())
					return false;
			}

			return true;
		}

		void satisfyDependencies(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			for(int i = 0; i < deps.size(); i++)
			{
				Dependency dep = deps.get(i);
				dep.satisfy(roster,installDirectory,downloadSource);
			}
		}

		public String toString()
		{
			return "[version=" + version + ",download=" + download
				+ ",obsolete=" + obsolete + ",deps=" + deps + ']';
		}
	} //}}}

	//{{{ Dependency class
	static class Dependency
	{
		final String what;
		final String from;
		final String to;
		// only used if what is "plugin"
		final String pluginName;
		Plugin plugin;

		Dependency(String what, String from, String to, String pluginName)
		{
			this.what = what;
			this.from = from;
			this.to = to;
			this.pluginName = pluginName;
		}

		boolean isSatisfied()
		{
			if(what.equals("plugin"))
			{
				for(int i = 0; i < plugin.branches.size(); i++)
				{
					String installedVersion = plugin.getInstalledVersion();
					if(installedVersion != null
						&&
					(from == null || StandardUtilities.compareStrings(
						installedVersion,from,false) >= 0)
						&&
						(to == null || StandardUtilities.compareStrings(
						      installedVersion,to,false) <= 0))
					{
						return true;
					}
				}

				return false;
			}
			else if(what.equals("jdk"))
			{
				String javaVersion = System.getProperty("java.version").substring(0,3);

				if((from == null || StandardUtilities.compareStrings(
					javaVersion,from,false) >= 0)
					&&
					(to == null || StandardUtilities.compareStrings(
						     javaVersion,to,false) <= 0))
					return true;
				else
					return false;
			}
			else if(what.equals("jedit"))
			{
				String build = jEdit.getBuild();

				if((from == null || StandardUtilities.compareStrings(
					build,from,false) >= 0)
					&&
					(to == null || StandardUtilities.compareStrings(
						     build,to,false) <= 0))
					return true;
				else
					return false;
			}
			else
			{
				Log.log(Log.ERROR,this,"Invalid dependency: " + what);
				return false;
			}
		}

		boolean canSatisfy()
		{
			if(isSatisfied())
				return true;
			if (what.equals("plugin"))
				return plugin.canBeInstalled();
			return false;
		}

		void satisfy(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			if(what.equals("plugin"))
			{
				String installedVersion = plugin.getInstalledVersion();
				for(int i = 0; i < plugin.branches.size(); i++)
				{
					Branch branch = plugin.branches.get(i);
					if((installedVersion == null
						||
					StandardUtilities.compareStrings(
						installedVersion,branch.version,false) < 0)
						&&
					(from == null || StandardUtilities.compareStrings(
						branch.version,from,false) >= 0)
						&&
						(to == null || StandardUtilities.compareStrings(
						      branch.version,to,false) <= 0))
					{
						plugin.install(roster,installDirectory,
							downloadSource);
						return;
					}
				}
			}
		}

		public String toString()
		{
			return "[what=" + what + ",from=" + from
				+ ",to=" + to + ",plugin=" + plugin + ']';
		}
	} //}}}
}
