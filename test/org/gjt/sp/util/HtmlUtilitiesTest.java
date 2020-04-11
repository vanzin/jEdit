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

import static javax.swing.plaf.metal.MetalLookAndFeel.getSystemTextFont;
import static org.junit.Assert.*;

public class HtmlUtilitiesTest
{
	private Font font;
	private Font fontBold;
	private Font fontItalic;
	private Font fontBoldItalic;

	@Before
	public void setUp() throws Exception
	{
		font = getSystemTextFont();
		fontBold = font.deriveFont(Font.BOLD);
		fontItalic = font.deriveFont(Font.ITALIC);
		fontBoldItalic = font.deriveFont(Font.BOLD | Font.ITALIC);
	}

	@Test
	public void parseHighlightStyle()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(null, Color.decode("#ccccff"), font);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgColor:#ccccff", font));
	}

	@Test
	public void parseHighlightStyleBold()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(null, Color.decode("#ccccff"), fontBold);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgColor:#ccccff style:b", font));
	}

	@Test
	public void parseHighlightStyleItalic()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(null, Color.decode("#ccccff"), fontItalic);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgColor:#ccccff style:i", font));
	}

	@Test
	public void parseHighlightStyleBoldItalic()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(null, Color.decode("#ccccff"), fontBoldItalic);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgColor:#ccccff style:bi", font));
	}

	@Test
	public void parseHighlightStyleBoldItalic2()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(null, Color.decode("#ccccff"), fontBoldItalic);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgColor:#ccccff style:ib", font));
	}

	@Test
	public void parseHighlightStyleFail()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(Color.decode("#000000"), null, font);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("bgCo4lor:#ccccff", font));
	}

	@Test
	public void parseHighlightStyleFail2()
	{
		SyntaxStyle syntaxStyle = new SyntaxStyle(Color.decode("#000000"), null, font);
		assertEquals(syntaxStyle, HtmlUtilities.parseHighlightStyle("style:a", font));
	}

	@Test
	public void style2htmlBoldItalic()
	{
		assertEquals("background:rgb(204,204,255);font-weight:bold;font-style: italic;",
			HtmlUtilities.style2html("bgColor:#ccccff style:bi", font));
	}

	@Test
	public void style2htmlBoldItalic2()
	{
		assertEquals("color:rgb(204,204,255);font-weight:bold;font-style: italic;",
			HtmlUtilities.style2html("color:#ccccff style:bi", font));
	}

	@Test
	public void highlightString()
	{
		assertEquals("<html><style>.highlight {color:#ccccff}</style><body><span class=\"highlight\">aa</span>a&nbsp;<span class=\"highlight\">bb</span>b&nbsp;aaa&nbsp;ccc</body></html>",
			HtmlUtilities.highlightString("aaa bbb aaa ccc", "color:#ccccff", java.util.List.of(0, 2, 4, 6)));
	}

	@Test
	public void appendString2html()
	{
		StringBuilder builder = new StringBuilder("bla");
		HtmlUtilities.appendString2html(builder,"u\"&<> h");
		assertEquals("blau&quot;&amp;&lt;&gt;&nbsp;h", builder.toString());
	}
}