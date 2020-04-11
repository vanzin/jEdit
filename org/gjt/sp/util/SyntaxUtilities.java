/*
 * SyntaxUtilities.java - Syntax and styles utility utility functions
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2008 Matthieu Casanova, Slava Pestov
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

//{{{ Imports
import java.awt.Color;
import java.awt.Font;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.IPropertyManager;

import static java.util.stream.Collectors.joining;
//}}}

/**
 * Syntax utilities that depends on JDK only and syntax package.
 *
 * @author Matthieu Casanova
 * @version $Id: StandardUtilities.java 9871 2007-06-28 16:33:20Z Vampire0 $
 * @since 4.3pre13
 */
public class SyntaxUtilities
{
	public static IPropertyManager propertyManager;
	private static final Pattern COLOR_MATRIX_PATTERN = Pattern.compile("(?x)\n" +
			"^\n" +
			"\\s*+ # optionally preceded by whitespace\n" +
			"\\[\n" +
			"    (?: # one or more comma-separated matrix rows\n" +
			"        \\s*+ # optionally preceded by whitespace\n" +
			"        (?> # one matrix row\n" +
			"            \\s*+ # optionally preceded by whitespace\n" +
			"            \\[\n" +
			"                (?: # one or more comma-separated colors\n" +
			"                    \\s*+ # optionally preceded by whitespace\n" +
			"                    (?>                                       # one color\n" +
			"                        \\#\\p{XDigit}{6}(?:\\p{XDigit}{2})?+ # with '#' and 6 to 8 hex digits\n" +
			"                        | red                                 # or as simple word\n" +
			"                        | green\n" +
			"                        | blue\n" +
			"                        | yellow\n" +
			"                        | orange\n" +
			"                        | white\n" +
			"                        | lightGray\n" +
			"                        | gray\n" +
			"                        | darkGray\n" +
			"                        | black\n" +
			"                        | cyan\n" +
			"                        | magenta\n" +
			"                        | pink\n" +
			"                    )\n" +
			"                    \\s*+ # optionally followed by whitespace\n" +
			"                    (?:           # optionally followed\n" +
			"                        ,         # by a comma\n" +
			"                        (?!\\s*]) # that is not followed by a closing bracket\n" +
			"                    )?+ \n" +
			"                )++\n" +
			"                \\s*+\n" +
			"            ]\n" +
			"        )\n" +
			"        \\s*+ # optionally followed by whitespace\n" +
			"        (?:           # optionally followed\n" +
			"            ,         # by a comma\n" +
			"            (?!\\s*]) # that is not followed by a closing bracket\n" +
			"        )?+\n" +
			"    )++\n" +
			"    \\s*+ # optionally followed by whitespace\n" +
			"]\n" +
			"\\s*+ # optionally followed by whitespace\n" +
			"$");
	private static final Pattern COMMA_BETWEEN_BRACKETS_SEPARATOR_PATTERN =
			Pattern.compile("(?<=])\\s*+,\\s*+(?=\\[)");
	private static final Pattern COMMA_SEPARATOR_PATTERN =
			Pattern.compile("\\s*+,\\s*+");

	//{{{ getColorHexString() method
	/**
	 * Converts a color object to its hex value. The hex value
	 * prefixed is with `#', for example `#ff0088'.
	 * @param c The color object
	 * @return the color (it is prefixed with alpha channel)
	 * @since jEdit 4.3pre13
	 */
	public static String getColorHexString(Color c)
	{
	    String mask = "#ff000000";
		String colString = Integer.toHexString(c.getRGB());
		return mask.substring(0, mask.length() - colString.length()).concat(colString);
	} //}}}

	//{{{ parseColor() method
	/**
	 * @since jEdit 4.3pre13
	 */
	public static Color parseColor(String name, Color defaultColor)
	{
		if(name == null || name.isEmpty())
			return defaultColor;

		name = name.trim();
		if(name.charAt(0) == '#')
		{
			try
			{
			    name = name.substring(1);
			    if (name.length() == 6)
			        name = "ff" + name;
			    BigInteger bi = new BigInteger(name, 16);
			    return new Color(bi.intValue(), true);
			}
			catch(NumberFormatException nf)
			{
				return defaultColor;
			}
		}
		else if("red".equals(name))
			return Color.red;
		else if("green".equals(name))
			return Color.green;
		else if("blue".equals(name))
			return Color.blue;
		else if("yellow".equals(name))
			return Color.yellow;
		else if("orange".equals(name))
			return Color.orange;
		else if("white".equals(name))
			return Color.white;
		else if("lightGray".equals(name))
			return Color.lightGray;
		else if("gray".equals(name))
			return Color.gray;
		else if("darkGray".equals(name))
			return Color.darkGray;
		else if("black".equals(name))
			return Color.black;
		else if("cyan".equals(name))
			return Color.cyan;
		else if("magenta".equals(name))
			return Color.magenta;
		else if("pink".equals(name))
			return Color.pink;
		else
			return defaultColor;
	} //}}}

