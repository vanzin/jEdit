/*
 * InstallThread.java
 * Copyright (C) 1999, 2000 Slava Pestov
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
		String installDir, String binDir, int size, Vector components)
	{
		super("Install thread");

		this.installer = installer;
		this.progress = progress;
		this.installDir = installDir;
		this.binDir = binDir;
		this.size = size;
		this.components = components;

		buf = new byte[32768];
	}

	public void setProgress(Progress progress)
	{
		this.progress = progress;
	}

	public void run()
	{
		progress.setMaximum(size * 1024);

		try
		{
			for(int i = 0; i < components.size(); i++)
			{
				installComponent((String)components.elementAt(i));
			}

			// create it in case it doesn't already exist
			if(binDir != null)
				OperatingSystem.getOperatingSystem().mkdirs(binDir);

			OperatingSystem.getOperatingSystem().createScript(
				installer,installDir,binDir,
				installer.getProperty("app.name"));
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
	private String binDir;
	private int size;
	private Vector components;
	private byte[] buf;

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

			copy(in,outfile);
			in.close();
		}

		fileList.close();
	}

	private void copy(InputStream in, String outfile) throws IOException
	{
		File outFile = new File(outfile);

		OperatingSystem.getOperatingSystem().mkdirs(outFile.getParent());

		BufferedOutputStream out = new BufferedOutputStream(
			new FileOutputStream(outFile));

		int count;

		for(;;)
		{
			count = in.read(buf,0,buf.length);
			if(count == -1)
				break;

			out.write(buf,0,count);
			progress.advance(count);
		}

		in.close();
		out.close();
	}
}
