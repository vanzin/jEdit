/*
 * ParserRuleSet.java - A set of parser rules
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 mike dillon
 * Portions copyright (C) 2001 Slava Pestov
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

import java.util.*;
import org.gjt.sp.jedit.Mode;
import javax.swing.text.Segment;

/**
 * A set of parser rules.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRuleSet
{
	//{{{ ParserRuleSet constructor
	public ParserRuleSet(String name, Mode mode)
	{
		this.name = name;
		this.mode = mode;
		ruleMapFirst = new ParserRule[RULE_BUCKET_COUNT];
		ruleMapLast = new ParserRule[RULE_BUCKET_COUNT];
	} //}}}

	//{{{ getMode() method
	public Mode getMode()
	{
		return mode;
	} //}}}

	//{{{ getProperties() method
	public Hashtable getProperties()
	{
		return props;
	} //}}}

	//{{{ setProperties() method
	public void setProperties(Hashtable props)
	{
		this.props = props;
	} //}}}

	//{{{ addRule() method
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
	} //}}}

	//{{{ getRules() method
	public ParserRule getRules(char ch)
	{
		int key = Character.toUpperCase(ch) % RULE_BUCKET_COUNT;
		return ruleMapFirst[key];
	} //}}}

	//{{{ getTerminateChar() method
	public int getTerminateChar()
	{
		return terminateChar;
	} //}}}

	//{{{ setTerminateChar() method
	public void setTerminateChar(int atChar)
	{
		terminateChar = (atChar >= 0) ? atChar : -1;
	} //}}}

	//{{{ getIgnoreCase() method
	public boolean getIgnoreCase()
	{
		return ignoreCase;
	} //}}}

	//{{{ setIgnoreCase() method
	public void setIgnoreCase(boolean b)
	{
		ignoreCase = b;
	} //}}}

	//{{{ getKeywords() method
	public KeywordMap getKeywords()
	{
		return keywords;
	} //}}}

	//{{{ setKeywords() method
	public void setKeywords(KeywordMap km)
	{
		keywords = km;
	} //}}}

	//{{{ getHighlightDigits() method
	public boolean getHighlightDigits()
	{
		return highlightDigits;
	} //}}}

	//{{{ setHighlightDigits() method
	public void setHighlightDigits(boolean highlightDigits)
	{
		this.highlightDigits = highlightDigits;
	} //}}}

	//{{{ getEscapeRule() method
	public ParserRule getEscapeRule()
	{
		return escapeRule;
	} //}}}

	//{{{ getEscapePattern() method
	public Segment getEscapePattern()
	{
		if (escapePattern == null && escapeRule != null)
		{
			escapePattern = new Segment(escapeRule.searchChars, 0,
				escapeRule.sequenceLengths[0]);
		}
		return escapePattern;
	} //}}}

	//{{{ setEscape() method
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
	} //}}}

	//{{{ getDefault() method
	public byte getDefault()
	{
		return defaultToken;
	} //}}}

	//{{{ setDefault() method
	public void setDefault(byte def)
	{
		defaultToken = def;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[" + mode.getName() + "::"
			+ name + "]";
	} //}}}

	//{{{ Private members
	private static final int RULE_BUCKET_COUNT = 32;

	private String name;
	private Mode mode;
	private Hashtable props;

	private KeywordMap keywords;

	private ParserRule[] ruleMapFirst;
	private ParserRule[] ruleMapLast;

	private ParserRule escapeRule;
	private Segment escapePattern;
	private int terminateChar = -1;
	private boolean ignoreCase = true;
	private boolean highlightDigits;
	private byte defaultToken;
	//}}}
}
