/*
 * EditBus.java - The EditBus
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit;

import java.util.*;
import org.gjt.sp.util.Log;

/**
 * The EditBus is jEdit's global event notification mechanism. A number of
 * messages are sent by jEdit; they are all instances of the classes found
 * in the <code>org.gjt.sp.jedit.msg</code> package. Plugins can also send
 * their own messages.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class EditBus
{
	/**
	 * Adds a component to the bus. It will receive all messages sent
	 * on the bus.
	 * @param comp The component to add
	 */
	public static void addToBus(EBComponent comp)
	{
		synchronized(components)
		{
			components.addElement(comp);
			copyComponents = null;
		}
	}

	/**
	 * Removes a component from the bus.
	 * @param comp The component to remove
	 */
	public static void removeFromBus(EBComponent comp)
	{
		synchronized(components)
		{
			components.removeElement(comp);
			copyComponents = null;
		}
	}

	/**
	 * Returns an array of all components connected to the bus.
	 */
	public static EBComponent[] getComponents()
	{
		synchronized(components)
		{
			if (copyComponents == null)
			{
				copyComponents = new EBComponent[components.size()];
				components.copyInto(copyComponents);
			}
			return copyComponents;
		}
	}

	/**
	 * Sends a message to all components on the bus.
	 * The message will be sent to all components in turn, with the
	 * original sender receiving it last.
	 * @param message The message
	 */
	public static void send(EBMessage message)
	{
		Log.log(Log.DEBUG,EditBus.class,message.toString());

		// To avoid any problems if components are added or removed
		// while the message is being sent
		EBComponent[] comps = getComponents();

		for(int i = 0; i < comps.length; i++)
		{
			try
			{
				comps[i].handleMessage(message);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,EditBus.class,"Exception"
					+ " while sending message on EditBus:");
				Log.log(Log.ERROR,EditBus.class,t);
			}
		}
	}

	/**
	 * @deprecated Do not use
	 */
	public static Object[] getNamedList(Object tag)
	{
		Object[] list = (Object[])listArrays.get(tag);
		if(list != null)
			return list;

		Vector listVector = (Vector)listVectors.get(tag);
		if(listVector != null)
		{
			list = new Object[listVector.size()];
			listVector.copyInto(list);
			listArrays.put(tag,list);
			return list;
		}

		return null;
	}

	/**
	 * @deprecated Do not use
	 */
	public static Enumeration getNamedLists()
	{
		return listVectors.keys();
	}

	/**
	 * @deprecated For dockable windows, write a <code>dockables.xml</code>
	 * file instead. For ErrorList error sources, use the appropriate
	 * ErrorList APIs.
	 */
	public static void addToNamedList(Object tag, Object entry)
	{
		if(tag.equals(org.gjt.sp.jedit.gui.DockableWindow.DOCKABLE_WINDOW_LIST))
		{
			// clumsy backwards compatibility hack
			org.gjt.sp.jedit.gui.DockableWindowManager
				.registerDockableWindow((String)entry,
				null,false,jEdit.getActionSets()[0]);
		}
		else
		{
			Vector listVector = (Vector)listVectors.get(tag);
			if(listVector == null)
			{
				listVector = new Vector();
				listVectors.put(tag,listVector);
			}

			listVector.addElement(entry);
			listArrays.remove(tag);
		}
	}

	/**
	 * @deprecated Do not use.
	 */
	public static void removeFromNamedList(Object tag, Object entry)
	{
		Vector listVector = (Vector)listVectors.get(tag);
		if(listVector == null)
			return;

		listVector.removeElement(entry);
		listArrays.remove(tag);
	}

	// private members
	private static Vector components = new Vector();
	private static EBComponent[] copyComponents;
	private static Hashtable listVectors = new Hashtable();
	private static Hashtable listArrays = new Hashtable();

	// can't create new instances
	private EditBus() {}
}
