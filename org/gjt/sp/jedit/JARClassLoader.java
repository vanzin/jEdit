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
		// see what JARClassLoader this class is in
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
		if(jar == null)
			return null;

		try
		{
			ZipFile zipFile = jar.getZipFile();
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
		if(jar == null)
			return null;

		try
		{
			ZipFile zipFile = jar.getZipFile();
			ZipEntry entry = zipFile.getEntry(name);
			if(entry == null)
				return getSystemResource(name);
			else
				return new URL(getResourceAsPath(name));
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			return null;
		}
	} //}}}

	//{{{ getResourceAsPath() method
	public String getResourceAsPath(String name)
	{
		if(jar == null)
			return null;

		if(!name.startsWith("/"))
			name = "/" + name;

		return "jeditresource:/" + MiscUtilities.getFileName(
			jar.getPath()) + "!" + name;
	} //}}}

	//{{{ getZipFile() method
	/**
	 * @deprecated Call <code>PluginJAR.getZipFile()</code> instead.
	 */
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
			Iterator entries = classHash.entrySet().iterator();
			while(entries.hasNext())
			{
				Map.Entry entry = (Map.Entry)entries.next();
				if(entry.getValue() != NO_CLASS)
				{
					Log.log(Log.DEBUG,JARClassLoader.class,
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
			return "<anonymous>(" + id + ")";
		else
			return jar.getPath() + " (" + id + ")";
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
		String[] classes = jar.getClasses();
		if(classes != null)
		{
			for(int i = 0; i < classes.length; i++)
			{
				classHash.put(classes[i],this);
			}
		}
	} //}}}

	//{{{ deactivate() method
	void deactivate()
	{
		String[] classes = jar.getClasses();
		if(classes == null)
			return;

		for(int i = 0; i < classes.length; i++)
		{
			Object loader = classHash.get(classes[i]);
			if(loader == this)
				classHash.remove(classes[i]);
			else
				/* two plugins provide same class! */;
		}
	} //}}}

	//}}}

	//{{{ Private members

	// used to mark non-existent classes in class hash
	private static final Object NO_CLASS = new Object();

	private static int INDEX;
	private static int live;
	private static Hashtable classHash = new Hashtable();

	private int id;
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

	//}}}
}
