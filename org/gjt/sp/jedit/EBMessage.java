/*
 * EBMessage.java - An EditBus message
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

import java.util.Enumeration;
import java.util.Vector;

/**
 * The base class of all EditBus messages.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public abstract class EBMessage
{
	/**
	 * Creates a new message.
	 * @param source The message source
	 */
	public EBMessage(EBComponent source)
	{
		this.source = source;
	}

	/**
	 * Returns the sender of this message.
	 */
	public EBComponent getSource()
	{
		return source;
	}

	/**
	 * Vetoes this message. It will not be passed on further
	 * on the bus, and instead will be returned directly to
	 * the sender with the vetoed flag on.
	 */
	public void veto()
	{
		vetoed = true;
	}

	/**
	 * Returns if this message has been vetoed by another
	 * bus component.
	 */
	public boolean isVetoed()
	{
		return vetoed;
	}

	/**
	 * Returns a string representation of this message.
	 */
	public String toString()
	{
		return getClass().getName() + "[" + paramString() + "]";
	}

	/**
	 * Returns a string representation of this message's parameters.
	 */
	public String paramString()
	{
		return "source=" + source;
	}

	// private members
	private EBComponent source;
	private boolean vetoed;

	/**
	 * A message implementation that cannot be vetoed.
	 */
	public static abstract class NonVetoable extends EBMessage
	{
		/**
		 * Creates a new non-vetoable message.
		 * @param source The message source
		 */
		public NonVetoable(EBComponent source)
		{
			super(source);
		}

		/**
		 * Disallows this message from being vetoed.
		 */
		public void veto()
		{
			throw new InternalError("Can't veto this message");
		}
	}

	/**
	 * A message implementation that allows components to attach return
	 * values to the message, thus allowing information to be collected
	 * from others on the bus. Because this type of message is indended
	 * to collect information from all other members of the bus, it is
	 * non-vetoable.
	 */
// 	public static abstract class ReturnValue extends NonVetoable
// 	{
// 		/**
// 		 * Creates a new return value message.
// 		 * @param source The message source
// 		 */
// 		public ReturnValue(EBComponent source)
// 		{
// 			super(source);
// 		}
// 
// 		/**
// 		 * Adds data to the return value list. Subclasses should
// 		 * check that the object is of the correct type.
// 		 * @param obj The object
// 		 */
// 		public void addReturn(Object obj)
// 		{
// 			if(returnValues == null)
// 				returnValues = new Vector();
// 			returnValues.addElement(obj);
// 		}
// 
// 		/**
// 		 * Returns the values added to the return list by other
// 		 * handlers of this message. Returns null if no values were
// 		 * present.
// 		 */
// 		public Object[] getReturnValues()
// 		{
// 			if(returnValues == null)
// 				return null;
// 			Object[] array = new Object[returnValues.size()];
// 			returnValues.copyInto(array);
// 			return array;
// 		}
// 
// 		/**
// 		 * Returns a string representation of this message's parameters.
// 		 */
// 		public String paramString()
// 		{
// 			return super.paramString() + ",returnValues=" + returnValues;
// 		}
// 
// 		// private members
// 		private Vector returnValues;
// 	}
}
