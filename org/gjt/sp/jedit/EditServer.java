/*
 * EditServer.java - jEdit server
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

package org.gjt.sp.jedit;

//{{{ Imports
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.*;
import java.util.Random;
import org.gjt.sp.util.Log;
//}}}

/**
 * The edit server protocol is very simple. <code>$HOME/.jedit/server</code>
 * is an ASCII file containing two lines, the first being the port number,
 * the second being the authorization key.<p>
 *
 * You connect to that port on the local machine, sending the authorization
 * key as ASCII, followed by a newline, followed by a BeanShell script.
 * Then close the socket and the BeanShell script will be executed by the
 * server instance of jEdit.<p>
 *
 * The snippet is executed in the AWT thread. None of the usual BeanShell
 * variables (view, buffer, textArea, editPane) are set so the script has to
 * figure things out by itself.<p>
 *
 * In most cases, the script will call the static
 * <code>EditServer.handleClient()</code> method, but of course more
 * complicated stuff can be done too.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class EditServer extends Thread
{
	//{{{ EditServer constructor
	EditServer(String portFile)
	{
		super("jEdit server daemon [" + portFile + "]");
		setDaemon(true);
		this.portFile = portFile;

		try
		{
			// Bind to any port on localhost; accept 2 simultaneous
			// connection attempts before rejecting connections
			socket = new ServerSocket(0, 2,
				InetAddress.getByName("127.0.0.1"));
			authKey = Math.abs(new Random().nextInt());
			int port = socket.getLocalPort();

			FileWriter out = new FileWriter(portFile);
			out.write(String.valueOf(port));
			out.write("\n");
			out.write(String.valueOf(authKey));
			out.write("\n");
			out.close();

			Log.log(Log.DEBUG,this,"jEdit server started on port "
				+ socket.getLocalPort());
			Log.log(Log.DEBUG,this,"Authorization key is "
				+ authKey);

			ok = true;
		}
		catch(IOException io)
		{
			/* on some Windows versions, connections to localhost
			 * fail if the network is not running. To avoid
			 * confusing newbies with weird error messages, log
			 * errors that occur while starting the server
			 * as NOTICE, not ERROR */
			Log.log(Log.NOTICE,this,io);
		}
	} //}}}

	//{{{ isOK() method
	public boolean isOK()
	{
		return ok;
	} //}}}

	//{{{ run() method
	public void run()
	{
		try
		{
			for(;;)
			{
				Socket client = socket.accept();
				Log.log(Log.MESSAGE,this,client + ": connected");

				BufferedReader in = new BufferedReader(
					new InputStreamReader(
					client.getInputStream(),
					"UTF8"));

				try
				{
					int key = Integer.parseInt(in.readLine());
					if(key != authKey)
					{
						Log.log(Log.ERROR,this,
							client + ": wrong"
							+ " authorization key");
						in.close();
						client.close();
						return;
					}
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,
							client + ": invalid"
							+ " authorization key");
					in.close();
					client.close();
					return;
				}

				Log.log(Log.DEBUG,this,client + ": authenticated"
					+ " successfully");
				handleClient(client,in);

				client.close();
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}
	} //}}}

	//{{{ handleClient() method
	/**
	 * @param restore Ignored unless no views are open
	 * @param parent The client's parent directory
	 * @param args A list of files. Null entries are ignored, for convinience
	 * @since jEdit 3.2pre7
	 */
	public static void handleClient(boolean restore, String parent,
		String[] args)
	{
		String splitConfig = null;

		boolean newView = jEdit.getBooleanProperty("client.newView");

		// we have to deal with a huge range of possible border cases here.
		if(jEdit.getFirstView() == null || newView)
		{
			// coming out of background mode.
			// no views open.
			// no buffers open if args empty.

			Buffer buffer = jEdit.openFiles(null,parent,args);

			if(restore)
			{
				if(jEdit.getFirstBuffer() == null
					|| (jEdit.getFirstBuffer().isUntitled()
					&& jEdit.getBufferCount() == 1))
					splitConfig = jEdit.restoreOpenFiles();
				else if(jEdit.getBooleanProperty("restore.cli"))
				{
					// no initial split config
					jEdit.restoreOpenFiles();
				}
			}

			// if session file is empty or -norestore specified,
			// we need an initial buffer
			if(jEdit.getFirstBuffer() == null
					|| (jEdit.getFirstBuffer().isUntitled()
                                        && jEdit.getBufferCount() == 1))
				buffer = jEdit.newFile(null);

			if(splitConfig != null)
				jEdit.newView(null,splitConfig);
			else
				jEdit.newView(null,buffer);
		}
		else
		{
			// no background mode, and reusing existing view
			View view = jEdit.getFirstView();

			jEdit.openFiles(view,parent,args);

			// un-iconify using JDK 1.3 API
			view.setState(java.awt.Frame.NORMAL);
			view.requestFocus();
			view.toFront();

			// do not create a new view
			return;
		}
	} //}}}

	// stopServer() method
	void stopServer()
	{
		stop();
		new File(portFile).delete();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private String portFile;
	private ServerSocket socket;
	private int authKey;
	private boolean ok;
	//}}}

	//{{{ handleClient() method
	private void handleClient(Socket client, Reader in)
		throws IOException
	{
		final StringBuffer script = new StringBuffer();
		char[] buf = new char[1024];
		int count;

		while((count = in.read(buf,0,buf.length)) != -1)
		{
			script.append(buf,0,count);
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				String scriptString = script.toString();
				Log.log(Log.DEBUG,this,scriptString);
				BeanShell.eval(null,scriptString,false);
			}
		});
	} //}}}

	//}}}
}
