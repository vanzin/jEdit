/*
 * SyntaxCharacterIterator.java - Painting text using Java2D
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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
package org.gjt.sp.jedit.syntax;

//{{{ Imports
import javax.swing.text.Segment;
import java.awt.font.*;
import java.text.*;
import java.util.*;
//}}}

public class SyntaxCharacterIterator implements AttributedCharacterIterator
{
	//{{{ SyntaxCharacterIterator constructor
	public SyntaxCharacterIterator(char[] array, int offset, int count,
		int lineStartOffset, SyntaxStyle[] styles, Token tokens)
	{
		this.array = array;
		this.offset = offset;
		this.count = count;
		this.lineStartOffset = lineStartOffset;
		this.styles = styles;
		this.tokens = tokens;

		updateToken();
	} //}}}

	//{{{ CharacterIterator implementation

	//{{{ first() method
	public char first()
	{
		if(count == 0)
			return DONE;
		else
		{
			pos = 0;
			updateToken();
			return array[offset];
		}
	} //}}}

	//{{{ last() method
	public char last()
	{
		if(count == 0)
			return DONE;
		else
		{
			pos = count - 1;
			updateToken();
			return array[offset + pos];
		}
	} //}}}

	//{{{ current() method
	public char current()
	{
		if(pos >= count)
			return DONE;
		else
			return array[offset + pos];
	} //}}}

	//{{{ next() method
	public char next()
	{
		++pos;
		if(pos >= count)
		{
			pos = count;
			return DONE;
		}
		else
		{
			if(pos >= tokenStartOffset + currentToken.length)
				updateToken();
			return array[offset + pos];
		}
	} //}}}

	//{{{ previous() method
	public char previous()
	{
		--pos;
		if(pos < 0)
		{
			pos = 0;
			return DONE;
		}
		else
		{
			if(pos < tokenStartOffset)
				updateToken();
			return array[offset + pos];
		}
	} //}}}

	//{{{ setIndex() method
	public char setIndex(int pos)
	{
		if(pos < 0 || pos > count)
			throw new IllegalArgumentException("Range check: <0:"
				+ pos + ":" + count + ">");
		else
		{
			this.pos = pos;
			updateToken();
			return array[offset + pos];
		}
	} //}}}

	//{{{ getIndex() method
	public int getIndex()
	{
		return pos;
	} //}}}

	//{{{ getBeginIndex() method
	public int getBeginIndex()
	{
		return 0;
	} //}}}

	//{{{ getEndIndex() method
	public int getEndIndex()
	{
		return count;
	} //}}}

	//{{{ clone() method
	public Object clone()
	{
		return new SyntaxCharacterIterator(this);
	} //}}}

	//}}}

	//{{{ AttributedCharacterIterator implementation

	//{{{ getRunStart() method
	public int getRunStart()
	{
		return tokenStartOffset;
	} //}}}

	//{{{ getRunStart() method
	public int getRunStart(Attribute attr)
	{
		return tokenStartOffset;
	} //}}}

	//{{{ getRunStart() method
	public int getRunStart(Set set)
	{
		return tokenStartOffset;
	} //}}}

	//{{{ getRunLimit() method
	public int getRunLimit()
	{
		return tokenStartOffset + currentToken.length;
	} //}}}

	//{{{ getRunLimit() method
	public int getRunLimit(Attribute attr)
	{
		return tokenStartOffset + currentToken.length;
	} //}}}

	//{{{ getRunLimit() method
	public int getRunLimit(Set set)
	{
		return tokenStartOffset + currentToken.length;
	} //}}}

	//{{{ getAttributes() method
	public Map getAttributes()
	{
		return currentStyle;
	} //}}}

	//{{{ getAttribute() method
	public Object getAttribute(Attribute attr)
	{
		return currentStyle.get(attr);
	} //}}}

	//{{{ getAttribute() method
	public Set getAllAttributeKeys()
	{
		return ALL_ATTRIBUTES;
	} //}}}

	//}}}

	//{{{ Private members
	private static HashSet ALL_ATTRIBUTES;
	private static HashMap NULL_MAP;

	//{{{ Instance members
	private char[] array;
	private int offset;
	private int count;
	private int lineStartOffset;
	private int pos;

	private Token tokens;
	private SyntaxStyle[] styles;

	private Token currentToken;
	private int tokenStartOffset;

	private Map currentStyle;
	//}}}

	//{{{ Class initializer
	static
	{
		ALL_ATTRIBUTES = new HashSet();
		ALL_ATTRIBUTES.add(TextAttribute.FONT);
		ALL_ATTRIBUTES.add(TextAttribute.FOREGROUND);
		ALL_ATTRIBUTES.add(TextAttribute.BACKGROUND);

		NULL_MAP = new HashMap();
	} //}}}

	//{{{ SyntaxCharacterIterator constructor
	private SyntaxCharacterIterator(SyntaxCharacterIterator copyFrom)
	{
		this.array = copyFrom.array;
		this.offset = copyFrom.offset;
		this.count = copyFrom.count;
		this.lineStartOffset = copyFrom.lineStartOffset;
		this.pos = copyFrom.pos;
		this.tokens = copyFrom.tokens;
		this.styles = copyFrom.styles;
		this.currentToken = copyFrom.currentToken;
		this.tokenStartOffset = copyFrom.tokenStartOffset;
		this.currentStyle = copyFrom.currentStyle;
	} //}}}

	//{{{ updateToken() method
	private void updateToken()
	{
		if(pos == 0 && tokens.id == Token.END)
		{
			currentToken = tokens;
			tokenStartOffset = 0;
			currentStyle = NULL_MAP;
			return;
		}

		currentToken = tokens;
		tokenStartOffset = 0;

		int posFromStartOfLine = (pos + offset - lineStartOffset);
		for(;;)
		{
			if(tokenStartOffset + currentToken.length > posFromStartOfLine
				|| currentToken.next == null)
				break;
			else
			{
				tokenStartOffset += currentToken.length;
				currentToken = currentToken.next;
			}
		}

		if(currentToken.id == Token.END)
			currentStyle = NULL_MAP;
		else
			currentStyle = styles[currentToken.id];

		tokenStartOffset = (tokenStartOffset + lineStartOffset - offset);
	} //}}}

	//}}}
}
