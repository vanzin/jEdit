/*
 * ParserRule.java - Parser rule for the token marker
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 mike dillon
 * Portions copyright (C) 2002 Slava Pestov
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

/**
 * A parser rule.
 * @author mike dillon, Slava Pestov
 * @version $Id$
 */
public class ParserRule
{
	public final char[] start;
	public final char[] end;

	public final int action;
	public final byte token;

	public ParserRule next;

	//{{{ ParserRule constructor
	ParserRule(int action, char[] start, char[] end,
		String delegate, byte token)
	{
		this.start = start;
		this.end = end;
		this.delegate = delegate;
		this.action = action;
		this.token = token;
	} //}}}

	//{{{ getRuleSet() method
	/**
	 * Returns the parser rule set used to highlight text matched by this
	 * rule. Only applicable for <code>SEQ</code>, <code>SPAN</code>,
	 * <code>EOL_SPAN</code>, and <code>MARK_FOLLOWING</code> rules.
	 *
	 * @param tokenMarker The token marker
	 */
	public ParserRuleSet getDelegateRuleSet(TokenMarker tokenMarker)
	{
		if(delegate == null)
			return ParserRuleSet.getStandardRuleSet(token);
		else
		{
			ParserRuleSet delegateSet = tokenMarker.getRuleSet(delegate);
			if(delegateSet == null)
			{
				return ParserRuleSet.getStandardRuleSet(
					Token.NULL);
			}
			else
				return delegateSet;
		}
	} //}}}

	//{{{ Private members
	private String delegate;
	//}}}

	//{{{ createSequenceRule() method
	public static final ParserRule createSequenceRule(String seq,
		String delegate, byte id, boolean atLineStart)
	{
		int ruleAction = TokenMarker.SEQ
			| ((atLineStart) ? TokenMarker.AT_LINE_START : 0);

		return new ParserRule(ruleAction, seq.toCharArray(), null,
			delegate, id);
	} //}}}

	//{{{ createSpanRule() method
	public static final ParserRule createSpanRule(String begin, String end,
		String delegate, byte id, boolean noLineBreak,
		boolean atLineStart, boolean excludeMatch,
		boolean noWordBreak)
	{
		int ruleAction = TokenMarker.SPAN |
			((noLineBreak) ? TokenMarker.NO_LINE_BREAK : 0) |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0) |
			((noWordBreak) ? TokenMarker.NO_WORD_BREAK : 0);

		return new ParserRule(ruleAction, begin.toCharArray(),
			end.toCharArray(), delegate, id);
	} //}}}

	//{{{ createEOLSpanRule() method
	public static final ParserRule createEOLSpanRule(String seq,
		String delegate, byte id, boolean atLineStart,
		boolean excludeMatch)
	{
		int ruleAction = TokenMarker.EOL_SPAN |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0)
			| TokenMarker.NO_LINE_BREAK;

		return new ParserRule(ruleAction, seq.toCharArray(), null,
			delegate, id);
	} //}}}

	//{{{ createMarkFollowingRule() method
	public static final ParserRule createMarkFollowingRule(String seq,
		byte id, boolean atLineStart, boolean excludeMatch)
	{
		int ruleAction = TokenMarker.MARK_FOLLOWING |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0);

		return new ParserRule(ruleAction, seq.toCharArray(), null,
			null, id);
	} //}}}

	//{{{ createMarkPreviousRule() method
	public static final ParserRule createMarkPreviousRule(String seq, byte id,
		boolean atLineStart, boolean excludeMatch)
	{
		int ruleAction = TokenMarker.MARK_PREVIOUS |
			((atLineStart) ? TokenMarker.AT_LINE_START : 0) |
			((excludeMatch) ? TokenMarker.EXCLUDE_MATCH : 0);

		return new ParserRule(ruleAction, seq.toCharArray(), null,
			null, id);
	} //}}}

	//{{{ createEscapeRule() method
	public static final ParserRule createEscapeRule(String seq)
	{
		int ruleAction = TokenMarker.IS_ESCAPE;

		return new ParserRule(ruleAction, seq.toCharArray(), null,
			null, Token.NULL);
	} //}}}
}
