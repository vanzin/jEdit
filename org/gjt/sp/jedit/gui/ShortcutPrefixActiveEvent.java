/*
 *  ShortcutPrefixActiveEvent.java - Event fired when jEdit starts and stops
 *  listening for shortcut completions
 *  :tabSize=8:indentSize=8:noTabs=false:
 *  :folding=explicit:collapseFolds=1:
 *
 *  Copyright (C) 2005 Jeffrey Hoyt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.util.Hashtable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.gjt.sp.util.Log;
//}}}

/**
 *  Description of the Class
 *
 * @author     jchoyt
 * created    December 17, 2005
 */
public class ShortcutPrefixActiveEvent extends ChangeEvent
{

	/**
	 * Description of the Field
	 */
	protected Hashtable bindings;
	/**
	 * Description of the Field
	 */
	protected boolean active;

	/**
	 * Description of the Field
	 */
	protected static EventListenerList listenerList = new EventListenerList();

	//{{{  Constructor
	/**
	 * Constructor for the ShortcutPrefixActiveEvent object
	 *
	 * @param bindings Description of the Parameter
	 * @param active   Description of the Parameter
	 */
	public ShortcutPrefixActiveEvent(Hashtable bindings, boolean active)
	{
		super(new Object());
		this.bindings = bindings;
		this.active = active;
	} //}}}

	//{{{ addChangeEventListener() method
	/**
	 * Adds a feature to the ChangeEventListener attribute of the
	 * ShortcutPrefixActiveEvent class
	 *
	 * @param l The feature to be added to the ChangeEventListener attribute
	 */
	public static void addChangeEventListener(ChangeListener l)
	{
		listenerList.add(ChangeListener.class, l);
		Log.log(Log.DEBUG, ShortcutPrefixActiveEvent.class, "Listener added.  " + listenerList.getListenerList().length + " left.");
	}//}}}

	//{{{ removeChangeEventListener() method
	/**
	 * Description of the Method
	 *
	 * @param l Description of the Parameter
	 */
	public static void removeChangeEventListener(ChangeListener l)
	{
		listenerList.remove(ChangeListener.class, l);
		Log.log(Log.DEBUG, ShortcutPrefixActiveEvent.class, "Listener removed.  " + listenerList.getListenerList().length + " left.");
	}//}}}

	//{{{ firePrefixStateChange() method
	/**
	 * Description of the Method
	 *
	 * @param bindings                       Description of the Parameter
	 * @param listeningForShortcutCompletion Description of the Parameter
	 */
	public static void firePrefixStateChange(Hashtable bindings, boolean listeningForShortcutCompletion)
	{
		//Log.log( Log.DEBUG, ShortcutPrefixActiveEvent.class, "firePrefixStateChange() called, listening? " + listeningForShortcutCompletion );
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		//Log.log( Log.DEBUG, ShortcutPrefixActiveEvent.class, listeners.length + " listeners." );
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			//Log.log( Log.DEBUG, ShortcutPrefixActiveEvent.class, "firePrefixStateChange() called, listening? " + listeningForShortcutCompletion );
			ChangeEvent event = new ShortcutPrefixActiveEvent(bindings, listeningForShortcutCompletion);
			((ChangeListener) listeners[i + 1]).stateChanged(event);
		}
	}//}}}


	//{{{  getBindings()
	/**
	 * Gets the bindings attribute of the ShortcutPrefixActiveEvent object
	 *
	 * @return The bindings value
	 */
	public Hashtable getBindings()
	{
		return bindings;
	}//}}}

	//{{{  getActive()
	/**
	 * Gets the active attribute of the ShortcutPrefixActiveEvent object
	 *
	 * @return The active value
	 */
	public boolean getActive()
	{
		return active;
	}
	//}}}
}

