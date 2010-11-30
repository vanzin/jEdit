/*
 * IndentRuleFactory.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2005 Slava Pestov
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

package org.gjt.sp.jedit.indent;

import java.util.regex.PatternSyntaxException;

public class IndentRuleFactory
{
	public static IndentRule indentNextLines(String regexp)
		throws PatternSyntaxException
	{
		return new RegexpIndentRule(regexp,
			null,
			new IndentAction.Increase(),
			null,false);
	}

	public static IndentRule indentNextLine(String regexp)
		throws PatternSyntaxException
	{
		return new RegexpIndentRule(regexp,
			new IndentAction.Decrease(),
			new IndentAction.Increase(),
			null,true);
	}

	public static IndentRule unindentThisLine(String regexp)
		throws PatternSyntaxException
	{
		return new RegexpIndentRule(regexp,
			null,
			new IndentAction.Increase(),
			new IndentAction.Decrease(),
			false);
	}

	public static IndentRule unindentNextLines(String regexp)
		throws PatternSyntaxException
	{
		return new RegexpIndentRule(regexp,
			null,
			new IndentAction.Decrease(),
			null,
			false);
	}

	public static IndentRule indentOpenBracket(char bracket)
		throws PatternSyntaxException
	{
		return new OpenBracketIndentRule(bracket,true);
	}

	public static IndentRule indentCloseBracket(char bracket)
		throws PatternSyntaxException
	{
		return new CloseBracketIndentRule(bracket,true);
	}

	public static IndentRule unalignedOpenBracket(char bracket)
		throws PatternSyntaxException
	{
		return new OpenBracketIndentRule(bracket,false);
	}

	public static IndentRule unalignedCloseBracket(char bracket)
		throws PatternSyntaxException
	{
		return new CloseBracketIndentRule(bracket,false);
	}
}
