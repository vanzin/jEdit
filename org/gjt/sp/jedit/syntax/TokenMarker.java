/*
 * TokenMarker.java - Tokenizes lines of text
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2002 Slava Pestov
 * Copyright (C) 1999, 2000 mike dillon
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
import javax.swing.text.Segment;
import java.util.*;
import org.gjt.sp.jedit.search.CharIndexedSegment;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * A token marker splits lines of text into tokens. Each token carries
 * a length field and an identification tag that can be mapped to a color
 * or font style for painting that token.
 *
 * @author Slava Pestov, mike dillon
 * @version $Id$
 *
 * @see org.gjt.sp.jedit.syntax.Token
 * @see org.gjt.sp.jedit.syntax.TokenHandler
 */
public class TokenMarker
{
	//{{{ Major actions (total: 8)
	public static final int MAJOR_ACTIONS = 0x000000FF;
	public static final int SEQ = 0;
	public static final int SPAN = 1 << 0;
	public static final int MARK_PREVIOUS = 1 << 1;
	public static final int MARK_FOLLOWING = 1 << 2;
	public static final int EOL_SPAN = 1 << 3;
//	public static final int MAJOR_ACTION_5 = 1 << 4;
//	public static final int MAJOR_ACTION_5 = 1 << 5;
//	public static final int MAJOR_ACTION_6 = 1 << 6;
//	public static final int MAJOR_ACTION_7 = 1 << 7;
	//}}}

	//{{{ Action hints (total: 8)
	public static final int ACTION_HINTS = 0x0000FF00;
	public static final int EXCLUDE_MATCH = 1 << 8;
	public static final int AT_LINE_START = 1 << 9;
	public static final int NO_LINE_BREAK = 1 << 10;
	public static final int NO_WORD_BREAK = 1 << 11;
	public static final int IS_ESCAPE = 1 << 12;
//	public static final int ACTION_HINT_13 = 1 << 13;
//	public static final int ACTION_HINT_14 = 1 << 14;
//	public static final int ACTION_HINT_15 = 1 << 15;
	//}}}

	//{{{ TokenMarker constructor
	public TokenMarker()
	{
		ruleSets = new Hashtable(64);
	} //}}}

	//{{{ getName() method
	public String getName()
	{
		return name;
	} //}}}

	//{{{ setName() method
	public void setName(String name)
	{
		if (name == null)
			throw new NullPointerException();

		this.name = name;
		rulePfx = name.concat("::");
	} //}}}

	//{{{ addRuleSet() method
	public void addRuleSet(String setName, ParserRuleSet rules)
	{
		if (rules == null)
			return;

		if (setName == null)
			setName = "MAIN";

		ruleSets.put(rulePfx.concat(setName), rules);

		if (setName.equals("MAIN"))
			mainRuleSet = rules;
	} //}}}

	//{{{ getMainRuleSet() method
	public ParserRuleSet getMainRuleSet()
	{
		return mainRuleSet;
	} //}}}

	//{{{ getRuleSet() method
	public ParserRuleSet getRuleSet(String setName)
	{
		ParserRuleSet rules;

		rules = (ParserRuleSet) ruleSets.get(setName);

		if (rules == null && !setName.startsWith(rulePfx))
		{
			int delim = setName.indexOf("::");
			if(delim == -1)
			{
				byte id = Token.stringToToken(setName);
				rules = ParserRuleSet.getStandardRuleSet(id);
			}
			else
			{
				String modeName = setName.substring(0, delim);

				Mode mode = jEdit.getMode(modeName);
				if(mode == null)
				{
					Log.log(Log.ERROR,TokenMarker.class,
						"Unknown edit mode: " + modeName);
					rules = null;
				}
				else
				{
					TokenMarker marker = mode.getTokenMarker();
					rules = marker.getRuleSet(setName);
				}
			}

			// store external ParserRuleSet in the local hashtable
			// for faster lookups later
			ruleSets.put(setName, rules);
		}

		if (rules == null)
		{
			Log.log(Log.ERROR,this,"Unresolved delegate target: " + setName);
			return ParserRuleSet.getStandardRuleSet(Token.INVALID);
		}
		else
			return rules;
	} //}}}

