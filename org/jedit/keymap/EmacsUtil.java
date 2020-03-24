package org.jedit.keymap;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.TextArea;

/** Emacs Macro utility functions. 

	These functions are based on EmacsUtil.bsh from the Emacs macros 
	by Brian M. Clapper.
	Rewritten in Java by Alan Ezust in 2013.
*/

public class EmacsUtil
{
	Buffer buffer; 
	TextArea textArea;
	
	public EmacsUtil()
	{
		buffer =  jEdit.getActiveView().getBuffer();
		textArea = jEdit.getActiveView().getTextArea();
	}
	

	public void emacsKillLine()
	{
		boolean lastActionWasThis = repeatingSameMacro ("Emacs/Emacs_Kill_Line");
	
		int caret = textArea.getCaretPosition();		
		int caretLine = textArea.getCaretLine();
		int lineEnd = textArea.getLineEndOffset (caretLine);					
		
		// If we're at the end of line (ignoring any trailing white space),
		// then kill the newline, too.
		int caret2 = caret + 1;		
		while (caret2 < lineEnd)
		{
			char ch = charAt (caret2);
			
			if (! Character.isWhitespace (ch))
				break;
	
			caret2++;
		}
	
		String deletedText = null;
		Selection selection = null;
	
		if (caret2 == lineEnd)
		{
			// We're at the end of the line. Join this line and the next line--but
			// do it with a true delete, not with textArea.joinLines(), to
			// emulate emacs better.
	
			if (caretLine != textArea.getLastPhysicalLine())
				selection = new Selection.Range (caret, caret2);
		}
	
		else
		{
			// Simple delete to end of line.
	
			selection = new Selection.Range (caret, lineEnd - 1);
			//textArea.deleteToEndOfLine();
		}
	
		if (selection != null)
		{
			textArea.setSelection (selection);
			deletedText = textArea.getSelectedText();
			textArea.replaceSelection ("");
			textArea.removeFromSelection (selection);
	
			if (lastActionWasThis)
			{
				String clipboard = getClipboard();
				if (clipboard == null)
					clipboard = "";
	
				setClipboard (clipboard + deletedText);
			}	
			else
			{
				setClipboard (deletedText);
			}
		}
	}



	public boolean repeatingSameMacro (String macroName)
	{
		InputHandler ih = jEdit.getActiveView().getInputHandler();
		EditAction lastAction = ih.getLastAction();
		int lastActionCount = ih.getLastActionCount();
		
		// When called from within a macro, the last action will be that macro.
		// But, if the last action count is greater than 1, then it's a repeat.
	
		boolean repeat = false;
		if ( (lastAction.getName().equals (macroName)) && (lastActionCount > 1) )
			repeat = true;
		
		return repeat;
	}
	
	public String lineAt (int i)
	{
		StringBuilder sb = new StringBuilder();
		
		while (! atEndOfBuffer (i))
		{
			char c = charAt (i);
			sb.append(c);
			if (c == '\n')
				break;
		}
		
		return sb.toString();
	}
	
	public char charAt (int i)
	{
		if (i >= buffer.getLength()) return 0;
		return buffer.getText (i, 1).charAt (0);
	}
	
	public char charAtCaret()
	{
		int caret = textArea.getCaretPosition();
		return (atEndOfBuffer() ? '\0' : buffer.getText (caret, 1).charAt (0));
	}
	
	public boolean atEndOfBuffer()
	{
		TextArea textArea = jEdit.getActiveView().getTextArea();
		return atEndOfBuffer (textArea.getCaretPosition());
	}
	
	public boolean atEndOfBuffer (int caret)
	{
		return (caret >= buffer.getLength());
	}
	
	public int eatNonAlphanums()
	{
		boolean eat = true;
		
		while (eat)
		{
			char ch = charAtCaret();
			
			if (ch == '\n')
			{
				textArea.goToNextLine (false);
				textArea.goToStartOfLine (false);
			}
			
			else
			{
				if (Character.isLetterOrDigit (ch))
					eat = false;
				
				else
					textArea.goToNextCharacter (false);
			}
		}
		
		return textArea.getCaretPosition();
	}
		   
	public int eatWhitespace()
	{
		boolean eat = true;
		
		while (eat)
		{
			char ch = charAtCaret();
			
			if (ch == '\n')
			{
				textArea.goToNextLine (false);
				textArea.goToStartOfLine (false);
			}
			
			else if (Character.isWhitespace (ch))
			{
				textArea.goToNextCharacter (false);
			}
			else
			{
				eat = false;
			}
		}
		
		return textArea.getCaretPosition();
	}
	
