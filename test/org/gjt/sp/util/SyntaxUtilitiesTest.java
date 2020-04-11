/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.util;

import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class SyntaxUtilitiesTest
{
	@Test
	public void getColorHexString()
	{
		assertEquals("#ff808080", SyntaxUtilities.getColorHexString(Color.gray));
	}

	@Test
	public void getColorHexStringRed()
	{
		assertEquals("#ffff0000", SyntaxUtilities.getColorHexString(Color.red));
	}

	@Test
	public void parseColorNull()
	{
		assertEquals(Color.red, SyntaxUtilities.parseColor(null, Color.red));
	}

	@Test
	public void parseColorRedAlpha()
	{
		assertEquals(Color.red, SyntaxUtilities.parseColor("#ffff0000", Color.black));
	}

	@Test
	public void parseColorRedNoAlpha()
	{
		assertEquals(Color.red, SyntaxUtilities.parseColor("#ff0000", Color.black));
	}

	@Test
	public void parseColorError()
	{
		assertEquals(Color.black, SyntaxUtilities.parseColor("#ffjj0000", Color.black));
	}

	@Test
	public void parseColorRed()
	{
		assertEquals(Color.red, SyntaxUtilities.parseColor("red", Color.black));
	}

	@Test
	public void parseColorGreen()
	{
		assertEquals(Color.green, SyntaxUtilities.parseColor("green", Color.black));
	}

	@Test
	public void parseColorBLue()
	{
		assertEquals(Color.blue, SyntaxUtilities.parseColor("blue", Color.black));
	}

	@Test
	public void parseColorYellow()
	{
		assertEquals(Color.yellow, SyntaxUtilities.parseColor("yellow", Color.black));
	}

	@Test
	public void parseColorOrange()
	{
		assertEquals(Color.orange, SyntaxUtilities.parseColor("orange", Color.black));
	}

	@Test
	public void parseColorWhite()
	{
		assertEquals(Color.white, SyntaxUtilities.parseColor("white", Color.black));
	}

	@Test
	public void parseColorlightGray()
	{
		assertEquals(Color.lightGray, SyntaxUtilities.parseColor("lightGray", Color.black));
	}

	@Test
	public void parseColorGray()
	{
		assertEquals(Color.gray, SyntaxUtilities.parseColor("gray", Color.black));
	}

	@Test
	public void parseColorDarkGray()
	{
		assertEquals(Color.darkGray, SyntaxUtilities.parseColor("darkGray", Color.black));
	}

	@Test
	public void parseColorBlack()
	{
		assertEquals(Color.black, SyntaxUtilities.parseColor("black", Color.yellow));
	}

	@Test
	public void parseColorCyan()
	{
		assertEquals(Color.cyan, SyntaxUtilities.parseColor("cyan", Color.black));
	}

	@Test
	public void parseColorMagenta()
	{
		assertEquals(Color.magenta, SyntaxUtilities.parseColor("magenta", Color.black));
	}

	@Test
	public void parseColorPink()
	{
		assertEquals(Color.pink, SyntaxUtilities.parseColor("pink", Color.black));
	}

	@Test
	public void parseColorWhat()
	{
		assertEquals(Color.black, SyntaxUtilities.parseColor("what", Color.black));
	}

	@Test
	public void parseStyle()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(Color.green, Color.red, Font.decode("Arial bolditalic"));
		SyntaxStyle arial = SyntaxUtilities.parseStyle("color:#ff00ff00 bgColor:#ff0000 style:bi", "Arial", 12, true);
		assertEquals(syntaxStyle, arial);
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseStyleError()
	{
		SyntaxUtilities.parseStyle("style:a", "Arial", 12, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseStyleError2()
	{
		SyntaxUtilities.parseStyle("stfyle:a", "Arial", 12, true);
	}
}