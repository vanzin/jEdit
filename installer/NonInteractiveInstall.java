/*
 * NonInteractiveInstall.java
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

import java.util.Vector;

/*
 * Performs non-interactive installation.
 */
public class NonInteractiveInstall
{
	public NonInteractiveInstall(String[] args)
	{
		String installDir = args[1];

		installer = new Install();

		OperatingSystem os = OperatingSystem.getOperatingSystem();
		OperatingSystem.OSTask[] osTasks = os.getOSTasks(installer);

		for(int i = 2; i < args.length; i++)
		{
			String arg = args[i];
			int index = arg.indexOf('=');
			if(index == -1)
			{
				System.err.println("Invalid parameter: " + arg);
				continue;
			}

			String taskName = arg.substring(0,index);
			String taskDir = arg.substring(index + 1);
			for(int j = 0; j < osTasks.length; j++)
			{
				OperatingSystem.OSTask osTask = osTasks[j];
				if(osTask.getName().equals(taskName))
				{
					System.out.println(arg);
					if(taskDir.equals("off"))
						osTask.setEnabled(false);
					else
					{
						osTask.setEnabled(true);
						osTask.setDirectory(taskDir);
					}
					break;
				}
			}
		}

		int compCount = installer.getIntegerProperty("comp.count");
		Vector components = new Vector(compCount);

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

			components.addElement(fileset);
		}

		//

		ConsoleProgress progress = new ConsoleProgress();
		InstallThread thread = new InstallThread(
			installer,progress,installDir,osTasks,
			0 /* XXX */,components);
		thread.start();
	}

	// private members
	private Install installer;
}
