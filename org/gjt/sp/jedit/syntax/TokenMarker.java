/*
 * TokenMarker.java - Tokenizes lines of text
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

import javax.swing.text.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * A token marker splits lines of text into tokens. Each token carries
 * a length field and an identification tag that can be mapped to a color
 * or font style for painting that token.
 *
 * @author Slava Pestov, mike dillon
 * @version $Id$
 *
 * @see org.gjt.sp.jedit.syntax.Token
 */
public class TokenMarker
{
	// major actions (total: 8)
	public static final int MAJOR_ACTIONS = 0x000000FF;
	public static final int WHITESPACE = 1 << 0;
	public static final int SPAN = 1 << 1;
	public static final int MARK_PREVIOUS = 1 << 2;
	public static final int MARK_FOLLOWING = 1 << 3;
	public static final int EOL_SPAN = 1 << 4;
//	public static final int MAJOR_ACTION_5 = 1 << 5;
//	public static final int MAJOR_ACTION_6 = 1 << 6;
//	public static final int MAJOR_ACTION_7 = 1 << 7;

	// action hints (total: 8)
	public static final int ACTION_HINTS = 0x0000FF00;
	public static final int EXCLUDE_MATCH = 1 << 8;
	public static final int AT_LINE_START = 1 << 9;
	public static final int NO_LINE_BREAK = 1 << 10;
	public static final int NO_WORD_BREAK = 1 << 11;
	public static final int IS_ESCAPE = 1 << 12;
	public static final int DELEGATE = 1 << 13;
//	public static final int ACTION_HINT_14 = 1 << 14;
//	public static final int ACTION_HINT_15 = 1 << 15;

	public TokenMarker()
	{
		ruleSets = new Hashtable(64);
	}

	public void addRuleSet(String setName, ParserRuleSet rules)
	{
		if (rules == null) return;

		if (setName == null) setName = "MAIN";

		ruleSets.put(rulePfx.concat(setName), rules);
	}

	public ParserRuleSet getMainRuleSet()
	{
		return getRuleSet(rulePfx + "MAIN");
	}

	public ParserRuleSet getRuleSet(String setName)
	{
		ParserRuleSet rules;

		rules = (ParserRuleSet) ruleSets.get(setName);

		if (rules == null && !setName.startsWith(rulePfx))
		{
			int delim = setName.indexOf("::");

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

			// store external ParserRuleSet in the local hashtable for
			// faster lookups later
			ruleSets.put(setName, rules);
		}

		if (rules == null)
		{
			Log.log(Log.ERROR,this,"Unresolved delegate target: " + setName);
		}

		return rules;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		if (name == null) throw new NullPointerException();

		this.name = name;
		rulePfx = name.concat("::");
	}

