/*
 * Roster.java - A list of things to do, used in various places
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

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;
import org.gjt.sp.jedit.io.VFSManager; // we use VFSManager.error() method
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

class Roster
{
	Roster()
	{
		operations = new Vector();
	}

	void addOperation(Operation op)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			if(operations.elementAt(i).equals(op))
				return;
		}

		operations.addElement(op);
	}

	int getOperationCount()
	{
		return operations.size();
	}

	boolean isEmpty()
	{
		return operations.size() == 0;
	}

	boolean performOperations(PluginManagerProgress progress)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			Operation op = (Operation)operations.elementAt(i);
			if(op.perform(progress))
				progress.done(true);
			else
			{
				progress.done(false);
				return false;
			}

			if(Thread.interrupted())
				return false;
		}

		return true;
	}

	// private members
	private Vector operations;

	static interface Operation
	{
		boolean perform(PluginManagerProgress progress);
		boolean equals(Object o);
	}

	static class Remove implements Operation
	{
		Remove(String plugin)
		{
			this.plugin = plugin;
		}

		public boolean perform(PluginManagerProgress progress)
		{
			progress.removing(MiscUtilities.getFileName(plugin));

			// close JAR file
			EditPlugin.JAR jar = jEdit.getPluginJAR(plugin);
			if(jar != null)
				jar.getClassLoader().closeZipFile();

			// move JAR first
			File jarFile = new File(plugin);
			File srcFile = new File(plugin.substring(0,plugin.length() - 4));

			boolean ok = true;
			ok &= deleteRecursively(jarFile);

			if(srcFile.exists())
				ok &= deleteRecursively(srcFile);

			String[] args = { plugin };
			if(!ok)
				GUIUtilities.error(progress,"plugin-manager.remove-failed",args);
			return ok;
		}

		public boolean equals(Object o)
		{
			if(o instanceof Remove
				&& ((Remove)o).plugin.equals(plugin))
				return true;
			else
				return false;
		}

		// private members
		private String plugin;

		private boolean deleteRecursively(File file)
		{
			Log.log(Log.NOTICE,this,"Deleting " + file + " recursively");

			boolean ok = true;

			if(file.isDirectory())
			{
				String path = file.getPath();
				String[] children = file.list();
				for(int i = 0; i < children.length; i++)
				{
					ok &= deleteRecursively(new File(path,children[i]));
				}
			}

			ok &= file.delete();

			return ok;
		}
	}

	static class Install implements Operation
	{
		Install(String url, String installDirectory)
		{
			// catch those hooligans passing null urls
			if(url == null)
				throw new NullPointerException();

			this.url = url;
			this.installDirectory = installDirectory;
		}

		public boolean perform(PluginManagerProgress progress)
		{
			try
			{
				String fileName = MiscUtilities.getFileName(url);
				progress.downloading(fileName);
				String path = download(progress,fileName,url);
				if(path == null)
				{
					// interrupted download
					return false;
				}

				progress.installing(fileName);
				install(progress,path,installDirectory);

				return true;
			}
			catch(InterruptedIOException iio)
			{
				// do nothing, user clicked 'Stop'
				return false;
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);

				String[] args = { io.getMessage() };
				VFSManager.error(progress,"ioerror",args);

				return false;
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);

				return false;
			}
		}

		public boolean equals(Object o)
		{
			if(o instanceof Install
				&& ((Install)o).url.equals(url))
			{
				/* even if installDirectory is different */
				return true;
			}
			else
				return false;
		}

		// private members
		private String url;
		private String installDirectory;

		private String download(PluginManagerProgress progress,
			String fileName, String url) throws Exception
		{
			URLConnection conn = new URL(url).openConnection();
			progress.setMaximum(Math.max(0,conn.getContentLength()));

			String path = MiscUtilities.constructPath(getDownloadDir(),fileName);

			if(!copy(progress,conn.getInputStream(),
				new FileOutputStream(path),true,true))
				return null;

			return path;
		}

		private boolean install(PluginManagerProgress progress,
			String path, String dir) throws Exception
		{
			progress.setMaximum(1);

			ZipFile zipFile = new ZipFile(path);
			Enumeration enum = zipFile.entries();
			while(enum.hasMoreElements())
			{
				ZipEntry entry = (ZipEntry)enum.nextElement();
				String name = entry.getName().replace('/',File.separatorChar);
				File file = new File(dir,name);
				if(entry.isDirectory())
					file.mkdirs();
				else
				{
					new File(file.getParent()).mkdirs();
					copy(progress,zipFile.getInputStream(entry),
						new FileOutputStream(file),false,false);
				}
			}

			new File(path).delete();

			progress.setValue(1);

			return true;
		}

		private boolean copy(PluginManagerProgress progress,
			InputStream in, OutputStream out, boolean canStop,
			boolean doProgress) throws Exception
		{
			in = new BufferedInputStream(in);
			out = new BufferedOutputStream(out);

			byte[] buf = new byte[4096];
			int copied = 0;
loop:			for(;;)
			{
				int count = in.read(buf,0,buf.length);
				if(count == -1)
					break loop;

				if(doProgress)
				{
					copied += count;
					progress.setValue(copied);
				}

				out.write(buf,0,count);
				if(canStop && Thread.interrupted())
				{
					in.close();
					out.close();
					return false;
				}
			}

			in.close();
			out.close();
			return true;
		}

		static File downloadDir;

		static String getDownloadDir()
		{
			if(downloadDir == null)
			{
				String settings = jEdit.getSettingsDirectory();
				if(settings == null)
					settings = System.getProperty("user.home");
				downloadDir = new File(MiscUtilities.constructPath(
					settings,"PluginManager.download"));
				downloadDir.mkdirs();
			}

			return downloadDir.getPath();
		}
	}
}