	public static int totalRuleScans;
	public static int totalRuleChecks;
	public static int matchedRule;
	public static int matchedWordSep;

	//{{{ markTokens() method
	/**
	 * Do not call this method directly; call Buffer.markTokens() instead.
	 */
	public LineContext markTokens(LineContext prevContext,
		TokenHandler tokenHandler, Segment line,
		String noWordSep)
	{
		//{{{ Set up some instance variables
		// this is to avoid having to pass around lots and lots of
		// parameters.
		this.tokenHandler = tokenHandler;
		this.line = line;

		lastOffset = line.offset;
		lineLength = line.count + line.offset;

		context = new LineContext();

		if(prevContext == null)
			context.rules = getMainRuleSet();
		else
		{
			context.parent = prevContext.parent;
			context.inRule = prevContext.inRule;
			context.rules = prevContext.rules;
		}

		keywords = context.rules.getKeywords();
		escaped = false;
		//}}}

		//{{{ Main parser loop
		ParserRule rule;
		int terminateChar = context.rules.getTerminateChar();
		boolean terminated = false;

main_loop:	for(pos = line.offset; pos < lineLength; pos++)
		{
			//{{{ check if we have to stop parsing
			if(terminateChar >= 0 && pos >= terminateChar
				&& !terminated)
			{
				terminated = true;
				context = new LineContext(ParserRuleSet
					.getStandardRuleSet(context.rules
					.getDefault()),context);
				keywords = context.rules.getKeywords();
			} //}}}

			//{{{ check for end of delegate
			if(context.parent != null)
			{
				rule = context.parent.inRule;
				if(rule != null && rule.end != null)
				{
					if(checkDelegateEnd())
						continue main_loop;
				}
			} //}}}

			totalRuleScans++;

			//{{{ check every rule
			char ch = line.array[pos];

			rule = context.rules.getRules(ch);
			while(rule != null)
			{
				totalRuleChecks++;
				// stop checking rules if there was a match
				if (handleRule(rule,false))
				{
					matchedRule++;
					continue main_loop;
				}

				rule = rule.next;
			} //}}}

			//{{{ check if current character is a word separator
			if(keywords != null)
			{
				String noWordSep2 = keywords.getNonAlphaNumericChars();

				if(!Character.isLetterOrDigit(ch)
					&& noWordSep.indexOf(ch) == -1
					&& noWordSep2.indexOf(ch) == -1)
				{
					markKeyword(true);
	
					tokenHandler.handleToken(1,
						context.rules.getDefault(),
						context.rules);
					lastOffset = pos + 1;

					matchedWordSep++;
				}
			} //}}}

			escaped = false;
		} //}}}

		//{{{ Mark all remaining characters
		pos = lineLength;

		handleSoftSpan();
		markKeyword(true);

		if(context.parent != null)
		{
			rule = context.parent.inRule;
			if(rule != null && (context.parent.inRule.action
				& NO_LINE_BREAK) == NO_LINE_BREAK)
			{
				context = context.parent;
				keywords = context.rules.getKeywords();
				context.inRule = null;
			}
		} //}}}

		tokenHandler.handleToken(0,Token.END,context.rules);

		return context.intern();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private Hashtable ruleSets;
	private String name;
	private String rulePfx;
	private ParserRuleSet mainRuleSet;

	// Instead of passing these around to each method, we just store them
	// as instance variables. Note that this is not thread-safe.
	private TokenHandler tokenHandler;
	private Segment line;
	private LineContext context;
	private KeywordMap keywords;
	private Segment pattern = new Segment();
	private int lastOffset;
	private int lineLength;
	private int pos;
	private boolean escaped;
	//}}}

	//{{{ checkDelegateEnd() method
	private boolean checkDelegateEnd()
	{
		LineContext tempContext = context;
		context = context.parent;
		keywords = context.rules.getKeywords();
		boolean tempEscaped = escaped;
		boolean b = handleRule(context.inRule,true);
		context = tempContext;
		keywords = context.rules.getKeywords();

		if(b && !tempEscaped)
		{
			markKeyword(true);

			context = (LineContext)context.parent.clone();
			keywords = context.rules.getKeywords();

			tokenHandler.handleToken(pattern.count,
				(context.inRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH
				? context.rules.getDefault() : context.inRule.token,
				context.rules);

			context.inRule = null;
			lastOffset = pos + pattern.count;

			// move pos to last character of match sequence
			pos += (pattern.count - 1);

			return true;
		}

		// check escape rule of parent
		ParserRule rule = context.parent.rules.getEscapeRule();
		if(rule != null && handleRule(rule,false))
			return true;

		return false;
	} //}}}

	//{{{ handleRule() method
	/**
	 * Checks if the rule matches the line at the current position
	 * and handles the rule if it does match
	 */
	private boolean handleRule(ParserRule checkRule, boolean end)
	{
		pattern.offset = 0;
		pattern.array = (end ? checkRule.end : checkRule.start);
		pattern.count = pattern.array.length;

		if(!TextUtilities.regionMatches(context.rules.getIgnoreCase(),
			line,pos,pattern.array))
		{
			return false;
		}

		//{{{ Check for an escape sequence
		if ((checkRule.action & IS_ESCAPE) == IS_ESCAPE)
		{
			escaped = !escaped;
			pos += pattern.count - 1;
		}
		else if (escaped)
		{
			escaped = false;
			pos += pattern.count - 1;
		} //}}}
		//{{{ Not inside a rule
		else if (context.inRule == null)
		{
			if(checkRule.token == Token.WHITESPACE)
			{
				if(handleSoftSpan())
					return true;
			}
			else if((checkRule.action & AT_LINE_START) == AT_LINE_START)
			{
				if((((checkRule.action & MARK_PREVIOUS) != 0) ?
					lastOffset : pos) != line.offset)
				{
					return false;
				}
			}

			markKeyword((checkRule.action & MARK_PREVIOUS) != MARK_PREVIOUS);

			switch(checkRule.action & MAJOR_ACTIONS)
			{
			//{{{ SEQ
			case SEQ:
				tokenHandler.handleToken(pattern.count,
					checkRule.token,
					context.rules);
				break;
			//}}}
			//{{{ SPAN, EOL_SPAN, MARK_FOLLOWING
			case SPAN: case EOL_SPAN: case MARK_FOLLOWING:
				context.inRule = checkRule;

				ParserRuleSet delegateSet = checkRule.getDelegateRuleSet(this);

				tokenHandler.handleToken(pattern.count,
					((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH
					? context.rules.getDefault() : checkRule.token),
					context.rules);

				context = new LineContext(delegateSet, context);
				keywords = context.rules.getKeywords();

				break;
			//}}}
			//{{{ MARK_PREVIOUS
			case MARK_PREVIOUS:
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					if(pos != lastOffset)
					{
						tokenHandler.handleToken(pos - lastOffset,
							checkRule.token,context.rules);
					}

					tokenHandler.handleToken(pattern.count,
						context.rules.getDefault(),
						context.rules);
				}
				else
				{
					tokenHandler.handleToken(pos - lastOffset + pattern.count,
						checkRule.token,context.rules);
				}

				break;
			//}}}
			default:
				throw new InternalError("Unhandled major action");
			}

			// move pos to last character of match sequence
			pos += (pattern.count - 1); 
			lastOffset = pos + 1;

			// break out of inner for loop to check next char
		} //}}}

		return true;
	} //}}}

	//{{{ handleSoftSpan() method
	private boolean handleSoftSpan()
	{
		if (context.parent != null)
		{
			ParserRule rule = context.parent.inRule;
			if(rule != null && (context.parent.inRule.action
				& (NO_WORD_BREAK | MARK_FOLLOWING)) != 0)
			{
				/* commented out for now... so token marker
				will never spit out INVALID tokens. need to
				sort this out before 4.1pre1. */

				/* if ((context.parent.inRule.action & NO_WORD_BREAK) == NO_WORD_BREAK)
				{
					tokenHandler.handleToken(pos - lastOffset,
						Token.INVALID,
						context.rules);
				}
				else */

				tokenHandler.handleToken(pos - lastOffset,
					rule.token,context.rules);

				lastOffset = pos;
				context = context.parent;
				keywords = context.rules.getKeywords();
				context.inRule = null;
				return true;
			}
		}

		return false;
	} //}}}

	//{{{ markKeyword() method
	private void markKeyword(boolean addRemaining)
	{
		int len = pos - lastOffset;
		if(len == 0)
			return;

		//{{{ Do digits
		if(context.rules.getHighlightDigits())
		{
			boolean digit = false;
			boolean mixed = false;

			for(int i = lastOffset; i < pos; i++)
			{
				char ch = line.array[i];
				if(Character.isDigit(ch))
					digit = true;
				else
					mixed = true;
			}

			if(mixed)
			{
				RE digitRE = context.rules.getDigitRegexp();

				// only match against regexp if its not all
				// digits; if all digits, no point matching
				if(digit)
				{ 
					if(digitRE == null)
					{
						// mixed digit/alpha keyword,
						// and no regexp... don't
						// highlight as DIGIT
						digit = false;
					}
					else
					{
						CharIndexedSegment seg = new CharIndexedSegment(
							line,false);
						int oldCount = line.count;
						int oldOffset = line.offset;
						line.offset = lastOffset;
						line.count = len;
						if(!digitRE.isMatch(seg))
							digit = false;
						line.offset = oldOffset;
						line.count = oldCount;
					}
				}
			}

			if(digit)
			{
				tokenHandler.handleToken(len,Token.DIGIT,context.rules);
				lastOffset = pos;

				return;
			}
		} //}}}

		//{{{ Do keywords
		if(keywords != null)
		{
			byte id = keywords.lookup(line, lastOffset, len);

			if(id != Token.NULL)
			{
				tokenHandler.handleToken(len,id,context.rules);
				lastOffset = pos;
				return;
			}
		} //}}}

		//{{{ Handle any remaining crud
		if(addRemaining)
		{
			tokenHandler.handleToken(pos - lastOffset,
				context.rules.getDefault(),
				context.rules);
			lastOffset = pos;
		} //}}}
	} //}}}

	//}}}

	//{{{ LineContext class
	public static class LineContext
	{
		//{{{ Debug code
		static int count;
		static int countGC;

		public String getAllocationStatistics()
		{
			return "total: " + count + ", in core: " +
				(count - countGC)
				+ ", interned: " + intern.size();
		} //}}}

		private static Hashtable intern = new Hashtable();

		public LineContext parent;
		public ParserRule inRule;
		public ParserRuleSet rules;

		//{{{ LineContext constructor
		public LineContext(ParserRule r, ParserRuleSet rs)
		{
			this();
			inRule = r;
			rules = rs;
		} //}}}

		//{{{ LineContext constructor
		public LineContext(ParserRuleSet rs, LineContext lc)
		{
			this();
			rules = rs;
			parent = (lc == null ? null : (LineContext)lc.clone());
		} //}}}

		//{{{ LineContext constructor
		public LineContext(ParserRule r)
		{
			this();
			inRule = r;
		} //}}}

		//{{{ LineContext constructor
		public LineContext()
		{
			count++;
		} //}}}

		//{{{ intern() method
		public LineContext intern()
		{
			Object obj = intern.get(this);
			if(obj == null)
			{
				intern.put(this,this);
				return this;
			}
			else
				return (LineContext)obj;
		} //}}}

		//{{{ finalize() method
		public void finalize()
		{
			countGC++;
		} //}}}

		//{{{ hashCode() method
		public int hashCode()
		{
			if(inRule != null)
				return inRule.hashCode();
			else if(rules != null)
				return rules.hashCode();
			else
				return 0;
		} //}}}

		//{{{ equals() method
		public boolean equals(Object obj)
		{
			if(obj instanceof LineContext)
			{
				LineContext lc = (LineContext)obj;
				if(lc.parent == null)
				{
					if(parent != null)
						return false;
				}
				else //if(lc.parent != null)
				{
					if(parent == null)
						return false;
					else if(!lc.parent.equals(parent))
						return false;
				}

				return lc.inRule == inRule && lc.rules == rules;
			}
			else
				return false;
		} //}}}

		//{{{ clone() method
		public Object clone()
		{
			LineContext lc = new LineContext();
			lc.inRule = inRule;
			lc.rules = rules;
			lc.parent = (parent == null) ? null : (LineContext) parent.clone();

			return lc;
		} //}}}
	} //}}}
}
