/*
 * CompleteWord.java - Complete word dialog
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.gui;

//{{{ Imports
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;

import java.awt.event.KeyEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.View;

import org.gjt.sp.jedit.syntax.KeywordMap;

import org.gjt.sp.jedit.textarea.JEditTextArea;

import org.gjt.sp.util.StandardUtilities;
//}}}

/**
 * A completion popup class.
 */
public class CompleteWord extends CompletionPopup
{
	//{{{ completeWord() method
	public static void completeWord(View view)
	{
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();
		int caretLine = textArea.getCaretLine();
		int caret = textArea.getCaretPosition();

		if(!buffer.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		KeywordMap keywordMap = buffer.getKeywordMapAtOffset(caret);
		String noWordSep = getNonAlphaNumericWordChars(
			buffer,keywordMap);
		String word = getWordToComplete(buffer,caretLine,
			caret,noWordSep);
		if(word == null)
		{
			textArea.getToolkit().beep();
			return;
		}

		Completion[] completions = getCompletions(buffer,word,caret);

		if(completions.length == 0)
		{
			textArea.getToolkit().beep();
		}
		//{{{ if there is only one competion, insert in buffer
		else if(completions.length == 1)
		{
			Completion c = completions[0];

			if(c.text.equals(word))
			{
				textArea.getToolkit().beep();
			}
			else
			{
				textArea.replaceSelection(c.text.substring(
					word.length()));
			}
		} //}}}
		//{{{ show popup if > 1
		else
		{
			String longestPrefix = MiscUtilities.getLongestPrefix(
				completions,
				keywordMap != null
				? keywordMap.getIgnoreCase()
				: false);

			if (word.length() < longestPrefix.length())
			{
				buffer.insert(caret,longestPrefix.substring(
					word.length()));
			}

			textArea.scrollToCaret(false);
			Point location = textArea.offsetToXY(
				caret - word.length());
			location.y += textArea.getPainter().getFontMetrics()
				.getHeight();

			SwingUtilities.convertPointToScreen(location,
				textArea.getPainter());
			new CompleteWord(view,longestPrefix,
				completions,location,noWordSep);
		} //}}}
	} //}}}

	//{{{ CompleteWord constructor
	public CompleteWord(View view, String word, Completion[] completions,
		Point location, String noWordSep)
	{
		super(view, location);

		this.noWordSep = noWordSep;
		this.view = view;
		this.textArea = view.getTextArea();
		this.buffer = view.getBuffer();
		this.word = word;

		reset(new Words(completions), true);
	} //}}}

	//{{{ Private members

	//{{{ getNonAlphaNumericWordChars() method
	private static String getNonAlphaNumericWordChars(Buffer buffer,
		KeywordMap keywordMap)
	{
		// figure out what constitutes a word character and what
		// doesn't
		String noWordSep = buffer.getStringProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";
		if(keywordMap != null)
		{
			String keywordNoWordSep = keywordMap.getNonAlphaNumericChars();
			if(keywordNoWordSep != null)
				noWordSep += keywordNoWordSep;
		}

		return noWordSep;
	} //}}}

	//{{{ getWordToComplete() method
	private static String getWordToComplete(Buffer buffer, int caretLine,
		int caret, String noWordSep)
	{
		CharSequence line = buffer.getLineSegment(caretLine);
		int dot = caret - buffer.getLineStartOffset(caretLine);
		if(dot == 0)
			return null;

		char ch = line.charAt(dot-1);
		if(!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1)
		{
			// attempting to expand non-word char
			return null;
		}

		int wordStart = TextUtilities.findWordStart(line,dot-1,noWordSep);
		CharSequence word = line.subSequence(wordStart,dot);
		if(word.length() == 0)
			return null;

		return word.toString();
	} //}}}
	
	//{{{ getVisibleBuffers() method
	private static Collection<Buffer> getVisibleBuffers()
	{
		final Set<Buffer> buffers = new HashSet<Buffer>();
		jEdit.visit(new JEditVisitorAdapter()
			{
				@Override
				public void visit(EditPane editPane)
				{
					buffers.add(editPane.getBuffer());
				}
			});
		return buffers;
	} //}}}

	//{{{ getCompletions() method
	private static Completion[] getCompletions(final Buffer buffer, final String word,
		final int caret)
	{
		// build a list of unique words in all buffers, or visible buffers,
		// depending on completeFromAllBuffers
		final Set<Completion> completions = new TreeSet<Completion>(new StandardUtilities
			.StringCompare<Completion>());

		// only complete current buffer's keyword map
		final KeywordMap keywordMap = buffer.getKeywordMapAtOffset(caret);
		final String noWordSep = getNonAlphaNumericWordChars(
			buffer,keywordMap);
		
		final Collection<Buffer> sourceBuffers = 
			jEdit.getBooleanProperty("completeFromAllBuffers") ?
				Arrays.asList(jEdit.getBuffers()) :
				getVisibleBuffers();

		for (Buffer b : sourceBuffers)
		{
			// only complete current buffer's keyword map
			KeywordMap _keywordMap;
			if(b == buffer)
				_keywordMap = keywordMap;
			else
				_keywordMap = null;

			int offset = (b == buffer ? caret : 0);

			getCompletions(b,word,keywordMap,noWordSep,
					offset,completions);
		}

		Completion[] completionArray = completions
			.toArray(new Completion[completions.size()]);

		return completionArray;
	} //}}}

	//{{{ getCompletions() method
	private static void getCompletions(Buffer buffer, String word,
		KeywordMap keywordMap, String noWordSep, int caret,
		Set<Completion> completions)
	{
		int wordLen = word.length();

		//{{{ try to find matching keywords
		if(keywordMap != null)
		{
			String[] keywords = keywordMap.getKeywords();
			for(int i = 0; i < keywords.length; i++)
			{
				String _keyword = keywords[i];
				if(_keyword.regionMatches(keywordMap.getIgnoreCase(),
					0,word,0,wordLen))
				{
					Completion keyword = new Completion(_keyword,true);
					if(!completions.contains(keyword))
					{
						completions.add(keyword);
					}
				}
			}
		} //}}}

		//{{{ loop through all lines of current buffer
		for(int i = 0; i < buffer.getLineCount(); i++)
		{
			CharSequence line = buffer.getLineSegment(i);
			int start = buffer.getLineStartOffset(i);

			// check for match at start of line

			if (StandardUtilities.startsWith(line, word) &&
			    caret != start + word.length())
			{
				String _word = completeWord(line,0,noWordSep);
				Completion comp = new Completion(_word,false);

				// remove duplicates
				if(!completions.contains(comp))
				{
					completions.add(comp);
				}
			}

			// check for match inside line
			int len = line.length() - word.length();
			for(int j = 0; j < len; j++)
			{
				char c = line.charAt(j);
				if(!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1)
				{
					if (StandardUtilities.regionMatches(line,j + 1,word,0,wordLen)
						&& caret != start + j + word.length() + 1)
					{
						String _word = completeWord(line,j + 1,noWordSep);
						Completion comp = new Completion(_word,false);

						// remove duplicates
						if(!completions.contains(comp))
						{
							completions.add(comp);
						}
					}
				}
			}
		} //}}}
	} //}}}

	//{{{ completeWord() method
	private static String completeWord(CharSequence line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.subSequence(offset,wordEnd).toString();
	} //}}}

	//{{{ Instance variables
	private View view;
	private JEditTextArea textArea;
	private Buffer buffer;
	private String word;
	private String noWordSep;
	//}}}

	//{{{ Completion class
	private static class Completion
	{
		final String text;
		final boolean keyword;

		Completion(String text, boolean keyword)
		{
			this.text = text;
			this.keyword = keyword;
		}

		public String toString()
		{
			return text;
		}

		public int hashCode()
		{
			return text.hashCode();
		}

		public boolean equals(Object obj)
		{
			if(obj instanceof Completion)
				return ((Completion)obj).text.equals(text);
			else
				return false;
		}
	} //}}}

	//{{{ Words class
	private class Words implements Candidates
	{
		private final DefaultListCellRenderer renderer;
		private final Completion[] completions;

		public Words(Completion[] completions)
		{
			this.renderer = new DefaultListCellRenderer();
			this.completions = completions;
		}

		public int getSize()
		{
			return completions.length;
		}

		public boolean isValid()
		{
			return true;
		}

		public void complete(int index)
		{
			String insertion = completions[index].toString().substring(word.length());
			textArea.replaceSelection(insertion);
		}

		public Component getCellRenderer(JList list, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			renderer.getListCellRendererComponent(list,
				null, index, isSelected, cellHasFocus);

			Completion comp = completions[index];

			String text = comp.text;
			Font font = list.getFont();

			if(index < 9)
				text = (index + 1) + ": " + text;
			else if(index == 9)
				text = "0: " + text;

			if(comp.keyword)
				font = font.deriveFont(Font.BOLD);

			renderer.setText(text);
			renderer.setFont(font);
			return renderer;
		}

		public String getDescription(int index)
		{
			return null;
		}
	} //}}}

	//{{{ resetWords() method
	private void resetWords(String newWord)
	{
		int caret = textArea.getCaretPosition();
		Completion[] completions = getCompletions(
			buffer,newWord,caret);
		if(completions.length > 0)
		{
			word = newWord;
			reset(new Words(completions), true);
		}
		else
		{
			dispose();
		}
	} //}}}

	//}}}

	//{{{ keyPressed() medhod
	protected void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
		{
			textArea.backspace();
			e.consume();

			if(word.length() == 1)
			{
				dispose();
			}
			else
			{
				resetWords(word.substring(0,word.length() - 1));
			}
		}
	} //}}}

	//{{{ keyTyped() medhod
	protected void keyTyped(KeyEvent e)
	{
		char ch = e.getKeyChar();
		if(Character.isDigit(ch))
		{
			int index = ch - '0';
			if(index == 0)
				index = 9;
			else
				index--;
			if(index < getCandidates().getSize())
			{
				setSelectedIndex(index);
				if(doSelectedCompletion())
				{
					e.consume();
					dispose();
				}
				return;
			}
			else
				/* fall through */;
		}

		// \t handled above
		if(ch != '\b' && ch != '\t')
		{
			/* eg, foo<C+b>, will insert foobar, */
			if(!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1)
			{
				doSelectedCompletion();
				textArea.userInput(ch);
				e.consume();
				dispose();
				return;
			}

			textArea.userInput(ch);
			e.consume();
			resetWords(word + ch);
		}
	} //}}}
}
