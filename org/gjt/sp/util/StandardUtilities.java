/*
 * StandardUtilities.java - Various miscallaneous utility functions
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2006 Matthieu Casanova, Slava Pestov
 * Portions copyright (C) 2000 Richard S. Hall
 * Portions copyright (C) 2001 Dirk Moebius
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
import javax.swing.text.Segment;
import java.util.Comparator;
import java.util.Stack;
//}}}

/**
 * Several tools that depends on JDK only.
 *
 * @author Matthieu Casanova
 * @version $Id$
 * @since 4.3pre5
 */
public class StandardUtilities
{

	//{{{ Text methods

	//{{{ getLeadingWhiteSpace() method
	/**
	 * Returns the number of leading white space characters in the
	 * specified string.
	 * @param str The string
	 */
	public static int getLeadingWhiteSpace(String str)
	{
		return getLeadingWhiteSpace((CharSequence)str);
	} //}}}

	//{{{ getLeadingWhiteSpace() method
	/**
	 * Returns the number of leading white space characters in the
	 * specified string.
	 * @param str The string
	 * @since jEdit 4.3pre10
	 */
	public static int getLeadingWhiteSpace(CharSequence str)
	{
		int whitespace = 0;
loop:		for(;whitespace < str.length();)
		{
			switch(str.charAt(whitespace))
			{
			case ' ':
			case '\t':
				whitespace++;
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	} //}}}

	//{{{ getTrailingWhiteSpace() method
	/**
	 * Returns the number of trailing whitespace characters in the
	 * specified string.
	 * @param str The string
	 */
	public static int getTrailingWhiteSpace(String str)
	{
		int whitespace = 0;
loop:		for(int i = str.length() - 1; i >= 0; i--)
		{
			switch(str.charAt(i))
			{
				case ' ':
				case '\t':
					whitespace++;
					break;
				default:
					break loop;
			}
		}
		return whitespace;
	} //}}}

	//{{{ getLeadingWhiteSpaceWidth() method
	/**
	 * Returns the width of the leading white space in the specified
	 * string.
	 * @param str The string
	 * @param tabSize The tab size
	 */
	public static int getLeadingWhiteSpaceWidth(String str, int tabSize)
	{
		return getLeadingWhiteSpaceWidth((CharSequence)str, tabSize);
	} //}}}

	//{{{ getLeadingWhiteSpaceWidth() method
	/**
	 * Returns the width of the leading white space in the specified
	 * string.
	 * @param str The string
	 * @param tabSize The tab size
	 * @since jEdit 4.3pre10
	 */
	public static int getLeadingWhiteSpaceWidth(CharSequence str, int tabSize)
	{
		int whitespace = 0;
loop:		for(int i = 0; i < str.length(); i++)
		{
			switch(str.charAt(i))
			{
				case ' ':
					whitespace++;
					break;
				case '\t':
					whitespace += tabSize -
						whitespace % tabSize;
					break;
				default:
					break loop;
			}
		}
		return whitespace;
	} //}}}

	//{{{ getLastIndexOf() method
	/**
	 * An implementation of String.lastIndexOf(char) for CharSequence
	 * instances.
	 *
	 * @since jEdit 4.3pre10
	 */
	public static int getLastIndexOf(CharSequence seq, char ch)
	{
		return getLastIndexOf(seq, ch, seq.length());
	} //}}}

	//{{{ getLastIndexOf() method
	/**
	 * An implementation of String.lastIndexOf(char) for CharSequence
	 * instances.
	 *
	 * @since jEdit 4.3pre10
	 */
	public static int getLastIndexOf(CharSequence seq, char ch, int pos)
	{
		assert (pos <= seq.length()) : "invalid start position";
		for (int i = pos - 1; i >= 0; i--)
			if (seq.charAt(i) == ch)
				return i;
		return -1;
	} //}}}

	//{{{ createWhiteSpace() method
	/**
	 * Creates a string of white space with the specified length.<p>
	 *
	 * To get a whitespace string tuned to the current buffer's
	 * settings, call this method as follows:
	 *
	 * <pre>myWhitespace = MiscUtilities.createWhiteSpace(myLength,
	 *     (buffer.getBooleanProperty("noTabs") ? 0
	 *     : buffer.getTabSize()));</pre>
	 *
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 */
	public static String createWhiteSpace(int len, int tabSize)
	{
		return createWhiteSpace(len,tabSize,0);
	} //}}}

	//{{{ createWhiteSpace() method
	/**
	 * Creates a string of white space with the specified length.<p>
	 *
	 * To get a whitespace string tuned to the current buffer's
	 * settings, call this method as follows:
	 *
	 * <pre>myWhitespace = MiscUtilities.createWhiteSpace(myLength,
	 *     (buffer.getBooleanProperty("noTabs") ? 0
	 *     : buffer.getTabSize()));</pre>
	 *
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 * @param start The start offset, for tab alignment
	 */
	public static String createWhiteSpace(int len, int tabSize, int start)
	{
		StringBuilder buf = new StringBuilder();
		if(tabSize == 0)
		{
			while(len-- > 0)
				buf.append(' ');
		}
		else if(len == 1)
			buf.append(' ');
		else
		{
			int count = (len + start % tabSize) / tabSize;
			if(count != 0)
				len += start;
			while(count-- > 0)
				buf.append('\t');
			count = len % tabSize;
			while(count-- > 0)
				buf.append(' ');
		}
		return buf.toString();
	} //}}}

	//{{{ getVirtualWidth() method
	/**
	 * Returns the virtual column number (taking tabs into account) of the
	 * specified offset in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 */
	public static int getVirtualWidth(Segment seg, int tabSize)
	{
		int virtualPosition = 0;

		for (int i = 0; i < seg.count; i++)
		{
			char ch = seg.array[seg.offset + i];

			if (ch == '\t')
			{
				virtualPosition += tabSize
					- virtualPosition % tabSize;
			}
			else
			{
				++virtualPosition;
			}
		}

		return virtualPosition;
	} //}}}

	//{{{ getOffsetOfVirtualColumn() method
	/**
	 * Returns the array offset of a virtual column number (taking tabs
	 * into account) in the segment.
	 *
	 * @param seg The segment
	 * @param tabSize The tab size
	 * @param column The virtual column number
	 * @param totalVirtualWidth If this array is non-null, the total
	 * virtual width will be stored in its first location if this method
	 * returns -1.
	 *
	 * @return -1 if the column is out of bounds
	 */
	public static int getOffsetOfVirtualColumn(Segment seg, int tabSize,
					    int column, int[] totalVirtualWidth)
	{
		int virtualPosition = 0;

		for (int i = 0; i < seg.count; i++)
		{
			char ch = seg.array[seg.offset + i];

			if (ch == '\t')
			{
				int tabWidth = tabSize
					- virtualPosition % tabSize;
				if(virtualPosition >= column)
					return i;
				else
					virtualPosition += tabWidth;
			}
			else
			{
				if(virtualPosition >= column)
					return i;
				else
					++virtualPosition;
			}
		}

		if(totalVirtualWidth != null)
			totalVirtualWidth[0] = virtualPosition;
		return -1;
	} //}}}

	//{{{ compareStrings() method
	/**
	 * Compares two strings.<p>
	 *
	 * Unlike <function>String.compareTo()</function>,
	 * this method correctly recognizes and handles embedded numbers.
	 * For example, it places "My file 2" before "My file 10".<p>
	 *
	 * @param str1 The first string
	 * @param str2 The second string
	 * @param ignoreCase If true, case will be ignored
	 * @return negative If str1 &lt; str2, 0 if both are the same,
	 * positive if str1 &gt; str2
	 * @since jEdit 4.3pre5
	 */
	public static int compareStrings(String str1, String str2, boolean ignoreCase)
	{
		char[] char1 = str1.toCharArray();
		char[] char2 = str2.toCharArray();

		int len = Math.min(char1.length,char2.length);

		for(int i = 0, j = 0; i < len && j < len; i++, j++)
		{
			char ch1 = char1[i];
			char ch2 = char2[j];
			if(Character.isDigit(ch1) && Character.isDigit(ch2)
				&& ch1 != '0' && ch2 != '0')
			{
				int _i = i + 1;
				int _j = j + 1;

				for(; _i < char1.length; _i++)
				{
					if(!Character.isDigit(char1[_i]))
					{
						//_i--;
						break;
					}
				}

				for(; _j < char2.length; _j++)
				{
					if(!Character.isDigit(char2[_j]))
					{
						//_j--;
						break;
					}
				}

				int len1 = _i - i;
				int len2 = _j - j;
				if(len1 > len2)
					return 1;
				else if(len1 < len2)
					return -1;
				else
				{
					for(int k = 0; k < len1; k++)
					{
						ch1 = char1[i + k];
						ch2 = char2[j + k];
						if(ch1 != ch2)
							return ch1 - ch2;
					}
				}

				i = _i - 1;
				j = _j - 1;
			}
			else
			{
				if(ignoreCase)
				{
					ch1 = Character.toLowerCase(ch1);
					ch2 = Character.toLowerCase(ch2);
				}

				if(ch1 != ch2)
					return ch1 - ch2;
			}
		}

		return char1.length - char2.length;
	} //}}}

	//{{{ StringCompare class
	/**
	 * Compares objects as strings.
	 */
	public static class StringCompare implements Comparator
		{
		public int compare(Object obj1, Object obj2)
		{
			return compareStrings(obj1.toString(),
				obj2.toString(),false);
		}
	} //}}}

	//{{{ objectsEqual() method
	/**
	 * Returns if two strings are equal. This correctly handles null pointers,
	 * as opposed to calling <code>o1.equals(o2)</code>.
	 * @since jEdit 4.3pre6
	 */
	public static boolean objectsEqual(Object o1, Object o2)
	{
		if(o1 == null)
		{
			if(o2 == null)
				return true;
			else
				return false;
		}
		else if(o2 == null)
			return false;
		else
			return o1.equals(o2);
	} //}}}

	//{{{ globToRE() method
	/**
	 * Converts a Unix-style glob to a regular expression.<p>
	 *
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 * @since jEdit 4.3pre7
	 */
	public static String globToRE(String glob)
	{
		final Object NEG = new Object();
		final Object GROUP = new Object();
		Stack state = new Stack();

		StringBuffer buf = new StringBuffer();
		boolean backslash = false;

		for(int i = 0; i < glob.length(); i++)
		{
			char c = glob.charAt(i);
			if(backslash)
			{
				buf.append('\\');
				buf.append(c);
				backslash = false;
				continue;
			}

			switch(c)
			{
			case '\\':
				backslash = true;
				break;
			case '?':
				buf.append('.');
				break;
			case '.':
			case '+':
			case '(':
			case ')':
				buf.append('\\');
				buf.append(c);
				break;
			case '*':
				buf.append(".*");
				break;
			case '|':
				if(backslash)
					buf.append("\\|");
				else
					buf.append('|');
				break;
			case '{':
				buf.append('(');
				if(i + 1 != glob.length() && glob.charAt(i + 1) == '!')
				{
					buf.append('?');
					state.push(NEG);
				}
				else
					state.push(GROUP);
				break;
			case ',':
				if(!state.isEmpty() && state.peek() == GROUP)
					buf.append('|');
				else
					buf.append(',');
				break;
			case '}':
				if(!state.isEmpty())
				{
					buf.append(")");
					if(state.pop() == NEG)
						buf.append(".*");
				}
				else
					buf.append('}');
				break;
			default:
				buf.append(c);
			}
		}

		return buf.toString();
	} //}}}

	//}}}

	private StandardUtilities(){}
}
