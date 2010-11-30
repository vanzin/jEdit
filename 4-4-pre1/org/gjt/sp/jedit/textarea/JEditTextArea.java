/*
 * JEditTextArea.java - jEdit's text component
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
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

package org.gjt.sp.jedit.textarea;

//{{{ Imports
import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.options.GlobalOptions;

import org.gjt.sp.jedit.msg.PositionChanging;
//}}}

/**
 * jEdit's text component.<p>
 *
 * Unlike most other text editors, the selection API permits selection and
 * concurrent manipulation of multiple, non-contiguous regions of text.
 * Methods in this class that deal with selecting text rely upon classes derived
 * the {@link Selection} class.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id$
 */
public class JEditTextArea extends TextArea
{

	//{{{ JEditTextArea constructor
	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea(View view)
	{
		super(jEdit.getPropertyManager(), view);
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
		this.view = view;
	} //}}}

	//{{{ getFoldPainter() method
	@Override
	public FoldPainter getFoldPainter()
	{
		FoldPainter foldPainter = (FoldPainter) ServiceManager.getService(
				FOLD_PAINTER_SERVICE, getFoldPainterName());
		if (foldPainter == null)
			foldPainter = (FoldPainter) ServiceManager.getService(
				FOLD_PAINTER_SERVICE,
				DEFAULT_FOLD_PAINTER_SERVICE);
		return foldPainter;
	} //}}}

