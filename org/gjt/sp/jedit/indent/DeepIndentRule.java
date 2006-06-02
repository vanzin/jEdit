package org.gjt.sp.jedit.indent;

import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.buffer.JEditBuffer;

import java.util.List;
import java.util.Stack;

/**
 * @author Matthieu Casanova
 */
public class DeepIndentRule implements IndentRule {

	//{{{ apply() method
	public void apply(JEditBuffer buffer, int thisLineIndex,
		int prevLineIndex, int prevPrevLineIndex,
		List indentActions) {
		if(prevLineIndex == -1)
			return;
		
		int prevLineUnclosedParenIndex = -1; // Index of the last unclosed parenthesis
		int prevLineParenWeight = 0;  // (openParens - closeParens)
		Stack openParens = new Stack();
		
		String prevLine = buffer.getLineText(prevLineIndex);
		for (int i = 0; i < prevLine.length(); i++)
		{
			char c = prevLine.charAt(i);
			switch (c)
			{
				case'(':
					openParens.push(new Integer(i));
					prevLineParenWeight++;
					break;
				case')':
				if (openParens.size() > 0)
					openParens.pop();
				prevLineParenWeight--;
				break;
			}
		}
		
		if (openParens.size() > 0)
		{
			prevLineUnclosedParenIndex = ((Integer) openParens.pop()).intValue();
		}
		
		
		
		if (prevLineParenWeight > 0)
		{
			// more opening (
				indentActions.add(new IndentAction.AlignParameter(prevLineUnclosedParenIndex, prevLine));
				
		}
		else if (prevLineParenWeight < 0)
		{
			// more closing )
			int openParenOffset = TextUtilities.findMatchingBracket(buffer, prevLineIndex, prevLine.lastIndexOf(')'));
			
			if (openParenOffset >= 0)
			{
				
				int openParensLine = buffer.getLineOfOffset(openParenOffset);
				int openParensColumn = openParenOffset - buffer.getLineStartOffset(openParensLine);
				String openParensLineText = buffer.getLineText(openParensLine);
				
				
				
				int startLineParenWeight = getLineParenWeight(openParensLineText);
				if (startLineParenWeight == 1)
				{
					indentActions.add(new IndentAction.AlignParameter(openParensColumn, openParensLineText));
				}
				else if (startLineParenWeight > 1)
				{
					int prevParens = openParensLineText.lastIndexOf('(', openParensColumn - 1);
						indentActions.add(new IndentAction.AlignParameter(prevParens, openParensLineText));
				}
				else
				{
					int indent = getOpenParenIndent(buffer, openParensLine, thisLineIndex);
					indentActions.add(new IndentAction.AlignOffset(indent));
				}
			}
		}
	}

  	//{{{ getOpenParenIndent() method
	/**
	 * Returns the appropriate indent based on open parenthesis on previous lines.
	 *
	 * @param startLine The line where parens were last balanced
	 * @param targetLine The line we're finding the indent for
	 */
	private int getOpenParenIndent(JEditBuffer buffer, int startLine, int targetLine)
	{
		Stack openParens = new Stack();
		String lineText;

		for(int lineIndex = startLine; lineIndex < targetLine; lineIndex++)
		{
			lineText = buffer.getLineText(lineIndex);
			for(int i = 0; i < lineText.length(); i++)
			{
				char c = lineText.charAt(i);
				switch(c)
				{
					case '(':
						openParens.push(new Integer(i));
						break;
					case ')':
						if(openParens.size() > 0)
							openParens.pop();
						break;
					default:
				}
			}
		}
		int indent = buffer.getCurrentIndentForLine(startLine,null);

		if(openParens.size() > 0)
			indent += ((Integer) openParens.pop()).intValue();

		return indent;
	}
	//}}}

	/**
	* took from Buffer method
	* @param lineText
	* @return
	*/
	private int getLineParenWeight(String lineText)
	{
		int parenWeight = 0;
		for(int i = 0; i < lineText.length(); i++)
		{
			char c = lineText.charAt(i);
			switch(c)
			{
				case '(':
					parenWeight++;
					break;
				case ')':
					parenWeight--;
					break;
				default:
			}
		}
		return parenWeight;
	}
} //}}}

