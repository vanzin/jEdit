/*
 * ParserRuleSet.java - A set of parser rules
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 mike dillon
 * Portions copyright (C) 2001, 2002 Slava Pestov
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
import gnu.regexp.RE;
import java.util.*;
//}}}

/**
 * A set of parser rules.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRuleSet
{
	//{{{ getStandardRuleSet() method
	/**
	 * Returns a parser rule set that highlights everything with the
	 * specified token type.
	 * @param id The token type
	 */
	public static ParserRuleSet getStandardRuleSet(byte id)
	{
		return standard[id];
	} //}}}

	//{{{ ParserRuleSet constructor
	public ParserRuleSet(String modeName, String setName)
	{
		this.modeName = modeName;
		this.setName = setName;
		ruleMapFirst = new ParserRule[RULE_BUCKET_COUNT];
		ruleMapLast = new ParserRule[RULE_BUCKET_COUNT];
	} //}}}

	//{{{ getModeName() method
	public String getModeName()
	{
		return modeName;
	} //}}}

	//{{{ getSetName() method
	public String getSetName()
	{
		return setName;
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
		_noWordSep = null;
	} //}}}

	//{{{ addRule() method
	public void addRule(ParserRule r)
	{
		ruleCount++;

		int key = Character.toUpperCase(r.hashChar)
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

	//{{{ getRuleCount() method
	public int getRuleCount()
	{
		return ruleCount;
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
		_noWordSep = null;
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

	//{{{ getDigitRegexp() method
	public RE getDigitRegexp()
	{
		return digitRE;
	} //}}}

	//{{{ setDigitRegexp() method
	public void setDigitRegexp(RE digitRE)
	{
		this.digitRE = digitRE;
	} //}}}

	//{{{ getEscapeRule() method
	public ParserRule getEscapeRule()
	{
		return escapeRule;
	} //}}}

	//{{{ setEscapeRule() method
	public void setEscapeRule(ParserRule escapeRule)
	{
		addRule(escapeRule);
		this.escapeRule = escapeRule;
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

	//{{{ getNoWordSep() method
	public String getNoWordSep()
	{
		if(_noWordSep == null)
		{
			_noWordSep = noWordSep;
			if(noWordSep == null)
				noWordSep = "";
			if(keywords != null)
				noWordSep += keywords.getNonAlphaNumericChars();
		}
		return noWordSep;
	} //}}}

	//{{{ setNoWordSep() method
	public void setNoWordSep(String noWordSep)
	{
		this.noWordSep = noWordSep;
		_noWordSep = null;
	} //}}}

	//{{{ isBuiltIn() method
	/**
	 * Returns if this is a built-in ruleset.
	 * @since jEdit 4.2pre1
	 */
	public boolean isBuiltIn()
	{
		return builtIn;
	} //}}}

	//{{{ toString() method
	public String toString()
	{
		return getClass().getName() + "[" + modeName + "::" + setName + "]";
	} //}}}

	//{{{ Private members
	private static ParserRuleSet[] standard;

	static
	{
		standard = new ParserRuleSet[Token.ID_COUNT];
		for(byte i = 0; i < standard.length; i++)
		{
			standard[i] = new ParserRuleSet(null,null);
			standard[i].setDefault(i);
			standard[i].builtIn = true;
		}
	}

	private static final int RULE_BUCKET_COUNT = 128;

	private String modeName, setName;
	private Hashtable props;

	private KeywordMap keywords;

	private int ruleCount;

	private ParserRule[] ruleMapFirst;
	private ParserRule[] ruleMapLast;

	private int terminateChar = -1;
	private boolean ignoreCase = true;
	private byte defaultToken;
	private ParserRule escapeRule;

	private boolean highlightDigits;
	private RE digitRE;

	private String _noWordSep;
	private String noWordSep;

	private boolean builtIn;
	//}}}
}
