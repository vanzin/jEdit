/*
 * TextUtilities.java - Various text functions
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.*;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import org.gjt.sp.jedit.syntax.*;
//}}}

/**
 * Class with several text utility functions.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextUtilities
{
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

		int tokenListOffset = 0;
		for(;;)
		{
			if(tokens.id == Token.END)
				throw new ArrayIndexOutOfBoundsException("offset > line length");

			if(tokenListOffset + tokens.length > offset)
				return tokens;
			else
			{
				tokenListOffset += tokens.length;
				tokens = tokens.next;
			}
		}
	} //}}}

	//{{{ lineToChunkList() method
	/**
	 * Converts a line of text into one or more chunk lists. There will be
	 * one chunk list if soft wrap is disabled, more than one otherwise.
	 * @param seg The segment containing line text
	 * @param tokens The line's syntax tokens
	 * @param styles The styles to highlight the line with
	 * @param fontRenderContext Text transform, anti-alias, fractional font
	 * metrics
	 * @param e Used for calculating tab offsets
	 * @param wrapMargin The wrap margin width, in pixels. 0 disables
	 * @param out All resulting chunk lists will be appended to this list
	 * @since jEdit 4.0pre4
	 */
	public static void lineToChunkList(Segment seg, Token tokens,
		SyntaxStyle[] styles, FontRenderContext fontRenderContext,
		TabExpander e, float wrapMargin, java.util.List out)
	{
		float x = 0.0f;
		boolean seenNonWhiteSpace = false;
		float firstNonWhiteSpace = 0.0f;

		Chunk first = null;
		Chunk current = null;

		int tokenListOffset = 0;

		while(tokens.id != Token.END)
		{
			int flushIndex = tokenListOffset;

			for(int i = tokenListOffset; i < tokenListOffset + tokens.length; i++)
			{
				char ch = seg.array[seg.offset + i];

				if(ch == ' ' || ch == '\t')
				{
					if(i != flushIndex)
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,flushIndex,
							i,styles,fontRenderContext);
						if(wrapMargin != 0
							&& x + newChunk.width
							> wrapMargin)
						{
							if(first != null)
								out.add(first);
							first = null;
							newChunk.x = firstNonWhiteSpace;
							x = firstNonWhiteSpace
								+ newChunk.width;
						}
						else
						{
							newChunk.x = x;
							x += newChunk.width;
						}

						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						seenNonWhiteSpace = true;
					}

					if(ch == ' ')
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,i,i + 1,
							styles,fontRenderContext);
						newChunk.x = x;
						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						x += current.width;
					}
					else if(ch == '\t')
					{
						Chunk newChunk = new Chunk(
							tokens.id,seg,i,i,
							styles,fontRenderContext);
						newChunk.x = x;
						if(first == null)
							first = current = newChunk;
						else
						{
							current.next = newChunk;
							current = newChunk;
						}

						x = e.nextTabStop(x,i + tokenListOffset);
						current.width = x - current.x;
						current.length = 1;
					}

					if(!seenNonWhiteSpace)
						firstNonWhiteSpace = x;

					if(first == null)
						first = current;

					flushIndex = i + 1;
				}
				else if(i == tokenListOffset + tokens.length - 1)
				{
					Chunk newChunk = new Chunk(
						tokens.id,seg,flushIndex,
						i + 1,styles,fontRenderContext);

					if(i == seg.count - 1 && wrapMargin != 0
						&& x + newChunk.width > wrapMargin)
					{
						if(first != null)
							out.add(first);
						first = null;
						x = newChunk.width;
					}
					else
					{
						newChunk.x = x;
						x += newChunk.width;
					}

					if(first == null)
						first = current = newChunk;
					else
					{
						current.next = newChunk;
						current = newChunk;
					}
				}
			}

			tokenListOffset += tokens.length;
			tokens = tokens.next;
		}

		if(first != null)
			out.add(first);
	} //}}}

	//{{{ paintChunkList() method
	/**
	 * Paints a chunk list.
	 * @param chunks The chunk list
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @param width The width of the painting area, used for a token
	 * background color hack
	 * @return The width of the painted text
	 * @since jEdit 4.0pre4
	 */
	public static float paintChunkList(Chunk chunks, Graphics2D gfx,
		float x, float y, float width)
	{
		float _x = 0.0f;

		Font lastFont = null;
		Color lastColor = null;

		while(chunks != null)
		{
			if(chunks.text != null)
			{
				Font font = chunks.style.getFont();
				Color bgColor = chunks.style.getBackgroundColor();
				if(bgColor != null)
				{
					float x1 = (_x == 0.0f ? x : x + chunks.x);
					float x2 = (chunks.next == null
						? width
						: x + chunks.x + chunks.width);

					//LineMetrics lm = font.getLineMetrics(
					//	chunks.text,gfx.getFontRenderContext());
					FontMetrics fm = gfx.getFontMetrics();
					gfx.setColor(bgColor);
					gfx.fill(new Rectangle2D.Float(
						x1,y - fm.getAscent(),
						x2 - x1,fm.getAscent()));
				}

				gfx.setFont(font);
				gfx.setColor(chunks.style.getForegroundColor());

				gfx.drawString(chunks.text,x + chunks.x,y);

				// Useful for debugging purposes
				//gfx.draw(new Rectangle2D.Float(x + chunks.x,y - 10,
				//	chunks.width,10));
			}

			_x = chunks.x + chunks.width;
			chunks = chunks.next;
		}

		return _x;
	} //}}}

	//{{{ offsetToX() method
	/**
	 * Converts an offset in a chunk list into an x co-ordinate.
	 * @param chunks The chunk list
	 * @param offset The offset
	 * @since jEdit 4.0pre4
	 */
	public static float offsetToX(Chunk chunks, int offset)
	{
		float x = 0.0f;

		while(chunks != null)
		{
			if(offset < chunks.offset + chunks.length)
			{
				if(chunks.text == null)
					return chunks.x;
				else
				{
					return chunks.x + chunks.positions[
						(offset - chunks.offset) * 2];
				}
			}

			x = chunks.x + chunks.width;
			chunks = chunks.next;
		}

		return x;
	} //}}}

	//{{{ xToOffset() method
	/**
	 * Converts an x co-ordinate in a chunk list into an offset.
	 * @param chunks The chunk list
	 * @param x The x co-ordinate
	 * @param round Round up to next letter if past the middle of a letter?
	 * @since jEdit 4.0pre4
	 */
	public static int xToOffset(Chunk chunks, float x, boolean round)
	{
		int length = 0;

		while(chunks != null)
		{
			if(x < chunks.x + chunks.width)
			{
				if(chunks.text == null)
				{
					if(round && chunks.x + chunks.width - x < x - chunks.x)
						return chunks.offset + chunks.length;
					else
						return chunks.offset;
				}
				else
				{
					float _x = x - chunks.x;

					for(int i = 0; i < chunks.length; i++)
					{
						float glyphX = chunks.positions[i*2];
						float nextX = (i == chunks.length - 1
							? chunks.width
							: chunks.positions[i*2+2]);

						if(nextX > _x)
						{
							if(round && nextX - _x > _x - glyphX)
								return chunks.offset + i;
							else
								return chunks.offset + i + 1;
						}
					}
				}
			}

			length = chunks.offset + chunks.length;
			chunks = chunks.next;
		}

		return length;
	} //}}}

	//{{{ Chunk class
	/**
	 * A linked-list useful for painting syntax highlighted text and
	 * calculating offsets.
	 * @since jEdit 4.0pre4
	 */
	public static class Chunk
	{
		public float x;
		public float width;
		public SyntaxStyle style;
		public int offset;
		public int length;
		public String text;
		public float[] positions;

		public Chunk next;

		Chunk(int tokenType, Segment seg, int offset, int end,
			SyntaxStyle[] styles, FontRenderContext fontRenderContext)
		{
			style = styles[tokenType];

			if(offset != end)
			{
				length = end - offset;
				text = new String(seg.array,seg.offset + offset,length);

				GlyphVector vector = style.getFont().createGlyphVector(
					fontRenderContext,text);
				width = (float)vector.getLogicalBounds().getWidth();
				positions = vector.getGlyphPositions(0,length,null);
			}

			this.offset = offset;
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
	public static int findMatchingBracket(Buffer buffer, int line, int offset)
	{
		return findMatchingBracket(buffer,line,offset,0,
			buffer.getLineCount() - 1);
	} //}}}

	//{{{ findMatchingBracket() method
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
	 * @since jEdit 2.7pre3
	 */
	public static int findMatchingBracket(Buffer buffer, int line, int offset,
		int startLine, int endLine)
	{
		if(buffer.getLength() == 0)
			return -1;

		Segment lineText = new Segment();
		buffer.getLineText(line,lineText);

		char c = lineText.array[lineText.offset + offset];
		char cprime; // corresponding character
		boolean direction; // false - backwards, true - forwards

		switch(c)
		{
		case '(': cprime = ')'; direction = true;  break;
		case ')': cprime = '('; direction = false; break;
		case '[': cprime = ']'; direction = true;  break;
		case ']': cprime = '['; direction = false; break;
		case '{': cprime = '}'; direction = true;  break;
		case '}': cprime = '{'; direction = false; break;
		default: return -1;
		}

		// 1 because we've already 'seen' the first bracket
		int count = 1;

		Buffer.TokenList tokenList = buffer.markTokens(line);

		// Get the syntax token at 'offset'
		// only tokens with the same type will be checked for
		// the corresponding bracket
		byte idOfBracket = getTokenAtOffset(tokenList.getFirstToken(),offset).id;

		boolean haveTokens = true;

		//{{{ Forward search
		if(direction)
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
							tokenList = buffer.markTokens(line);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenList.getFirstToken(),i).id == idOfBracket)
							count++;
					}
					else if(ch == cprime)
					{
						if(!haveTokens)
						{
							tokenList = buffer.markTokens(line);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenList.getFirstToken(),i).id == idOfBracket)
						{
							count--;
							if(count == 0)
								return buffer.getLineStartOffset(line) + i;
						}
					}
				}

				//{{{ Go on to next line
				line++;
				if(line > endLine)
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
							tokenList = buffer.markTokens(line);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenList.getFirstToken(),i).id == idOfBracket)
							count++;
					}
					else if(ch == cprime)
					{
						if(!haveTokens)
						{
							tokenList = buffer.markTokens(line);
							haveTokens = true;
						}
						if(getTokenAtOffset(tokenList.getFirstToken(),i).id == idOfBracket)
						{
							count--;
							if(count == 0)
								return buffer.getLineStartOffset(line) + i;
						}
					}
				}

				//{{{ Go on to next line
				line--;
				if(line < startLine)
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
		return findWordStart(line,pos,noWordSep,false);
	} //}}}

	//{{{ findWordStart() method
	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param whiteSpace If true, any whitespace at the end of the
	 * word is also included
	 * @since jEdit 4.0pre3
	 */
	public static int findWordStart(String line, int pos, String noWordSep,
		boolean whiteSpace)
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

		boolean seenWhiteSpace = false;
		int whiteSpaceEnd = 0;
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
				else
					return i + 1; //}}}
			//{{{ Word character...
			case WORD_CHAR:
				// if we see whitespace, set flag.
				if(Character.isWhitespace(ch) && whiteSpace)
				{
					if(!seenWhiteSpace)
						whiteSpaceEnd = i + 1;
					seenWhiteSpace = true;
					break;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					// next word?
					if(seenWhiteSpace)
						return i + 1;
					else
						break;
				}
				else
					return i + 1; //}}}
			//{{{ Symbol...
			case SYMBOL:
				// if we see whitespace, set flag.
				if(Character.isWhitespace(ch))
				{
					if(whiteSpace)
					{
						if(!seenWhiteSpace)
							whiteSpaceEnd = i + 1;
						seenWhiteSpace = true;
						break;
					}
					else
						return i + 1;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
					return i + 1;
				else
				{
					// next word?
					if(seenWhiteSpace)
						return i + 1;
					else
						break;
				} //}}}
			}
		}

		return whiteSpaceEnd;
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
		return findWordEnd(line,pos,noWordSep,false);
	} //}}}

	//{{{ findWordEnd() method
	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 * @param whiteSpace If true, any whitespace at the start of the
	 * word is also included
	 * @since jEdit 4.0pre3
	 */
	public static int findWordEnd(String line, int pos, String noWordSep,
		boolean whiteSpace)
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

		boolean seenWhiteSpace = false;
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
				// if we see whitespace, set flag.
				if(Character.isWhitespace(ch) && whiteSpace)
				{
					seenWhiteSpace = true;
					break;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
				{
					// next word?
					if(seenWhiteSpace)
						return i;
					else
						break;
				}
				else
					return i; //}}}
			//{{{ Symbol...
			case SYMBOL:
				// if we see whitespace, set flag.
				if(Character.isWhitespace(ch))
				{
					if(whiteSpace)
					{
						seenWhiteSpace = true;
						break;
					}
					else
						return i;
				}
				else if(Character.isLetterOrDigit(ch) ||
					noWordSep.indexOf(ch) != -1)
					return i;
				else
				{
					// next word?
					if(seenWhiteSpace)
						return i;
					else
						break;
				} //}}}
			}
		}

		return line.length();
	} //}}}

	//{{{ regionMatches() method
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
	//}}}
}
