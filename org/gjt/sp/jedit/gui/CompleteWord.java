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
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
//}}}

public class CompleteWord extends JWindow
{
	//{{{ completeWord() method
	public static void completeWord(JEditTextArea textArea)
	{
		Buffer buffer = textArea.getBuffer();
		int caretLine = textArea.getCaretLine();
		int caret = textArea.getCaretPosition();

		if(!buffer.isEditable())
		{
			textArea.getToolkit().beep();
			return;
		}

		KeywordMap keywordMap = buffer.getKeywordMapAtOffset(caret);
		String noWordSep = getNonAlphaNumericWordChars(buffer,keywordMap,caret);
		String word = getWordToComplete(buffer,caretLine,caret,noWordSep);
		if(word == null)
		{
			textArea.getToolkit().beep();
			return;
		}

		Vector completions = getCompletions(buffer,word,keywordMap,
			noWordSep);

		if(completions.size() == 0)
			textArea.getToolkit().beep();
		//{{{ if there is only one competion, insert in buffer
		else if(completions.size() == 1)
		{
			textArea.setSelectedText(completions
				.elementAt(0).toString()
				.substring(word.length()));
		} //}}}
		//{{{ show popup if > 1
		else
		{
			Point location = new Point(
				textArea.offsetToX(caretLine,
				caret - buffer.getLineStartOffset(caretLine)),
				textArea.getPainter().getFontMetrics().getHeight()
				* (textArea.physicalToVirtual(caretLine)
				- textArea.getFirstLine() + 1));

			SwingUtilities.convertPointToScreen(location,
				textArea.getPainter());
			new CompleteWord(textArea,word,completions,location);
		} //}}}
	} //}}}

	//{{{ CompleteWord constructor
	public CompleteWord(JEditTextArea textArea, String word, Vector completions,
		Point location)
	{
		super(JOptionPane.getFrameForComponent(textArea));

		this.view = GUIUtilities.getView(textArea);
		this.word = word;

		words = new JList(completions);

		words.setVisibleRowCount(Math.min(completions.size(),8));

		words.addMouseListener(new MouseHandler());
		words.setSelectedIndex(0);
		words.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// stupid scrollbar policy is an attempt to work around
		// bugs people have been seeing with IBM's JDK -- 7 Sep 2000
		JScrollPane scroller = new JScrollPane(words,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		getContentPane().add(scroller, BorderLayout.CENTER);

		GUIUtilities.requestFocus(this,words);

		pack();
		setLocation(location);
		show();

		KeyHandler keyHandler = new KeyHandler();
		addKeyListener(keyHandler);
		getRootPane().addKeyListener(keyHandler);
		words.addKeyListener(keyHandler);
		view.setKeyEventInterceptor(keyHandler);
	} //}}}

	//{{{ dispose() method
	public void dispose()
	{
		view.setKeyEventInterceptor(null);
		super.dispose();
	} //}}}

	//{{{ Private members

	//{{{ getNonAlphaNumericWordChars() method
	private static String getNonAlphaNumericWordChars(Buffer buffer,
		KeywordMap keywordMap, int caret)
	{
		// figure out what constitutes a word character and what
		// doesn't
		String noWordSep = buffer.getStringProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";
		String keywordNoWordSep = keywordMap.getNonAlphaNumericChars();
		if(keywordNoWordSep != null)
			noWordSep = noWordSep + keywordNoWordSep;

		return noWordSep;
	} //}}}

	//{{{ getWordToComplete() method
	private static String getWordToComplete(Buffer buffer, int caretLine,
		int caret, String noWordSep)
	{
		String line = buffer.getLineText(caretLine);
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
		String word = line.substring(wordStart,dot);
		if(word.length() == 0)
			return null;

		return word;
	} //}}}

	//{{{ getCompletions() method
	private static Vector getCompletions(Buffer buffer, String word,
		KeywordMap keywordMap, String noWordSep)
	{
		Vector completions = new Vector();

		int wordLen = word.length();

		//{{{ loop through all lines of current buffer
		for(int i = 0; i < buffer.getLineCount(); i++)
		{
			String line = buffer.getLineText(i);

			// check for match at start of line

			if(line.startsWith(word))
			{
				String _word = completeWord(line,0,noWordSep);
				if(_word.length() != wordLen)
				{
					// remove duplicates
					if(completions.indexOf(_word) == -1)
						completions.addElement(_word);
				}
			}

			// check for match inside line
			int len = line.length() - word.length();
			for(int j = 0; j < len; j++)
			{
				char c = line.charAt(j);
				if(!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1)
				{
					if(line.regionMatches(j + 1,word,0,wordLen))
					{
						String _word = completeWord(line,j + 1,noWordSep);
						if(_word.length() != wordLen)
						{
							// remove duplicates
							if(completions.indexOf(_word) == -1)
								completions.addElement(_word);
						}
					}
				}
			}
		} //}}}

		//{{{ try to find matching keywords
		String[] keywords = keywordMap.getKeywords();
		for(int i = 0; i < keywords.length; i++)
		{
			String keyword = keywords[i];
			if(keyword.regionMatches(keywordMap.getIgnoreCase(),
				0,word,0,wordLen))
			{
				if(completions.indexOf(keyword) == -1)
					completions.addElement(keyword);
			}
		} //}}}

		// sort completion list
		MiscUtilities.quicksort(completions,new MiscUtilities.StringICaseCompare());

		return completions;
	} //}}}

	//{{{ completeWord() method
	private static String completeWord(String line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.substring(offset,wordEnd);
	} //}}}

	//{{{ Instance variables
	private View view;
	private String word;
	private JList words;
	//}}}

	//{{{ insertSelected() method
	private void insertSelected()
	{
		view.getTextArea().setSelectedText(words
			.getSelectedValue().toString()
			.substring(word.length()));
		dispose();
	} //}}}

	//}}}

	//{{{ Completion class
	static class Completion
	{
		String text;
		boolean keyword;

		Completion(String text, boolean keyword)
		{
			this.text = text;
			this.keyword = keyword;
		}

		public String toString()
		{
			return text;
		}
	} //}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		//{{{ keyPressed() method
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_TAB:
			case KeyEvent.VK_ENTER:
				insertSelected();
				evt.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				dispose();
				evt.consume();
				break;
			case KeyEvent.VK_UP:
				if(getFocusOwner() == words)
					return;

				int selected = words.getSelectedIndex();
				if(selected == 0)
					return;

				selected = selected - 1;
	
				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			case KeyEvent.VK_DOWN:
				if(getFocusOwner() == words)
					return;

				selected = words.getSelectedIndex();
				if(selected == words.getModel().getSize() - 1)
					return;

				selected = selected + 1;

				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			default:
				dispose();
				view.processKeyEvent(evt);
				break;
			}
		} //}}}

		//{{{ keyTyped() method
		public void keyTyped(KeyEvent evt)
		{
			evt = KeyEventWorkaround.processKeyEvent(evt);
			if(evt == null)
				return;
			else
			{
				dispose();
				view.processKeyEvent(evt);
			}
		} //}}}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			insertSelected();
		}
	} //}}}
}
