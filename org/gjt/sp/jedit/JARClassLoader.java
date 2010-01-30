/*
 * JARClassLoader.java - Loads classes from JAR files
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.gjt.sp.util.Log;

import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.net.MalformedURLException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
//}}}

/**
 * A class loader implementation that loads classes from JAR files. All
 * instances share the same set of classes.
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
		this(true);
	}

	/**
	 * Creates a class loader that will optionally delegate the
	 * finding of classes to the parent class loader by default.
	 *
	 * @since jEdit 4.3pre6
	 */
	public JARClassLoader(boolean delegateFirst)
	{
		this.delegateFirst = delegateFirst;
		// for debugging
		id = INDEX++;
		live++;
	} //}}}

	//{{{ loadClass() method
	/**
	 * @exception ClassNotFoundException if the class could not be found
	 */
	public Class loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		ClassNotFoundException pending = null;
		if (delegateFirst)
		{
			try
			{
				return loadFromParent(clazz);
			}
			catch (ClassNotFoundException cnf)
			{
				// keep going if class was not found.
				pending = cnf;
			}
		}

		Object obj = classHash.get(clazz);
		if(obj == NO_CLASS)
		{
			// we remember which classes we don't exist
			// because BeanShell tries loading all possible
			// <imported prefix>.<class name> combinations
			throw new ClassNotFoundException(clazz);
		}
		else if(obj instanceof JARClassLoader)
		{
			JARClassLoader classLoader = (JARClassLoader)obj;
			try
			{
				return classLoader._loadClass(clazz,resolveIt);
			} catch (ClassNotFoundException cnf2)
			{
				classHash.put(clazz,NO_CLASS);
				throw cnf2;
			}
		}
		else if (delegateFirst)
		{
			// if delegating, reaching this statement means
			// the class was really not found. Otherwise
			// we'll try loading from the parent class loader.
			throw pending;
		}

		return loadFromParent(clazz);
	} //}}}

	//{{{ getResourceAsStream() method
	public InputStream getResourceAsStream(String name)
	{
		try
		{
			// try in current jar first
			if(jar != null)
			{
				ZipFile zipFile = jar.getZipFile();
				ZipEntry entry = zipFile.getEntry(name);
				if(entry != null)
				{
					return zipFile.getInputStream(entry);
				}
			}
			// then try from another jar
			Object obj = resourcesHash.get(name);
			if(obj instanceof JARClassLoader)
			{
				JARClassLoader classLoader = (JARClassLoader)obj;
				return classLoader.getResourceAsStream(name);
			}
			// finally try from the system class loader
			return getSystemResourceAsStream(name);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);

			return null;
		}
	} //}}}

	//{{{ getResource() method
	/**
	 * overriding getResource() because we want to search FIRST in this
	 * ClassLoader, then the parent, the path, etc.
	 */
	public URL getResource(String name)
	{
		try
		{
			if(jar != null)
			{
				ZipFile zipFile = jar.getZipFile();
				ZipEntry entry = zipFile.getEntry(name);
				if(entry != null)
				{
					return new URL(getResourceAsPath(name));
				}
			}
			
			Object obj = resourcesHash.get(name);
			if(obj instanceof JARClassLoader)
			{
				JARClassLoader classLoader = (JARClassLoader)obj;
				return classLoader.getResource(name);
			} else
			{
				URL ret = getSystemResource(name); 
				if(ret != null)
				{
					Log.log(Log.DEBUG,JARClassLoader.class,"Would have returned null for getResource("+name+")");
					Log.log(Log.DEBUG,JARClassLoader.class,"returning("+ret+")");
				}
				return ret;
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			return null;
		}
	} //}}}

	//{{{ getResourceAsPath() method
	/**
	 * construct a jeditresource:/etc path from the name
	 * of a resource in the associated jar.
	 * The existence of the resource is not actually checked.
	 *
	 * @param name name of the resource
	 * @return jeditresource:/path_to_the_jar!name_of_the_resource
	 * @throws UnsupportedOperationException if this is an anonymous
	 * JARClassLoader (no associated jar).
	 */
	public String getResourceAsPath(String name)
	{
		// this must be fixed during plugin development
		if(jar == null)
			throw new UnsupportedOperationException(
				"don't call getResourceAsPath() on anonymous JARClassLoader");

		if(!name.startsWith("/"))
			name = '/' + name;

		return "jeditresource:/" + MiscUtilities.getFileName(
			jar.getPath()) + '!' + name;
	} //}}}

	//{{{ getZipFile() method
	/**
	 * @deprecated Call <code>PluginJAR.getZipFile()</code> instead.
	 */
	@Deprecated
	public ZipFile getZipFile()
	{
		try
		{
			return jar.getZipFile();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			return null;
		}
	} //}}}

	//{{{ dump() method
	/**
	 * For debugging.
	 */
	public static void dump()
	{
		Log.log(Log.DEBUG,JARClassLoader.class,
			"Total instances created: " + INDEX);
		Log.log(Log.DEBUG,JARClassLoader.class,
			"Live instances: " + live);
		synchronized(classHash)
		{
			for (Map.Entry<String, Object> entry : classHash.entrySet())
			{
				if (entry.getValue() != NO_CLASS)
				{
					Log.log(Log.DEBUG, JARClassLoader.class,
						entry.getKey() + " ==> "
							+ entry.getValue());
				}
			}
		}
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		if(jar == null)
			return "<anonymous>(" + id + ')';
		else
			return jar.getPath() + " (" + id + ')';
	} //}}}

	//{{{ findResources() method
	/**
	 * @return zero or one resource, as returned by getResource()
	 */
	public Enumeration getResources(String name) throws IOException
	{
		class SingleElementEnumeration implements Enumeration
		{
			private Object element;

			SingleElementEnumeration(Object element)
			{
				this.element = element;
			}

			public boolean hasMoreElements()
			{
				return element != null;
			}

			public Object nextElement()
			{
				if(element != null)
				{
					Object retval = element;
					element = null;
					return retval;
				}
				else
					throw new NoSuchElementException();
			}
		}

		URL resource = getResource(name);
		return new SingleElementEnumeration(resource);
	} //}}}

	//{{{ finalize() method
	protected void finalize()
	{
		live--;
	} //}}}

	//{{{ Package-private members

	//{{{ JARClassLoader constructor
	/**
	 * @since jEdit 4.2pre1
	 */
	JARClassLoader(PluginJAR jar)
	{
		this();
		this.jar = jar;
	} //}}}

	//{{{ activate() method
	void activate()
	{
		if (jar.getPlugin() != null)
		{
			String _delegate = jEdit.getProperty(
				"plugin." + jar.getPlugin().getClassName() + ".class_loader_delegate");
			delegateFirst = _delegate == null || "true".equals(_delegate);
		}

		String[] classes = jar.getClasses();
		if(classes != null)
		{
			for(int i = 0; i < classes.length; i++)
			{
				classHash.put(classes[i],this);
			}
		}

		String[] resources = jar.getResources();
		if(resources != null)
		{
			for(int i = 0; i < resources.length; i++)
			{
				resourcesHash.put(resources[i],this);
			}
		}
	} //}}}

	//{{{ deactivate() method
	void deactivate()
	{
		String[] classes = jar.getClasses();
		if(classes != null)
		{
			for(int i = 0; i < classes.length; i++)
			{
				Object loader = classHash.get(classes[i]);
				if(loader == this)
					classHash.remove(classes[i]);
				else
					/* two plugins provide same class! */;
			}
		}

		String[] resources = jar.getResources();
		if(resources == null)
			return;

		for(int i = 0; i < resources.length; i++)
		{
			Object loader = resourcesHash.get(resources[i]);
			if(loader == this)
				resourcesHash.remove(resources[i]);
			else
				/* two plugins provide same resource! */;
		}
	} //}}}

	//}}}

	//{{{ Private members

	// used to mark non-existent classes in class hash
	private static final Object NO_CLASS = new Object();

	private static int INDEX;
	private static int live;
	private static Map<String, Object> classHash = new Hashtable<String, Object>();
	private static Map<String, Object> resourcesHash = new HashMap<String, Object>();

	private int id;
	private boolean delegateFirst;
	private PluginJAR jar;

	//{{{ _loadClass() method
	/**
	 * Load class from this JAR only.
	 */
	private synchronized Class _loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		jar.activatePlugin();

		synchronized(this)
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
				definePackage(clazz);
				ZipFile zipFile = jar.getZipFile();
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
		}
	} //}}}

	//{{{ definePackage(clazz) method
	private void definePackage(String clazz) throws IOException
	{
		int idx = clazz.lastIndexOf('.');
		if (idx != -1)
		{
			String name = clazz.substring(0, idx);
			if (getPackage(name) == null) definePackage(name, new JarFile(jar.getFile()).getManifest());
		}
	} //}}}

	//{{{ getMfValue() method
	private static String getMfValue(Attributes sectionAttrs, Attributes mainAttrs, Attributes.Name name)
	{
		String value=null;
		if (sectionAttrs != null)
			value = sectionAttrs.getValue(name);
		else if (mainAttrs != null)
		{
			value = mainAttrs.getValue(name);
		}
		return value;
	}
	//}}}

	//{{{ definePackage(packageName, manifest) method
	private void definePackage(String name, Manifest mf)
	{
		if (mf==null)
		{
			definePackage(name, null, null, null, null, null,
			null, null);
			return;
		}

		Attributes sa = mf.getAttributes(name.replace('.', '/') + '/');
		Attributes ma = mf.getMainAttributes();

		URL sealBase = null;
		if (Boolean.valueOf(getMfValue(sa, ma, Name.SEALED)).booleanValue())
		{
			try
			{
				sealBase = jar.getFile().toURL();
			}
			catch (MalformedURLException e) {}
		}

		definePackage(
			name,
			getMfValue(sa, ma, Name.SPECIFICATION_TITLE),
			getMfValue(sa, ma, Name.SPECIFICATION_VERSION),
			getMfValue(sa, ma, Name.SPECIFICATION_VENDOR),
			getMfValue(sa, ma, Name.IMPLEMENTATION_TITLE),
			getMfValue(sa, ma, Name.IMPLEMENTATION_VERSION),
			getMfValue(sa, ma, Name.IMPLEMENTATION_VENDOR),
			sealBase);
	} //}}}

	//{{{ loadFromParent() method
	private Class loadFromParent(String clazz)
		throws ClassNotFoundException
	{
		Class cls;

		ClassLoader parentLoader = getClass().getClassLoader();
		if (parentLoader != null)
			cls = parentLoader.loadClass(clazz);
		else
			cls = findSystemClass(clazz);

		return cls;
	} //}}}

	//}}}
}
