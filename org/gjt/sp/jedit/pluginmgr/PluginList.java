/*
 * PluginList.java - Plugin list
 * Copyright (C) 2001 Slava Pestov
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

import com.microstar.xml.*;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.*;

/**
 * Plugin list downloaded from server.
 * @since jEdit 3.2pre2
 */
class PluginList
{
	Vector plugins;
	Hashtable pluginHash;
	Vector pluginSets;

	PluginList() throws Exception
	{
		plugins = new Vector();
		pluginHash = new Hashtable();
		pluginSets = new Vector();

		String path = jEdit.getProperty("plugin-manager.url");
		PluginListHandler handler = new PluginListHandler(this,path);
		XmlParser parser = new XmlParser();
		parser.setHandler(handler);

		parser.parse(null,null,new BufferedReader(new InputStreamReader(
			new URL(path).openStream(),"UTF8")));
	}

	void addPlugin(Plugin plugin)
	{
		plugin.checkIfInstalled();
		plugins.addElement(plugin);
		pluginHash.put(plugin.name,plugin);
	}

	void addPluginSet(PluginSet set)
	{
		pluginSets.addElement(set);
	}

	void finished()
	{
		// after the entire list is loaded, fill out plugin field
		// in dependencies
		for(int i = 0; i < plugins.size(); i++)
		{
			Plugin plugin = (Plugin)plugins.elementAt(i);
			for(int j = 0; j < plugin.branches.size(); j++)
			{
				Branch branch = (Branch)plugin.branches.elementAt(j);
				for(int k = 0; k < branch.deps.size(); k++)
				{
					Dependency dep = (Dependency)branch.deps.elementAt(k);
					if(dep.what.equals("plugin"))
						dep.plugin = (Plugin)pluginHash.get(dep.pluginName);
				}
			}
		}
	}

	void dump()
	{
		for(int i = 0; i < plugins.size(); i++)
		{
			System.err.println((Plugin)plugins.elementAt(i));
			System.err.println();
		}
	}

	static class PluginSet
	{
		String name;
		String description;
		Vector plugins = new Vector();

		void install(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			for(int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = (Plugin)plugins.elementAt(i);
				if(plugin.canBeInstalled())
				{
					plugin.install(roster,installDirectory,
						downloadSource);
				}
			}
		}

		public String toString()
		{
			return plugins.toString();
		}
	}

	static class Plugin
	{
		String jar;
		String name;
		String description;
		String author;
		Vector branches = new Vector();
		String installed;
		String installedVersion;

		void checkIfInstalled()
		{
			// check if the plugin is already installed.
			// this is a bit of hack
			EditPlugin.JAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();
				if(!new File(path).exists())
					continue;

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;

					EditPlugin[] plugins = jars[i].getPlugins();
					if(plugins.length >= 1)
					{
						installedVersion = jEdit.getProperty(
							"plugin." + plugins[0].getClassName()
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
			}
		}

		/**
		 * Find the first branch compatible with the running jEdit release.
		 */
		Branch getCompatibleBranch()
		{
			for(int i = 0; i < branches.size(); i++)
			{
				Branch branch = (Branch)branches.elementAt(i);
				if(branch.canSatisfyDependencies())
					return branch;
			}

			return null;
		}

		boolean canBeInstalled()
		{
			Branch branch = getCompatibleBranch();
			return branch != null && !branch.obsolete;
		}

		void install(Roster roster, String installDirectory, boolean downloadSource)
		{
			if(installed != null)
				roster.addOperation(new Roster.Remove(installed));

			Branch branch = getCompatibleBranch();
			if(branch.obsolete)
				return;

			branch.satisfyDependencies(roster,installDirectory,
				downloadSource);

			if(installed != null)
			{
				installDirectory = MiscUtilities.getParentOfPath(
					installed);
			}

			roster.addOperation(new Roster.Install((downloadSource
				? branch.downloadSource : branch.download),
				installDirectory));

		}

		public String toString()
		{
			return name;
		}
	}

	static class Branch
	{
		String version;
		String date;
		String download;
		String downloadSource;
		boolean obsolete;
		Vector deps = new Vector();

		boolean canSatisfyDependencies()
		{
			for(int i = 0; i < deps.size(); i++)
			{
				Dependency dep = (Dependency)deps.elementAt(i);
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
				Dependency dep = (Dependency)deps.elementAt(i);
				dep.satisfy(roster,installDirectory,downloadSource);
			}
		}

		public String toString()
		{
			return "[version=" + version + ",download=" + download
				+ ",obsolete=" + obsolete + ",deps=" + deps + "]";
		}
	}

	static class Dependency
	{
		String what;
		String from;
		String to;
		// only used if what is "plugin"
		String pluginName;
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
					Branch branch = (Branch)plugin.branches
						.elementAt(i);
					if(plugin.installedVersion != null
						&&
					(from == null || MiscUtilities.compareVersions(
						plugin.installedVersion,from) >= 0)
						&&
					   (to == null || MiscUtilities.compareVersions(
					   	plugin.installedVersion,to) <= 0))
					{
						return true;
					}
				}

				return false;
			}
			else if(what.equals("jdk"))
			{
				String javaVersion = System.getProperty("java.version").substring(0,3);

				if((from == null || MiscUtilities.compareVersions(
					javaVersion,from) >= 0)
					&&
				   (to == null || MiscUtilities.compareVersions(
				   	javaVersion,to) <= 0))
					return true;
				else
					return false;
			}
			else if(what.equals("jedit"))
			{
				String build = jEdit.getBuild();

				if((from == null || MiscUtilities.compareVersions(
					build,from) >= 0)
					&&
				   (to == null || MiscUtilities.compareVersions(
				   	build,to) <= 0))
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
			// new plugins can always be downloaded (assuming Mike
			// maintains plugin central properly)
			return (what.equals("plugin") || isSatisfied());
		}

		void satisfy(Roster roster, String installDirectory,
			boolean downloadSource)
		{
			if(what.equals("plugin"))
			{
				for(int i = 0; i < plugin.branches.size(); i++)
				{
					Branch branch = (Branch)plugin.branches
						.elementAt(i);
					if((plugin.installedVersion == null
						||
					MiscUtilities.compareVersions(
						plugin.installedVersion,branch.version) < 0)
						&&
					(from == null || MiscUtilities.compareVersions(
						branch.version,from) >= 0)
						&&
					   (to == null || MiscUtilities.compareVersions(
					   	branch.version,to) <= 0))
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
				+ ",to=" + to + ",plugin=" + plugin + "]";
		}
	}
}
