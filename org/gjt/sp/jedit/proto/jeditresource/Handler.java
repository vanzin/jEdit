/*
 * Handler.java - jEdit plugin list URL protocol handler
 * Copyright (C) 1999 Slava Pestov
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

import java.io.IOException;
import java.net.*;
/**
 *  
 *
 * One somewhat unconventional requirement of URLStreamHandler classes 
 * is that the class name and even the package name have certain restrictions. 
 * You must name the handler class Handler, as in the previous example. 
 * The package name must include the protocol name as the last dot-separated token.
 * This way, the Handler is automatically created in a lazy-fashion by the default
 * URLStreamHandlerFactory.
 *
 * see http://java.sun.com/developer/onlineTraining/protocolhandlers/
 * 
 * You should never need to create an instance of this class directly. 
 *
 */
public class Handler extends URLStreamHandler
{
	
	public URLConnection openConnection(URL url)
		throws IOException
	{
		PluginResURLConnection c = new PluginResURLConnection(url);
		c.connect();
		return c;
	}
}
