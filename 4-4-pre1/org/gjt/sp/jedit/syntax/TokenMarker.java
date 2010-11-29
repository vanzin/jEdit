/*
 * TokenMarker.java - Tokenizes lines of text
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1998, 2003 Slava Pestov
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
import javax.swing.text.Segment;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.util.SegmentCharSequence;
import org.gjt.sp.util.StandardUtilities;
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
	//{{{ TokenMarker constructor
	public TokenMarker()
	{} //}}}

	//{{{ addRuleSet() method
	public void addRuleSet(ParserRuleSet rules)
	{
		ruleSets.put(rules.getSetName(), rules);

		if (rules.getSetName().equals("MAIN"))
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
		return ruleSets.get(setName);
	} //}}}

	//{{{ getRuleSets() method
	/**
	 * @since jEdit 4.2pre3
	 */
	public ParserRuleSet[] getRuleSets()
	{
		return ruleSets.values().toArray(new ParserRuleSet[ruleSets.size()]);
	} //}}}

	//{{{ markTokens() method
	/**
	 * Do not call this method directly; call Buffer.markTokens() instead.
	 *
	 * @param prevContext the context of the previous line, it can be null
	 * @param tokenHandler the token handler
	 * @param line a segment containing the content of the line
	 */
	public synchronized LineContext markTokens(LineContext prevContext,
		TokenHandler tokenHandler, Segment line)
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
		{
			context.rules = getMainRuleSet();
			context.escapeRule = context.rules.getEscapeRule();
		}
		else
		{
			context.parent = prevContext.parent;
			context.setInRule(prevContext.inRule);
			context.rules = prevContext.rules;
			context.spanEndSubst = prevContext.spanEndSubst;
		}

		keywords = context.rules.getKeywords();

		seenWhitespaceEnd = false;
		whitespaceEnd = line.offset;
		//}}}

		//{{{ Main parser loop
		int terminateChar = context.rules.getTerminateChar();
		boolean terminated = false;
main_loop:	for(pos = line.offset; pos < lineLength; pos++)
		{
			//{{{ check if we have to stop parsing (happens if the terminateChar has been exceeded)
			if(terminateChar >= 0 && pos - line.offset >= terminateChar
				&& !terminated)
			{
				terminated = true;
				context = new LineContext(ParserRuleSet
					.getStandardRuleSet(context.rules
					.getDefault()),context);
				keywords = context.rules.getKeywords();
			} //}}}

			//{{{ Check for the escape rule before anything else.
			if (context.escapeRule != null &&
				handleRuleStart(context.escapeRule))
			{
				continue main_loop;
			} //}}}

			//{{{ check for end of delegate
			if (context.parent != null
			    && context.parent.inRule != null
			    && checkDelegateEnd(context.parent.inRule))
			{
				seenWhitespaceEnd = true;
				continue main_loop;
			} //}}}

			//{{{ check every rule
			Character ch = Character.valueOf(line.array[pos]);
			List<ParserRule> rules = context.rules.getRules(ch);
			for (ParserRule rule : rules)
			{
				// stop checking rules if there was a match
				if (handleRuleStart(rule))
				{
					seenWhitespaceEnd = true;
					continue main_loop;
				}
			} //}}}

			//{{{ check if current character is a word separator
			if(Character.isWhitespace(ch))
			{
				if(!seenWhitespaceEnd)
					whitespaceEnd = pos + 1;

				if(context.inRule != null)
					handleRuleEnd(context.inRule);

				handleNoWordBreak();

				markKeyword(false);

				if(lastOffset != pos)
				{
					tokenHandler.handleToken(line,
						context.rules.getDefault(),
						lastOffset - line.offset,
						pos - lastOffset,
						context);
				}

				tokenHandler.handleToken(line,
					context.rules.getDefault(),
					pos - line.offset,1,context);
				lastOffset = pos + 1;
			}
			else
			{
				if(keywords != null || context.rules.getRuleCount() != 0)
				{
					String noWordSep = context.rules.getNoWordSep();

					if(!Character.isLetterOrDigit(ch)
						&& noWordSep.indexOf(ch) == -1)
					{
						if(context.inRule != null)
							handleRuleEnd(context.inRule);

						handleNoWordBreak();

						markKeyword(true);

						tokenHandler.handleToken(line,
							context.rules.getDefault(),
							lastOffset - line.offset,1,
							context);
						lastOffset = pos + 1;
					}
				}

				seenWhitespaceEnd = true;
			} //}}}
		} //}}}

		//{{{ Mark all remaining characters
		pos = lineLength;

		if(context.inRule != null)
			handleRuleEnd(context.inRule);

		handleNoWordBreak();
		markKeyword(true);
		//}}}

		//{{{ Unwind any NO_LINE_BREAK parent delegates
