/*
 * TextUtilities.java - Various text functions
 * Copyright (C) 1998, 2005 Slava Pestov
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
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

//{{{ Imports
import java.util.*;
import javax.swing.text.Segment;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.*;
//}}}

/**
 * Contains several text manipulation methods.
 *
 * <ul>
 * <li>Bracket matching
 * <li>Word start and end offset calculation
 * <li>String comparison
 * <li>Converting tabs to spaces and vice versa
 * <li>Wrapping text
 * <li>String case conversion
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class TextUtilities
{
	// to avoid slowdown with large files; only scan 10000 lines either way
	public static final int BRACKET_MATCH_LIMIT = 10000;

	//{{{ getTokenAtOffset() method
	/**
	 * Returns the token that contains the specified offset.
	 * @param tokens The token list
	 * @param offset The offset
	 * @since jEdit 4.0pre3
	 */
	public static Token getTokenAtOffset(Token tokens, int offset)
	{
		if(offset == 0 && tokens.id == Token.END)
			return tokens;

		for(;;)
		{
			if(tokens.id == Token.END)
				throw new ArrayIndexOutOfBoundsException("offset > line length");

			if(tokens.offset + tokens.length > offset)
				return tokens;
			else
				tokens = tokens.next;
		}
	} //}}}

	//{{{ getComplementaryBracket() method
	/**
	 * Given an opening bracket, return the corresponding closing bracket
	 * and store true in <code>direction[0]</code>. Given a closing bracket,
	 * return the corresponding opening bracket and store false in
	 * <code>direction[0]</code>. Otherwise, return <code>\0</code>.
	 * @since jEdit 4.3pre2
	 */
	public static char getComplementaryBracket(char ch, boolean[] direction)
	{
		switch(ch)
		{
		case '(': direction[0] = true;  return ')';
		case ')': direction[0] = false; return '(';
		case '[': direction[0] = true;  return ']';
		case ']': direction[0] = false; return '[';
		case '{': direction[0] = true;  return '}';
		case '}': direction[0] = false; return '{';
		default:  return '\0';
		}
	} //}}}

	//{{{ findMatchingBracket() method
	/**
	 * Returns the offset of the bracket matching the one at the
	 * specified offset of the buffer, or -1 if the bracket is
	 * unmatched (or if the character is not a bracket).
	 * @param buffer The buffer
	 * @param line The line
	 * @param offset The offset within that line
	 * @since jEdit 2.6pre1
	 */
	public static int findMatchingBracket(JEditBuffer buffer, int line, int offset)
	{
		if(offset < 0 || offset >= buffer.getLineLength(line))
		{
			throw new ArrayIndexOutOfBoundsException(offset + ":"
				+ buffer.getLineLength(line));
		}

		Segment lineText = new Segment();
		buffer.getLineText(line,lineText);

		char c = lineText.array[lineText.offset + offset];
		// false - backwards, true - forwards
		boolean[] direction = new boolean[1];

		// corresponding character
		char cprime = getComplementaryBracket(c,direction);

		// 1 because we've already 'seen' the first bracket
		int count = 1;

		DefaultTokenHandler tokenHandler = new DefaultTokenHandler();
		buffer.markTokens(line,tokenHandler);

		// Get the syntax token at 'offset'
		// only tokens with the same type will be checked for
		// the corresponding bracket
		byte idOfBracket = getTokenAtOffset(tokenHandler.getTokens(),offset).id;

		boolean haveTokens = true;

		int startLine = line;

		//{{{ Forward search
		if(direction[0])
		{
			offset++;

			for(;;)
			{
				for(int i = offset; i < lineText.count; i++)
				{
					char ch = lineText.array[lineText.offset + i];
					if(ch == c)
					{
						if(!haveTokens)
						{
							tokenHandler.init();
							buffer.markTokens(line,tokenHandler);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenHandler.getTokens(),i).id == idOfBracket)
							count++;
					}
					else if(ch == cprime)
					{
						if(!haveTokens)
						{
							tokenHandler.init();
							buffer.markTokens(line,tokenHandler);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenHandler.getTokens(),i).id == idOfBracket)
						{
							count--;
							if(count == 0)
								return buffer.getLineStartOffset(line) + i;
						}
					}
				}

				//{{{ Go on to next line
				line++;
				if(line >= buffer.getLineCount() || (line - startLine) > BRACKET_MATCH_LIMIT)
					break;
				buffer.getLineText(line,lineText);
				offset = 0;
				haveTokens = false;
				//}}}
			}
		} //}}}
		//{{{ Backward search
		else
		{
			offset--;

			for(;;)
			{
				for(int i = offset; i >= 0; i--)
				{
					char ch = lineText.array[lineText.offset + i];
					if(ch == c)
					{
						if(!haveTokens)
						{
							tokenHandler.init();
							buffer.markTokens(line,tokenHandler);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenHandler.getTokens(),i).id == idOfBracket)
							count++;
					}
					else if(ch == cprime)
					{
						if(!haveTokens)
						{
							tokenHandler.init();
							buffer.markTokens(line,tokenHandler);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenHandler.getTokens(),i).id == idOfBracket)
						{
							count--;
							if(count == 0)
								return buffer.getLineStartOffset(line) + i;
						}
					}
				}

				//{{{ Go on to previous line
				line--;
				if(line < 0 || (startLine - line) > BRACKET_MATCH_LIMIT)
					break;
				buffer.getLineText(line,lineText);
				offset = lineText.count - 1;
				haveTokens = false;
				//}}}
			}
		} //}}}

		// Nothing found
		return -1;
	} //}}}

	//{{{ findWordStart() method
	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordStart(String line, int pos, String noWordSep)
	{
		return findWordStart(line, pos, noWordSep, true, false);
	} //}}}

	
	/** Similar to perl's join() method on lists,
	 *    but works with all collections.
	 * 
	 * @param c An iterable collection of Objects
	 * @param delim a string to put between each object
	 * @return a joined toString() representation of the collection
	 */
	public static String join(Collection c, String delim) {
		StringBuffer retval = new StringBuffer();
		Iterator itr = c.iterator();
		if (itr.hasNext()) {
			retval.append( itr.next().toString() );
		}
		else return "";
		while (itr.hasNext()) {
			retval.append(delim);
			retval.append(itr.next().toString());
		}
		return retval.toString();
	}
	
	//{{{ findWordStart() method
	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param joinNonWordChars Treat consecutive non-alphanumeric
	 * characters as one word
	 * @since jEdit 4.2pre5
	 */
	public static int findWordStart(String line, int pos, String noWordSep,
		boolean joinNonWordChars)
	{
		return findWordStart(line,pos,noWordSep,joinNonWordChars,false);
	} //}}}

	//{{{ findWordStart() method
	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param joinNonWordChars Treat consecutive non-alphanumeric
	 * characters as one word
	 * @param eatWhitespace Include whitespace at start of word
	 * @since jEdit 4.1pre2
	 */
	public static int findWordStart(String line, int pos, String noWordSep,
		boolean joinNonWordChars, boolean eatWhitespace)
	{
		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";

		//{{{ the character under the cursor changes how we behave.
		int type;
		if(Character.isWhitespace(ch))
			type = WHITESPACE;
		else if(Character.isLetterOrDigit(ch)
			|| noWordSep.indexOf(ch) != -1)
			type = WORD_CHAR;
		else
			type = SYMBOL;
		//}}}

loop:		for(int i = pos; i >= 0; i--)
		{
			ch = line.charAt(i);
			switch(type)
			{
			//{{{ Whitespace...
			case WHITESPACE:
				// only select other whitespace in this case
				if(Character.isWhitespace(ch))
					break;
				// word char or symbol; stop
				else
					return i + 1; //}}}
			//{{{ Word character...
			case WORD_CHAR:
				// word char; keep going
				if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					break;
				}
				// whitespace; include in word if eating
				else if(Character.isWhitespace(ch)
					&& eatWhitespace)
				{
					type = WHITESPACE;
					break;
				}
				else
					return i + 1; //}}}
			//{{{ Symbol...
			case SYMBOL:
				if(!joinNonWordChars && pos != i)
					return i + 1;

				// whitespace; include in word if eating
				if(Character.isWhitespace(ch))
				{
					if(eatWhitespace)
					{
						type = WHITESPACE;
						break;
					}
					else
						return i + 1;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					return i + 1;
				}
				else
				{
					break;
				} //}}}
			}
		}

		return 0;
	} //}}}

	//{{{ findWordEnd() method
	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordEnd(String line, int pos, String noWordSep)
	{
		return findWordEnd(line, pos, noWordSep, true);
	} //}}}

	//{{{ findWordEnd() method
	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param joinNonWordChars Treat consecutive non-alphanumeric
	 * characters as one word
	 * @since jEdit 4.1pre2
	 */
	public static int findWordEnd(String line, int pos, String noWordSep,
		boolean joinNonWordChars)
	{
		return findWordEnd(line,pos,noWordSep,joinNonWordChars,false);
	} //}}}

	//{{{ findWordEnd() method
	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param joinNonWordChars Treat consecutive non-alphanumeric
	 * characters as one word
	 * @param eatWhitespace Include whitespace at end of word
	 * @since jEdit 4.2pre5
	 */
	public static int findWordEnd(String line, int pos, String noWordSep,
		boolean joinNonWordChars, boolean eatWhitespace)
	{
		if(pos != 0)
			pos--;

		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";

		//{{{ the character under the cursor changes how we behave.
		int type;
		if(Character.isWhitespace(ch))
			type = WHITESPACE;
		else if(Character.isLetterOrDigit(ch)
			|| noWordSep.indexOf(ch) != -1)
			type = WORD_CHAR;
		else
			type = SYMBOL;
		//}}}

loop:		for(int i = pos; i < line.length(); i++)
		{
			ch = line.charAt(i);
			switch(type)
			{
			//{{{ Whitespace...
			case WHITESPACE:
				// only select other whitespace in this case
				if(Character.isWhitespace(ch))
					break;
				else
					return i; //}}}
			//{{{ Word character...
			case WORD_CHAR:
				if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					break;
				}
				// whitespace; include in word if eating
				else if(Character.isWhitespace(ch)
					&& eatWhitespace)
				{
					type = WHITESPACE;
					break;
				}
				else
					return i; //}}}
			//{{{ Symbol...
			case SYMBOL:
				if(!joinNonWordChars && i != pos)
					return i;

				// if we see whitespace, set flag.
				if(Character.isWhitespace(ch))
				{
					if(eatWhitespace)
					{
						type = WHITESPACE;
						break;
					}
					else
						return i;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					return i;
				}
				else
				{
					break;
				} //}}}
			}
		}

		return line.length();
	} //}}}

	//{{{ spacesToTabs() method
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
						.createWhiteSpace(whitespace,tabSize,
						width - whitespace));
				}
				whitespace = 0;
				width = 0;
				buf.append('\n');
				break;
			default:
				if(whitespace != 0)
				{
					buf.append(MiscUtilities
						.createWhiteSpace(whitespace,tabSize,
						width - whitespace));
					whitespace = 0;
				}
				buf.append(in.charAt(i));
				width++;
				break;
			}
		}

		if(whitespace != 0)
		{
			buf.append(MiscUtilities.createWhiteSpace(whitespace,tabSize,
				width - whitespace));
		}

                return buf.toString();
	} //}}}

	//{{{ tabsToSpaces() method
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
	} //}}}

	//{{{ format() method
	/**
	 * Formats the specified text by merging and breaking lines to the
	 * specified width.
	 * @param text The text
	 * @param maxLineLength The maximum line length
	 * @param tabSize The tab size
	 */
	public static String format(String text, int maxLineLength, int tabSize)
	{
		StringBuffer buf = new StringBuffer();

		int index = 0;

		for(;;)
		{
			int newIndex = text.indexOf("\n\n",index);
			if(newIndex == -1)
				break;

			formatParagraph(text.substring(index,newIndex),
				maxLineLength,tabSize,buf);
			buf.append("\n\n");
			index = newIndex + 2;
		}

		if(index != text.length())
		{
			formatParagraph(text.substring(index),
				maxLineLength,tabSize,buf);
		}

		return buf.toString();
	} //}}}

	//{{{ indexIgnoringWhitespace() method
	/**
	 * Inverse of <code>ignoringWhitespaceIndex()</code>.
	 * @param str A string
	 * @param index The index
	 * @return The number of non-whitespace characters that precede the index.
	 * @since jEdit 4.3pre2
	 */
	public static int indexIgnoringWhitespace(String str, int index)
	{
		int j = 0;
		for(int i = 0; i < index; i++)
			if(!Character.isWhitespace(str.charAt(i))) j++;
		return j;
	} //}}}

	//{{{ ignoringWhitespaceIndex() method
	/**
	 * Inverse of <code>indexIgnoringWhitespace()</code>.
	 * @param str A string
	 * @param index The index
	 * @return The index into the string where the number of non-whitespace
	 * characters that precede the index is count.
	 * @since jEdit 4.3pre2
	 */
	public static int ignoringWhitespaceIndex(String str, int index)
	{
		int j = 0;
		for(int i = 0;;i++)
		{
			if(!Character.isWhitespace(str.charAt(i))) j++;

			if(j > index)
				return i;
			if(i == str.length() - 1)
				return i + 1;
		}
	} //}}}

	//{{{ getStringCase() method
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
	} //}}}

	//{{{ toTitleCase() method
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
	} //}}}

	//{{{ Private members
	private static final int WHITESPACE = 0;
	private static final int WORD_CHAR = 1;
	private static final int SYMBOL = 2;

	//{{{ formatParagraph() method
	private static void formatParagraph(String text, int maxLineLength,
		int tabSize, StringBuffer buf)
	{
		// align everything to paragraph's leading indent
		int leadingWhitespaceCount = MiscUtilities.getLeadingWhiteSpace(text);
		String leadingWhitespace = text.substring(0,leadingWhitespaceCount);
		int leadingWhitespaceWidth = MiscUtilities.getLeadingWhiteSpaceWidth(text,tabSize);

		buf.append(leadingWhitespace);

		int lineLength = leadingWhitespaceWidth;
		StringTokenizer st = new StringTokenizer(text);
		while(st.hasMoreTokens())
		{
			String word = st.nextToken();
			if(lineLength == leadingWhitespaceWidth)
			{
				// do nothing
			}
			else if(lineLength + word.length() + 1 > maxLineLength)
			{
				buf.append('\n');
				buf.append(leadingWhitespace);
				lineLength = leadingWhitespaceWidth;
			}
			else
			{
				buf.append(' ');
				lineLength++;
			}
			buf.append(word);
			lineLength += word.length();
		}
	} //}}}
	
	//{{{ indexIgnoringWhitespace() method
	public static void indexIgnoringWhitespace(String text, int maxLineLength,
		int tabSize, StringBuffer buf)
	{
		// align everything to paragraph's leading indent
		int leadingWhitespaceCount = MiscUtilities.getLeadingWhiteSpace(text);
		String leadingWhitespace = text.substring(0,leadingWhitespaceCount);
		int leadingWhitespaceWidth = MiscUtilities.getLeadingWhiteSpaceWidth(text,tabSize);

		buf.append(leadingWhitespace);

		int lineLength = leadingWhitespaceWidth;
		StringTokenizer st = new StringTokenizer(text);
		while(st.hasMoreTokens())
		{
			String word = st.nextToken();
			if(lineLength == leadingWhitespaceWidth)
			{
				// do nothing
			}
			else if(lineLength + word.length() + 1 > maxLineLength)
			{
				buf.append('\n');
				buf.append(leadingWhitespace);
				lineLength = leadingWhitespaceWidth;
			}
			else
			{
				buf.append(' ');
				lineLength++;
			}
			buf.append(word);
			lineLength += word.length();
		}
	} //}}}

	//}}}
}
