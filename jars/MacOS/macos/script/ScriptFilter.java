/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * ScriptFilter.java
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
//}}}

public class ScriptFilter implements FileFilter
{
	//{{{ accept() method
	public boolean accept(File pathname)
	{
		if (pathname.getName().endsWith(".scpt"))
			return true;
		if (pathname.getName().endsWith(".applescript"))
			return true;
		
		// Replace this is Cocoa API calls later
		/*try
		{
			MRJOSType type = MRJFileUtils.getFileType(pathname);
			MRJOSType creator = MRJFileUtils.getFileCreator(pathname);
			if (type.equals(new MRJOSType("osas")))
				return true;
			else if (type.equals(new MRJOSType("APPL")) && creator.equals(new MRJOSType("dplt")))
				return true;
		}
		catch (Exception e)
		{
			return false;
		}*/
		
		return false;
	} //}}}
}
