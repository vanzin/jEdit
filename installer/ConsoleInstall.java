/*
 * ConsoleInstall.java
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
 * Performs text-only installation.
 */
public class ConsoleInstall
{
	public ConsoleInstall()
	{
		installer = new Install();

		String appName = installer.getProperty("app.name");
		String appVersion = installer.getProperty("app.version");

		BufferedReader in = new BufferedReader(new InputStreamReader(
			System.in));

		System.out.println("*** " + appName + " " + appVersion + " installer");

		OperatingSystem os = OperatingSystem.getOperatingSystem();

		String installDir = os.getInstallDirectory(appName,appVersion);

		System.out.print("Installation directory [" + installDir + "]: ");
		System.out.flush();

		String _installDir = readLine(in);
		if(_installDir.length() != 0)
			installDir = _installDir;

		String binDir = os.getShortcutDirectory(appName,appVersion);

		if(binDir != null)
		{
			System.out.print("Shortcut directory [" + binDir + "]: ");
			System.out.flush();

			String _binDir = readLine(in);
			if(_binDir.length() != 0)
				binDir = _binDir;
		}

		int compCount = installer.getIntProperty("comp.count");
		Vector components = new Vector(compCount);

		System.out.println("*** Program components to install");
		for(int i = 0; i < compCount; i++)
		{
			String fileset = installer.getProperty("comp." + i + ".fileset");

			String osDep = installer.getProperty("comp." + i + ".os");
			if(osDep != null)
			{
				if(!os.getClass().getName().endsWith(osDep))
				{
					continue;
				}
			}

			System.out.print("Install "
				+ installer.getProperty("comp." + i + ".name")
				+ " ("
				+ installer.getProperty("comp." + i + ".disk-size")
				+ "Kb) [Y/n]? ");

			String line = readLine(in);
			if(line.length() == 0 || line.charAt(0) == 'y'
				|| line.charAt(0) == 'Y')
				components.addElement(fileset);
		}

		System.out.println("*** Starting installation...");
		ConsoleProgress progress = new ConsoleProgress();
		InstallThread thread = new InstallThread(
			installer,progress,installDir,binDir,
			0 /* XXX */,components);
		thread.start();
	}

	// private members
	private Install installer;

	private String readLine(BufferedReader in)
	{
		try
		{
			String line = in.readLine();
			if(line == null)
			{
				System.err.println("\nEOF in input!");
				System.exit(1);
				// can't happen
				throw new InternalError();
			}
			return line;
		}
		catch(IOException io)
		{
			System.err.println("\nI/O error: " + io);
			System.exit(1);
			// can't happen
			throw new InternalError();
		}
	}
}
