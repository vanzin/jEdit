/*
 * PluginResURLConnection.java - jEdit plugin resource URL connection
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 1999 - 2014 Slava Pestov, Eric Le Lay, Alan Ezust
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

package org.gjt.sp.jedit.proto.jeditresource;

//{{{ Imports
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nonnull;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
//}}}

//{{{ class PluginResURLConnection
/** An implementation of jeditresource:/ url protocol.

	Can be used for accessing jEdit core resources as well.
*/
public class PluginResURLConnection extends URLConnection
{
	//{{{ constructor
	public PluginResURLConnection(URL url)
		throws IOException
	{
		super(url);

		String file = url.getFile();

		int index = file.indexOf('!',0);
		if(index == -1)
		{
			plugin = null;
			resource = file;
		}
		else
		{
			int start;
			if(file.charAt(0) == '/')
				start = 1;
			else
				start = 0;

			plugin = file.substring(start,index);
			resource = file.substring(index + 1);
		}

		if(plugin != null && resource.startsWith("/"))
			resource = resource.substring(1);
	}//}}}

	//{{{ connect()
	/**
	 * @throws	IOException	on error
	 * @throws	FileNotFoundException if resource is not found
	 */
	public void connect() throws IOException, FileNotFoundException
	{
		if(!connected)
		{
			if(plugin == null)
			{
				in = jEdit.class.getResourceAsStream(resource);
			}
			else
			{
				boolean pluginFoundInPluginJARs = false;
				PluginJAR[] plugins = jEdit.getPluginJARs();
				for (PluginJAR jar : plugins)
				{
					String jarName = MiscUtilities.getFileName(jar.getPath()).toLowerCase();
					if (plugin.equalsIgnoreCase(jarName))
					{
						pluginFoundInPluginJARs = true;
						in = jar.getClassLoader().getResourceAsStream(resource);
						break;
					}
				}
				if(!pluginFoundInPluginJARs){
					Log.log(Log.DEBUG, PluginResURLConnection.class,
							"reading resource from not loaded plugin "
							+" => will always fail !");
				}
			}

			if((in == null) && (plugin == null))
			{
				// can't find it in jEdit.jar, look in getJEditHome().
				File f = new File(jEdit.getJEditHome(), resource);
				if (f.exists())
					in = new FileInputStream(f);
			}
			connected = true;
		}
		if(in == null)
		{
			if(plugin != null)
			{
				throw new FileNotFoundException("Resource not found: " + plugin + "!" + resource);
			}
			else
			{
				throw new FileNotFoundException("Resource not found: " + getURL());
			}
		}
	}//}}}

	//{{{ getInputStream()
	/**
	 * @return	input stream to read the resource's contents. never null
	 * @throws	IOException	on error
	 * @throws	FileNotFoundException if resource is not found
	 */
	@Nonnull
	public InputStream getInputStream()
		throws IOException, FileNotFoundException
	{
		connect();
		return in;
	}//}}}

	//{{{ getHeaderField()
	public String getHeaderField(String name)
	{
		if(name.equals("content-type"))
		{
			String lcResource = resource.toLowerCase();
			if(lcResource.endsWith(".html"))
				return "text/html";
			else if(lcResource.endsWith(".txt"))
				return "text/plain";
			else if(lcResource.endsWith(".rtf"))
				return "text/rtf";
			else if(lcResource.endsWith(".gif"))
				return "image/gif";
			else if(lcResource.endsWith(".jpg")
				|| lcResource.endsWith(".jpeg"))
				return "image/jpeg";
			else
				return null;
		}
		else
			return null;
	}//}}}

	//{{{ private members
	private InputStream in;
	private String plugin;
	private String resource;
	//}}}
}//}}}
