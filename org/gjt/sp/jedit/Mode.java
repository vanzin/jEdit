/*
 * Mode.java - jEdit editing mode
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
 * Copyright (C) 1999 mike dillon
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

import gnu.regexp.*;
import java.util.Hashtable;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.util.Log;

/**
 * An edit mode defines specific settings for editing some type of file.
 * One instance of this class is created for each supported edit mode.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Mode
{
	/**
	 * Creates a new edit mode.
	 *
	 * @param name The name used in mode listings and to query mode
	 * properties
	 * @see #getProperty(String)
	 */
	public Mode(String name)
	{
		this.name = name;
		props = new Hashtable();
	}

	/**
	 * Initializes the edit mode. Should be called after all properties
	 * are loaded and set.
	 */
	public void init()
	{
		try
		{
			String filenameGlob = (String)getProperty("filenameGlob");
			if(filenameGlob != null && filenameGlob.length() != 0)
			{
				filenameRE = new RE(MiscUtilities.globToRE(
					filenameGlob),RE.REG_ICASE);
			}

			String firstlineGlob = (String)getProperty("firstlineGlob");
			if(firstlineGlob != null && firstlineGlob.length() != 0)
			{
				firstlineRE = new RE(MiscUtilities.globToRE(
					firstlineGlob),RE.REG_ICASE);
			}
		}
		catch(REException re)
		{
			Log.log(Log.ERROR,this,"Invalid filename/firstline"
				+ " globs in mode " + name);
			Log.log(Log.ERROR,this,re);
		}
	}

	/**
	 * Returns the token marker specified with
	 * <code>setTokenMarker()</code>. Should only be called by
	 * <code>TokenMarker.getExternalRuleSet()</code>.
	 */
	public TokenMarker getTokenMarker()
	{
		loadIfNecessary();
		return marker;
	}

	/**
	 * Sets the token marker for this mode. This token marker will be
	 * cloned to obtain new instances.
	 * @param marker The new token marker
	 */
	public void setTokenMarker(TokenMarker marker)
	{
		this.marker = marker;
	}

	/**
	 * Loads the mode from disk if it hasn't been loaded already.
	 * @since jEdit 2.5pre3
	 */
	public void loadIfNecessary()
	{
		if(marker == null)
			jEdit.loadMode(this);
	}

	/**
	 * Returns a mode property.
	 * @param key The property name
	 *
	 * @since jEdit 2.2pre1
	 */
	public Object getProperty(String key)
	{
		String prefix = "mode." + name + ".";

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
	}

	/**
	 * Returns the value of a boolean property.
	 * @param key The property name
	 *
	 * @since jEdit 2.5pre3
	 */
	public boolean getBooleanProperty(String key)
	{
		Object value = getProperty(key);
		if("true".equals(value) || "on".equals(value) || "yes".equals(value))
			return true;
		else
			return false;
	}

	/**
	 * Sets a mode property.
	 * @param key The property name
	 * @param value The property value
	 */
	public void setProperty(String key, Object value)
	{
		props.put(key,value);
	}

	/**
	 * Unsets a mode property.
	 * @param key The property name
	 * @since jEdit 3.2pre3
	 */
	public void unsetProperty(String key)
	{
		props.remove(key);
	}

	/**
	 * Returns if the edit mode is suitable for editing the specified
	 * file. The buffer name and first line is checked against the
	 * file name and first line globs, respectively.
	 * @param fileName The buffer's name
	 * @param firstLine The first line of the buffer
	 *
	 * @since jEdit 3.2pre3
	 */
	public boolean accept(String fileName, String firstLine)
	{
		if(filenameRE != null && filenameRE.isMatch(fileName))
			return true;

		if(firstlineRE != null && firstlineRE.isMatch(firstLine))
			return true;

		return false;
	}

	/**
	 * Returns the internal name of this edit mode.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns a string representation of this edit mode.
	 */
	public String toString()
	{
		return getClass().getName() + "[" + getName() + "]";
	}

	// private members
	private String name;
	private Hashtable props;
	private RE firstlineRE;
	private RE filenameRE;
	private TokenMarker marker;
}
