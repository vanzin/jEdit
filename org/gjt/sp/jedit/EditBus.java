/*
 * EditBus.java - The EditBus
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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
	//{{{ addToBus() method
	/**
	 * Adds a component to the bus. It will receive all messages sent
	 * on the bus.
	 * @param comp The component to add
	 */
	public static void addToBus(EBComponent comp)
	{
		synchronized(components)
		{
			components.add(comp);
			copyComponents = null;
		}
	} //}}}

	//{{{ removeFromBus() method
	/**
	 * Removes a component from the bus.
	 * @param comp The component to remove
	 */
	public static void removeFromBus(EBComponent comp)
	{
		synchronized(components)
		{
			components.remove(comp);
			copyComponents = null;
		}
	} //}}}

	//{{{ getComponents() method
	/**
	 * Returns an array of all components connected to the bus.
	 */
	public static EBComponent[] getComponents()
	{
		synchronized(components)
		{
			if (copyComponents == null)
			{
				copyComponents = (EBComponent[])components.toArray(
					new EBComponent[components.size()]);
			}
			return copyComponents;
		}
	} //}}}

	//{{{ timeTest() method
	/*static long timeTest(int msgCount)
	{
		EBMessage msg = new EBMessage(null) {};

		// To avoid any problems if components are added or removed
		// while the message is being sent
		EBComponent[] comps = getComponents();

		long start = System.currentTimeMillis();
		for(int i = 0; i < msgCount; i++)
		{
			for(int j = 0; j < comps.length; j++)
			{
				try
				{
					comps[j].handleMessage(msg);
				}
				catch(Throwable t)
				{
					Log.log(Log.ERROR,EditBus.class,"Exception"
						+ " while sending message on EditBus:");
					Log.log(Log.ERROR,EditBus.class,t);
				}
			}
		}

		return System.currentTimeMillis() - start;
	}*/ //}}}

	//{{{ send() method
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
	} //}}}

	//{{{ Private members
	private static ArrayList components = new ArrayList();
	private static EBComponent[] copyComponents;

	// can't create new instances
	private EditBus() {}
	//}}}
}
