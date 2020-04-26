/*
 * ParserRuleSet.java - A set of parser rules
 * :tabSize=4:indentSize=4:noTabs=false:
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
import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Pattern;
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
		allRules = new ArrayList<>();
		rulesForNull = new ArrayList<>();
		ruleArray = new List[BASE_CACHE];
		imports = new ArrayList<>();
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

	//{{{ getName() method
	public String getName()
	{
		return modeName + "::" + setName;
	} //}}}

	//{{{ getProperties() method
	public Hashtable<String, String> getProperties()
	{
		return props;
	} //}}}

	//{{{ setProperties() method
	public void setProperties(Hashtable<String, String> props)
	{
		this.props = props;
		_noWordSep = null;
	} //}}}

	//{{{ resolveImports() method
	/**
	 * Resolves all rulesets added with {@link #addRuleSet(ParserRuleSet)}.
	 * @since jEdit 4.2pre3
	 */
	public void resolveImports()
	{
		for (ParserRuleSet ruleset : imports)
		{
			if (!ruleset.imports.isEmpty())
			{
				//prevent infinite recursion
				ruleset.imports.remove(this);
				ruleset.resolveImports();
			}

			ruleset.allRules.forEach(this::addRule);

			if (ruleset.keywords != null)
			{
				if (keywords == null)
					keywords = new KeywordMap(ignoreCase);
				keywords.add(ruleset.keywords);
			}
		}
		imports.clear();
	} //}}}

	//{{{ addRuleSet() method
	/**
	 * Adds all rules contained in the given ruleset.
	 * @param ruleset The ruleset
	 * @since jEdit 4.2pre3
	 */
	public void addRuleSet(ParserRuleSet ruleset)
	{
		imports.add(ruleset);
	} //}}}

	//{{{ addRule() method
	public void addRule(ParserRule parserRule)
	{
		ruleCount++;
		allRules.add(parserRule);
		if (parserRule.upHashChars == null)
		{
			if (parserRule.upHashChar == null || parserRule.upHashChar.length == 0)
			{
				rulesForNull.add(parserRule);
			}
			else
			{
				addRule(parserRule.upHashChar[0], parserRule);
				addRule(Character.toLowerCase(parserRule.upHashChar[0]), parserRule);
			}
		}
		else
		{
			for (char upHashChar : parserRule.upHashChars)
			{
				addRule(upHashChar, parserRule);
				addRule(Character.toLowerCase(upHashChar), parserRule);
			}
		}
	} //}}}

	//{{{ addRule() method
	private void addRule(char ch, ParserRule parserRule)
	{
		if (ch >= ruleArray.length)
		{
			ruleArray = Arrays.copyOf(ruleArray,
				Math.min(Math.min(ruleArray.length * 2, ch + 1),
					Character.MAX_VALUE * 2 + 1));
		}

		List<ParserRule> parserRules = ruleArray[ch];
		if (parserRules == null)
		{
			parserRules = new ArrayList<>();
			ruleArray[ch] = parserRules;
		}
		parserRules.add(parserRule);
	} //}}}

	//{{{ getRules() method
	@Nonnull
	public List<ParserRule> getRules(char key)
	{
		List<ParserRule> rulesForKey = null;

		if (key < ruleArray.length)
			rulesForKey = ruleArray[key];

		if (rulesForNull.isEmpty())
		{
			if (rulesForKey == null)
				return Collections.emptyList();
			return rulesForKey;
		}

		// here rulesForNull is not empty
		if (rulesForKey == null || rulesForKey.isEmpty())
			return rulesForNull;

		int size = rulesForNull.size() + rulesForKey.size();
		List<ParserRule> mixed = new ArrayList<>(size);
		mixed.addAll(rulesForKey);
		mixed.addAll(rulesForNull);
		return mixed;
	} //}}}

	//{{{ getRuleCount() method
	public int getRuleCount()
	{
		return ruleCount;
	} //}}}

	//{{{ getTerminateChar() method
	/**
	 * Returns the number of chars that can be read before the rule parsing stops.
	 *
	 * @return a number of chars or -1 (default value) if there is no limit
	 */
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
	public Pattern getDigitRegexp()
	{
		return digitRE;
	} //}}}

	//{{{ setDigitRegexp() method
	public void setDigitRegexp(Pattern digitRE)
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
	@Override
	public String toString()
	{
		return getClass().getName() + '[' + modeName + "::" + setName + ']';
	} //}}}

	//{{{ Private members
	private static final ParserRuleSet[] standard;
	/**
	 * The base size for the rule array, after that value, chars are less frequent.
	 */
	private static final int BASE_CACHE = 165 + 1;

	static
	{
		standard = new ParserRuleSet[Token.ID_COUNT];
		for(byte i = 0; i < Token.ID_COUNT; i++)
		{
			standard[i] = new ParserRuleSet(null,null);
			standard[i].setDefault(i);
			standard[i].builtIn = true;
		}
	}

	private final String modeName;
	private final String setName;
	private Hashtable<String, String> props;

	private KeywordMap keywords;

	private int ruleCount;

	private final List<ParserRule> allRules;
	private List<ParserRule>[] ruleArray;
	private final List<ParserRule> rulesForNull;

	private final List<ParserRuleSet> imports;

	/**
	 * The number of chars that can be read before the parsing stops.
	 * &lt;TERMINATE AT_CHAR="1" /&gt;
	 */
	private int terminateChar = -1;
	private boolean ignoreCase = true;
	private byte defaultToken;
	private ParserRule escapeRule;

	private boolean highlightDigits;
	private Pattern digitRE;

	private String _noWordSep;
	private String noWordSep;

	private boolean builtIn;
	//}}}
}
