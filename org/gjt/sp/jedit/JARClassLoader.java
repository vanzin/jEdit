/*
 * JARClassLoader.java - Loads classes from JAR files
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001, 2002 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
 * Portions copyright (C) 2002 Marco Hunsicker
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
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.util.Log;
//}}}

/**
 * A class loader implementation that loads classes from JAR files.
 * @author Slava Pestov
 * @version $Id$
 */
public class JARClassLoader extends ClassLoader
{
	//{{{ JARClassLoader constructor
	/**
	 * This constructor creates a class loader for loading classes from all
	 * plugins. For example BeanShell uses one of these so that scripts can
	 * use plugin classes.
	 */
	public JARClassLoader()
	{
	} //}}}

	//{{{ JARClassLoader constructor
	public JARClassLoader(String path)
		throws IOException
	{
		zipFile = new JarFile(path);
		definePackages();

		jar = new EditPlugin.JAR(path,this);

		Enumeration entires = zipFile.entries();
		while(entires.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)entires.nextElement();
			String name = entry.getName();
			String lname = name.toLowerCase();
			if(lname.equals("actions.xml"))
			{
				jEdit.loadActions(
					path + "!actions.xml",
					new BufferedReader(new InputStreamReader(
					zipFile.getInputStream(entry))),
					jar.getActions());
			}
			if(lname.equals("dockables.xml"))
			{
				DockableWindowManager.loadDockableWindows(
					path + "!dockables.xml",
					new BufferedReader(new InputStreamReader(
					zipFile.getInputStream(entry))),
					jar.getActions());
			}
			else if(lname.endsWith(".props"))
				jEdit.loadProps(zipFile.getInputStream(entry),true);
			else if(name.endsWith(".class"))
			{
				classHash.put(MiscUtilities.fileToClass(name),this);

				if(name.endsWith("Plugin.class"))
					pluginClasses.addElement(name);
			}
		}