	//{{{ getColorMatrixString() method
	public static String getColorMatrixString(Color[][] matrix)
	{
		if (matrix == null)
			return null;

		return Arrays.stream(matrix)
				.map(row -> Arrays.stream(row)
						.map(SyntaxUtilities::getColorHexString)
						.collect(joining(",", "[", "]")))
				.collect(joining(",", "[", "]"));
	} //}}}

	//{{{ parseColorMatrix() method
	/**
	 * Parses a color matrix of the form {@code "[[red, green], [blue, yellow]]"} into
	 * a two dimensional array. If the syntax is invalid, the given default value is
	 * returned. For the individual colors the same syntax as for
	 * {@link #parseColor(String, Color)} is supported.
	 *
	 * <p>Only rectangular matrices are supported. If the lines of the matrix have
	 * different lengths, also the default value is returned.
	 *
	 * @param matrix The color matrix
	 * @param defaultColorMatrix The default value if there was a syntax error
	 *
	 * @since jEdit 5.6
	 */
	public static Color[][] parseColorMatrix(String matrix, Color[][] defaultColorMatrix)
	{
		if(matrix == null || !COLOR_MATRIX_PATTERN.matcher(matrix).matches())
			return defaultColorMatrix;

		matrix = matrix.strip();
		Color[][] result = COMMA_BETWEEN_BRACKETS_SEPARATOR_PATTERN
				.splitAsStream(matrix.substring(1, matrix.length() - 1))
				.map(row ->
				{
					row = row.strip();
					return COMMA_SEPARATOR_PATTERN
							.splitAsStream(row.substring(1, row.length() - 1))
							.map(String::strip)
							.map(color -> parseColor(color, null))
							.toArray(Color[]::new);
				})
				.toArray(Color[][]::new);

		// if any color was invalid
		// or rows are not of same length
		// return default value
		if (Arrays.stream(result).flatMap(Arrays::stream).anyMatch(Objects::isNull)
				|| (Arrays.stream(result).mapToInt(row -> row.length).distinct().count() != 1)) {
			return defaultColorMatrix;
		}

		return result;
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @param color If false, the styles will be monochrome
	 * @param defaultFgColor Default foreground color (if not specified in style string)
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 4.3pre17
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size,
		boolean color, Color defaultFgColor)
		throws IllegalArgumentException
	{
		Color fgColor = defaultFgColor;
		Color bgColor = null;
		boolean italic = false;
		boolean bold = false;
		StringTokenizer st = new StringTokenizer(str);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			if(s.startsWith("color:"))
			{
				if(color)
					fgColor = parseColor(s.substring(6), Color.black);
			}
			else if(s.startsWith("bgColor:"))
			{
				if(color)
					bgColor = parseColor(s.substring(8), null);
			}
			else if(s.startsWith("style:"))
			{
				for(int i = 6; i < s.length(); i++)
				{
					if(s.charAt(i) == 'i')
						italic = true;
					else if(s.charAt(i) == 'b')
						bold = true;
					else
						throw new IllegalArgumentException(
								"Invalid style: " + s);
				}
			}
			else
				throw new IllegalArgumentException(
						"Invalid directive: " + s);
		}
		return new SyntaxStyle(fgColor,bgColor,
				new Font(family,
						(italic ? Font.ITALIC : 0) | (bold ? Font.BOLD : 0),
						size));
	} //}}}

	//{{{ parseStyle() method
	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @param family Style strings only specify font style, not font family
	 * @param size Style strings only specify font style, not font family
	 * @param color If false, the styles will be monochrome
	 * @exception IllegalArgumentException if the style is invalid
	 * @since jEdit 4.3pre13
	 */
	public static SyntaxStyle parseStyle(String str, String family, int size,
		boolean color)
		throws IllegalArgumentException
	{
		return parseStyle(str, family, size, color, Color.black);
	} //}}}

	//{{{ loadStyles() methods
	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @since jEdit 4.3pre13
	 */
	public static SyntaxStyle[] loadStyles(String family, int size)
	{
		return loadStyles(family,size,true);
	}

	/**
	 * Loads the syntax styles from the properties, giving them the specified
	 * base font family and size.
	 * @param family The font family
	 * @param size The font size
	 * @param color If false, the styles will be monochrome
	 * @since jEdit 4.3pre13
	 */
	public static SyntaxStyle[] loadStyles(String family, int size, boolean color)
	{
		SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

		// start at 1 not 0 to skip Token.NULL
		for(int i = 1; i < styles.length; i++)
		{
			try
			{
				String styleName = "view.style."
					+ Token.tokenToString((byte)i)
					.toLowerCase(Locale.ENGLISH);
				styles[i] = parseStyle(
					propertyManager.getProperty(styleName),
					family,size,color);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,StandardUtilities.class,e);
			}
		}

		return styles;
	} //}}}

	private SyntaxUtilities(){}
}
