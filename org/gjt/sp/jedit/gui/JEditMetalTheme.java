/*
 * JEditMetalTheme.java - Minor Metal L&F tweaks for jEdit
 * Copyright (C) 2001 Slava Pestov
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

package org.gjt.sp.jedit.gui;

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;
import javax.swing.*;
import org.gjt.sp.jedit.jEdit;

public class JEditMetalTheme extends DefaultMetalTheme
{
	public String getName()
	{
		return "jEdit";
	}

	public ColorUIResource getSystemTextColor()
	{
		return getBlack();
	}

	public FontUIResource getControlTextFont()
	{
		return primaryFont;
	}

	public FontUIResource getSystemTextFont()
	{
		return secondaryFont;
	}

	public FontUIResource getUserTextFont()
	{
		return secondaryFont;
	}

	public FontUIResource getMenuTextFont()
	{
		return primaryFont;
	}

	public ColorUIResource getPrimary1()
	{
		return primary1;
	}

	public ColorUIResource getPrimary2()
	{
		return primary2;
	}

	public ColorUIResource getPrimary3()
	{
		return primary3;
	}

	public ColorUIResource getSecondary1()
	{
		return secondary1;
	}

	public ColorUIResource getSecondary2()
	{
		return secondary2;
	}

	public ColorUIResource getSecondary3()
	{
		return secondary3;
	}

	public void propertiesChanged()
	{
		primaryFont = new FontUIResource(
			jEdit.getFontProperty("metal.primary.font",
			super.getControlTextFont()));
		secondaryFont = new FontUIResource(
			jEdit.getFontProperty("metal.secondary.font",
			super.getSystemTextFont()));

		if(jEdit.getBooleanProperty("plasticColors"))
		{
			primary1 = new ColorUIResource(32, 32, 64);
			primary2 = new ColorUIResource(160, 160, 180);
			primary3 = new ColorUIResource(200, 200, 224);
			secondary1 = new ColorUIResource(130, 130, 130);
			secondary2 = new ColorUIResource(180, 180, 180);
			secondary3 = new ColorUIResource(224, 224, 224);
		}
		else
		{
			primary1 = new ColorUIResource(102, 102, 153);
			primary2 = new ColorUIResource(153, 153, 204);
			primary3 = new ColorUIResource(204, 204, 255);
			secondary1 = new ColorUIResource(102, 102, 102);
			secondary2 = new ColorUIResource(153, 153, 153);
			secondary3 = new ColorUIResource(204, 204, 204);
		}
	}

	// private members
	private FontUIResource primaryFont;
	private FontUIResource secondaryFont;
	private ColorUIResource primary1;
	private ColorUIResource primary2;
	private ColorUIResource primary3;
	private ColorUIResource secondary1;
	private ColorUIResource secondary2;
	private ColorUIResource secondary3;
}
