/*
 * ServerKiller.java - Utility class for the installer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2009 Eric Le Lay
 *
 * this code is freely adapted from org/gjt/sp/jedit/jEdit.java
 * Copyright (C) 1998, 2005 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package installer;

import java.io.*;
import java.net.*;

/**
 * Utility class to check for a running jEdit server,
 * and stop it.
 * Useful on windows platform, where the jedit.jar archive
 * is locked and can't be overwritten by the installer.
 *
 * NB: The server file must be in the standard location (i.e. $HOME/.jedit/server)
 * for the server to be found.
 * @version	$Id$
 */
public class ServerKiller
{
	
	/**
	 * try to contact a running instance of jEdit Server
	 * and ask it to close.
	 * @return	true	either if no server was detected, or the server was shut-down,
	 *		false otherwise
	 */
	public static boolean quitjEditServer()
	{
		
		/* {{{ default server file location */
		String settingsDirectory = System.getProperty("user.home");
		File portFile;
		File f = new File(settingsDirectory);
		portFile = new File(f,".jedit/server");
		/* }}} */
		
		if(portFile.exists())
		{
			try
			{
				BufferedReader in = new BufferedReader(new FileReader(portFile));
				String check = in.readLine();
				if(!check.equals("b"))
				{
					System.out.println("Wrong port file format");
					return false;
				}
 
				int port = Integer.parseInt(in.readLine());
				int key = Integer.parseInt(in.readLine());

				Socket socket = new Socket(InetAddress.getByName("127.0.0.1"),port);
				DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
				out.writeInt(key);

				// we can't close the socket cleanly, because we want
				// to wait for complete exit, and then it's too late.
				// so the socket is closed when the JVM is shut down.
				String script;
					script = "jEdit.exit(null,true);\n";

				out.writeUTF(script);

				// block until its closed
				try
				{
					socket.getInputStream().read();
				}
				catch(Exception e)
				{
					//should get an exception !
				}

				in.close();
				out.close();
			}
			catch(FileNotFoundException fnfe)
			{
				//it exists : we checked that earlier !
			}
			catch(UnknownHostException uhe)
			{
				//localhost doesn't exist ?
			}
			catch(IOException ioe)
			{
				System.out.println("Exception while trying to connect to existing server:");
				System.out.println(ioe);
				System.out.println("Don't worry too much !");
				return false; //warn the user
			}
		}
		return true;
	}
	
	/**
	 * try to connect to any running server instance and close it.
	 * exit with an error code on failure, but not if no server was found.
	 */
	public static void main(String[] args)
	{
		boolean success = quitjEditServer();
		if(!success)
		{
			System.exit(-1);
		}
	}
}

 	  	 
