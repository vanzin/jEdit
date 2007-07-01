package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 * Sent on the editbus when events are coming from the TextArea.
 * 
 * @deprecated - use EditPaneUpdate
 * @author ezust
 * @since jedit 4.3pre5
 *
 */
public class TextAreaUpdate extends EBMessage
{
	/** @deprecated */
	public static final String CARET_CHANGING ="CARET_CHANGING";
	String what;
	int caret = 0;
	public TextAreaUpdate(JEditTextArea textArea, String what) 
	{	
		super(textArea);
		this.what = what;
		caret = textArea.getCaretPosition();
	}
	public String getWhat() {
		return what;
	}
	public JEditTextArea getTextArea() {
		return (JEditTextArea) getSource();
	}
}
