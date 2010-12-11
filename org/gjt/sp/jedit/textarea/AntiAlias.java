/*
 * AntiAlias.java - a small helper class for AntiAlias settings.
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Alan Ezust
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
package org.gjt.sp.jedit.textarea;

import java.awt.RenderingHints;

/**
 * Class for representing AntiAlias values. The following modes are supported:
 * none standard lcd subpixel (JDK 1.6 only)
 * 
 * @author ezust
 * @since jedit 4.3pre4
 */
public class AntiAlias
{
	public static final String NONE = "none";

	public static final String STANDARD = "standard";

	public static final String SUBPIXEL = "subpixel";
	
	public static final String SUBPIXEL_HRGB = "subpixel HRGB";

	public static final String SUBPIXEL_VRGB = "subpixel VRGB";
	
	public static final String SUBPIXEL_HBGR = "subpixel HBGR";
	
	public static final String SUBPIXEL_VBGR = "subpixel VBGR";
	
	public static final String[] comboChoices = { NONE, STANDARD, SUBPIXEL_HRGB, SUBPIXEL_VRGB, 
		SUBPIXEL_HBGR, SUBPIXEL_VBGR };

	public static final Object[] renderHints = {RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
		RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR,
		RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR};
	
	public void set(int newValue)
	{
		m_val = newValue;
	}

	public Object renderHint() {
		return renderHints[m_val];
	}
	
	public AntiAlias(boolean isEnabled)
	{
		m_val = isEnabled ? 1 : 0;
	}

	public AntiAlias(int val)
	{
		m_val = val;
	}

	public AntiAlias(String v)
	{
		fromString(v);
	}

	public boolean equals(Object other)
	{
		return toString().equals(other.toString());

	}

	public void fromString(String v)
	{
		m_val = 0;
		if (v.equals(SUBPIXEL)) v = SUBPIXEL_HRGB;
		for (int i = 0; i < comboChoices.length; ++i)
		{
			if (comboChoices[i].equals(v))
			{
				m_val = i;
				break;
			}
		}
	}

	public String toString()
	{
		return comboChoices[m_val].toString();
	}

	public int val()
	{
		return m_val;
	}

	private int m_val;
}
