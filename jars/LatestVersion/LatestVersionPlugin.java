/*
 * LatestVersionPlugin.java - Latest Version Check Plugin
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

import javax.swing.JOptionPane;
import java.io.*;
import java.net.URL;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class LatestVersionPlugin extends EditPlugin
{
	public void createMenuItems(Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenuItem("version-check"));
	}

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
			String version = null;
			String build = null;
			while((line = bin.readLine()) != null)
			{
				if(line.startsWith(".version"))
					version = line.substring(8).trim();
				else if(line.startsWith(".build"))
					build = line.substring(6).trim();
			}

			bin.close();

			if(version != null && build != null)
			{
				if(jEdit.getBuild().compareTo(build) < 0)
					newVersionAvailable(view,version,url);
				else
				{
					GUIUtilities.message(view,"version-check"
						+ ".up-to-date",new String[0]);
				}
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

	public static void newVersionAvailable(View view, String version, URL url)
	{
		String[] args = { version };

		int result = GUIUtilities.confirm(view,"version-check.new-version",
			args,JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);

		if(result == JOptionPane.YES_OPTION)
			jEdit.openFile(view,url.toString());
	}
}
