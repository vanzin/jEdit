/*
 * JEditMode.java - jEdit editing mode
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2007 Matthieu Casanova
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

import org.gjt.sp.util.Log;

/**
 * @author Matthieu Casanova
 * @version $Id: Buffer.java 8190 2006-12-07 07:58:34Z kpouer $
 * @since jEdit 4.3pre10
 */
class JEditMode extends Mode
{
	//{{{ JEditMode constructor
	JEditMode(String name)
	{
		super(name);
	} //}}}

	//{{{ getProperty() method
	/**
	 * Returns a mode property.
	 *
	 * @param key The property name
	 * @since jEdit 4.3pre10
	 */
	@Override
	public Object getProperty(String key)
	{
		String prefix = "mode." + name + '.';

		//if(jEdit.getBooleanProperty(prefix + "customSettings"))
		//{
			String property = jEdit.getProperty(prefix + key);
			if(property != null)
			{
				Object value;
				try
				{
					value = new Integer(property);
				}
				catch(NumberFormatException nf)
				{
					value = property;
				}
				return value;
			}
		//}

		Object value = props.get(key);
		if(value != null)
			return value;

		String global = jEdit.getProperty("buffer." + key);
		if(global != null)
		{
			try
			{
				return new Integer(global);
			}
			catch(NumberFormatException nf)
			{
				return global;
			}
		}
		else
			return null;
	} //}}}

	//{{{ loadIfNecessary() method
	/**
	 * Loads the mode from disk if it hasn't been loaded already.
	 * @since jEdit 4.3pre10
	 */
	@Override
	public void loadIfNecessary()
	{
		if(marker == null)
		{
			jEdit.loadMode(this);
			if (marker == null)
				Log.log(Log.ERROR, this, "Mode not correctly loaded, token marker is still null");
		}
	} //}}}
}
