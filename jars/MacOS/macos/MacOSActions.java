/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSActions.java
 * Copyright (C) 2002 Kris Kopicki
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

package macos;

//{{{ Imports
import java.io.*;
import javax.swing.*;
import com.apple.cocoa.application.*;
import org.gjt.sp.jedit.*;
//}}}

public class MacOSActions
{
	//{{{ showInFinder() method
	public static void showInFinder(String path)
	{
		if (new File(path).exists())
		{
			//Remember to make this an option later
			//NSApplication.sharedApplication().hide(jEdit.getPlugin("MacOSPlugin"));
			NSWorkspace.sharedWorkspace().selectFile(path,path);
		}
	} //}}}
	
	//{{{ runScript() method
	public static void runScript(String path)
	{
		if (new File(path).exists())
		{
			try {
				String[] args = {"osascript",path};
				Process proc = Runtime.getRuntime().exec(args);
				BufferedReader r = new BufferedReader(
					new InputStreamReader(proc.getErrorStream()));
				proc.waitFor();
				
				String mesg = new String();
				String line;
				while ((line = r.readLine()) != null)
				{
					if (!line.startsWith("##"))
						mesg += line;
				}
				r.close();
				
				if (proc.exitValue() != 0)
					JOptionPane.showMessageDialog(null,mesg,
						"Script Error",JOptionPane.ERROR_MESSAGE);
			} catch (Exception ex) {}
		}
	} //}}}
}