	public int getCardinalProperty (String name, int defaultValue)
	{
		int result = jEdit.getIntegerProperty (name, defaultValue);
	
		if (result <= 0)
			result = defaultValue;
		
		return result;
	}
	
	public String makeBufferPropertyName (String prefix)
	{	
		return makeBufferPropertyName (buffer, prefix);
	}
	
	public String makeBufferPropertyName (Buffer theBuffer, String prefix)
	{
		StringBuilder propName = new StringBuilder(prefix);
	
		// Convert any Windows-style file separators to Unix ones, since
		// backslashes are special characters in properties files.
	
		String fileSep = System.getProperty ("file.separator");
		String bufName;
		if (! fileSep.equals ("/"))
		{
			// Backslash is also special in regular expressions. Since, in theory,
			// the file separator could be *anything*, we check explicitly for
			// backslash here.
			if (fileSep.equals ("\\"))
				fileSep = new StringBuilder(fileSep).append('\\').toString();
			bufName = theBuffer.getPath().replaceAll (fileSep, "/");
		}
		else
		{
			bufName = theBuffer.getPath();
		}
		propName.append (bufName);
		return propName.toString();
	}
	
	public int getDefaultWrap()
	{
		return getCardinalProperty ("buffer.maxLineLen", 79);
	}
	
	public int getMark (Buffer buffer)
	{
		String propName = makeBufferPropertyName ("emacs.mark");
		int mark = getCardinalProperty (propName, -1);
		if (mark != -1 && mark >= buffer.getLength())
		{
				mark = buffer.getLength() - 1;
		}
		return mark;
	}
	
	public void setMark (Buffer buffer, int pos)
	{
		String propName = makeBufferPropertyName (buffer, "emacs.mark");
		jEdit.setTemporaryProperty (propName, String.valueOf (pos));
	}
	
	public void beep()
	{
		javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
	}
	
	public Selection getKillRegion()
	{
		// If there's a selection, use it instead.
		int caret = textArea.getCaretPosition();
		Selection selection = textArea.getSelectionAtOffset (caret);
		if (selection == null)
		{
			int mark = getMark (buffer);
			if (mark == -1)
			{
				beep();
				return null;
			}
	
			selection = new Selection.Range (Math.min (caret, mark), Math.max (caret, mark));
			textArea.setSelection (selection);
		}
	
		return selection;
	}
	
	public String getClipboard()
	{
		return String.valueOf(Registers.getRegister ('$'));
	}
	
	public void setClipboard (String string)
	{
		Registers.setRegister ('$', string);
	}
	
	public void setClipboard (Selection selection)
	{
		TextArea textArea = jEdit.getActiveView().getTextArea();
		setClipboard (textArea.getSelectedText (selection));
	}
	
	public void addToClipboardAndHistory (String string)
	{
		// The special register '$' is the clipboard.
	
		setClipboard (string);
		
		// Save the text in the history, too.
		
		HistoryModel.getModel ("clipboard").addItem (string);
	}
	
	public void addToClipboardAndHistory (Selection selection)
	{
		addToClipboardAndHistory (textArea.getSelectedText (selection));
	}

	public int findEndOfSentence()
	{
		int caret = textArea.getCaretPosition();

		for (;;)
		{
			if (atEndOfBuffer (caret))
				break;

			char ch = charAt (caret);
			if (ch == '.' && Character.isWhitespace (charAt (caret + 1)))
			{
				caret++;
				break;
			}

			caret++;
		}

		return caret;
	}
	
	public int findBeginningOfSentence()
	{
		int caret = textArea.getCaretPosition() - 1;
		if (charAt (caret) == '.')
			caret--;
	
		for (;;)
		{
			if (caret <= 0)
				break;
	
			char ch = charAt (caret);
			if (ch == '.')
			{
				if (Character.isWhitespace (charAt (caret + 1)))
				{
					caret++;
					break;
				}
			}
			
			else if (Character.isUpperCase (ch))
			{
				caret--;
				if (caret <= 0)
					break;
				if (Character.isWhitespace (charAt (caret)))
					break;
			}
	
			caret--;
		}
		
		return caret;
	}
}

   