		jEdit.addPluginJAR(jar);
	} //}}}

	//{{{ loadClass() method
	/**
	 * @exception ClassNotFoundException if the class could not be found
	 */
	public Class loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		// see what JARClassLoader this class is in
		Object obj = classHash.get(clazz);
		if(obj == NO_CLASS)
		{
			// we remember which classes we don't exist
			// because BeanShell tries loading all possible
			// <imported prefix>.<class name> combinations
			throw new ClassNotFoundException(clazz);
		}
		else if(obj instanceof ClassLoader)
		{
			JARClassLoader classLoader = (JARClassLoader)obj;
			return classLoader._loadClass(clazz,resolveIt);
		}

		// if it's not in the class hash, and not marked as
		// non-existent, try loading it from the CLASSPATH
		try
		{
			Class cls;

			/* Defer to whoever loaded us (such as JShell,
			 * Echidna, etc) */
			ClassLoader parentLoader = getClass().getClassLoader();
			if (parentLoader != null)
				cls = parentLoader.loadClass(clazz);
			else
				cls = findSystemClass(clazz);

			return cls;
		}
		catch(ClassNotFoundException cnf)
		{
			// remember that this class doesn't exist for
			// future reference
			classHash.put(clazz,NO_CLASS);

			throw cnf;
		}
	} //}}}

	//{{{ getResourceAsStream() method
	public InputStream getResourceAsStream(String name)
	{
		if(zipFile == null)
			return null;

		try
		{
			ZipEntry entry = zipFile.getEntry(name);
			if(entry == null)
				return getSystemResourceAsStream(name);
			else
				return zipFile.getInputStream(entry);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);

			return null;
		}
	} //}}}

	//{{{ getResource() method
	public URL getResource(String name)
	{
		if(zipFile == null)
			return null;

		ZipEntry entry = zipFile.getEntry(name);
		if(entry == null)
			return getSystemResource(name);

		try
		{
			return new URL(getResourceAsPath(name));
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			return null;
		}
	} //}}}

	//{{{ getResourceAsPath() method
	public String getResourceAsPath(String name)
	{
		if(zipFile == null)
			return null;

		if(!name.startsWith("/"))
			name = "/" + name;

		return "jeditresource:/" + MiscUtilities.getFileName(
			jar.getPath()) + "!" + name;
	} //}}}

	//{{{ closeZipFile() method
	/**
	 * Closes the ZIP file. This plugin will no longer be usable
	 * after this.
	 * @since jEdit 2.6pre1
	 */
	public void closeZipFile()
	{
		if(zipFile == null)
			return;

		try
		{
			zipFile.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}

		zipFile = null;
	} //}}}

	//{{{ getZipFile() method
	/**
	 * Returns the ZIP file associated with this class loader.
	 * @since jEdit 3.0final
	 */
	public ZipFile getZipFile()
	{
		return zipFile;
	} //}}}

	//{{{ startAllPlugins() method
	void startAllPlugins()
	{
		for(int i = 0; i < pluginClasses.size(); i++)
		{
			String name = (String)pluginClasses.elementAt(i);
			name = MiscUtilities.fileToClass(name);

			try
			{
				loadPluginClass(name);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error while starting plugin " + name);
				Log.log(Log.ERROR,this,t);

				jar.addPlugin(new EditPlugin.Broken(name));
				String[] args = { t.toString() };
				jEdit.pluginError(jar.getPath(),
					"plugin-error.start-error",args);
			}
		}
	} //}}}

	//{{{ Private members

	// used to mark non-existent classes in class hash
	private static final Object NO_CLASS = new Object();

	private static Hashtable classHash = new Hashtable();

	private EditPlugin.JAR jar;
	private Vector pluginClasses = new Vector();
	private JarFile zipFile;

	//{{{ loadPluginClass() method
	private void loadPluginClass(String name)
		throws Exception
	{
		// Check if a plugin with the same name is already loaded
		EditPlugin[] plugins = jEdit.getPlugins();

		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClass().getName().equals(name))
			{
				jEdit.pluginError(jar.getPath(),
					"plugin-error.already-loaded",null);
				return;
			}
		}

		/* This is a bit silly... but WheelMouse seems to be
		 * unmaintained so the best solution is to add a hack here.
		 */
		if(name.equals("WheelMousePlugin")
			&& OperatingSystem.hasJava14())
		{
			jar.addPlugin(new EditPlugin.Broken(name));
			jEdit.pluginError(jar.getPath(),"plugin-error.obsolete",null);
			return;
		}

		// Check dependencies
		if(!checkDependencies(name))
		{
			jar.addPlugin(new EditPlugin.Broken(name));
			return;
		}

		// JDK 1.1.8 throws a GPF when we do an isAssignableFrom()
		// on an unresolved class
		Class clazz = loadClass(name,true);
		int modifiers = clazz.getModifiers();
		if(!Modifier.isInterface(modifiers)
			&& !Modifier.isAbstract(modifiers)
			&& EditPlugin.class.isAssignableFrom(clazz))
		{
			String label = jEdit.getProperty("plugin."
				+ name + ".name");
			String version = jEdit.getProperty("plugin."
				+ name + ".version");

			if(version == null)
			{
				Log.log(Log.ERROR,this,"Plugin " +
					name + " needs"
					+ " 'name' and 'version' properties.");
				jar.addPlugin(new EditPlugin.Broken(name));
				return;
			}

			jar.getActions().setLabel(jEdit.getProperty(
				"action-set.plugin",
				new String[] { label }));

			Log.log(Log.NOTICE,this,"Starting plugin " + label
					+ " (version " + version + ")");

			jar.addPlugin((EditPlugin)clazz.newInstance());
		}
	} //}}}

	//{{{ checkDependencies() method
	private boolean checkDependencies(String name)
	{
		int i = 0;

		String dep;
		while((dep = jEdit.getProperty("plugin." + name + ".depend." + i++)) != null)
		{
			int index = dep.indexOf(' ');
			if(index == -1)
			{
				Log.log(Log.ERROR,this,name + " has an invalid"
					+ " dependency: " + dep);
				return false;
			}

			String what = dep.substring(0,index);
			String arg = dep.substring(index + 1);

			if(what.equals("jdk"))
			{
				if(MiscUtilities.compareStrings(
					System.getProperty("java.version"),
					arg,false) < 0)
				{
					String[] args = { arg,
						System.getProperty("java.version") };
					jEdit.pluginError(jar.getPath(),"plugin-error.dep-jdk",args);
					return false;
				}
			}
			else if(what.equals("jedit"))
			{
				if(arg.length() != 11)
				{
					Log.log(Log.ERROR,this,"Invalid jEdit version"
						+ " number: " + arg);
					return false;
				}

				if(MiscUtilities.compareStrings(
					jEdit.getBuild(),arg,false) < 0)
				{
					String needs = MiscUtilities.buildToVersion(arg);
					String[] args = { needs,
						jEdit.getVersion() };
					jEdit.pluginError(jar.getPath(),
						"plugin-error.dep-jedit",args);
					return false;
				}
			}
			else if(what.equals("plugin"))
			{
				int index2 = arg.indexOf(' ');
				if(index2 == -1)
				{
					Log.log(Log.ERROR,this,name 
						+ " has an invalid dependency: "
						+ dep + " (version is missing)");
					return false;
				}
				
				String plugin = arg.substring(0,index2);
				String needVersion = arg.substring(index2 + 1);
				String currVersion = jEdit.getProperty("plugin." 
					+ plugin + ".version");

				if(currVersion == null)
				{
					String[] args = { needVersion, plugin };
					jEdit.pluginError(jar.getPath(),
						"plugin-error.dep-plugin.no-version",
						args);
					return false;
				}

				if(MiscUtilities.compareStrings(currVersion,
					needVersion,false) < 0)
				{
					String[] args = { needVersion, plugin, currVersion };
					jEdit.pluginError(jar.getPath(),
						"plugin-error.dep-plugin",args);
					return false;
				}

				if(jEdit.getPlugin(plugin) instanceof EditPlugin.Broken)
				{
					String[] args = { plugin };
					jEdit.pluginError(jar.getPath(),
						"plugin-error.dep-plugin.broken",args);
					return false;
				}
			}
			else if(what.equals("class"))
			{
				try
				{
					loadClass(arg,false);
				}
				catch(Exception e)
				{
					String[] args = { arg };
					jEdit.pluginError(jar.getPath(),
						"plugin-error.dep-class",args);
					return false;
				}
			}
			else
			{
				Log.log(Log.ERROR,this,name + " has unknown"
					+ " dependency: " + dep);
				return false;
			}
		}

		return true;
	} //}}}

	//{{{ _loadClass() method
	/**
	 * Load class from this JAR only.
	 */
	private Class _loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		Class cls = findLoadedClass(clazz);
		if(cls != null)
		{
			if(resolveIt)
				resolveClass(cls);
			return cls;
		}

		String name = MiscUtilities.classToFile(clazz);

		try
		{
			ZipEntry entry = zipFile.getEntry(name);

			if(entry == null)
				throw new ClassNotFoundException(clazz);

			InputStream in = zipFile.getInputStream(entry);

			int len = (int)entry.getSize();
			byte[] data = new byte[len];
			int success = 0;
			int offset = 0;
			while(success < len)
			{
				len -= success;
				offset += success;
				success = in.read(data,offset,len);
				if(success == -1)
				{
					Log.log(Log.ERROR,this,"Failed to load class "
						+ clazz + " from " + zipFile.getName());
					throw new ClassNotFoundException(clazz);
				}
			}

			cls = defineClass(clazz,data,0,data.length);

			if(resolveIt)
				resolveClass(cls);

			return cls;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);

			throw new ClassNotFoundException(clazz);
		}
	} //}}}

	//{{{ definePackages() method
	/**
	 * Defines all packages found in the given Java archive file. The
	 * attributes contained in the specified Manifest will be used to obtain
	 * package version and sealing information.
	 */
	private void definePackages()
	{
		try
		{
			Manifest manifest = zipFile.getManifest();

			if(manifest != null)
			{
				Map entries = manifest.getEntries();
				Iterator i = entries.keySet().iterator();

				while(i.hasNext())
				{
					String path = (String)i.next();

					if(!path.endsWith(".class"))
					{
						String name = path.replace('/', '.');

						if(name.endsWith("."))
							name = name.substring(0, name.length() - 1);

						// code url not implemented
						definePackage(path,name,manifest,null);
					}
				}
			}
		}
		catch (Exception ex)
		{
			// should never happen, not severe anyway
			Log.log(Log.ERROR, this,"Error extracting manifest info "
				+ "for file " + zipFile);
			Log.log(Log.ERROR, this, ex);
		}
	} //}}}

	//{{{ definePackage() method
	/**
	 * Defines a new package by name in this ClassLoader. The attributes
	 * contained in the specified Manifest will be used to obtain package
	 * version and sealing information. For sealed packages, the additional
	 * URL specifies the code source URL from which the package was loaded.
	 */
	private Package definePackage(String path, String name, Manifest man,
		URL url) throws IllegalArgumentException
	{
		String specTitle = null;
		String specVersion = null;
		String specVendor = null;
		String implTitle = null;
		String implVersion = null;
		String implVendor = null;
		String sealed = null;
		URL sealBase = null;

		Attributes attr = man.getAttributes(path);

		if(attr != null)
		{
			specTitle = attr.getValue(
				Attributes.Name.SPECIFICATION_TITLE);
			specVersion = attr.getValue(
				Attributes.Name.SPECIFICATION_VERSION);
			specVendor = attr.getValue(
				Attributes.Name.SPECIFICATION_VENDOR);
			implTitle = attr.getValue(
				Attributes.Name.IMPLEMENTATION_TITLE);
			implVersion = attr.getValue(
				Attributes.Name.IMPLEMENTATION_VERSION);
			implVendor = attr.getValue(
				Attributes.Name.IMPLEMENTATION_VENDOR);
			sealed = attr.getValue(Attributes.Name.SEALED);
		}

		attr = man.getMainAttributes();

		if (attr != null)
		{
			if (specTitle == null)
			{
				specTitle = attr.getValue(
					Attributes.Name.SPECIFICATION_TITLE);
			}

			if (specVersion == null)
			{
				specVersion = attr.getValue(
					Attributes.Name.SPECIFICATION_VERSION);
			}

			if (specVendor == null)
			{
				specVendor = attr.getValue(
					Attributes.Name.SPECIFICATION_VENDOR);
			}

			if (implTitle == null)
			{
				implTitle = attr.getValue(
					Attributes.Name.IMPLEMENTATION_TITLE);
			}

			if (implVersion == null)
			{
				implVersion = attr.getValue(
					Attributes.Name.IMPLEMENTATION_VERSION);
			}

			if (implVendor == null)
			{
				implVendor = attr.getValue(
					Attributes.Name.IMPLEMENTATION_VENDOR);
			}

			if (sealed == null)
			{
				sealed = attr.getValue(Attributes.Name.SEALED);
			}
		}

		//if("true".equalsIgnoreCase(sealed))
		//	sealBase = url;

		return super.definePackage(name, specTitle, specVersion, specVendor,
			implTitle, implVersion, implVendor,
			sealBase);
	} //}}}

	//}}}
}
