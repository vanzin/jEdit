/*
 * XModeHandler.java - XML handler for mode files
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 mike dillon
 * Portions copyright (C) 2000, 2001 Slava Pestov
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
import com.microstar.xml.*;
import gnu.regexp.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
//}}}

/**
 * XML handler for mode definition files.
 */
public class XModeHandler extends HandlerBase
{
	//{{{ XModeHandler constructor
	public XModeHandler (XmlParser parser, String modeName, String path)
	{
		this.modeName = modeName;
		this.parser = parser;
		this.path = path;
		stateStack = new Stack();

		// default value
		lastNoWordSep = "_";
	} //}}}

	//{{{ resolveEntity() method
	public Object resolveEntity(String publicId, String systemId)
	{
		if("xmode.dtd".equals(systemId))
		{
			// this will result in a slight speed up, since we
			// don't need to read the DTD anyway, as AElfred is
			// non-validating
			return new StringReader("<!-- -->");

			/* try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream(
					"/org/gjt/sp/jedit/syntax/xmode.dtd")));
			}
			catch(Exception e)
			{
				error("dtd",e);
			} */
		}

		return null;
	} //}}}

	//{{{ attribute() method
	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();

		if (aname == "NAME")
		{
			propName = value;
		}
		else if (aname == "VALUE")
		{
			propValue = value;
		}
		else if (aname == "TYPE")
		{
			lastTokenID = Token.stringToToken(value);
			if(lastTokenID == -1)
				error("token-invalid",value);
		}
		else if (aname == "AT_LINE_START")
		{
			lastAtLineStart = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "AT_WHITESPACE_END")
		{
			lastAtWhitespaceEnd = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "AT_WORD_START")
		{
			lastAtWordStart = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "NO_LINE_BREAK")
		{
			lastNoLineBreak = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "NO_WORD_BREAK")
		{
			lastNoWordBreak = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "EXCLUDE_MATCH")
		{
			lastExcludeMatch = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "IGNORE_CASE")
		{
			lastIgnoreCase = (isSpecified) ? (value.equals("TRUE")) :
				true;
		}
		else if (aname == "HIGHLIGHT_DIGITS")
		{
			lastHighlightDigits = (isSpecified) ? (value.equals("TRUE")) :
				false;
		}
		else if (aname == "DIGIT_RE")
		{
			lastDigitRE = value;
		}
		else if (aname == "NO_WORD_SEP")
		{
			if(isSpecified)
				lastNoWordSep = value;
		}
		else if (aname == "AT_CHAR")
		{
			try
			{
				if (isSpecified) termChar =
					Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				error("termchar-invalid",value);
				termChar = -1;
			}
		}
		else if (aname == "ESCAPE")
		{
			lastEscape = value;
		}
		else if (aname == "SET")
		{
			lastSetName = value;
		}
		else if (aname == "DELEGATE")
		{
			lastDelegateSet = value;
			if (lastDelegateSet != null
				&& lastDelegateSet.indexOf("::") == -1)
			{
				lastDelegateSet = modeName + "::" + lastDelegateSet;
			}
		}
		else if (aname == "DEFAULT")
		{
			lastDefaultID = Token.stringToToken(value);
			if(lastDefaultID == -1)
			{
				error("token-invalid",value);
				lastDefaultID = Token.NULL;
			}
		}
		else if (aname == "HASH_CHAR")
		{
			if(value.length() != 1)
			{
				error("hash-char-invalid",value);
				lastDefaultID = Token.NULL;
			}
			else
				lastHashChar = value.charAt(0);
		}
	} //}}}

	//{{{ doctypeDecl() method
	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if ("MODE".equalsIgnoreCase(name)) return;

		error("doctype-invalid",name);
	} //}}}

	//{{{ charData() method
	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if (tag == "EOL_SPAN" ||
			tag == "EOL_SPAN_REGEXP" ||
			tag == "MARK_PREVIOUS" ||
			tag == "MARK_FOLLOWING" ||
			tag == "SEQ" ||
			tag == "SEQ_REGEXP" ||
			tag == "BEGIN"
		)
		{
			lastStart = text;
			lastStartPosMatch = ((lastAtLineStart ? ParserRule.AT_LINE_START : 0)
				| (lastAtWhitespaceEnd ? ParserRule.AT_WHITESPACE_END : 0)
				| (lastAtWordStart ? ParserRule.AT_WORD_START : 0));
			lastAtLineStart = false;
			lastAtWordStart = false;
			lastAtWhitespaceEnd = false;
		}
		else if (tag == "END")
		{
			lastEnd = text;
			lastEndPosMatch = ((lastAtLineStart ? ParserRule.AT_LINE_START : 0)
				| (lastAtWhitespaceEnd ? ParserRule.AT_WHITESPACE_END : 0)
				| (lastAtWordStart ? ParserRule.AT_WORD_START : 0));
			lastAtLineStart = false;
			lastAtWordStart = false;
			lastAtWhitespaceEnd = false;
		}
		else
		{
			lastKeyword = text;
		}
	} //}}}

	//{{{ startElement() method
	public void startElement (String tag)
	{
		tag = pushElement(tag);

		if (tag == "WHITESPACE")
		{
			Log.log(Log.WARNING,this,path + ": WHITESPACE rule "
				+ "no longer needed");
		}
		else if (tag == "MODE")
		{
			mode = jEdit.getMode(modeName);
			if (mode == null)
			{
				mode = new Mode(modeName);
				jEdit.addMode(mode);
			}
		}
		else if (tag == "KEYWORDS")
		{
			keywords = new KeywordMap(rules.getIgnoreCase());
		}
		else if (tag == "RULES")
		{
			rules = new ParserRuleSet(lastSetName,mode);
			rules.setIgnoreCase(lastIgnoreCase);
			rules.setHighlightDigits(lastHighlightDigits);
			if(lastDigitRE != null)
			{
				try
				{
					rules.setDigitRegexp(new RE(lastDigitRE,
						lastIgnoreCase
						? RE.REG_ICASE : 0,
						RESearchMatcher.RE_SYNTAX_JEDIT));
				}
				catch(REException e)
				{
					error("regexp",e);
				}
			}

			if(lastEscape != null)
				rules.setEscapeRule(ParserRule.createEscapeRule(lastEscape));
			rules.setDefault(lastDefaultID);
			rules.setNoWordSep(lastNoWordSep);
		}
	} //}}}

	//{{{ endElement() method
	public void endElement (String name)
	{
		if (name == null) return;

		String tag = popElement();

		if (name.equals(tag))
		{
			//{{{ MODE
			if (tag == "MODE")
			{
				// no need for this anymore
				//mode.init();
				mode.setTokenMarker(marker);
			} //}}}
			//{{{ PROPERTY
			else if (tag == "PROPERTY")
			{
				props.put(propName,propValue);
			} //}}}
			//{{{ PROPS
			else if (tag == "PROPS")
			{
				if(peekElement().equals("RULES"))
					rules.setProperties(props);
				else
					mode.setProperties(props);

				props = new Hashtable();
			} //}}}
			//{{{ RULES
			else if (tag == "RULES")
			{
				rules.setKeywords(keywords);
				marker.addRuleSet(lastSetName, rules);
				keywords = null;
				lastSetName = null;
				lastEscape = null;
				lastIgnoreCase = true;
				lastHighlightDigits = false;
				lastDigitRE = null;
				lastDefaultID = Token.NULL;
				lastNoWordSep = "_";
				rules = null;
			} //}}}
			//{{{ TERMINATE
			else if (tag == "TERMINATE")
			{
				rules.setTerminateChar(termChar);
				termChar = -1;
			} //}}}
			//{{{ SEQ
			else if (tag == "SEQ")
			{
				if(lastStart == null)
				{
					error("empty-tag","SEQ");
					return;
				}

				rules.addRule(ParserRule.createSequenceRule(
					lastStartPosMatch,lastStart,lastDelegateSet,
					lastTokenID));
				reset();
			} //}}}
			//{{{ SEQ_REGEXP
			else if (tag == "SEQ_REGEXP")
			{
				if(lastStart == null)
				{
					error("empty-tag","SEQ_REGEXP");
					return;
				}

				try
				{
					rules.addRule(ParserRule.createRegexpSequenceRule(
						lastHashChar,lastStartPosMatch,
						lastStart,lastDelegateSet,lastTokenID,
						lastIgnoreCase));
				}
				catch(REException re)
				{
					error("regexp",re);
				}

				reset();
			} //}}}
			//{{{ SPAN
			else if (tag == "SPAN")
			{
				if(lastStart == null)
				{
					error("empty-tag","BEGIN");
					return;
				}

				if(lastEnd == null)
				{
					error("empty-tag","END");
					return;
				}

				rules.addRule(ParserRule
					.createSpanRule(
					lastStartPosMatch,lastStart,
					lastEndPosMatch,lastEnd,
					lastDelegateSet,
					lastTokenID,lastExcludeMatch,
					lastNoLineBreak,
					lastNoWordBreak));

				reset();
			} //}}}
			//{{{ SPAN_REGEXP
			else if (tag == "SPAN_REGEXP")
			{
				if(lastStart == null)
				{
					error("empty-tag","BEGIN");
					return;
				}

				if(lastEnd == null)
				{
					error("empty-tag","END");
					return;
				}

				try
				{
					rules.addRule(ParserRule
						.createRegexpSpanRule(
						lastHashChar,
						lastStartPosMatch,lastStart,
						lastEndPosMatch,lastEnd,
						lastDelegateSet,
						lastTokenID,
						lastExcludeMatch,
						lastNoLineBreak,
						lastNoWordBreak,
						lastIgnoreCase));
				}
				catch(REException re)
				{
					error("regexp",re);
				}

				reset();
			} //}}}
			//{{{ EOL_SPAN
			else if (tag == "EOL_SPAN")
			{
				if(lastStart == null)
				{
					error("empty-tag","EOL_SPAN");
					return;
				}

				rules.addRule(ParserRule.createEOLSpanRule(
					lastStartPosMatch,lastStart,
					lastDelegateSet,lastTokenID,
					lastExcludeMatch));

				reset();
			} //}}}
			//{{{ EOL_SPAN_REGEXP
			else if (tag == "EOL_SPAN_REGEXP")
			{
				if(lastStart == null)
				{
					error("empty-tag","EOL_SPAN_REGEXP");
					return;
				}

				try
				{
					rules.addRule(ParserRule.createRegexpEOLSpanRule(
						lastHashChar,lastStartPosMatch,lastStart,
						lastDelegateSet,lastTokenID,
						lastExcludeMatch,lastIgnoreCase));
				}
				catch(REException re)
				{
					error("regexp",re);
				}

				reset();
			} //}}}
			//{{{ MARK_FOLLOWING
			else if (tag == "MARK_FOLLOWING")
			{
				if(lastStart == null)
				{
					error("empty-tag","MARK_FOLLOWING");
					return;
				}

				rules.addRule(ParserRule
					.createMarkFollowingRule(
					lastStartPosMatch,lastStart,
					lastTokenID,lastExcludeMatch));
				reset();
			} //}}}
			//{{{ MARK_PREVIOUS
			else if (tag == "MARK_PREVIOUS")
			{
				if(lastStart == null)
				{
					error("empty-tag","MARK_PREVIOUS");
					return;
				}

				rules.addRule(ParserRule
					.createMarkPreviousRule(
					lastStartPosMatch,lastStart,
					lastTokenID,lastExcludeMatch));
				reset();
			} //}}}
			//{{{ Keywords
			else
			{
				byte token = Token.stringToToken(tag);
				if(token != -1)
					addKeyword(lastKeyword,token);
			} //}}}
		}
		else
		{
			// can't happen
			throw new InternalError();
		}
	} //}}}

	//{{{ startDocument() method
	public void startDocument()
	{
		marker = new TokenMarker();
		marker.setName(modeName);
		props = new Hashtable();

		pushElement(null);
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private XmlParser parser;
	private String modeName;
	private String path;

	private TokenMarker marker;
	private KeywordMap keywords;
	private Mode mode;
	private Stack stateStack;
	private String propName;
	private String propValue;
	private Hashtable props;
	private String lastStart;
	private String lastEnd;
	private String lastKeyword;
	private String lastSetName;
	private String lastEscape;
	private String lastDelegateSet;
	private String lastNoWordSep;
	private ParserRuleSet rules;
	private byte lastDefaultID = Token.NULL;
	private byte lastTokenID;
	private int termChar = -1;
	private boolean lastNoLineBreak;
	private boolean lastNoWordBreak;
	private boolean lastExcludeMatch;
	private boolean lastIgnoreCase = true;
	private boolean lastHighlightDigits;
	private boolean lastAtLineStart;
	private boolean lastAtWhitespaceEnd;
	private boolean lastAtWordStart;
	private int lastStartPosMatch;
	private int lastEndPosMatch;
	private String lastDigitRE;
	private char lastHashChar;
	//}}}

	//{{{ reset() method
	private void reset()
	{
		lastHashChar = '\0';
		lastStartPosMatch = 0;
		lastStart = null;
		lastEndPosMatch = 0;
		lastEnd = null;
		lastDelegateSet = null;
		lastTokenID = Token.NULL;
		lastExcludeMatch = false;
		lastNoLineBreak = false;
		lastNoWordBreak = false;
	} //}}}

	//{{{ addKeyword() method
	private void addKeyword(String k, byte id)
	{
		if(k == null)
		{
			error("empty-keyword");
			return;
		}

		if (keywords == null) return;
		keywords.add(k,id);
	} //}}}

	//{{{ pushElement() method
	private String pushElement(String name)
	{
		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	} //}}}

	//{{{ peekElement() method
	private String peekElement()
	{
		return (String) stateStack.peek();
	} //}}}

	//{{{ popElement() method
	private String popElement()
	{
		return (String) stateStack.pop();
	} //}}}

	//{{{ error() method
	private void error(String msg)
	{
		_error(jEdit.getProperty("xmode-error." + msg));
	} //}}}

	//{{{ error() method
	private void error(String msg, String subst)
	{
		_error(jEdit.getProperty("xmode-error." + msg,new String[] { subst }));
	} //}}}

	//{{{ error() method
	private void error(String msg, Throwable t)
	{
		_error(jEdit.getProperty("xmode-error." + msg,new String[] { t.toString() }));
		Log.log(Log.ERROR,this,t);
	} //}}}

	//{{{ _error() method
	private void _error(String msg)
	{
		Object[] args = { path, new Integer(parser.getLineNumber()),
			new Integer(parser.getColumnNumber()), msg };

		GUIUtilities.error(null,"xmode-error",args);
	} //}}}

	//}}}
}
