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