	/**
	 * Do not call this method directly; call Buffer.markTokens() instead.
	 */
	public void markTokens(Buffer.LineInfo prevInfo,
		Buffer.LineInfo info,
		Buffer.TokenList tokenList,
		Segment line)
	{
		context = info.getLineContext();

		if(prevInfo == null)
		{
			context.parent = null;
			context.inRule = null;
			context.rules = getMainRuleSet();
		}
		else
		{
			LineContext lastContext = prevInfo.getLineContext();

			context.parent = lastContext.parent;
			context.inRule = lastContext.inRule;
			context.rules = lastContext.rules;
		}

		lastOffset = lastKeyword = line.offset;
		lineLength = line.count + line.offset;

		int terminateChar = context.rules.getTerminateChar();
		int searchLimit = (terminateChar >= 0 && terminateChar < line.count)
			? line.offset + terminateChar : lineLength;

		escaped = false;

		boolean b;
		boolean tempEscaped;
		Segment tempPattern;
		ParserRule rule;
		LineContext tempContext;

		for(pos = line.offset; pos < searchLimit; pos++)
		{
			// if we are not in the top level context, we are delegated
			if (context.parent != null)
			{
				tempContext = context;

				context = context.parent;

				pattern.array = context.inRule.searchChars;
				pattern.count = context.inRule.sequenceLengths[1];
				pattern.offset = context.inRule.sequenceLengths[0];

				b = handleRule(tokenList, line, context.inRule);

				context = tempContext;

				if (!b)
				{
					if (escaped)
					{
						escaped = false;
					}
					else
					{
						if (pos != lastOffset)
						{
							if (context.inRule == null)
							{
								markKeyword(tokenList,line,lastKeyword,pos);

								tokenList.addToken(pos - lastOffset,
									context.rules.getDefault());
							}
							else if ((context.inRule.action & (NO_LINE_BREAK | NO_WORD_BREAK)) == 0)
							{
								tokenList.addToken(pos - lastOffset,
									context.inRule.token);
							}
							else
							{
								tokenList.addToken(pos - lastOffset, Token.INVALID);
							}
						}

						context = (LineContext)context.parent.clone();

						if ((context.inRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
						{
							tokenList.addToken(pattern.count,
								context.rules.getDefault());
						}
						else
						{
							tokenList.addToken(pattern.count,context.inRule.token);
						}

						context.inRule = null;

						lastKeyword = lastOffset = pos + pattern.count;
					}

					pos += (pattern.count - 1); // move pos to last character of match sequence

					continue;
				}
			}

			// check the escape rule for the current context, if there is one
			if ((rule = context.rules.getEscapeRule()) != null)
			{
				// assign tempPattern to mutable "buffer" pattern
				tempPattern = pattern;

				// swap in the escape pattern
				pattern = context.rules.getEscapePattern();

				tempEscaped = escaped;

				b = handleRule(tokenList, line, rule);

				// swap back the buffer pattern
				pattern = tempPattern;

				if (!b)
				{
					if (tempEscaped) escaped = false;
					continue;
				}
			}

			// if we are inside a span, check for its end sequence
			rule = context.inRule;
			if(rule != null && (rule.action & SPAN) == SPAN)
			{
				pattern.array = rule.searchChars;
				pattern.count = rule.sequenceLengths[1];
				pattern.offset = rule.sequenceLengths[0];

				// if we match the end of the span, or if this is a "hard" span,
				// we continue to the next character; otherwise, we check all
				// applicable rules below
				if (!handleRule(tokenList,line,rule)
					|| (rule.action & SOFT_SPAN) == 0)
				{
					escaped = false;
					continue;
				}
			}

			// now check every rule
			rule = context.rules.getRules(line.array[pos]);
			while(rule != null)
			{
				pattern.array = rule.searchChars;

				if (context.inRule == rule && (rule.action & SPAN) == SPAN)
				{
					pattern.count = rule.sequenceLengths[1];
					pattern.offset = rule.sequenceLengths[0];
				}
				else
				{
					pattern.count = rule.sequenceLengths[0];
					pattern.offset = 0;
				}

				// stop checking rules if there was a match and go to next pos
				if (!handleRule(tokenList,line,rule))
					break;

				rule = rule.next;
			}

			escaped = false;
		}

		// check for keywords at the line's end
		if(context.inRule == null)
			markKeyword(tokenList, line, lastKeyword, lineLength);

		// mark all remaining characters
		if(lastOffset != lineLength)
		{
			if (context.inRule == null)
			{
				tokenList.addToken(lineLength - lastOffset,
					context.rules.getDefault());
			}
			else if (
				(context.inRule.action & SPAN) == SPAN &&
				(context.inRule.action & (NO_LINE_BREAK | NO_WORD_BREAK)) != 0
			)
			{
				tokenList.addToken(lineLength - lastOffset,Token.INVALID);
				context.inRule = null;
			}
			else
			{
				tokenList.addToken(lineLength - lastOffset,context.inRule.token);

				if((context.inRule.action & MARK_FOLLOWING) == MARK_FOLLOWING)
				{
					context.inRule = null;
				}
			}
		}

		info.setLineContext(context);
	}

	// private members
	private static final int SOFT_SPAN = MARK_FOLLOWING | NO_WORD_BREAK;

	private String name;
	private String rulePfx;
	private Hashtable ruleSets;

	private LineContext context;
	private Segment pattern = new Segment(new char[0],0,0);
	private int lastOffset;
	private int lastKeyword;
	private int lineLength;
	private int pos;
	private boolean escaped;

	/**
	 * Checks if the rule matches the line at the current position
	 * and handles the rule if it does match
	 * @param line Segment to check rule against
	 * @param checkRule ParserRule to check against line
	 * @return true,  keep checking other rules
	 *     <br>false, stop checking other rules
	 */
	private boolean handleRule(Buffer.TokenList tokenList, Segment line,
		ParserRule checkRule)
	{
		if (pattern.count == 0) return true;

		if (lineLength - pos < pattern.count) return true;

		char a, b;
		for (int k = 0; k < pattern.count; k++)
		{
			a = pattern.array[pattern.offset + k];
			b = line.array[pos + k];

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
			) return true;
		}

		if (escaped)
		{
			pos += pattern.count - 1;
			return false;
		}
		else if ((checkRule.action & IS_ESCAPE) == IS_ESCAPE)
		{
			escaped = true;
			pos += pattern.count - 1;
			return false;
		}

		// handle soft spans
		if (context.inRule != checkRule && context.inRule != null
			&& (context.inRule.action & SOFT_SPAN) != 0)
		{
			if ((context.inRule.action & NO_WORD_BREAK) == NO_WORD_BREAK)
			{
				tokenList.addToken(pos - lastOffset, Token.INVALID);
			}
			else
			{
				tokenList.addToken(pos - lastOffset,context.inRule.token);
			}
			lastOffset = lastKeyword = pos;
			context.inRule = null;
		}

		if (context.inRule == null)
		{
			if ((checkRule.action & AT_LINE_START) == AT_LINE_START)
			{
				if (
					(((checkRule.action & MARK_PREVIOUS) != 0) ?
					lastKeyword :
					pos) != line.offset
				)
				{
					return true;
				}
			}

			markKeyword(tokenList, line, lastKeyword, pos);

			if ((checkRule.action & MARK_PREVIOUS) != MARK_PREVIOUS)
			{
				lastKeyword = pos + pattern.count;

				if ((checkRule.action & WHITESPACE) == WHITESPACE)
				{
					return false; // break out of inner for loop to check next char
				}

				// mark previous sequence as NULL (plain text)
				if (lastOffset < pos)
				{
					tokenList.addToken(pos - lastOffset,
						context.rules.getDefault());
				}
			}

			switch(checkRule.action & MAJOR_ACTIONS)
			{
			case 0:
				// this is a plain sequence rule
				tokenList.addToken(pattern.count,checkRule.token);
				lastOffset = pos + pattern.count;

				break;
			case SPAN:
				context.inRule = checkRule;

				if ((checkRule.action & DELEGATE) != DELEGATE)
				{
					if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
					{
						tokenList.addToken(pattern.count,
							context.rules.getDefault());
						lastOffset = pos + pattern.count;
					}
					else
					{
						lastOffset = pos;
					}
				}
				else
				{
					String setName = new String(checkRule.searchChars,
						checkRule.sequenceLengths[0] + checkRule.sequenceLengths[1],
						checkRule.sequenceLengths[2]);

					ParserRuleSet delegateSet = getRuleSet(setName);

					if (delegateSet != null)
					{
						if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
						{
							tokenList.addToken(pattern.count,
								context.rules.getDefault());
						}
						else
						{
							tokenList.addToken(pattern.count,checkRule.token);
						}
						lastOffset = pos + pattern.count;

						context = new LineContext(delegateSet, context);
					}
				}

				break;
			case EOL_SPAN:
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenList.addToken(pattern.count,
						context.rules.getDefault());
					tokenList.addToken(lineLength - (pos + pattern.count),
						checkRule.token);
				}
				else
				{
					tokenList.addToken(lineLength - pos,
						checkRule.token);
				}
				lastOffset = lineLength;
				lastKeyword = lineLength;
				pos = lineLength;

				return false;
			case MARK_PREVIOUS:
				if (lastKeyword > lastOffset)
				{
					tokenList.addToken(lastKeyword - lastOffset,
						context.rules.getDefault());
					lastOffset = lastKeyword;
				}

				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenList.addToken(pos - lastOffset, checkRule.token);
					tokenList.addToken(pattern.count,
						context.rules.getDefault());
				}
				else
				{
					tokenList.addToken(pos - lastOffset + pattern.count,
						checkRule.token);
				}
				lastOffset = pos + pattern.count;

				break;
			case MARK_FOLLOWING:
				context.inRule = checkRule;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenList.addToken(pattern.count,
						context.rules.getDefault());
					lastOffset = pos + pattern.count;
				}
				else
				{
					lastOffset = pos;
				}

				break;
			default:
				throw new InternalError("Unhandled major action");
			}

