/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Handler.java - Handler interface
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
import org.gjt.sp.jedit.msg.*;
//}}}

public interface Handler
{
	
	// handleQuit() method
	public void handleQuit();
	
	// handleAbout() method
	public void handleAbout();

	// handlePrefs() method
	public void handlePrefs();
	
	// handleOpenFile() method
	public void handleOpenFile(File file);

	// handleOpenFile() method
	public void handleOpenFile(ViewUpdate msg);
	
	// handleFileCodes() method
	public void handleFileCodes(BufferUpdate msg);

}