	// {{{ Overrides for macro recording.
	//{{{ home() method
	/**
	 * An override to record the acutual action taken for home().
	 */
	@Override
	public void home(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();
		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");			
			goToStartOfWhiteSpace(select);
			break;
		default:
			if(recorder != null)
				recorder.record("textArea.goToStartOfLine(" + select + ");");			
			goToStartOfLine(select);
			break;
		}
	} //}}}

	//{{{ end() method
	/**
	 * An override to record the acutual action taken for end().
	 */
	@Override
	public void end(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");
			goToEndOfWhiteSpace(select);
			break;
		default:
			if(recorder != null)
				recorder.record("textArea.goToEndOfLine(" + select + ");");
			goToEndOfLine(select);
			break;
		}
	} //}}}

	//{{{ smartHome() method
	/**
	 * An override to record the acutual action taken for smartHome().
	 */
	@Override
	public void smartHome(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");

			goToStartOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToStartOfLine(" + select + ");");

			goToStartOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToFirstVisibleLine(" + select + ");");

			goToFirstVisibleLine(select);
			break;
		}
	} //}}}

	//{{{ smartEnd() method
	/**
	 * An override to record the acutual action taken for smartHome().
	 */
	@Override
	public void smartEnd(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");

			goToEndOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToEndOfLine(" + select + ");");

			goToEndOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToLastVisibleLine(" + select + ");");
			goToLastVisibleLine(select);
			break;
		}
	} //}}}
	// }}}

	// {{{ overrides from the base class that are EditBus  aware
	public void goToBufferEnd(boolean select)
	{
		EditBus.send(new PositionChanging(this));
		super.goToBufferEnd(select);
	}

	//{{{ goToMatchingBracket() method
	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * Also sends PositionChanging if it goes somewhere.
	 * @since jEdit 4.3pre18
	 */
	public void goToMatchingBracket()
	{
		if(getLineLength(caretLine) != 0)
		{
			int dot = caret - getLineStartOffset(caretLine);

			int bracket = TextUtilities.findMatchingBracket(
				buffer,caretLine,Math.max(0,dot - 1));
			if(bracket != -1)
			{
				EditBus.send(new PositionChanging(this));
				selectNone();
				moveCaretPosition(bracket + 1,false);
				return;
			}
		}
		getToolkit().beep();
	} //}}}


	public void goToBufferStart(boolean select)
	{
		EditBus.send(new PositionChanging(this));
		super.goToBufferStart(select);
	} // }}}

	// {{{ replaceSelection(String)
	@Override
	public int replaceSelection(String selectedText)
	{
		EditBus.send(new PositionChanging(this));
		return super.replaceSelection(selectedText);
	}//}}}

	//{{{ showGoToLineDialog() method
	/**
	 * Displays the 'go to line' dialog box, and moves the caret to the
	 * specified line number.
	 * @since jEdit 2.7pre2
	 */
	public void showGoToLineDialog()
	{
		String line = GUIUtilities.input(view,"goto-line",null);
		if(line == null)
			return;

		try
		{
			int lineNumber = Integer.parseInt(line) - 1;
			EditBus.send(new PositionChanging(this));
			setCaretPosition(getLineStartOffset(lineNumber));
		}
		catch(Exception e)
		{
			getToolkit().beep();
		}
	} //}}}

	//{{{ userInput() method
	/**
	 * Handles the insertion of the specified character. It performs the
	 * following operations in addition to TextArea#userInput(char):
	 * <ul>
	 * <li>Inserting a space with automatic abbrev expansion enabled will
	 * try to expand the abbrev
	 * </ul>
	 *
	 * @param ch The character
	 * @since jEdit 2.7pre3
	 */
	@Override
	public void userInput(char ch)
	{
		if(ch == ' ' && Abbrevs.getExpandOnInput()
			&& Abbrevs.expandAbbrev(view,false))
			return;

		super.userInput(ch);
	} //}}}

	//{{{ addExplicitFold() method
	/**
	 * Surrounds the selection with explicit fold markers.
	 * @since jEdit 4.0pre3
	 */
	@Override
	public void addExplicitFold()
	{
		try
		{
			super.addExplicitFold();
		}
		catch (TextAreaException e)
		{
			GUIUtilities.error(view,"folding-not-explicit",null);
		}
	} //}}}

	//{{{ formatParagraph() method
	/**
	 * Formats the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	@Override
	public void formatParagraph()
	{
		try
		{
			super.formatParagraph();
		}
		catch (TextAreaException e)
		{
			GUIUtilities.error(view,"format-maxlinelen",null);
		}
	} //}}}

	//{{{ doWordCount() method
	protected static void doWordCount(View view, String text)
	{
		char[] chars = text.toCharArray();
		int characters = chars.length;
		int words = 0;
		int lines = 1;

		boolean word = true;
		for(int i = 0; i < chars.length; i++)
		{
			switch(chars[i])
			{
			case '\r': case '\n':
				lines++;
			case ' ': case '\t':
				word = true;
				break;
			default:
				if(word)
				{
					words++;
					word = false;
				}
				break;
			}
		}

		Object[] args = { characters, words, lines };
		GUIUtilities.message(view,"wordcount",args);
	} //}}}

	//{{{ showWordCountDialog() method
	/**
	 * Displays the 'word count' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showWordCountDialog()
	{
		String selection = getSelectedText();
		if(selection != null)
		{
			doWordCount(view,selection);
			return;
		}

		doWordCount(view,buffer.getText(0,buffer.getLength()));
	} //}}}

	//{{{ Getters and setters

	//{{{ getView() method
	/**
	 * Returns this text area's view.
	 * @since jEdit 4.2pre5
	 */
	public View getView()
	{
		return view;
	} //}}}

	//}}}

	//{{{ Deprecated methods

	//{{{ getSelectionStart() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStart()</code> method
	 */
	@Deprecated
	public final int getSelectionStart()
	{
		if(getSelectionCount() != 1)
			return caret;

		return getSelection(0).getStart();
	} //}}}

	//{{{ getSelectionStart() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStart(int)</code> method
	 */
	@Deprecated
	public int getSelectionStart(int line)
	{
		if(getSelectionCount() != 1)
			return caret;

		return getSelection(0).getStart(buffer,line);
	} //}}}

	//{{{ getSelectionStartLine() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getStartLine()</code> method
	 */
	@Deprecated
	public final int getSelectionStartLine()
	{
		if(getSelectionCount() != 1)
			return caret;

		return getSelection(0).getStartLine();
	} //}}}

	//{{{ setSelectionStart() method
	/**
	 * @deprecated Do not use.
	 */
	@Deprecated
	public final void setSelectionStart(int selectionStart)
	{
		int selectionEnd = getSelectionCount() == 1 ? getSelection(0).getEnd() : caret;
		setSelection(new Selection.Range(selectionStart, selectionEnd));
		moveCaretPosition(selectionEnd,true);
	} //}}}

	//{{{ getSelectionEnd() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEnd()</code> method
	 */
	@Deprecated
	public final int getSelectionEnd()
	{
		return getSelectionCount() == 1 ? getSelection(0).getEnd() : caret;

	} //}}}

	//{{{ getSelectionEnd() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEnd(int)</code> method
	 */
	@Deprecated
	public int getSelectionEnd(int line)
	{
		if(getSelectionCount() != 1)
			return caret;

		return getSelection(0).getEnd(buffer,line);
	} //}}}

	//{{{ getSelectionEndLine() method
	/**
	 * @deprecated Instead, obtain a Selection instance using
	 * any means, and call its <code>getEndLine()</code> method
	 */
	@Deprecated
	public final int getSelectionEndLine()
	{
		if(getSelectionCount() != 1)
			return caret;

		return getSelection(0).getEndLine();
	} //}}}

	//{{{ setSelectionEnd() method
	/**
	 * @deprecated Do not use.
	 */
	@Deprecated
	public final void setSelectionEnd(int selectionEnd)
	{
		int selectionStart = getSelectionCount() == 1 ?	getSelection(0).getStart() : caret;
		setSelection(new Selection.Range(selectionStart, selectionEnd));
		moveCaretPosition(selectionEnd,true);
	} //}}}

	//{{{ select() method
	/**
	 * @deprecated Instead, call either <code>addToSelection()</code>,
	 * or <code>setSelection()</code> with a new Selection instance.
	 */
	@Deprecated
	public void select(int start, int end)
	{
		setSelection(new Selection.Range(start, end));
		moveCaretPosition(end,true);
	} //}}}

	//{{{ select() method
	/**
	 * @deprecated Instead, call either <code>addToSelection()</code>,
	 * or <code>setSelection()</code> with a new Selection instance.
	 */
	@Deprecated
	public void select(int start, int end, boolean doElectricScroll)
	{
		selectNone();

		int newStart, newEnd;
		if(start < end)
		{
			newStart = start;
			newEnd = end;
		}
		else
		{
			newStart = end;
			newEnd = start;
		}

		setSelection(new Selection.Range(newStart,newEnd));
		moveCaretPosition(end,doElectricScroll);
	} //}}}

	//{{{ isSelectionRectangular() method
	/**
	 * @deprecated Instead, check if the appropriate Selection
	 * is an instance of the Selection.Rect class.
	 */
	@Deprecated
	public boolean isSelectionRectangular()
	{
		Selection s = getSelectionAtOffset(caret);
		return s != null && s instanceof Selection.Rect;
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private View view;
	//}}}
	//}}}

	//{{{ Fold painters
	/**
	 * Fold painter service.
	 * @since jEdit 4.3pre16
	 */
	public static final String FOLD_PAINTER_PROPERTY = "foldPainter";
	public static final String FOLD_PAINTER_SERVICE = "org.gjt.sp.jedit.textarea.FoldPainter";
	public static final String DEFAULT_FOLD_PAINTER_SERVICE = "Triangle";

	//{{{ getFoldPainterService() method
	public static String getFoldPainterName()
	{
		return jEdit.getProperty(FOLD_PAINTER_PROPERTY, DEFAULT_FOLD_PAINTER_SERVICE);
	} //}}}

	//}}} Fold painters

	//{{{ handlePopupTrigger() method
	/**
	 * Do the same thing as right-clicking on the text area. The Gestures
	 * plugin uses this API.
	 * @since jEdit 4.2pre13
	 */
	@Override
	public void handlePopupTrigger(MouseEvent evt)
	{

		if(popup.isVisible())
			popup.setVisible(false);
		else
		{
			// Rebuild popup menu every time the menu is requested.
			createPopupMenu(evt);

			int x = evt.getX();
			int y = evt.getY();

			int dragStart = xyToOffset(x,y,
				!(painter.isBlockCaretEnabled()
				|| isOverwriteEnabled()));

			if(getSelectionCount() == 0 || multi)
				moveCaretPosition(dragStart,false);
			GUIUtilities.showPopupMenu(popup,painter,x,y);
		}
	} //}}}

	//{{{ createPopupMenu() method
	/**
	 * Creates the popup menu.
	 * @since 4.3pre15
	 */
	@Override
	public void createPopupMenu(MouseEvent evt)
	{
		popup = GUIUtilities.loadPopupMenu("view.context", this, evt);
		JMenuItem customize = new JMenuItem(jEdit.getProperty(
			"view.context.customize"));
		customize.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				new GlobalOptions(view,"context");
			}
		});
		popup.addSeparator();
		popup.add(customize);
	} //}}}

	//{{{ showPopupMenu() method
	/**
	 * Shows the popup menu below the current caret position.
	 * @since 4.3pre10
	 */
	@Override
	public void showPopupMenu()
	{
		if (!popup.isVisible() && hasFocus())
		{
			Point caretPos = offsetToXY(getCaretPosition());
			if (caretPos != null)
			{
				// Open the context menu below the caret
				int charHeight = getPainter().getFontMetrics().getHeight();
				GUIUtilities.showPopupMenu(popup,
					painter,caretPos.x,caretPos.y + charHeight,true);
			}
		}
	} //}}}


}
