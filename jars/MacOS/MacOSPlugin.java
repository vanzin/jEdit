/*
 * MacOSPlugin.java - Main class Mac OS Plugin
 * Copyright (C) 2001 Kris Kopicki
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any prior version.
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

import com.apple.cocoa.application.*;
import com.apple.mrj.*;
import java.io.*;
import java.lang.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;

public class MacOSPlugin extends EBPlugin implements MRJQuitHandler
, MRJAboutHandler, MRJOpenDocumentHandler
{
    private boolean onStartup = true;
    private String lastFilePath;
	private ExitThread exitThread = new ExitThread();
	
	private final MRJOSType defaultType = new MRJOSType(jEdit.getProperty("MacOS.default.type"));
	private final MRJOSType defaultCreator = new MRJOSType(jEdit.getProperty("MacOS.default.creator"));
    
/* ------------------------------------------------------------ */
	public void start()
    {		
		if(System.getProperty("os.name").indexOf("Mac OS") != -1)
        {
			// Register handlers
			MRJApplicationUtils.registerQuitHandler(this);
    		MRJApplicationUtils.registerAboutHandler(this);
			MRJApplicationUtils.registerOpenDocumentHandler(this);
    	} else {
            Log.log(Log.ERROR,this,"This plugin requires Mac OS.");
        }
	}
	
/* ------------------------------------------------------------ */
	public void handleQuit()
    {
		// Need this to get around the double call bug
		// in MRJ.
		if (!exitThread.isAlive())
			// Spawn a new thread. This is a work around because of a
			// bug in Mac OS X 10.1's MRJToolkit
			exitThread.start();
		else
			Log.log(Log.DEBUG,this,"exitThread still alive.");
	}
	
/* ------------------------------------------------------------ */
	public void handleAbout()
    {
		new AboutDialog(jEdit.getLastView());
	}

/* ------------------------------------------------------------ */	
	public void handleOpenFile(File file)
    {
		if (jEdit.openFile(jEdit.getLastView(),file.getPath()) != null)
        {
            lastFilePath = file.getPath();
        } else {
            Log.log(Log.ERROR,this,"Error opening file.");
        }
	}

/* ------------------------------------------------------------ */    
    public void handleMessage(EBMessage message)
    {
        // This is necessary to have a file opened from the Finder
    	// before jEdit is running set as the currently active
    	// buffer.
        if ((message instanceof ViewUpdate) && onStartup)
        {
            if( ((ViewUpdate)message).getWhat() == ViewUpdate.CREATED)
            {
                if(lastFilePath != null)
                {
                	jEdit.getLastView().setBuffer(jEdit.getBuffer(lastFilePath));
                }
                onStartup = false;
            }
        }
		// Set type/creator codes for files
        else if (message instanceof BufferUpdate)
        {
            Buffer buffer = ((BufferUpdate)message).getBuffer();
			if ( ((BufferUpdate)message).getWhat() == BufferUpdate.DIRTY_CHANGED && !buffer.isDirty() )
            {
				try
                {
					MRJFileUtils.setFileTypeAndCreator( buffer.getFile(),
					(MRJOSType)(buffer.getProperty("MacOS.type")),
					(MRJOSType)(buffer.getProperty("MacOS.creator")) );
				}
            	catch (Exception e)
            	{
            		Log.log(Log.ERROR,this,"Error setting type/creator: file missing");
            	}
            }
			// Add type/creator to local buffer property list, if they exist.
			else if ( ((BufferUpdate)message).getWhat() == BufferUpdate.CREATED )
			{
				buffer.setProperty("MacOS.type",defaultType);
				buffer.setProperty("MacOS.creator",defaultCreator);
				try
				{
					MRJOSType	type	= MRJFileUtils.getFileType(buffer.getFile());
					MRJOSType	creator	= MRJFileUtils.getFileCreator(buffer.getFile());
					if (!type.equals(new MRJOSType("")) && !creator.equals(new MRJOSType("")))
					{
						buffer.setProperty("MacOS.type",type);
						buffer.setProperty("MacOS.creator",creator);
					}
				}
				catch (Exception e) {}
				Log.log(Log.DEBUG,this,"Assigned MRJOSTypes " + buffer.getProperty("MacOS.type")
				+ "/" + buffer.getProperty("MacOS.creator") + " to " + buffer.getName());
			}
        }
    }
	
	class ExitThread extends Thread
	{
		public void run()
		{
			jEdit.exit(jEdit.getLastView(),false);
			exitThread = new ExitThread();
		}
	}
}
