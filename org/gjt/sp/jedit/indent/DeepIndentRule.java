/*
 * DeepIndentRule.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Matthieu Casanova
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

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import java.util.List;

/**
 * Deep indent rule.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public class DeepIndentRule implements IndentRule
{

	/**
	 * Return the indexes of the last closing and the last opening parens in a line
	 *
	 * @param s   the line text
	 * @param pos the starting pos from the end (or -1 for entire line)
	 *
	 * @return the last pos of the parens in the line
	 */
	 private Parens getLastParens(String s, int pos)
	 {
		 int lastClose;
		 int lastOpen;
		 if (pos == -1)
		 {
			 lastClose = s.lastIndexOf(')');
			 lastOpen = s.lastIndexOf('(');
		 }
		 else
		 {
			 lastClose = s.lastIndexOf(')', pos);
			 lastOpen = s.lastIndexOf('(', pos);
		 }
		 return new Parens(lastOpen, lastClose);
	 }

	 //{{{ apply() method
	 public void apply(JEditBuffer buffer, int thisLineIndex,
			    int prevLineIndex, int prevPrevLineIndex,
			    List indentActions)
	 {
		 if (prevLineIndex == -1)
			 return;
		 
		 int lineIndex = prevLineIndex;
		 int searchPos = -1;
		 boolean shouldIndent = false;
		 while (true)
		 {
			 String lineText = buffer.getLineText(lineIndex);
			 Parens parens = getLastParens(lineText, searchPos);
			 if (parens.openOffset > parens.closeOffset)
			 {
				 indentActions.add(new IndentAction.AlignParameter(parens.openOffset, lineText));
				 return;
			 }
			 
			 // No parens on prev line
			 if (parens.openOffset == -1 && parens.closeOffset == -1)
			 {
				 if (shouldIndent)
				 {
					 int i = buffer.getCurrentIndentForLine(lineIndex, null);
					 indentActions.add(new IndentAction.AlignParameter(i - 1, lineText));
				 }
				 return;
			 }
			 shouldIndent = true;
			 int openParenOffset = TextUtilities.findMatchingBracket(buffer, lineIndex, parens.closeOffset);
			 if (openParenOffset >= 0)
			 {
				 lineIndex = buffer.getLineOfOffset(openParenOffset);
				 searchPos = openParenOffset - buffer.getLineStartOffset(lineIndex) - 1;
			 }
			 else
				 break;
		 }
	 }
	 
	 private class Parens
	 {
		 int openOffset;
		 int closeOffset;
		 
		 public Parens(int openOffset, int closeOffset)
		 {
			 this.openOffset = openOffset;
			 this.closeOffset = closeOffset;
		 }
	 } //}}}
}

