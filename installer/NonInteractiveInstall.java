/*
 * NonInteractiveInstall.java
 *
 * Originally written by Slava Pestov for the jEdit installer project. This work
 * has been placed into the public domain. You may use this work in any way and
 * for any purpose you wish.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
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
