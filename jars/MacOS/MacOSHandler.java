/* 
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSHandler.java - Various actions for Mac OS Plugin
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

//{{{ Imports
import com.apple.mrj.*;
import java.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

public class MacOSHandler implements MRJQuitHandler,
	MRJAboutHandler, MRJOpenDocumentHandler, MRJPrefsHandler
{
	
//{{{ Variables
    private String		lastOpenFile;
	private ExitThread	et = new ExitThread();
	
	private final MRJOSType defaultType = new MRJOSType(jEdit.getProperty("MacOSPlugin.default.type"));
	private final MRJOSType defaultCreator = new MRJOSType(jEdit.getProperty("MacOSPlugin.default.creator"));
//}}}
	
	//{{{ Constructor
	public MacOSHandler()
	{
		if (jEdit.getProperty("MacOSPlugin.useScreenMenuBar") == null)
			jEdit.setProperty("MacOSPlugin.useScreenMenuBar",jEdit.getProperty("MacOSPlugin.default.useScreenMenuBar"));
		System.setProperty("com.apple.macos.useScreenMenuBar",jEdit.getProperty("MacOSPlugin.useScreenMenuBar"));
		
		if (jEdit.getProperty("MacOSPlugin.preserveCodes") == null)
			jEdit.setProperty("MacOSPlugin.preserveCodes",jEdit.getProperty("MacOSPlugin.default.preserveCodes"));
		jEdit.getProperty("MacOSPlugin.preserveCodes",jEdit.getProperty("MacOSPlugin.preserveCodes"));
	}//}}}
	
	//{{{ handleQuit() method
	public void handleQuit()
    {
		// Need this to get around the double call bug
		// in MRJ.
		if (!et.isAlive())
			// Spawn a new thread. This is a work around because of a
			// bug in Mac OS X 10.1's MRJToolkit
			et.start();
		else
			Log.log(Log.DEBUG,jEdit.getPlugin("MacOSPlugin"),"ExitThread still alive.");
	}//}}}
	
	//{{{ handleAbout() method
	public void handleAbout()
    {
		new AboutDialog(jEdit.getLastView());
	}//}}}

	//{{{ handlePrefs() method
	public void handlePrefs()
	{
		new OptionsDialog(jEdit.getLastView());
	}//}}}
	
	//{{{ handleOpenFile() method
	public void handleOpenFile(File file)
    {
		if (jEdit.openFile(jEdit.getLastView(),file.getPath()) != null)
        {
            lastOpenFile = file.getPath();
        } else {
            Log.log(Log.ERROR,jEdit.getPlugin("MacOSPlugin"),"Error opening file.");
        }
	}//}}}

	//{{{ handleOpenFile() method
	public void handleOpenFile(ViewUpdate msg)
    {
		if(msg.getWhat() == ViewUpdate.CREATED)
		{
			if(lastOpenFile != null)
			{
				jEdit.getLastView().setBuffer(jEdit.getBuffer(lastOpenFile));
			}
			((MacOSPlugin)jEdit.getPlugin("MacOSPlugin")).started(true);
		}
	}//}}}
	
	//{{{ handleFileCodes() method
	public void handleFileCodes(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		
		// Set type/creator on save
		if (!buffer.isDirty() && msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			try
			{
				MRJFileUtils.setFileTypeAndCreator( buffer.getFile(),
					(MRJOSType)buffer.getProperty("MacOSPlugin.type"),
					(MRJOSType)buffer.getProperty("MacOSPlugin.creator") );
			}
			catch (Exception e)
			{
				Log.log(Log.ERROR,jEdit.getPlugin("MacOSPlugin"),"Error setting type/creator for "+
					buffer.getFile().getPath());
			}
		}
		// Add type/creator to local buffer property list on open
		else if (msg.getWhat() == BufferUpdate.CREATED )
		{
			// This ensures that a type/creator will be assigned if an error occurs
			buffer.setProperty("MacOSPlugin.type",defaultType);
			buffer.setProperty("MacOSPlugin.creator",defaultCreator);
			
			if (jEdit.getProperty("MacOSPlugin.preserveCodes").equals("true"))
			{
				try
				{
					MRJOSType	type	= MRJFileUtils.getFileType(buffer.getFile());
					MRJOSType	creator	= MRJFileUtils.getFileCreator(buffer.getFile());
					
					if (!type.equals(new MRJOSType("")))
						buffer.setProperty("MacOSPlugin.type",type);
					if (!creator.equals(new MRJOSType("")))
						buffer.setProperty("MacOSPlugin.creator",creator);
				}
				catch (Exception e) {} // This will happen when a new file is created
			}
			Log.log(Log.DEBUG,jEdit.getPlugin("MacOSPlugin"),"Assigned MRJOSTypes " + buffer.getProperty("MacOSPlugin.type")
			+ "/" + buffer.getProperty("MacOSPlugin.creator") + " to " + buffer.getPath());
		}
	}//}}}
	
//{{{ ExitThread class
	class ExitThread extends Thread
	{
		public void run()
		{
			jEdit.exit(jEdit.getLastView(),false);
			et = new ExitThread();
		}
	}
//}}}

}
