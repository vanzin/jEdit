/*
 * HtmlUtilities.java - HTML utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Shlomy Reinstein
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

package org.gjt.sp.util;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.gjt.sp.jedit.syntax.SyntaxStyle;

/**
 * HTML utility methods for conversion of strings to HTML and highlighting matches
 * in search results.
 * Some of these methods were moved here from HyperSearchResults.HighlightingTree
 * to make them available for plugins.
 *
 * @author Shlomy Reinstein
 * @version $Id: $
 * @since 4.4pre1
 */

public class HtmlUtilities
{
	//{{{ public section

	//{{{ parseHighlightStyle()
	/**
	 * Parses a string specifying a syntax highlight style.
	 *
	 * The syntax highlight string should be in the same format used to
	 * store syntax highlight styles in the properties.
	 *
	 * @param style The syntax highlight style string.
	 * @param f The font to which the syntax style will apply.
	 * @return The SyntaxStyle object represented by the style string.
	 */
	public static SyntaxStyle parseHighlightStyle(String style, Font f)
	{
		SyntaxStyle s;
		try
		{
			s = SyntaxUtilities.parseStyle(style, f.getFamily(), f.getSize(), true, null);
		}
		catch (Exception e)
		{
			style = "color:#000000";
			s = SyntaxUtilities.parseStyle(style, f.getFamily(), f.getSize(), true);
		}
		return s;
	} //}}}

	//{{{ style2html()
	/**
	 * Parses a string specifying a syntax highlight style, and creates an
	 * HTML representation for it.
	 *
	 * The syntax highlight string should be in the same format used to
	 * store syntax highlight styles in the properties.
	 *
	 * @param prop The syntax highlight style string.
	 * @param f The font to which the syntax style will apply.
	 * @return The HTML representation of the given syntax style. 
	 */
	public static String style2html(String prop, Font f)
	{
		StringBuilder tag = new StringBuilder();
		SyntaxStyle style = parseHighlightStyle(prop, f);
		Color c = style.getForegroundColor();
		if (c != null)
			tag.append("color:").append(color2html(c));
		c = style.getBackgroundColor();
		if (c != null)
			tag.append("background:").append(color2html(c));
		f = style.getFont();
		if (f.isBold())
			tag.append("font-weight:bold;");
		if (f.isItalic())
			tag.append("font-style: italic;");
		return tag.toString();
	} //}}}

	//{{{ highlightString()
	/**
	 * Creates an HTML presentation of a given string, where selected substrings
	 * are highlighted with a given syntax style tag.
	 *
	 * @param s The (non-HTML) string to highlight. 
	 * @param styleTag The HTML string representing the highlight style.
	 * @param ranges The indices of the substrings to highlight, in pairs: The start
	 *               index of a substring followed by the end index of the substring.
	 * @return The HTML representation of the string with highlighted substrings. 
	 */
	public static String highlightString(String s, String styleTag, List<Integer> ranges)
	{
		StringBuilder sb = new StringBuilder("<html><style>.highlight {");
		sb.append(styleTag);
		sb.append("}</style><body>");
		int lastIndex = 0;
		for (int i = 0; i < ranges.size(); i += 2)
		{
			int rangeStart = ranges.get(i);
			int rangeEnd = ranges.get(i + 1);
			appendString2html(sb, s.substring(lastIndex, rangeStart));
			sb.append("<span class=\"highlight\">");
			appendString2html(sb, s.substring(rangeStart, rangeEnd));
			sb.append("</span>");
			lastIndex = rangeEnd;
		}
		appendString2html(sb, s.substring(lastIndex));
		sb.append("</body></html>");
		return sb.toString();
	} //}}}

	//{{{ appendString2html
	/**
	 * Appends a given non-HTML string to an HTML string, translating character
	 * entities to the appropriate HTML form.
	 * 
	 * @param sb The HTML string to which the non-HTML string is appended.
	 * @param s The non-HTML string to append.
	 */
	public static void appendString2html(StringBuilder sb, String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			String r;
			switch (c)
			{
			case '"':
				r = "&quot;";
				break;
			// case '\'': r = "&apos;"; break;
			case '&':
				r = "&amp;";
				break;
			case '<':
				r = "&lt;";
				break;
			case '>':
				r = "&gt;";
				break;
			case ' ':
				r = "&nbsp;";	// Maintain amount of whitespace in line
				break;
			default:
				r = String.valueOf(c);
				break;
			}
			sb.append(r);
		}
	} //}}}
	
	//}}}

	//{{{ private section

	//{{{ color2html()
	private static String color2html(Color c)
	{
		StringBuilder cs = new StringBuilder("rgb(");
		cs.append(c.getRed());
		cs.append(",");
		cs.append(c.getGreen());
		cs.append(",");
		cs.append(c.getBlue());
		cs.append(");");
		return cs.toString();
	} //}}}

	//}}}
}