			lastKeyword = lastOffset;

			pos += (pattern.count - 1); // move pos to last character of match sequence
			return false; // break out of inner for loop to check next char
		}
		else if ((checkRule.action & SPAN) == SPAN)
		{
			if ((checkRule.action & DELEGATE) != DELEGATE)
			{
				context.inRule = null;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					tokenList.addToken(pos - lastOffset,checkRule.token);
					tokenList.addToken(pattern.count,
						context.rules.getDefault());
				}
				else
				{
					tokenList.addToken((pos + pattern.count) - lastOffset,
						checkRule.token);
				}
				lastKeyword = lastOffset = pos + pattern.count;

				pos += (pattern.count - 1); // move pos to last character of match sequence
			}

			return false; // break out of inner for loop to check next char
		}
		return true;
	}

	private void markKeyword(Buffer.TokenList tokenList, Segment line,
		int start, int end)
	{
		KeywordMap keywords = context.rules.getKeywords();

		int len = end - start;

		// do digits
		if(context.rules.getHighlightDigits())
		{
			boolean digit = true;
			char[] array = line.array;
			boolean octal = false;
			boolean hex = false;
			boolean seenSomeDigits = false;
loop:			for(int i = 0; i < len; i++)
			{
				char ch = array[start+i];
				switch(ch)
				{
				case '0':
					if(i == 0)
						octal = true;
					seenSomeDigits = true;
					continue loop;
				case '1': case '2': case '3':
				case '4': case '5': case '6':
				case '7': case '8': case '9':
					seenSomeDigits = true;
					continue loop;
				case 'x': case 'X':
					if(octal && i == 1)
					{
						hex = true;
						continue loop;
					}
					else
						break;
				case 'd': case 'D':
					if(hex)
						continue loop;
					else
						break;
				case 'f': case 'F':
					if(hex || seenSomeDigits)
						continue loop;
					else
						break;
				case 'l': case 'L':
					if(seenSomeDigits)
						continue loop;
					else
						break;
				case 'e': case 'E':
					if(seenSomeDigits)
						continue loop;
					else
						break;
				case 'a': case 'A': case 'b': case 'B':
				case 'c': case 'C':
					if(hex)
						continue loop;
					else
						break;
				case '.': case '-':
					// normally, this shouldn't be
					// necessary, because most modes
					// define '.' and '-' SEQs. However,
					// in props mode, we can't define
					// such a SEQ because it would
					// break the AT_LINE_START
					// MARK_PREVIOUS rule.

					continue loop;
				default:
					break;
				}

				// if we ended up here, then we have found a
				// non-digit character.
				digit = false;
				break loop;
			}

			// if we got this far with digit = true, then the keyword
			// consists of all digits. Add it as such.
			if(digit && seenSomeDigits)
			{
				if(start != lastOffset)
				{
					tokenList.addToken(start - lastOffset,
						context.rules.getDefault());
				}
				tokenList.addToken(len,Token.DIGIT);
				lastKeyword = lastOffset = end;

				return;
			}
		}

		if(keywords != null)
		{
			byte id = keywords.lookup(line, start, len);

			if(id != Token.NULL)
			{
				if(start != lastOffset)
				{
					tokenList.addToken(start - lastOffset,
						context.rules.getDefault());
				}
				tokenList.addToken(len, id);
				lastKeyword = lastOffset = end;
			}
		}
	}

	public static class LineContext
	{
		public static int COUNT;
		public static int COUNT_GC;
		public LineContext parent;
		public ParserRule inRule;
		public ParserRuleSet rules;

		public LineContext(ParserRule r, ParserRuleSet rs)
		{
			this();
			inRule = r;
			rules = rs;
		}

		public LineContext(ParserRuleSet rs, LineContext lc)
		{
			this();
			rules = rs;
			parent = (lc == null ? null : (LineContext)lc.clone());
		}

		public LineContext(ParserRule r)
		{
			this();
			inRule = r;
		}

		public LineContext()
		{
			COUNT++;
			COUNT_GC++;
		}

		public void finalize()
		{
			COUNT_GC--;
		}

		public Object clone()
		{
			LineContext lc = new LineContext();
			lc.inRule = inRule;
			lc.rules = rules;
			lc.parent = (parent == null) ? null : (LineContext) parent.clone();

			return lc;
		}
	}
}
