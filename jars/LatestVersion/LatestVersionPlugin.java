/*
 * LatestVersionPlugin.java - Latest Version Check Plugin
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

import javax.swing.JOptionPane;
import java.io.*;
import java.net.URL;
import org.gjt.sp.jedit.*;

public class LatestVersionPlugin extends EditPlugin
{
	public static void doVersionCheck(View view)
	{
		view.showWaitCursor();

		try
		{
			URL url = new URL(jEdit.getProperty(
				"version-check.url"));
			InputStream in = url.openStream();
			BufferedReader bin = new BufferedReader(
				new InputStreamReader(in));

			String line;
			String develBuild = null;
			String stableBuild = null;
			while((line = bin.readLine()) != null)
			{
				if(line.startsWith(".build"))
					develBuild = line.substring(6).trim();
				else if(line.startsWith(".stablebuild"))
					stableBuild = line.substring(12).trim();
			}

			bin.close();

			if(develBuild != null && stableBuild != null)
			{
				doVersionCheck(view,stableBuild,develBuild);
			}
		}
		catch(IOException e)
		{
			String[] args = { jEdit.getProperty("version-check.url"),
				e.toString() };
			GUIUtilities.error(view,"read-error",args);
		}

		view.hideWaitCursor();
	}

	public static void doVersionCheck(View view, String stableBuild,
		String develBuild)
	{
		String myBuild = jEdit.getBuild();
		String pre = myBuild.substring(6,7);
		String variant;
		String build;

		if(pre.equals("99"))
		{
			variant = "stable";
			build = stableBuild;
		}
		else
		{
			variant = "devel";
			build = develBuild;
		}

		// special case: no current development version
		if(develBuild.compareTo(stableBuild) < 0)
			variant += "-nodevel";

		int retVal = GUIUtilities.confirm(view,"version-check." + variant,
			new String[] { MiscUtilities.buildToVersion(myBuild),
				MiscUtilities.buildToVersion(stableBuild),
				MiscUtilities.buildToVersion(develBuild) },
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if(retVal == JOptionPane.YES_OPTION)
			jEdit.openFile(view,jEdit.getProperty("version-check.url"));
	}
}
