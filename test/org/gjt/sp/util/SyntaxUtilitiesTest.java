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
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SyntaxUtilitiesTest
{
	@Before
	public void setUp() throws Exception
	{
		Map<String, String> map = new HashMap<>();
		map.put("view.style.comment1", "color:#cc0000");
		map.put("view.style.comment2", "color:#ff8400");
		map.put("view.style.comment3", "color:#6600cc");
		map.put("view.style.comment4", "color:#cc6600");
		map.put("view.style.digit", "color:#ff0000");
		map.put("view.style.foldLine.0", "color:#000000 bgColor:#dafeda style:b");
		map.put("view.style.foldLine.1", "color:#000000 bgColor:#fff0cc style:b");
		map.put("view.style.foldLine.2", "color:#000000 bgColor:#e7e7ff style:b");
		map.put("view.style.foldLine.3", "color:#000000 bgColor:#ffe0f0 style:b");
		map.put("view.style.function", "color:#9966ff");
		map.put("view.style.invalid", "color:#ff0066 bgColor:#ffffcc");
		map.put("view.style.keyword1", "color:#006699 style:b");
		map.put("view.style.keyword2", "color:#009966 style:b");
		map.put("view.style.keyword3", "color:#0099ff style:b");
		map.put("view.style.keyword4", "color:#66ccff style:b");
		map.put("view.style.label", "color:#02b902");
		map.put("view.style.literal1", "color:#ff00cc");
		map.put("view.style.literal2", "color:#cc00cc");
		map.put("view.style.literal3", "color:#9900cc");
		map.put("view.style.literal4", "color:#6600cc");
		map.put("view.style.markup", "color:#0000ff");
		map.put("view.style.operator", "color:#000000 style:b");
		map.put("view.style.wrap", "color:#000000 style:b");
		SyntaxUtilities.propertyManager = map::get;
	}

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

	@Test
	public void loadStyles()
	{
		SyntaxUtilities.loadStyles("Monospaced", 12);
	}
}