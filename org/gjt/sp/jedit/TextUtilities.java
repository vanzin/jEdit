/*
 * TextUtilities.java - Various text functions
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import javax.swing.text.*;
import org.gjt.sp.jedit.syntax.*;

/**
 * Class with several text utility functions.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextUtilities
{
	/**
	 * Returns the offset of the bracket matching the one at the
	 * specified offset of the buffer, or -1 if the bracket is
	 * unmatched (or if the character is not a bracket).
	 * @param buffer The buffer
	 * @param line The line
	 * @param offset The offset within that line
	 * @exception BadLocationException If an out-of-bounds access
	 * was attempted on the buffer's text
	 * @since jEdit 2.6pre1
	 */
	public static int findMatchingBracket(Buffer buffer, int line, int offset)
		throws BadLocationException
	{
		return findMatchingBracket(buffer,line,offset,0,
			buffer.getDefaultRootElement().getElementCount());
	}

	/**
	 * Returns the offset of the bracket matching the one at the
	 * specified offset of the buffer, or -1 if the bracket is
	 * unmatched (or if the character is not a bracket).
	 * @param buffer The buffer
	 * @param line The line
	 * @param offset The offset within that line
	 * @param startLine The first line to scan. This is used to speed up
	 * on-screen bracket matching because only visible lines need to be
	 * scanned
	 * @param endLine The last line to scan. This is used to speed up
	 * on-screen bracket matching because only visible lines need to be
	 * scanned
	 * @exception BadLocationException If an out-of-bounds access
	 * was attempted on the buffer's text
	 * @since jEdit 2.7pre3
	 */
	public static int findMatchingBracket(Buffer buffer, int line, int offset,
		int startLine, int endLine) throws BadLocationException
	{
		if(buffer.getLength() == 0)
			return -1;

		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(line);
		Segment lineText = new Segment();
		int lineStart = lineElement.getStartOffset();
		buffer.getText(lineStart,lineElement.getEndOffset() - lineStart - 1,
			lineText);

		char c = lineText.array[lineText.offset + offset];
		char cprime; // c` - corresponding character
		boolean direction; // true = back, false = forward

		switch(c)
		{
		case '(': cprime = ')'; direction = false; break;
		case ')': cprime = '('; direction = true; break;
		case '[': cprime = ']'; direction = false; break;
		case ']': cprime = '['; direction = true; break;
		case '{': cprime = '}'; direction = false; break;
		case '}': cprime = '{'; direction = true; break;
		default: return -1;
		}

		int count;

		// Get the syntax token at 'offset'
		// only tokens with the same type will be checked for
		// the corresponding bracket
		byte idOfBracket = Token.NULL;

		Buffer.TokenList tokenList = buffer.markTokens(line);
		Token lineTokens = tokenList.getFirstToken();

		int tokenListOffset = 0;
		for(;;)
		{
			if(lineTokens.id == Token.END)
				throw new InternalError("offset > line length");
	
			if(tokenListOffset + lineTokens.length > offset)
			{
				idOfBracket = lineTokens.id;
				break;
			}
			else
			{
				tokenListOffset += lineTokens.length;
				lineTokens = lineTokens.next;
			}
		}

		if(direction)
		{
			// scan backwards

			count = 0;

			for(int i = line; i >= startLine; i--)
			{
				// get text
				lineElement = map.getElement(i);
				lineStart = lineElement.getStartOffset();
				int lineLength = lineElement.getEndOffset()
					- lineStart - 1;

				buffer.getText(lineStart,lineLength,lineText);

				int scanStartOffset;
				if(i != line)
				{
					lineTokens = buffer.markTokens(i).getLastToken();
					tokenListOffset = scanStartOffset = lineLength - 1;
				}
				else
				{
 					if(tokenListOffset != lineLength)
 						tokenListOffset += lineTokens.length;
					scanStartOffset = offset;
					//System.err.println("sso=" + scanStartOffset + ",tlo=" + tokenListOffset);
				}

				// only check tokens with id 'idOfBracket'
				while(lineTokens != null)
				{
					byte id = lineTokens.id;
					if(id == Token.END)
					{
						lineTokens = lineTokens.prev;
						continue;
					}

					int len = lineTokens.length;
					if(id == idOfBracket)
					{
						StringBuffer sb = new StringBuffer();
						for(int j = scanStartOffset; j >= Math.max(0,tokenListOffset - len); j--)
						{
							if(j >= lineText.count)
								System.err.println("WARNING: " + j + " >= " + lineText.count);
							else if(j < 0)
							{
								System.err.println("sso=" + scanStartOffset + ", tlo=" + tokenListOffset + ",len=" + len);
								System.err.println("WARNING: " + j + " < 0");
							}

							char ch = lineText.array[lineText.offset + j];
							sb.append(ch);
							if(ch == c)
								count++;
							else if(ch == cprime)
							{
								if(--count == 0)
									return lineStart + j;
							}
						}
						//System.err.println("[" + sb.reverse() + "]");
					}

					scanStartOffset = tokenListOffset = tokenListOffset - len;
					lineTokens = lineTokens.prev;
				}
			}
		}
		else
		{
			// scan forwards

			count = 0;

			for(int i = line; i < endLine; i++)
			{
				// get text
				lineElement = map.getElement(i);
				lineStart = lineElement.getStartOffset();
				buffer.getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,lineText);

				int scanStartOffset;
				if(i != line)
				{
					lineTokens = buffer.markTokens(i).getFirstToken();
					tokenListOffset = 0;
					scanStartOffset = 0;
				}
				else
					scanStartOffset = offset + 1;

				// only check tokens with id 'idOfBracket'
				for(;;)
				{
					byte id = lineTokens.id;
					if(id == Token.END)
						break;

					int len = lineTokens.length;
					if(id == idOfBracket)
					{
						for(int j = scanStartOffset; j < tokenListOffset + len; j++)
						{
							char ch = lineText.array[lineText.offset + j];
							if(ch == c)
								count++;
							else if(ch == cprime)
							{
								if(count-- == 0)
									return lineStart + j;
							}
						}
					}

					scanStartOffset = tokenListOffset = tokenListOffset + len;
					lineTokens = lineTokens.next;
				}
			}
		}

		// Nothing found
		return -1;
	}

	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordStart(String line, int pos, String noWordSep)
	{
		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";
		boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1);

		int wordStart = 0;
		for(int i = pos; i >= 0; i--)
		{
			ch = line.charAt(i);
			if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordStart = i + 1;
				break;
			}
		}

		return wordStart;
	}

	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordEnd(String line, int pos, String noWordSep)
	{
		if(pos != 0)
			pos--;

		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";
		boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1);

		int wordEnd = line.length();
		for(int i = pos; i < line.length(); i++)
		{
			ch = line.charAt(i);
			if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordEnd = i;
				break;
			}
		}
		return wordEnd;
	}

	/**
	 * Checks if a subregion of a <code>Segment</code> is equal to a
	 * character array.
	 * @param ignoreCase True if case should be ignored, false otherwise
	 * @param text The segment
	 * @param offset The offset into the segment
	 * @param match The character array to match
	 * @since jEdit 2.7pre1
	 */
	public static boolean regionMatches(boolean ignoreCase, Segment text,
					    int offset, char[] match)
	{
		int length = offset + match.length;
		char[] textArray = text.array;
		if(length > text.offset + text.count)
			return false;
		for(int i = offset, j = 0; i < length; i++, j++)
		{
			char c1 = textArray[i];
			char c2 = match[j];
			if(ignoreCase)
			{
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
			}
			if(c1 != c2)
				return false;
		}
		return true;
	}

	/**
	 * Converts consecutive spaces to tabs in the specified string.
	 * @param in The string
	 * @param tabSize The tab size
	 */
	public static String spacesToTabs(String in, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		int width = 0;
		int whitespace = 0;
		for(int i = 0; i < in.length(); i++)
		{
			switch(in.charAt(i))
			{
			case ' ':
				whitespace++;
				width++;
				break;
			case '\t':
				int tab = tabSize - (width % tabSize);
				width += tab;
				whitespace += tab;
				break;
			case '\n':
				if(whitespace != 0)
				{
					buf.append(MiscUtilities
						.createWhiteSpace(whitespace,tabSize));
				}
				whitespace = 0;
				width = 0;
				buf.append('\n');
				break;
			default:
				if(whitespace != 0)
				{
					buf.append(MiscUtilities
						.createWhiteSpace(whitespace,tabSize));
					whitespace = 0;
				}
				buf.append(in.charAt(i));
				width++;
				break;
			}
		}

		if(whitespace != 0)
		{
			buf.append(MiscUtilities.createWhiteSpace(whitespace,tabSize));
		}

                return buf.toString();
	}

	/**
	 * Converts tabs to consecutive spaces in the specified string.
	 * @param in The string
	 * @param tabSize The tab size
	 */
	public static String tabsToSpaces(String in, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		int width = 0;
		for(int i = 0; i < in.length(); i++)
		{
			switch(in.charAt(i))
			{
			case '\t':
				int count = tabSize - (width % tabSize);
				width += count;
				while(--count >= 0)
					buf.append(' ');
				break;
			case '\n':
				width = 0;
				buf.append(in.charAt(i));
				break;
			default:
				width++;
				buf.append(in.charAt(i));
				break;
                        }
                }
                return buf.toString();
	}

	/**
	 * Formats the specified text by merging and breaking lines to the
	 * specified width.
	 * @param text The text
	 * @param maxLineLen The maximum line length
	 */
	public static String format(String text, int maxLineLength)
	{
		StringBuffer buf = new StringBuffer();
		StringBuffer word = new StringBuffer();
		int lineLength = 0;
		boolean newline = true;
		boolean space = false;
		char[] chars = text.toCharArray();
		for(int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			switch(c)
			{
			case '\n':
				if(i == 0 || chars.length - i <= 2)
				{
					if(lineLength + word.length() >= maxLineLength)
						buf.append('\n');
					else if(space && word.length() != 0)
						buf.append(' ');
					buf.append(word);
					word.setLength(0);
					buf.append('\n');
					newline = true;
					space = false;
					break;
				}
				else if(newline)
				{
					if(lineLength + word.length() >= maxLineLength)
						buf.append('\n');
					else if(space && word.length() != 0)
						buf.append(' ');
					buf.append(word);
					word.setLength(0);
					buf.append("\n\n");
					newline = space = false;
					lineLength = 0;
					break;
				}
				else
					newline = true;
			case ' ':
				if(lineLength + word.length() >= maxLineLength)
				{
					buf.append('\n');
					lineLength = 0;
					newline = true;
				}
				else if(space && lineLength != 0 && word.length() != 0)
				{
					buf.append(' ');
					lineLength++;
					space = false;
				}
				else
					space = true;
				buf.append(word);
				lineLength += word.length();
				word.setLength(0);
				break;
			default:
				newline = false;
				// without this test, we would have spaces
				// at the start of lines
				if(lineLength != 0)
					space = true;
				word.append(c);
				break;
			}
		}
		if(lineLength + word.length() >= maxLineLength)
			buf.append('\n');
		else if(space && word.length() != 0)
			buf.append(' ');
		buf.append(word);
		return buf.toString();
	}

	public static final int MIXED = 0;
	public static final int LOWER_CASE = 1;
	public static final int UPPER_CASE = 2;
	public static final int TITLE_CASE = 3;

	/**
	 * Returns if the specified string is all upper case, all lower case,
	 * or title case (first letter upper case, rest lower case).
	 * @param str The string
	 * @since jEdit 4.0pre1
	 */
	public static int getStringCase(String str)
	{
		if(str.length() == 0)
			return MIXED;

		int state = -1;

		char ch = str.charAt(0);
		if(Character.isLetter(ch))
		{
			if(Character.isUpperCase(ch))
				state = UPPER_CASE;
			else
				state = LOWER_CASE;
		}

		for(int i = 1; i < str.length(); i++)
		{
			ch = str.charAt(i);
			if(!Character.isLetter(ch))
				continue;

			switch(state)
			{
			case UPPER_CASE:
				if(Character.isLowerCase(ch))
				{
					if(i == 1)
						state = TITLE_CASE;
					else
						return MIXED;
				}
				break;
			case LOWER_CASE:
			case TITLE_CASE:
				if(Character.isUpperCase(ch))
					return MIXED;
				break;
			}
		}

		return state;
	}

	/**
	 * Converts the specified string to title case, by capitalizing the
	 * first letter.
	 * @param str The string
	 * @since jEdit 4.0pre1
	 */
	public static String toTitleCase(String str)
	{
		if(str.length() == 0)
			return str;
		else
		{
			return Character.toUpperCase(str.charAt(0))
				+ str.substring(1).toLowerCase();
		}
	}
}
