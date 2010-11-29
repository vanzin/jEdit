/*
 * Roster.java - A list of things to do, used in various places
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
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

//{{{ Imports
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import static org.gjt.sp.jedit.io.FileVFS.recursiveDelete;
//}}}

/**
 * @author $Id$
 */
class Roster
{
	//{{{ Roster constructor
	Roster()
	{
		operations = new ArrayList<Operation>();
		toLoad = new ArrayList<String>();
	} //}}}

	//{{{ addRemove() method
	/**
	 * Add a remove operation for the given jar
	 * @param jar the jar name
	 */
	void addRemove(String jar)
	{
		addOperation(new Remove(jar));
	} //}}}

	//{{{ addInstall() method
	void addInstall(String installed, String url, String installDirectory,
		int size)
	{
		addOperation(new Install(installed,url,installDirectory,size));
	} //}}}

	//{{{ getOperation() method
	public Operation getOperation(int i)
	{
		return operations.get(i);
	} //}}}

	//{{{ getOperationCount() method
	int getOperationCount()
	{
		return operations.size();
	} //}}}

	//{{{ isEmpty() method
	boolean isEmpty()
	{
		return operations.isEmpty();
	} //}}}

	//{{{ performOperationsInWorkThread() method
	void performOperationsInWorkThread(PluginManagerProgress progress)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			Operation op = operations.get(i);
			op.runInWorkThread(progress);
			progress.done();

