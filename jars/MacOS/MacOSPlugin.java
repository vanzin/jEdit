/* 
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * MacOSPlugin.java - Main class Mac OS Plugin
 * Copyright (C) 2001, 2002 Kris Kopicki
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
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;
//}}}

public class MacOSPlugin extends EBPlugin
{
	
//{{{ Variables
	private boolean started = false;
	private boolean osok;
	private MacOSHandler handler;
//}}}
	
	//{{{ start() method
	public void start()
    {
		if(osok = osok())
        {	
			handler = new MacOSHandler();
			// Register handlers
			MRJApplicationUtils.registerQuitHandler(handler);
    		MRJApplicationUtils.registerAboutHandler(handler);
			MRJApplicationUtils.registerPrefsHandler(handler);
			MRJApplicationUtils.registerOpenDocumentHandler(handler);
        }
	}//}}}
	
	//{{{ createOptionPanes() method
	public void createOptionPanes(OptionsDialog od) {
		if (osok)
			od.addOptionPane(new MacOSOptionPane());
    }//}}}

	//{{{ handleMessage() method
    public void handleMessage(EBMessage message)
    {
		if (osok)
		{
			// This is necessary to have a file opened from the Finder
			// before jEdit is running set as the currently active
			// buffer.
			if (!started && message instanceof ViewUpdate)
			{
				handler.handleOpenFile((ViewUpdate)message);
			}
			// Set type/creator codes for files
			else if (message instanceof BufferUpdate)
			{
				handler.handleFileCodes((BufferUpdate)message);
			}
		}
    }//}}}
	
	//{{{ started() method
	/**
	 * Returns true once all initialisations have been done
	 */
	public boolean started()
	{
		return started;
	}//}}}
	
	//{{{ started() method
	public void started(boolean v)
	{
		started = v;
	}//}}}
	
	//{{{ osok() method
	private boolean osok()
	{
		final String osname = jEdit.getProperty("MacOSPlugin.depend.os.name");
		final String osversion = jEdit.getProperty("MacOSPlugin.depend.os.version");
		
		if (!System.getProperty("os.name").equals(osname))
		{
			SwingUtilities.invokeLater( new Runnable() { public void run() {
				GUIUtilities.error(null,"MacOSPlugin.dialog.osname",new Object[] {osname});
			}});
			return false;
		}
		if (System.getProperty("os.version").compareTo(osversion) <= 0)
		{
			SwingUtilities.invokeLater( new Runnable() { public void run() {
				GUIUtilities.error(null,"MacOSPlugin.dialog.osversion",new Object[] {osversion});
			}});
			return false;
		}
		return true;
	}//}}}
	
}
