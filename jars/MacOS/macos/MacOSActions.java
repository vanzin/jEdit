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
import com.apple.cocoa.foundation.*;
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
		new ScriptRunner(path).start();
		//SwingUtilities.invokeLater(new ScriptRunner(path));
	} //}}}
	
	//{{{ ScriptRunner class
	static class ScriptRunner extends Thread
	{
		private String path;
		
		public ScriptRunner(String path)
		{
			this.path = path;
		}
		
		public void run()
		{
			File file = new File(path);
			
			if (file.exists())
			{
				try {
					BufferedReader reader = new BufferedReader(new FileReader(file));
					StringBuffer code = new StringBuffer();
					String line;
					
					while ((line = reader.readLine()) != null)
						code.append(line+"\n");
					
					NSAppleScript script = new NSAppleScript(code.toString());
					NSMutableDictionary compileErrInfo = new NSMutableDictionary();
					NSMutableDictionary execErrInfo = new NSMutableDictionary();
					if (script.compile(compileErrInfo))
					{
						if (script.execute(execErrInfo) == null)
						{
							JOptionPane.showMessageDialog(null,
								execErrInfo.objectForKey("NSAppleScriptErrorBriefMessage"),
								jEdit.getProperty("MacOSPlugin.dialog.script.title"),
								JOptionPane.ERROR_MESSAGE);
						}
					}
					else
					{
						JOptionPane.showMessageDialog(null,
							compileErrInfo.objectForKey("NSAppleScriptErrorBriefMessage"),
							jEdit.getProperty("MacOSPlugin.dialog.script.title"),
							JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception ex) {}
			}
		}
	} //}}}
}
