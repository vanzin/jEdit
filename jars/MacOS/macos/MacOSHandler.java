/* 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSHandler.java - Various handlers for Mac OS Plugin
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
import com.apple.mrj.*;
import java.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.options.GlobalOptions;
import org.gjt.sp.util.Log;
//}}}

public class MacOSHandler implements MRJQuitHandler, MRJAboutHandler,
	MRJOpenDocumentHandler, MRJPrefsHandler, Handler
{
	//{{{ Variables
	private Buffer		lastOpenFile;
	private ExitThread	et = new ExitThread();
	
	private final MRJOSType defaultType = new MRJOSType(jEdit.getProperty("MacOSPlugin.default.type"));
	private final MRJOSType defaultCreator = new MRJOSType(jEdit.getProperty("MacOSPlugin.default.creator"));
	//}}}
	
	//{{{ Constructor
	public MacOSHandler()
	{
		if (jEdit.getBooleanProperty("MacOSPlugin.useScreenMenuBar",
			jEdit.getBooleanProperty("MacOSPlugin.default.useScreenMenuBar"))
		)
			System.setProperty("com.apple.macos.useScreenMenuBar","true");
		else
			System.setProperty("com.apple.macos.useScreenMenuBar","false");
		
		if (jEdit.getBooleanProperty("MacOSPlugin.liveResize",
			jEdit.getBooleanProperty("MacOSPlugin.default.liveResize"))
		)
			System.setProperty("com.apple.mrj.application.live-resize","true");
		else
			System.setProperty("com.apple.mrj.application.live-resize","false");
	} //}}}
	
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
			Log.log(Log.DEBUG,this,"ExitThread still alive.");
		
		throw new IllegalStateException("Exiting: aborting default exit");
	} //}}}
	
	//{{{ handleAbout() method
	public void handleAbout()
	{
		new AboutDialog(jEdit.getActiveView());
	} //}}}

	//{{{ handlePrefs() method
	public void handlePrefs()
	{
		new GlobalOptions(jEdit.getActiveView());
	} //}}}
	
	//{{{ handleOpenFile() method
	public void handleOpenFile(File file)
	{
		View view = jEdit.getActiveView();
		Buffer buffer;
		
		if (view == null && MacOSPlugin.started)
		{
			if (jEdit.getBooleanProperty("restore.cli"))
				view = jEdit.newView(null,jEdit.restoreOpenFiles());
			else
				view = jEdit.newView(null,jEdit.newFile(null));
		}
		
		if ((buffer = jEdit.openFile(view,file.getPath())) != null)
		{
			lastOpenFile = buffer;
		}
		else
			Log.log(Log.ERROR,this,"Error opening file.");
	} //}}}

	//{{{ handleOpenFile() method
	public void handleOpenFile(ViewUpdate msg)
	{
		if(msg.getWhat() == ViewUpdate.CREATED)
		{
			if(lastOpenFile != null)
				msg.getView().setBuffer(lastOpenFile);
			MacOSPlugin.started = true;
		}
	} //}}}
	
	//{{{ handleFileCodes() method
	public void handleFileCodes(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		File bufFile = new File(buffer.getPath());
		
		// Set type/creator on save
		if (!buffer.isDirty() && msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			try
			{
				MRJFileUtils.setFileTypeAndCreator( bufFile,
					(MRJOSType)buffer.getProperty("MacOSPlugin.type"),
					(MRJOSType)buffer.getProperty("MacOSPlugin.creator") );
			}
			catch (Exception e)
			{
				Log.log(Log.ERROR,this,"Error setting type/creator for "+bufFile.getPath());
			}
		}
		// Add type/creator to local buffer property list on open
		else if (msg.getWhat() == BufferUpdate.CREATED )
		{
			// This ensures that a type/creator will be assigned if an error occurs
			buffer.setProperty("MacOSPlugin.type",defaultType);
			buffer.setProperty("MacOSPlugin.creator",defaultCreator);
			
			if (jEdit.getBooleanProperty("MacOSPlugin.preserveCodes",
				jEdit.getBooleanProperty("MacOSPlugin.default.preserveCodes")))
			{
				try
				{
					MRJOSType type = MRJFileUtils.getFileType(bufFile);
					MRJOSType creator = MRJFileUtils.getFileCreator(bufFile);
					
					if (!type.equals(new MRJOSType("")))
						buffer.setProperty("MacOSPlugin.type",type);
					if (!creator.equals(new MRJOSType("")))
						buffer.setProperty("MacOSPlugin.creator",creator);
				}
				catch (Exception e) {} // This will happen when a new file is created
			}
			Log.log(Log.DEBUG,this,"Assigned MRJOSTypes " + buffer.getProperty("MacOSPlugin.type")
			+ "/" + buffer.getProperty("MacOSPlugin.creator") + " to " + bufFile.getPath());
		}
	} //}}}
	
	//{{{ ExitThread class
	class ExitThread extends Thread
	{
		public void run()
		{
			jEdit.exit(jEdit.getActiveView(),true);
			et = new ExitThread();
		}
	} //}}}
}
