/*
 * InstallThread.java
 * Copyright (C) 1999, 2003 Slava Pestov
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

package installer;

import java.io.*;
import java.util.Vector;

/*
 * The thread that performs installation.
 */
public class InstallThread extends Thread
{
	public InstallThread(Install installer, Progress progress,
		String installDir, OperatingSystem.OSTask[] osTasks,
		int size, Vector components)
	{
		super("Install thread");

		this.installer = installer;
		this.progress = progress;
		this.installDir = installDir;
		this.osTasks = osTasks;
		this.size = size;
		this.components = components;
	}

	public void run()
	{
		progress.setMaximum(size * 1024);

		try
		{
			// install user-selected packages
			for(int i = 0; i < components.size(); i++)
			{
				installComponent((String)components.elementAt(i));
			}

			// do operating system specific stuff (creating startup
			// scripts, installing man pages, etc.)
			for(int i = 0; i < osTasks.length; i++)
			{
				osTasks[i].perform(installDir);
			}
		}
		catch(FileNotFoundException fnf)
		{
			progress.error("The installer could not create the "
				+ "destination directory.\n"
				+ "Maybe you do not have write permission?");
			return;
		}
		catch(IOException io)
		{
			progress.error(io.toString());
			return;
		}

		progress.done();
	}

	// private members
	private Install installer;
	private Progress progress;
	private String installDir;
	private OperatingSystem.OSTask[] osTasks;
	private int size;
	private Vector components;

	private void installComponent(String name) throws IOException
	{
		BufferedReader fileList = new BufferedReader(
			new InputStreamReader(getClass()
			.getResourceAsStream(name)));

		String fileName;
		while((fileName = fileList.readLine()) != null)
		{
			String outfile = installDir + File.separatorChar
				+ fileName.replace('/',File.separatorChar);

			InputStream in = new BufferedInputStream(
				getClass().getResourceAsStream("/" + fileName));

			if(in == null)
				throw new FileNotFoundException(fileName);

			installer.copy(in,outfile,progress);
			in.close();
		}

		fileList.close();
	}
}
