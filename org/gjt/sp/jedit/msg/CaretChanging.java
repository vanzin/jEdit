package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 * (should be) sent on the editbus when an asset is selected from sidekick/codebrowser, before it
 * performs a setCursorPosition. 
 * 
 * This lets plugins like navigator remember position histories.
 * @author ezust
 * @since jedit 4.3pre5
 *
 */
public class CaretChanging extends EditPaneUpdate
{
	int caret = 0;
	public CaretChanging(EditPane pane) 
	{
		super(pane, CARET_CHANGING);
		JEditTextArea ta = pane.getTextArea();
		caret = ta.getCaretPosition();
	}
}
