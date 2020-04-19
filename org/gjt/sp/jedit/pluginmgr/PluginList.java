/*
 * PluginList.java - Plugin list downloaded from server
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.net.HttpURLConnection;
import java.util.*;

import org.gjt.sp.util.*;
import org.jedit.io.HttpException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.gjt.sp.jedit.*;
//}}}


/**
 * Plugin list downloaded from server.
 * @since jEdit 3.2pre2
 * @version $Id$
 */
class PluginList extends Task
{
	final List<Plugin> plugins = new ArrayList<>();
	final Map<String, Plugin> pluginHash = new HashMap<>();
	final List<PluginSet> pluginSets = new ArrayList<>();

	/**
	 * The mirror id.
	 */
	private String id;
	private final Runnable dispatchThreadTask;

	//{{{ PluginList constructor
	/**
	 * Instantiate the PluginList.
	 *
	 * @param dispatchThreadTask the task to execute in dispatch thread after the list was loaded
	 */
	PluginList(Runnable dispatchThreadTask)
	{
		this.dispatchThreadTask = dispatchThreadTask;
	} //}}}

	//{{{ _run() method
	@Override
	public void _run()
	{
		id = jEdit.getProperty("plugin-manager.mirror.id");
		CachePluginList cachePluginList = new CachePluginList(id);
		RemotePluginList remotePluginList = new RemotePluginList(this, id);

		setStatus(jEdit.getProperty("plugin-manager.list-download-connect"));
		try
		{
			String pluginListXml = cachePluginList.getPluginList();
			if (pluginListXml != null)
			{
				try
				{
					loadPluginList(pluginListXml);
				}
				catch (SAXException | ParserConfigurationException | IOException e)
				{
					cachePluginList.deleteCache();
					String newPluginList = remotePluginList.getPluginList();
					loadPluginList(newPluginList);
					cachePluginList.saveCache(newPluginList);
				}
			}
			else
			{
				String newPluginList = remotePluginList.getPluginList();
				loadPluginList(newPluginList);
				cachePluginList.saveCache(newPluginList);
			}
		}
		catch (HttpException e)
		{
			int responseCode = e.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_PROXY_AUTH)
			{
				Log.log(Log.ERROR, this, "CacheRemotePluginList: proxy requires authentication");
				ThreadUtilities.runInDispatchThread(() -> GUIUtilities.error(jEdit.getActiveView(),
					"plugin-manager.list-download.need-password",
					new Object[]{}));
			}
			else
			{
				String responseMessage = e.getMessage();
				Log.log(Log.ERROR, this, "CacheRemotePluginList: HTTP error: " + responseCode + ' ' + responseMessage);
				ThreadUtilities.runInDispatchThread(() ->
					GUIUtilities.error(jEdit.getActiveView(),
						"plugin-manager.list-download.generic-error",
						new Object[]{responseCode, responseMessage}));
			}
		}
		catch (Exception e)
		{
			Log.log (Log.ERROR, this, "CacheRemotePluginList: error", e);
			ThreadUtilities.runInDispatchThread(() -> GUIUtilities.error(jEdit.getActiveView(),
				"plugin-manager.list-download.disconnected",
				new Object[]{e.getMessage()}));
		}
		// even if there was an error we want to update the panels
		ThreadUtilities.runInDispatchThread(dispatchThreadTask);
	} //}}}

	//{{{ loadPluginList() method
	private void loadPluginList(String pluginListXml) throws IOException, SAXException, ParserConfigurationException
	{
		PluginListHandler handler = new PluginListHandler(this);
		XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		InputSource isrc = new InputSource(new StringReader(pluginListXml));
		isrc.setSystemId("jedit.jar");
		parser.setContentHandler(handler);
		parser.setDTDHandler(handler);
		parser.setEntityResolver(handler);
		parser.setErrorHandler(handler);
		parser.parse(isrc);
	} //}}}

	//{{{ addPlugin() method
	void addPlugin(Plugin plugin)
	{
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
		for (Plugin plugin : plugins)
		{
			for (int j = 0; j < plugin.branches.size(); j++)
			{
				Branch branch = plugin.branches.get(j);
				for (int k = 0; k < branch.deps.size(); k++)
				{
					Dependency dep = branch.deps.get(k);
					if (dep.what.equals("plugin")) dep.plugin = pluginHash.get(dep.pluginName);
				}
			}
		}
	} //}}}

	//{{{ dump() method
	void dump()
	{
		for (Plugin plugin : plugins)
		{
			System.err.println(plugin);
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
		final List<String> plugins = new ArrayList<>();

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
		final List<Branch> branches = new ArrayList<>();
		String installedVersion = null;
		String installedPath = null;
		boolean loaded = false;

		String getInstalledVersion()
		{
			this.loaded = false;
			PluginJAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					EditPlugin plugin = jars[i].getPlugin();
					if(plugin != null)
					{
						installedVersion = jEdit.getProperty(
							"plugin." + plugin.getClassName()
							+ ".version");
						this.loaded = true;
						return installedVersion;
					}
					else
						return null;
				}
			}
			String[] notLoadedJars = jEdit.getNotLoadedPluginJARs();
			for(String path: notLoadedJars){
				if(MiscUtilities.getFileName(path).equals(jar))
				{
					try
					{
						PluginJAR.PluginCacheEntry cacheEntry = PluginJAR.getPluginCacheEntry(path);
						if(cacheEntry != null)
						{
							String versionKey = "plugin." + cacheEntry.pluginClass + ".version";
							installedVersion = cacheEntry.cachedProperties.getProperty(versionKey);
							Log.log(Log.DEBUG, PluginList.class, "found installed but not loaded "+ jar + " version=" + installedVersion);
							installedPath = path;
							return installedVersion;
						}
					}
					catch (IOException e)
					{
						Log.log(Log.WARNING, "Unable to access cache for "+jar, e);
					}
				}
			}

			return null;
		}

		String getInstalledPath()
		{
			if(installedPath != null){
				if(new File(installedPath).exists()){
					return installedPath;
				}else{
					installedPath = null;
				}
			}

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
			for (Branch branch : branches)
			{
				if (branch.canSatisfyDependencies())
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

		void install(Roster roster, String installDirectory, boolean downloadSource, boolean asDependency)
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

			if(installedVersion != null && installedPath!= null && !loaded && asDependency)
			{
				roster.addLoad(installedPath);
				return;
			}

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
		final List<Dependency> deps = new ArrayList<>();

		boolean canSatisfyDependencies()
		{
			for (Dependency dep : deps)
			{
				if (!dep.canSatisfy())
					return false;
			}

			return true;
		}

		void satisfyDependencies(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			deps.forEach(dep -> dep.satisfy(roster, installDirectory, downloadSource));
		}

		public String depsToString()
		{
			StringBuilder sb = new StringBuilder();
			for (Dependency dep : deps)
			{
				if ("plugin".equals(dep.what) && dep.pluginName != null)
				{
					sb.append(dep.pluginName).append('\n');
				}
			}
			return sb.toString();
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
			switch (what)
			{
				case "plugin":
					return isSatisfiedByPlugin();
				case "jdk":
					return isSatisfiedByJdk();
				case "jedit":
					return isSatisfiedByJEdit();
				default:
					Log.log(Log.ERROR, this, "Invalid dependency: " + what);
					return false;
			}
		}

		private boolean isSatisfiedByJEdit()
		{
			String build = jEdit.getBuild();

			return (from == null ||
				StandardUtilities.compareStrings(build, from, false) >= 0) &&
				(to == null ||
					StandardUtilities.compareStrings(build, to, false) <= 0);
		}

		private boolean isSatisfiedByJdk()
		{
			String javaVersion = System.getProperty("java.version");
			// openjdk 9 returns just "9", not 1.X.X like previous versions
			javaVersion = javaVersion.length() >= 3 ? javaVersion.substring(0, 3) : javaVersion;

			return (from == null ||
				StandardUtilities.compareStrings(javaVersion, from, false) >= 0) &&
				(to == null ||
					StandardUtilities.compareStrings(javaVersion, to, false) <= 0);
		}

		private boolean isSatisfiedByPlugin()
		{
			for(int i = 0; i < plugin.branches.size(); i++)
			{
				String installedVersion = plugin.getInstalledVersion();
				if(installedVersion != null &&
					(from == null ||
						StandardUtilities.compareStrings(installedVersion,from,false) >= 0) &&
					(to == null ||
						StandardUtilities.compareStrings(installedVersion,to,false) <= 0))
				{
					return true;
				}
			}

			return false;
		}

		boolean canSatisfy()
		{
			if(isSatisfied())
				return true;
			if (what.equals("plugin"))
				return plugin.canBeInstalled();
			return false;
		}

		void satisfy(Roster roster, String installDirectory, boolean downloadSource)
		{
			if ("plugin".equals(what))
			{
				String installedVersion = plugin.getInstalledVersion();
				for (int i = 0; i < plugin.branches.size(); i++)
				{
					Branch branch = plugin.branches.get(i);
					if ((installedVersion == null ||
						StandardUtilities.compareStrings(installedVersion,branch.version,false) < 0) &&
						(from == null ||
							StandardUtilities.compareStrings(branch.version,from,false) >= 0) &&
						(to == null ||
							StandardUtilities.compareStrings(branch.version,to,false) <= 0))
					{
						plugin.install(roster,installDirectory,
							downloadSource, false);
						return;
					}
				}
			}
		}

		public String toString()
		{
			return "[what=" + what + ",from=" + from + ",to=" + to + ",plugin=" + plugin + ']';
		}
	} //}}}

	//{{{ Private members

	// TODO: this isn't used, should it be?
	private static String getAutoSelectedMirror()
		throws java.io.IOException
	{
		final String samplerUrl = "http://sourceforge.net/projects/jedit/files/latest/download";
		final HttpURLConnection connection = (HttpURLConnection)((new URL(samplerUrl)).openConnection());
		connection.setInstanceFollowRedirects(false);
		final int response = connection.getResponseCode();
		if (response != HttpURLConnection.HTTP_MOVED_TEMP)
		{
			throw new RuntimeException("Unexpected response: " + response + ": from " + samplerUrl);
		}
		final String redirected = connection.getHeaderField("Location");
		if (redirected == null)
		{
			throw new RuntimeException("Missing Location header: " + samplerUrl);
		}
		final String prefix = "use_mirror=";
		final int found = redirected.lastIndexOf(prefix);
		if (found == -1)
		{
			throw new RuntimeException("Mirror prefix \"use_mirror\" was not found in redirected URL: " + redirected);
		}
		final int start = found + prefix.length();
		final int end = redirected.indexOf('&', start);
		return end != -1 ?
			redirected.substring(start, end) :
			redirected.substring(start);
	}
	//}}}
}
