/*
 * TokenMarker.java - Tokenizes lines of text
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 1999, 2000, 2001, 2002 Slava Pestov
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

	//{{{ markTokens() method
	/**
	 * Do not call this method directly; call Buffer.markTokens() instead.
	 */
	public LineContext markTokens(LineContext prevContext,
		TokenHandler tokenHandler, Segment line)
	{
		//{{{ Set up some instance variables
		// this is to avoid having to pass around lots and lots of
		// parameters.
		this.tokenHandler = tokenHandler;
		this.line = line;

		lastOffset = lastKeyword = line.offset;
		lineLength = line.count + line.offset;

		context = new LineContext();

		if(prevContext == null)
			context.rules = getMainRuleSet();
		else
		{
			context.parent = prevContext.parent;
			context.inRule = prevContext.inRule;
			context.rules = prevContext.rules;
		} //}}}

		//{{{ Main parser loop
		int terminateChar = context.rules.getTerminateChar();
		int searchLimit = (terminateChar >= 0
			&& terminateChar < line.count)
			? terminateChar + line.offset
			: line.count + line.offset;

main_loop:	for(pos = line.offset; pos < searchLimit; pos++)
		{
			// check for end of delegate
			if(context.parent != null)
			{
				if(checkDelegateEnd())
					continue main_loop;
			}

			if(context.inRule != null)
			{
				System.err.println("in rule--can't happen?");
				System.err.println(context.inRule.action
					& MAJOR_ACTIONS);
			} 

			// Check every rule
			ParserRule rule = context.rules.getRules(line.array[pos]);
			while(rule != null)
			{
				// stop checking rules if there was a match
				if (handleRule(rule,false))
					continue main_loop;

				rule = rule.next;
			}

			escaped = false;
		} //}}}

		//{{{ Mark all remaining characters
		if(context.inRule == null)
			markKeyword(lastKeyword, lineLength);
		if(context.parent != null && (context.parent.inRule.action
			& EOL_SPAN) == EOL_SPAN)
		{
			context = context.parent;
			context.inRule = null;
		}

		if(lastOffset != lineLength)
		{
			if (context.inRule == null)
			{
				tokenHandler.handleToken(lineLength - lastOffset,
					context.rules.getDefault(),
					context.rules);
			}
			else
			{
				tokenHandler.handleToken(lineLength - lastOffset,
					context.inRule.token,
					context.rules);

				if((context.inRule.action & MARK_FOLLOWING) == MARK_FOLLOWING)
				{
					context.inRule = null;
				}
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
	private Segment pattern = new Segment(new char[0],0,0);
	private int lastOffset;
	private int lastKeyword;
	private int lineLength;
	private int pos;
	private boolean escaped;
	//}}}

	//{{{ checkDelegateEnd() method
	private boolean checkDelegateEnd()
	{
		LineContext tempContext = context;
		context = context.parent;
		boolean b = handleRule(context.inRule,true);
		
		context = tempContext;

		if (b)
		{
			if (pos > lastOffset)
			{
				markKeyword(lastKeyword,pos);

				tokenHandler.handleToken(pos - lastOffset,
					context.rules.getDefault(),
					context.rules);
			}

			context = (LineContext)context.parent.clone();

			if ((context.inRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
			{
				tokenHandler.handleToken(pattern.count,
					context.rules.getDefault(),
					context.rules);
			}
			else
			{
				tokenHandler.handleToken(pattern.count,
					context.inRule.token,
					context.rules);
			}

			context.inRule = null;
			lastKeyword = lastOffset = pos + pattern.count;

			// move pos to last character of match sequence
			pos += (pattern.count - 1);

			return true;
		}

		// check escape rule of parent
		ParserRule rule = context.parent.rules.getEscapeRule();
		if(rule != null)
		{
			if (handleRule(rule,false))
				return true;
		}

		return false;
	} //}}}

	//{{{ handleRule() method
	/**
	 * Checks if the rule matches the line at the current position
	 * and handles the rule if it does match
	 */
	private boolean handleRule(ParserRule checkRule, boolean end)
	{
		pattern.array = checkRule.searchChars;

		if(end)
		{
			// is this an EOL_SPAN?
			if(checkRule.sequenceLengths.length == 1)
			{
				pattern.offset = 0;
				pattern.count = 1;
				return (pos == lineLength - 1);
			}

			pattern.offset = checkRule.sequenceLengths[0];
			pattern.count = checkRule.sequenceLengths[1];
		}
		else
		{
			pattern.offset = 0;
			pattern.count = checkRule.sequenceLengths[0];
		}

		if (pattern.count == 0)
			return false;

		if (lineLength - pos < pattern.count)
			return false;

		for (int k = 0; k < pattern.count; k++)
		{
			char a = pattern.array[pattern.offset + k];
			char b = line.array[pos + k];

			// break out and check the next rule if there is a mismatch
			if (
				!(
					a == b ||
					context.rules.getIgnoreCase() &&
					(
						Character.toLowerCase(a) == b ||
						a == Character.toLowerCase(b)
					)
				)
			) return false;
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
			//{{{ handle soft spans
			if (context.parent != null
				&& (context.parent.inRule.action
				& (NO_WORD_BREAK | MARK_FOLLOWING)) != 0
				&& checkRule.token == Token.WHITESPACE)
			{
				if ((context.inRule.action & NO_WORD_BREAK) == NO_WORD_BREAK)
				{
					tokenHandler.handleToken(pos - lastOffset,
						Token.INVALID,
						context.rules);
				}
				else
				{
					tokenHandler.handleToken(pos - lastOffset,
						context.inRule.token,
						context.rules);
				}
				lastOffset = lastKeyword = pos;
				context = context.parent;
				context.inRule = null;
				return true;
			} //}}}

			if ((checkRule.action & AT_LINE_START) == AT_LINE_START)
			{
				if (
					(((checkRule.action & MARK_PREVIOUS) != 0) ?
					lastKeyword :
					pos) != line.offset
				)
				{
					return false;
				}
			}

			markKeyword(lastKeyword, pos);

			if ((checkRule.action & MARK_PREVIOUS) != MARK_PREVIOUS)
			{
				lastKeyword = pos + pattern.count;

				// mark previous sequence as NULL (plain text)
				if (lastOffset < pos)
				{
					tokenHandler.handleToken(pos - lastOffset,
						context.rules.getDefault(),
						context.rules);
				}
			}

			switch(checkRule.action & MAJOR_ACTIONS)
			{
			//{{{ SEQ
			case 0:
				// this is a plain sequence rule
				tokenHandler.handleToken(pattern.count,checkRule.token,
					context.rules);
				lastOffset = pos + pattern.count;
				break;
			//}}}
			//{{{ SPAN
			case SPAN:
				context.inRule = checkRule;

				String setName = new String(checkRule.searchChars,
					checkRule.sequenceLengths[0] + checkRule.sequenceLengths[1],
					checkRule.sequenceLengths[2]);

				ParserRuleSet delegateSet = getRuleSet(setName);

				if (delegateSet != null)
				{
					if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
					{
						tokenHandler.handleToken(pattern.count,
							context.rules.getDefault(),
							context.rules);
					}
					else
					{
						tokenHandler.handleToken(pattern.count,
							checkRule.token,
							context.rules);
					}
					lastOffset = pos + pattern.count;

					context = new LineContext(delegateSet, context);
				}

				break;
			//}}}
			//{{{ EOL_SPAN
			case EOL_SPAN:
				tokenHandler.handleToken(pattern.count,
					((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH
					? context.rules.getDefault() : checkRule.token),
					context.rules);
				lastOffset = pos + pattern.count;

				context.inRule = checkRule;
				context = new LineContext(
					ParserRuleSet.getStandardRuleSet(
					checkRule.token),context);
				break;
			//}}}
			//{{{ MARK_PREVIOUS
			case MARK_PREVIOUS:
				if (lastKeyword > lastOffset)
				{
					tokenHandler.handleToken(lastKeyword - lastOffset,
						context.rules.getDefault(),
						context.rules);
					lastOffset = lastKeyword;
				}

				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenHandler.handleToken(pos - lastOffset,
						checkRule.token,context.rules);
					tokenHandler.handleToken(pattern.count,
						context.rules.getDefault(),
						context.rules);
				}
				else
				{
					tokenHandler.handleToken(pos - lastOffset + pattern.count,
						checkRule.token,context.rules);
				}
				lastOffset = pos + pattern.count;

				break;
			//}}}
			//{{{ MARK_FOLLOWING
			case MARK_FOLLOWING:
				context.inRule = checkRule;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenHandler.handleToken(pattern.count,
						context.rules.getDefault(),
						context.rules);
					lastOffset = pos + pattern.count;
				}
				else
				{
					lastOffset = pos;
				}

				break;
			//}}}
			default:
				throw new InternalError("Unhandled major action");
			}

			lastKeyword = lastOffset;

			pos += (pattern.count - 1); // move pos to last character of match sequence
			 // break out of inner for loop to check next char
		} //}}}

		return true;
	} //}}}

	//{{{ markKeyword() method
	private void markKeyword(int start, int end)
	{
		int len = end - start;

		//{{{ Do digits.
		if(context.rules.getHighlightDigits())
		{
			boolean digit = false;
			boolean mixed = false;

			for(int i = start; i < end; i++)
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
						line.offset = start;
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
				if(start != lastOffset)
				{
					tokenHandler.handleToken(start - lastOffset,
						context.rules.getDefault(),
						context.rules);
				}
				tokenHandler.handleToken(len,Token.DIGIT,context.rules);
				lastKeyword = lastOffset = end;

				return;
			}
		} //}}}

		//{{{ Do keywords
		KeywordMap keywords = context.rules.getKeywords();

		if(keywords != null)
		{
			byte id = keywords.lookup(line, start, len);

			if(id != Token.NULL)
			{
				if(start != lastOffset)
				{
					tokenHandler.handleToken(start - lastOffset,
						context.rules.getDefault(),
						context.rules);
				}
				tokenHandler.handleToken(len,id,context.rules);
				lastKeyword = lastOffset = end;
				return;
			}
		} //}}}

		// XXX: put plain 'insertion of default token' here to
		// avoid duplication
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
