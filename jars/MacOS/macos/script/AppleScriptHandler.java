/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * AppleScriptHandler.java
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

package macos.script;

//{{{ Imports
import java.io.*;
import org.gjt.sp.jedit.*;
import macos.*;
//}}}

public class AppleScriptHandler extends Macros.Handler
{
	//{{{ accept() method
	public boolean accept(String path)
	{
		return ffilter.accept(new File(path));
	} //}}}

	//{{{ createMacro() method
	public Macros.Macro createMacro(String macroName, String path)
	{
		if (macroName.toLowerCase().endsWith(".scpt"))
			macroName = macroName.substring(0, macroName.length() - 5);
		else if (macroName.toLowerCase().endsWith(".applescript"))
			macroName = macroName.substring(0, macroName.length() - 12);
		return new Macros.Macro(this,macroName,
			Macros.Macro.macroNameToLabel(macroName),path);
	} //}}}

	//{{{ runMacro() method
	public void runMacro(View view, Macros.Macro macro)
	{
		MacOSActions.runScript(macro.getPath());
	}
	//}}}

	//{{{ runMacro() method
	public void runMacro(View view, Macros.Macro macro, boolean ownNamespace)
	{
		runMacro(view,macro);
	} //}}}

	//{{{ Handler constructor
	public AppleScriptHandler()
	{
		super("applescript");
		ffilter = new ScriptFilter();
	} //}}}

	//{{{ Private members
	private FileFilter ffilter;
	//}}}
}
