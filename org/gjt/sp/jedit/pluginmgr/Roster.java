/*
 * Roster.java - A list of things to do, used in various places
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.PluginUpdate;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.ProgressObserver;

import static org.gjt.sp.jedit.io.FileVFS.recursiveDelete;
//}}}

/**
 * @author $Id$
 */
class Roster
{
	private static final Pattern HOST_REGEX = Pattern.compile("(?<=/)\\w++(?=\\.dl\\.sourceforge\\.net)");

	//{{{ Roster constructor
	Roster()
	{
		operations = new ArrayList<>();
		toLoad = new ArrayList<>();
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
	void addInstall(String installed, String url, String installDirectory, int size)
	{
		addOperation(new Install(installed,url,installDirectory,size));
	} //}}}

	//{{{ addLoad() method
	void addLoad(String path)
	{
		toLoad.add(path);
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
		for (Operation op : operations)
		{
			op.runInWorkThread(progress);
			progress.done();

			if (Thread.interrupted())
				return;
		}
	} //}}}

	//{{{ performOperationsInAWTThread() method
	void performOperationsInAWTThread(Component comp)
	{
		for (Operation op : operations)
			op.runInAWTThread(comp);

		// add the JARs before checking deps since dep check might
		// require all JARs to be present
		for (String pluginName : toLoad)
		{
			if (jEdit.getPluginJAR(pluginName) != null)
				Log.log(Log.WARNING, this, "Already loaded: " + pluginName);
			else
				jEdit.addPluginJAR(pluginName);
		}

		for (String pluginName : toLoad)
		{
			PluginJAR plugin = jEdit.getPluginJAR(pluginName);
			if (plugin != null)
				plugin.checkDependencies();
		}

		// now activate the plugins
		for (String pluginName : toLoad)
		{
			PluginJAR plugin = jEdit.getPluginJAR(pluginName);
			if (plugin != null)
				plugin.activatePluginIfNecessary();
		}
	} //}}}

	//{{{ Private members
	private static File downloadDir;

	private final List<Operation> operations;
	private final List<String> toLoad;

	//{{{ addOperation() method
	private void addOperation(Operation op)
	{
		for (Operation operation : operations)
		{
			if (operation.equals(op))
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
		@Override
		public void runInAWTThread(Component comp)
		{
			// close JAR file and all JARs that depend on this
			PluginJAR jar = jEdit.getPluginJAR(this.jar);
			if(jar != null)
			{
				unloadPluginJAR(jar);
			}

			toLoad.remove(this.jar);

			File jarFile = new File(this.jar);
			File srcFile = new File(this.jar.substring(0, this.jar.length() - 4));

			if(jarFile.exists())
			{
				Log.log(Log.NOTICE,this,"Deleting " + jarFile);

				boolean ok = jarFile.delete();
				if (ok)
				{
					EditBus.send(new PluginUpdate(jarFile, PluginUpdate.REMOVED, false));
				}

				if(srcFile.exists())
				{
					ok &= recursiveDelete(srcFile);
				}

				if(!ok)
				{
					String[] args = {this.jar};
					GUIUtilities.error(comp,"plugin-manager.remove-failed",args);
				}
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
		Install(String installed, @Nonnull String url, String installDirectory, int size)
		{
			// catch those hooligans passing null urls
			Objects.requireNonNull(url);

			this.installed = installed;
			this.url = url;
			this.installDirectory = installDirectory;
			this.size = size;
		} //}}}

		//{{{ getMaximum() method
		@Override
		public int getMaximum()
		{
			return size;
		} //}}}

		//{{{ runInWorkThread() method
		@Override
		public void runInWorkThread(PluginManagerProgress progress)
		{
			path = download(progress,url);
		} //}}}

		//{{{ runInAWTThread() method
		@Override
		public void runInAWTThread(Component comp)
		{
			// check if download failed
			if(path == null)
				return;

			/* if download OK, remove existing version
			 * and bundled jars and files */
			if(installed != null)
			{
				PluginJAR pluginJar = jEdit.getPluginJAR(installed);
				Collection<String> libs = new LinkedList<>();
				libs.add(installed);
				if(pluginJar == null)
				{
					Log.log(Log.ERROR, Roster.Remove.class,
						 "unable to get PluginJAR for "+installed);
				}
				else
				{
					 libs.addAll(pluginJar.getJars());
					 libs.addAll(pluginJar.getFiles());
				}

				for(String lib: libs)
				{
					new Remove(lib).runInAWTThread(comp);
				}
			}

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
							// According to java 6/7 doc "in" should never be
							// null, but it happens with filenames
							// containing non-ascii characaters, #3531320
							if (in == null)
								throw new ZipException("Entry "
									+ entry.getName() + " from archive "
									+ zipFile.getName()
									+ " could not be processed.");
							out = new FileOutputStream(file);
							IOUtilities.copyStream(4096,
								null,
								in,
								out,false);
						}
						finally
						{
							IOUtilities.closeQuietly((Closeable)in);
							IOUtilities.closeQuietly((Closeable)out);
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

				if(jEdit.getBooleanProperty("plugin-manager.deleteDownloads"))
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
		private final String installed;
		private final String url;
		private final String installDirectory;
		private String path;

		//{{{ download() method
		private String download(ProgressObserver progress, String url)
		{
			try
			{
				String host = jEdit.getProperty("plugin-manager.mirror.id");
				if (host == null || host.equals(MirrorList.Mirror.NONE))
					host = "default";

				// follow HTTP redirects
				boolean finalUrlFound = false;
				String finalUrl = url;
				URLConnection conn = null;
				while (!finalUrlFound)
				{
					Log.log(Log.DEBUG, this, String.format("Trying URL '%s'", finalUrl));
					conn = new URL(finalUrl).openConnection();
					HttpURLConnection httpConn = (HttpURLConnection) conn;
					httpConn.setInstanceFollowRedirects(false);
					httpConn.connect();
					int responseCode = httpConn.getResponseCode();
					String locationHeader = httpConn.getHeaderField("Location");
					if ((responseCode >= 300) && (responseCode < 400) && (locationHeader != null))
						finalUrl = locationHeader.replaceFirst("^https:", "http:");
					else
						finalUrlFound = true;
				}
				Log.log(Log.DEBUG, this, String.format("Final URL '%s' found", finalUrl));

				String fileName = MiscUtilities.getFileName(finalUrl);
				String path = MiscUtilities.constructPath(getDownloadDir(),fileName);
				Matcher hostMatcher = HOST_REGEX.matcher(finalUrl);
				if (hostMatcher.find())
					host = hostMatcher.group();
				String progressMessage = jEdit.getProperty("plugin-manager.progress", new String[]{fileName, host});
				progress.setStatus(progressMessage);
				try (InputStream in = conn.getInputStream();
				     FileOutputStream out = new FileOutputStream(path))
				{
					if(!IOUtilities.copyStream(progress,progressMessage,in,out,true))
						return null;
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

				SwingUtilities.invokeLater(() -> GUIUtilities.error(null,"plugin-error-download",new Object[]{""}));

				return null;
			}
			catch(final IOException io)
			{
				Log.log(Log.ERROR,this,io);

				SwingUtilities.invokeLater(() ->
				{
					String[] args = { io.getMessage() };
					GUIUtilities.error(null,"plugin-error-download",args);
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
