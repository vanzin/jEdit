/*
 * PluginResURLConnection.java - jEdit plugin resource URL connection
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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
import java.io.*;
import java.net.*;
import org.gjt.sp.jedit.*;
//}}}

public class PluginResURLConnection extends URLConnection
{
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
	}

	public void connect() throws IOException
	{
		if(!connected)
		{
			if(plugin == null)
			{
				in = jEdit.class.getResourceAsStream(resource);
			}
			else
			{
				PluginJAR[] plugins = jEdit.getPluginJARs();
				for(int i = 0; i < plugins.length; i++)
				{
					PluginJAR jar = plugins[i];
					String jarName =MiscUtilities.getFileName(jar.getPath()).toLowerCase(); 
					if(plugin.equalsIgnoreCase(jarName))
					{
						in = jar.getClassLoader()
						            .getResourceAsStream(resource);
						break;
					}
				}
			}

			if(in == null)
			{
				throw new IOException("Resource not found: " + plugin + "!" 
					+ resource);
			}

			connected = true;
		}
	}

	public InputStream getInputStream()
		throws IOException
	{
		connect();
		return in;
	}

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
	}

	// private members
	private InputStream in;
	private String plugin;
	private String resource;
}
