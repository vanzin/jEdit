/*
 * ParserRuleSet.java - A set of parser rules
 * Copyright (C) 1999 mike dillon
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

import java.util.Enumeration;
import java.util.Vector;
import javax.swing.text.Segment;

/**
 * A set of parser rules.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRuleSet
{
	public ParserRuleSet()
	{
		ruleMapFirst = new ParserRule[RULE_BUCKET_COUNT];
		ruleMapLast = new ParserRule[RULE_BUCKET_COUNT];
	}

	public void addRule(ParserRule r)
	{
		int key = Character.toUpperCase(r.searchChars[0])
			% RULE_BUCKET_COUNT;
		ParserRule last = ruleMapLast[key];
		if(last == null)
			ruleMapFirst[key] = ruleMapLast[key] = r;
		else
		{
			last.next = r;
			ruleMapLast[key] = r;
		}
	}

	public void dump()
	{
		for(int i = 0; i < RULE_BUCKET_COUNT; i++)
		{
			ParserRule first = ruleMapFirst[i];
			if(first == null)
				System.err.println(0);
			else
			{
				int j = 0;
				while(first != null)
				{
					j++;
					first = first.next;
				}
				System.err.println(j);
			}
		}
	}

	public ParserRule getRules(char ch)
	{
		int key = Character.toUpperCase(ch) % RULE_BUCKET_COUNT;
		return ruleMapFirst[key];
	}

	public int getTerminateChar()
	{
		return terminateChar;
	}

	public void setTerminateChar(int atChar)
	{
		terminateChar = (atChar >= 0) ? atChar : -1;
	}

	public boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	public void setIgnoreCase(boolean b)
	{
		ignoreCase = b;
	}

	public KeywordMap getKeywords()
	{
		return keywords;
	}

	public void setKeywords(KeywordMap km)
	{
		keywords = km;
	}

	public boolean getHighlightDigits()
	{
		return highlightDigits;
	}

	public void setHighlightDigits(boolean highlightDigits)
	{
		this.highlightDigits = highlightDigits;
	}

	public ParserRule getEscapeRule()
	{
		return escapeRule;
	}

	public Segment getEscapePattern()
	{
		if (escapePattern == null && escapeRule != null)
		{
			escapePattern = new Segment(escapeRule.searchChars, 0,
				escapeRule.sequenceLengths[0]);
		}
		return escapePattern;
	}

	public void setEscape(String esc)
	{
		if (esc == null)
		{
			escapeRule = null;
		}
		else
		{
			escapeRule = ParserRuleFactory.createEscapeRule(esc);
		}
		escapePattern = null;
	}

	public byte getDefault()
	{
		return defaultToken;
	}

	public void setDefault(byte def)
	{
		defaultToken = def;
	}

	private static final int RULE_BUCKET_COUNT = 32;
	private KeywordMap keywords;

	private ParserRule[] ruleMapFirst;
	private ParserRule[] ruleMapLast;

	private ParserRule escapeRule;
	private Segment escapePattern;
	private int terminateChar = -1;
	private boolean ignoreCase = true;
	private boolean highlightDigits;
	private byte defaultToken;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2001/09/02 05:38:02  spestov
 * Initial revision
 *
 * Revision 1.7  2000/04/09 10:41:26  sp
 * NO_WORD_BREAK SPANs fixed, action tokens removed
 *
 * Revision 1.6  2000/04/08 09:34:58  sp
 * Documentation updates, minor syntax changes
 *
 * Revision 1.5  2000/04/08 06:57:14  sp
 * Parser rules are now hashed; this dramatically speeds up tokenization
 *
 * Revision 1.4  2000/04/08 06:10:51  sp
 * Digit highlighting, search bar bug fix
 *
 * Revision 1.3  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.2  2000/04/01 08:40:55  sp
 * Streamlined syntax highlighting, Perl mode rewritten in XML
 *
 */