unwind:		while(context.parent != null)
		{
			ParserRule rule = context.parent.inRule;
			if((rule != null && (rule.action
				& ParserRule.NO_LINE_BREAK) == ParserRule.NO_LINE_BREAK)
				|| terminated)
			{
				context = context.parent;
				keywords = context.rules.getKeywords();
				context.setInRule(null);
			}
			else
				break unwind;
		} //}}}

		tokenHandler.handleToken(line,Token.END,
			pos - line.offset,0,context);

		context = context.intern();
		tokenHandler.setLineContext(context);

		/* for GC. */
		this.tokenHandler = null;
		this.line = null;

		return context;
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final Map<String, ParserRuleSet> ruleSets = new Hashtable<String, ParserRuleSet>(64);
	private ParserRuleSet mainRuleSet;

	// Instead of passing these around to each method, we just store them
	// as instance variables. Note that this is not thread-safe.
	private TokenHandler tokenHandler;
	/** The line from which we will mark the tokens. */
	private Segment line;
	/** The context of the current line. */
	private LineContext context;
	private KeywordMap keywords;
	private final Segment pattern = new Segment();
	private int lastOffset;
	private int lineLength;
	private int pos;

	private int whitespaceEnd;
	private boolean seenWhitespaceEnd;
	//}}}

	//{{{ checkDelegateEnd() method
	private boolean checkDelegateEnd(ParserRule rule)
	{
		if(rule.end == null)
			return false;

		LineContext tempContext = context;
		context = context.parent;
		keywords = context.rules.getKeywords();
		boolean handled = handleRuleEnd(rule);
		context = tempContext;
		keywords = context.rules.getKeywords();

		if (handled)
		{
			if(context.inRule != null)
				handleRuleEnd(context.inRule);

			markKeyword(true);

			context = (LineContext)context.parent.clone();

			tokenHandler.handleToken(line,
				matchToken(context.inRule, context.inRule, context),
				pos - line.offset,pattern.count,context);

			keywords = context.rules.getKeywords();
			context.setInRule(null);
			lastOffset = pos + pattern.count;

			// move pos to last character of match sequence
			pos += pattern.count - 1;

			return true;
		}

		return false;
	} //}}}

	//{{{ offsetMatches
	/**
	 * Checks if the offset matches given position-match-hint of
	 * ParserRule.
	 */
	private boolean offsetMatches(int offset, int posMatch)
	{
		if((posMatch & ParserRule.AT_LINE_START)
			== ParserRule.AT_LINE_START)
		{
			if(offset != line.offset)
			{
				return false;
			}
		}
		else if((posMatch & ParserRule.AT_WHITESPACE_END)
			== ParserRule.AT_WHITESPACE_END)
		{
			if(offset != whitespaceEnd)
			{
				return false;
			}
		}
		else if((posMatch & ParserRule.AT_WORD_START)
			== ParserRule.AT_WORD_START)
		{
			if(offset != lastOffset)
			{
				return false;
			}
		}

		return true;
	} //}}}

	//{{{ handleRuleStart() method
	/**
	 * Checks if the rule matches the line at the current position
	 * as its start and handles the rule if it does match
	 */
	private boolean handleRuleStart(ParserRule checkRule)
	{
		// Some rules can only match in certain locations
		if (null == checkRule.upHashChars)
		{
			if (checkRule.upHashChar != null &&
			    (pos + checkRule.upHashChar.length() < line.array.length) &&
			    !checkHashString(checkRule))
			{
				return false;
			}
		}
		else
		{
			if (-1 == Arrays.binarySearch(
					checkRule.upHashChars,
					Character.toUpperCase(line.array[pos])))
			{
				return false;
			}
		}

		int offset = (checkRule.action & ParserRule.MARK_PREVIOUS) != 0 ? lastOffset : pos;
		if(!offsetMatches(offset, checkRule.startPosMatch))
		{
			return false;
		}

		int matchedChars = 1;
		CharSequence charSeq = null;
		Matcher match = null;

		// See if the rule's start sequence matches here
		if((checkRule.action & ParserRule.REGEXP) == 0)
		{
			pattern.array = checkRule.start;
			pattern.offset = 0;
			pattern.count = pattern.array.length;
			matchedChars = pattern.count;

			if(!SyntaxUtilities.regionMatches(context.rules
				.getIgnoreCase(),line,pos,pattern.array))
			{
				return false;
			}
		}
		else
		{
			// note that all regexps start with \A so they only
			// match the start of the string
			//int matchStart = pos - line.offset;
			charSeq = new SegmentCharSequence(line, pos - line.offset,
							  line.count - (pos - line.offset));
			match = checkRule.startRegexp.matcher(charSeq);
			if(!match.lookingAt())
			{
				return false;
			}
			else if(match.start() != 0)
			{
				throw new InternalError("Can't happen");
			}
			else
			{
				matchedChars = match.end();
				/* workaround for hang if match was
				 * zero-width. not sure if there is
				 * a better way to handle this */
				if(matchedChars == 0)
					matchedChars = 1;
			}
		}

		if((checkRule.action & ParserRule.IS_ESCAPE) == ParserRule.IS_ESCAPE)
		{
			pos += pattern.count;
		}
		else
		{
			if(context.inRule != null)
				handleRuleEnd(context.inRule);

			markKeyword((checkRule.action & ParserRule.MARK_PREVIOUS)
				!= ParserRule.MARK_PREVIOUS);

			switch(checkRule.action & ParserRule.MAJOR_ACTIONS)
			{
			//{{{ SEQ
			case ParserRule.SEQ:
				context.spanEndSubst = null;

				if((checkRule.action & ParserRule.REGEXP) != 0)
				{
					handleTokenWithSpaces(tokenHandler,
						checkRule.token,
						pos - line.offset,
						matchedChars,
						context);
				}
				else
				{
					tokenHandler.handleToken(line,
						checkRule.token,
						pos - line.offset,
						matchedChars,context);
				}

				// a DELEGATE attribute on a SEQ changes the
				// ruleset from the end of the SEQ onwards
				if(checkRule.delegate != null)
				{
					context = new LineContext(
						checkRule.delegate,
						context.parent);
					keywords = context.rules.getKeywords();
				}
				break;
			//}}}
			//{{{ SPAN, EOL_SPAN
			case ParserRule.SPAN:
			case ParserRule.EOL_SPAN:
				context.setInRule(checkRule);

				byte tokenType = matchToken(checkRule,
							context.inRule, context);

				if((checkRule.action & ParserRule.REGEXP) != 0)
				{
					handleTokenWithSpaces(tokenHandler,
						tokenType,
						pos - line.offset,
						matchedChars,
						context);
				}
				else
				{
					tokenHandler.handleToken(line,tokenType,
						pos - line.offset,
						matchedChars,context);
				}

				char[] spanEndSubst = null;
				/* substitute result of matching the rule start
				 * into the end string.
				 *
				 * eg, in shell script mode, <<\s*(\w+) is
				 * matched into \<$1\> to construct rules for
				 * highlighting read-ins like this <<EOF
				 * ...
				 * EOF
				 */
				if(charSeq != null && checkRule.end != null)
				{
					spanEndSubst = substitute(match,
						checkRule.end);
				}

				context.spanEndSubst = spanEndSubst;
				context = new LineContext(
					checkRule.delegate,
					context);
				keywords = context.rules.getKeywords();

				break;
			//}}}
			//{{{ MARK_FOLLOWING
			case ParserRule.MARK_FOLLOWING:
				tokenHandler.handleToken(line,
					matchToken(checkRule, checkRule, context),
					pos - line.offset,
					pattern.count,context);

				context.spanEndSubst = null;
				context.setInRule(checkRule);
				break;
			//}}}
			//{{{ MARK_PREVIOUS
			case ParserRule.MARK_PREVIOUS:
				context.spanEndSubst = null;

				if(pos != lastOffset)
				{
					tokenHandler.handleToken(line,
						checkRule.token,
						lastOffset - line.offset,
						pos - lastOffset,
						context);
				}

				tokenHandler.handleToken(line,
					matchToken(checkRule, checkRule, context),
					pos - line.offset,pattern.count,
					context);

				break;
			//}}}
			default:
				throw new InternalError("Unhandled major action");
			}

			// move pos to last character of match sequence
			pos += matchedChars - 1;
			lastOffset = pos + 1;

			// break out of inner for loop to check next char
		}

		return true;
	} //}}}

	//{{{ handleRuleEnd() method
	/**
	 * Checks if the rule matches the line at the current position
	 * as its end and handles the rule if it does match
	 */
	private boolean handleRuleEnd(ParserRule checkRule)
	{
		// Some rules can only match in certain locations
		int offset = (checkRule.action & ParserRule.MARK_PREVIOUS) != 0 ? lastOffset : pos;
		if (!offsetMatches(offset, checkRule.endPosMatch))
		{
			return false;
		}

		// See if the rule's end sequence matches here
		if((checkRule.action & ParserRule.MARK_FOLLOWING) == 0)
		{
			if(context.spanEndSubst != null)
				pattern.array = context.spanEndSubst;
			else
				pattern.array = checkRule.end;
			pattern.offset = 0;
			pattern.count = pattern.array.length;

			if(!SyntaxUtilities.regionMatches(context.rules
				.getIgnoreCase(),line,pos,pattern.array))
			{
				return false;
			}
		}

		// Escape rules are handled in handleRuleStart()
		assert (checkRule.action & ParserRule.IS_ESCAPE) == 0;

		// Handle end of MARK_FOLLOWING
		if((context.inRule.action & ParserRule.MARK_FOLLOWING) != 0)
		{
			if(pos != lastOffset)
			{
				tokenHandler.handleToken(line,
					context.inRule.token,
					lastOffset - line.offset,
					pos - lastOffset,context);
			}

			lastOffset = pos;
			context.setInRule(null);
		}

		return true;
	} //}}}

	//{{{ handleNoWordBreak() method
	private void handleNoWordBreak()
	{
		if(context.parent != null)
		{
			ParserRule rule = context.parent.inRule;
			if(rule != null && (context.parent.inRule.action
				& ParserRule.NO_WORD_BREAK) != 0)
			{
				if(pos != lastOffset)
				{
					tokenHandler.handleToken(line,
						rule.token,
						lastOffset - line.offset,
						pos - lastOffset,context);
				}

				lastOffset = pos;
				context = context.parent;
				keywords = context.rules.getKeywords();
				context.setInRule(null);
			}
		}
	} //}}}

	//{{{ handleTokenWithSpaces() method
	private void handleTokenWithSpaces(TokenHandler tokenHandler,
		byte tokenType, int start, int len, LineContext context)
	{
		int last = start;
		int end = start + len;

		for(int i = start; i < end; i++)
		{
			if(Character.isWhitespace(line.array[i + line.offset]))
			{
				if(last != i)
				{
					tokenHandler.handleToken(line,
					tokenType,last,i - last,context);
				}
				tokenHandler.handleToken(line,tokenType,i,1,context);
				last = i + 1;
			}
		}

		if(last != end)
		{
			tokenHandler.handleToken(line,tokenType,last,
				end - last,context);
		}
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
				Pattern digitRE = context.rules.getDigitRegexp();

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
						int oldCount = line.count;
						int oldOffset = line.offset;
						line.offset = lastOffset;
						line.count = len;
						CharSequence seq = new SegmentCharSequence(line);
						digit = digitRE.matcher(seq).matches();
						line.offset = oldOffset;
						line.count = oldCount;
					}
				}
			}

			if(digit)
			{
				tokenHandler.handleToken(line,Token.DIGIT,
					lastOffset - line.offset,
					len,context);
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
				tokenHandler.handleToken(line,id,
					lastOffset - line.offset,
					len,context);
				lastOffset = pos;
				return;
			}
		} //}}}

		//{{{ Handle any remaining crud
		if(addRemaining)
		{
			tokenHandler.handleToken(line,context.rules.getDefault(),
				lastOffset - line.offset,len,context);
			lastOffset = pos;
		} //}}}
	} //}}}

	//{{{ substitute() method
	private static char[] substitute(Matcher match, char[] end)
	{
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < end.length; i++)
		{
			char ch = end[i];
			if(ch == '$' || ch == '~')
			{
				if(i == end.length - 1)
					buf.append(ch);
				else
				{
					char digit = end[i + 1];
					if(!Character.isDigit(digit))
						buf.append(ch);
					else if (ch == '$')
					{
						buf.append(match.group(
							digit - '0'));
						i++;
					}
					else
					{
						String s = match.group(digit - '0');
						if (s.length() == 1)
						{
							char b = TextUtilities.getComplementaryBracket(s.charAt(0), null);
							if (b == '\0')
								b = s.charAt(0);
							buf.append(b);
						}
						else
							buf.append(ch);
						i++;
					}
				}
			}
			else
				buf.append(ch);
		}

		char[] returnValue = new char[buf.length()];
		buf.getChars(0,buf.length(),returnValue,0);
		return returnValue;
	} //}}}

	//{{{ matchToken() method
	private byte matchToken(ParserRule rule, ParserRule base, LineContext ctx)
	{
		switch (rule.matchType)
		{
			case ParserRule.MATCH_TYPE_RULE:
				return base.token;

			case ParserRule.MATCH_TYPE_CONTEXT:
				return context.rules.getDefault();

			default:
				return rule.matchType;
		}
	} //}}}

	//{{{ checkHashString() method
	private boolean checkHashString(ParserRule rule)
	{
		for (int i = 0; i < rule.upHashChar.length(); i++)
		{
			if (Character.toUpperCase(line.array[pos+i]) != rule.upHashChar.charAt(i))
			{
				return false;
			}
		}
		return true;
	} //}}}

	//}}}

	//{{{ LineContext class
	/**
	 * Stores persistent per-line syntax parser state.
	 */
	public static class LineContext
	{
		private static final Map<LineContext, LineContext> intern = new HashMap<LineContext, LineContext>();

		public LineContext parent;
		public ParserRule inRule;
		public ParserRuleSet rules;
		// used for SPAN_REGEXP rules; otherwise null
		public char[] spanEndSubst;
		public ParserRule escapeRule;

		//{{{ LineContext constructor
		public LineContext(ParserRuleSet rs, LineContext lc)
		{
			rules = rs;
			parent = (lc == null ? null : (LineContext)lc.clone());
			/*
			 * SPANs with no delegate need to propagate the
			 * escape rule to the child context, so this is
			 * needed.
			 */
			if (rs.getModeName() != null)
				escapeRule = rules.getEscapeRule();
			else
				escapeRule = lc.escapeRule;
		} //}}}

		//{{{ LineContext constructor
		public LineContext()
		{
		} //}}}

		//{{{ intern() method
		public LineContext intern()
		{
			LineContext obj = intern.get(this);
			if(obj == null)
			{
				intern.put(this,this);
				return this;
			}
			else
				return obj;
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
				return lc.inRule == inRule && lc.rules == rules
					&& StandardUtilities.objectsEqual(parent,lc.parent)
					&& charArraysEqual(spanEndSubst,lc.spanEndSubst);
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
			lc.spanEndSubst = spanEndSubst;
			lc.escapeRule = escapeRule;

			return lc;
		} //}}}

		//{{{ charArraysEqual() method
		private static boolean charArraysEqual(char[] c1, char[] c2)
		{
			if(c1 == null)
				return c2 == null;

			// c1 is not null
			if(c2 == null)
				return false;

			if(c1.length != c2.length)
				return false;

			for(int i = 0; i < c1.length; i++)
			{
				if(c1[i] != c2[i])
					return false;
			}

			return true;
		} //}}}

		//{{{ setInRule() method
		/**
		 * Sets the current rule being processed and adjusts the
		 * escape rule for the context based on the rule.
		 */
		public void setInRule(ParserRule rule)
		{
			inRule = rule;
			if (rule != null && rule.escapeRule != null)
				escapeRule = rule.escapeRule;
			else if (rules != null && rules.getModeName() != null)
				escapeRule = rules.getEscapeRule();
			else if (parent != null)
				escapeRule = parent.escapeRule;
			else
				escapeRule = null;
		} //}}}

	} //}}}
}
