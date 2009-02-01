/*
 * KeywordMap.java - Fast keyword->id map
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2002 Slava Pestov
 * Copyright (C) 1999 Mike Dillon
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

import javax.swing.text.Segment;
import java.util.List;
import java.util.ArrayList;

/**
 * A <code>KeywordMap</code> is similar to a hashtable in that it maps keys
 * to values. However, the `keys' are Swing segments. This allows lookups of
 * text substrings without the overhead of creating a new string object.
 *
 * @author Slava Pestov, Mike Dillon
 * @version $Id$
 */
public class KeywordMap
{
	//{{{ KeywordMap constructor
	/**
	 * Creates a new <code>KeywordMap</code>.
	 * @param ignoreCase True if keys are case insensitive
	 */
	public KeywordMap(boolean ignoreCase)
	{
		this(ignoreCase, 52);
		this.ignoreCase = ignoreCase;
		noWordSep = new StringBuilder();
	} //}}}

	//{{{ KeywordMap constructor
	/**
	 * Creates a new <code>KeywordMap</code>.
	 * @param ignoreCase True if the keys are case insensitive
	 * @param mapLength The number of `buckets' to create.
	 * A value of 52 will give good performance for most maps.
	 */
	public KeywordMap(boolean ignoreCase, int mapLength)
	{
		this.mapLength = mapLength;
		this.ignoreCase = ignoreCase;
		map = new Keyword[mapLength];
	} //}}}

	//{{{ lookup() method
	/**
	 * Looks up a key.
	 * @param text The text segment
	 * @param offset The offset of the substring within the text segment
	 * @param length The length of the substring
	 */
	public byte lookup(Segment text, int offset, int length)
	{
		if(length == 0)
			return Token.NULL;
		Keyword k = map[getSegmentMapKey(text, offset, length)];
		while(k != null)
		{
			if(length != k.keyword.length)
			{
				k = k.next;
				continue;
			}
			if(SyntaxUtilities.regionMatches(ignoreCase,text,offset,
				k.keyword))
				return k.id;
			k = k.next;
		}
		return Token.NULL;
	} //}}}

	//{{{ add() method
	/**
	 * Adds a key-value mapping.
	 * @param keyword The key
	 * @param id The value
	 */
	public void add(String keyword, byte id)
	{
		add(keyword.toCharArray(),id);
	} //}}}

	//{{{ add() method
	/**
	 * Adds a key-value mapping.
	 * @param keyword The key
	 * @param id The value
	 * @since jEdit 4.2pre3
	 */
	public void add(char[] keyword, byte id)
	{
		int key = getStringMapKey(keyword);

		// complete-word command needs a list of all non-alphanumeric
		// characters used in a keyword map.
loop:		for(int i = 0; i < keyword.length; i++)
		{
			char ch = keyword[i];
			if(!Character.isLetterOrDigit(ch))
			{
				for(int j = 0; j < noWordSep.length(); j++)
				{
					if(noWordSep.charAt(j) == ch)
						continue loop;
				}

				noWordSep.append(ch);
			}
		}

		map[key] = new Keyword(keyword,id,map[key]);
	} //}}}

	//{{{ getNonAlphaNumericChars() method
	/**
	 * Returns all non-alphanumeric characters that appear in the
	 * keywords of this keyword map.
	 * @since jEdit 4.0pre3
	 */
	public String getNonAlphaNumericChars()
	{
		return noWordSep.toString();
	} //}}}

	//{{{ getKeywords() method
	/**
	 * Returns an array containing all keywords in this keyword map.
	 * @since jEdit 4.0pre3
	 */
	public String[] getKeywords()
	{
		List<String> vector = new ArrayList<String>(100);
		for(int i = 0; i < map.length; i++)
		{
			Keyword keyword = map[i];
			while(keyword != null)
			{
				vector.add(new String(keyword.keyword));
				keyword = keyword.next;
			}
		}
		String[] retVal = new String[vector.size()];
		vector.toArray(retVal);
		return retVal;
	} //}}}

	//{{{ getIgnoreCase() method
	/**
	 * Returns true if the keyword map is set to be case insensitive,
	 * false otherwise.
	 */
	public boolean getIgnoreCase()
	{
		return ignoreCase;
	} //}}}

	//{{{ setIgnoreCase() method
	/**
	 * Sets if the keyword map should be case insensitive.
	 * @param ignoreCase True if the keyword map should be case
	 * insensitive, false otherwise
	 */
	public void setIgnoreCase(boolean ignoreCase)
	{
		this.ignoreCase = ignoreCase;
	} //}}}

	//{{{ add() method
	/**
	 * Adds the content of another keyword map to this one.
	 * @since jEdit 4.2pre3
	 */
	public void add(KeywordMap map)
	{
		for(int i = 0; i < map.map.length; i++)
		{
			Keyword k = map.map[i];
			while(k != null)
			{
				add(k.keyword,k.id);
				k = k.next;
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private int mapLength;
	private Keyword[] map;
	private boolean ignoreCase;
	private StringBuilder noWordSep;
	//}}}

	//{{{ getStringMapKey() method
	private int getStringMapKey(char[] s)
	{
		return (Character.toUpperCase(s[0]) +
				Character.toUpperCase(s[s.length-1]))
				% mapLength;
	} //}}}

	//{{{ getSegmentMapKey() method
	protected int getSegmentMapKey(Segment s, int off, int len)
	{
		return (Character.toUpperCase(s.array[off]) +
				Character.toUpperCase(s.array[off + len - 1]))
				% mapLength;
	} //}}}

	//}}}

	//{{{ Keyword class
	private static class Keyword
	{
		Keyword(char[] keyword, byte id, Keyword next)
		{
			this.keyword = keyword;
			this.id = id;
			this.next = next;
		}

		public char[] keyword;
		public byte id;
		public Keyword next;
	} //}}}
}