			if(Thread.interrupted())
				return;
		}
	} //}}}

	//{{{ performOperationsInAWTThread() method
	void performOperationsInAWTThread(Component comp)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			Operation op = operations.get(i);
			op.runInAWTThread(comp);
		}

		// add the JARs before checking deps since dep check might
		// require all JARs to be present
		for(int i = 0; i < toLoad.size(); i++)
		{
			String pluginName = toLoad.get(i);
			if(jEdit.getPluginJAR(pluginName) != null)
			{
				Log.log(Log.WARNING,this,"Already loaded: "
					+ pluginName);
			}
			else
				jEdit.addPluginJAR(pluginName);
		}

		for(int i = 0; i < toLoad.size(); i++)
		{
			String pluginName = toLoad.get(i);
			PluginJAR plugin = jEdit.getPluginJAR(pluginName);
			if(plugin != null)
				plugin.checkDependencies();
		}

		// now activate the plugins
		for(int i = 0; i < toLoad.size(); i++)
		{
			String pluginName = toLoad.get(i);
			PluginJAR plugin = jEdit.getPluginJAR(pluginName);
			if(plugin != null)
				plugin.activatePluginIfNecessary();
		}
	} //}}}

	//{{{ Private members
	private static File downloadDir;

	private List<Operation> operations;
	private List<String> toLoad;

	//{{{ addOperation() method
	private void addOperation(Operation op)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			if(operations.get(i).equals(op))
				return;
		}

		operations.add(op);
	} //}}}

	//{{{ getDownloadDir() method
	private static String getDownloadDir()
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
	} //}}}

	//}}}

	//{{{ Operation interface
	abstract static class Operation
	{
		public void runInWorkThread(PluginManagerProgress progress)
		{
		}

		public void runInAWTThread(Component comp)
		{
		}

		public int getMaximum()
		{
			return 0;
		}
	} //}}}

	//{{{ Remove class
	class Remove extends Operation
	{
		//{{{ Remove constructor
		Remove(String jar)
		{
			this.jar = jar;
		} //}}}

		//{{{ runInAWTThread() method
		public void runInAWTThread(Component comp)
		{
			// close JAR file and all JARs that depend on this
			PluginJAR jar = jEdit.getPluginJAR(this.jar);
			if(jar != null)
			{
				unloadPluginJAR(jar);
			}

			toLoad.remove(this.jar);

			// remove cache file

			// move JAR first
			File jarFile = new File(this.jar);
			File srcFile = new File(this.jar.substring(0, this.jar.length() - 4));

			Log.log(Log.NOTICE,this,"Deleting " + jarFile);

			boolean ok = jarFile.delete();

			if(srcFile.exists())
			{
				ok &= recursiveDelete(srcFile);
			}

			if(!ok)
			{
				String[] args = {this.jar};
				GUIUtilities.error(comp,"plugin-manager.remove-failed",args);
			}
		} //}}}

		//{{{ unloadPluginJAR() method
		/**
		 * This should go into a public method somewhere.
		 * @param jar the jar of the plugin
		 */
		private void unloadPluginJAR(PluginJAR jar)
		{
			String[] dependents = jar.getDependentPlugins();
			for (String path: dependents) 
			{
				PluginJAR _jar = jEdit.getPluginJAR(path);
				if(_jar != null)
				{
					toLoad.add(path);
					unloadPluginJAR(_jar);
					// clear cache file
					String cachePath = jar.getCachePath();
					if(cachePath != null)
						new File(cachePath).delete();

				}
			}
			jEdit.removePluginJAR(jar,false);
			
		} //}}}

		//{{{ equals() method
		public boolean equals(Object o)
		{
			return o instanceof Remove
			       && ((Remove) o).jar.equals(jar);
		} //}}}

		//{{{ Private members
		private final String jar;
		//}}}
	} //}}}

	//{{{ Install class
	class Install extends Operation
	{
		int size;

		//{{{ Install constructor
		Install(String installed, String url, String installDirectory,
			int size)
		{
			// catch those hooligans passing null urls
			if(url == null)
				throw new NullPointerException();

			this.installed = installed;
			this.url = url;
			this.installDirectory = installDirectory;
			this.size = size;
		} //}}}

		//{{{ getMaximum() method
		public int getMaximum()
		{
			return size;
		} //}}}

		//{{{ runInWorkThread() method
		public void runInWorkThread(PluginManagerProgress progress)
		{
			String fileName = MiscUtilities.getFileName(url);

			path = download(progress,fileName,url);
		} //}}}

		//{{{ runInAWTThread() method
		public void runInAWTThread(Component comp)
		{
			// check if download failed
			if(path == null)
				return;

			// if download OK, remove existing version
			if(installed != null)
				new Remove(installed).runInAWTThread(comp);

			ZipFile zipFile = null;

			try
			{
				zipFile = new ZipFile(path);

				Enumeration<? extends ZipEntry> e = zipFile.entries();
				while(e.hasMoreElements())
				{
					ZipEntry entry = e.nextElement();
					String name = entry.getName().replace('/',File.separatorChar);
					File file = new File(installDirectory,name);
					if(entry.isDirectory())
						file.mkdirs();
					else
					{
						new File(file.getParent()).mkdirs();
						InputStream in = null;
						FileOutputStream out = null;
						try
						{
							in = zipFile.getInputStream(entry);
							out = new FileOutputStream(file);
							IOUtilities.copyStream(4096,
								null,
								in,
								out,false);
						}
						finally
						{
							IOUtilities.closeQuietly(in);
							IOUtilities.closeQuietly(out);
						}
						if(file.getName().toLowerCase().endsWith(".jar"))
							toLoad.add(file.getPath());
					}
				}
			}
			catch(InterruptedIOException iio)
			{
			}
			catch(ZipException e)
			{
				Log.log(Log.ERROR,this,e);
				GUIUtilities.error(null,"plugin-error-download",new Object[]{""});
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);

				String[] args = { io.getMessage() };
				GUIUtilities.error(null,"ioerror",args);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}
			finally
			{
				try
				{
					if(zipFile != null)
						zipFile.close();
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,this,io);
				}

				if(jEdit.getBooleanProperty(
					"plugin-manager.deleteDownloads"))
				{
					new File(path).delete();
				}
			}
		} //}}}

		//{{{ equals() method
		public boolean equals(Object o)
		{
			return o instanceof Install
			       && ((Install) o).url.equals(url);
		} //}}}

		//{{{ Private members
		private String installed;
		private final String url;
		private String installDirectory;
		private String path;

		//{{{ download() method
		private String download(PluginManagerProgress progress,
			String fileName, String url)
		{
			try
			{
				String host = jEdit.getProperty("plugin-manager.mirror.id");
				if (host == null || host.equals(MirrorList.Mirror.NONE))
					host = "default";
				
				String path = MiscUtilities.constructPath(getDownloadDir(),fileName);
				URLConnection conn = new URL(url).openConnection();
				progress.setStatus(jEdit.getProperty("plugin-manager.progress",new String[] {fileName, host}));
				InputStream in = null;
				FileOutputStream out = null;
				try
				{
					in = conn.getInputStream();
					out = new FileOutputStream(path);
					if(!IOUtilities.copyStream(progress,in,out,true))
						return null;
				}
				finally
				{
					IOUtilities.closeQuietly(in);
					IOUtilities.closeQuietly(out);
				}
				
				return path;
			}
			catch(InterruptedIOException iio)
			{
				// do nothing, user clicked 'Stop'
				return null;
			}
			catch(FileNotFoundException e)
			{
				Log.log(Log.ERROR,this,e);

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						GUIUtilities.error(null,"plugin-error-download",new Object[]{""});
					}
				});

				return null;
			}
			catch(final IOException io)
			{
				Log.log(Log.ERROR,this,io);

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						String[] args = { io.getMessage() };
						GUIUtilities.error(null,"plugin-error-download",args);
					}
				});

				return null;
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);

				return null;
			}
		} //}}}

		//}}}
	} //}}}
}
