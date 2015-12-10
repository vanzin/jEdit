/*
 * MouseHandler.java
 * :tabSize=4:indentSize=4:noTabs=false:
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
import java.awt.event.*;

import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.msg.PositionChanging;
//}}}

/** The mouseHandler used for jEdit.
 *
 */
public class MouseHandler extends TextAreaMouseHandler
{
	//{{{ MouseHandler constructor
	public MouseHandler(JEditTextArea textArea)
	{
		super(textArea);
		this.textArea = textArea;
	} //}}}

	//{{{ mousePressed() method
	@Override
	public void mousePressed(MouseEvent evt)
	{
		showCursor();

		int btn = evt.getButton();
		if (btn != MouseEvent.BUTTON1 && btn != MouseEvent.BUTTON2 && btn != MouseEvent.BUTTON3)
		{
			// Suppress presses with unknown button, to avoid
			// problems due to horizontal scrolling.
			return;
		}

		control = (OperatingSystem.isMacOS() && evt.isMetaDown())
			|| (!OperatingSystem.isMacOS() && evt.isControlDown());

		ctrlForRectangularSelection = textArea.isCtrlForRectangularSelection();

		// so that Home <mouse click> Home is not the same
		// as pressing Home twice in a row
		textArea.getInputHandler().resetLastActionCount();

		quickCopyDrag = (textArea.isQuickCopyEnabled() &&
			isMiddleButton(evt.getModifiers()));

		if(!quickCopyDrag)
		{
			textArea.requestFocus();
			TextArea.focusedComponent = textArea;
		}

		if(textArea.getBuffer().isLoading())
			return;
		EditBus.send(new PositionChanging(textArea));
		int x = evt.getX();
		int y = evt.getY();

		dragStart = textArea.xyToOffset(x,y,
			!(textArea.getPainter().isBlockCaretEnabled()
			|| textArea.isOverwriteEnabled()));
		dragStartLine = textArea.getLineOfOffset(dragStart);
		dragStartOffset = dragStart - textArea.getLineStartOffset(
			dragStartLine);

		if(isPopupTrigger(evt)
			&& textArea.getRightClickPopup() != null)
		{
			if(textArea.isRightClickPopupEnabled())
				textArea.handlePopupTrigger(evt);
			return;
		}

		dragged = false;

		textArea.blink = true;
		textArea.invalidateLine(textArea.getCaretLine());

		clickCount = evt.getClickCount();

		if(textArea.isDragEnabled()
			&& textArea.selectionManager.insideSelection(x,y)
			&& clickCount == 1 && !evt.isShiftDown())
		{
			maybeDragAndDrop = true;

			textArea.moveCaretPosition(dragStart,false);
			return;
		}

		maybeDragAndDrop = false;

		if(quickCopyDrag)
		{
			// ignore double clicks of middle button
			doSingleClick(evt);
		}
		else
		{
			switch(clickCount)
			{
			case 1:
				doSingleClick(evt);
				break;
			case 2:
				doDoubleClick();
				break;
			default: //case 3:
				doTripleClick();
				break;
			}
		}
	} //}}}

	//{{{ mouseReleased() method
	@Override
	public void mouseReleased(MouseEvent evt)
	{
		int btn = evt.getButton();
		if (btn != MouseEvent.BUTTON1 && btn != MouseEvent.BUTTON2 && btn != MouseEvent.BUTTON3)
		{
			// Suppress releases with unknown button, to avoid
			// problems due to horizontal scrolling.
			return;
		}

		// middle mouse button drag inserts selection
		// at caret position
		Selection sel = textArea.getSelectionAtOffset(dragStart);
		if(dragged && sel != null)
		{
			Registers.setRegister('%',textArea.getSelectedText(sel));
			if(quickCopyDrag)
			{
				textArea.removeFromSelection(sel);
				Registers.paste(TextArea.focusedComponent,
					'%',sel instanceof Selection.Rect);

				TextArea.focusedComponent.requestFocus();
			}
		}
		else if(!dragged && textArea.isQuickCopyEnabled() &&
			isMiddleButton(evt.getModifiers()))
		{
			textArea.requestFocus();
			TextArea.focusedComponent = textArea;

			textArea.setCaretPosition(dragStart,false);
			if(!textArea.isEditable())
				javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(null); 
			else
				Registers.paste(textArea,'%',control);
		}
		else if(maybeDragAndDrop
			&& !textArea.isMultipleSelectionEnabled())
		{
			textArea.selectNone();
		}

		maybeDragAndDrop = false;
		dragged = false;
		if(!(textArea.isRectangularSelectionEnabled()
			|| (control && ctrlForRectangularSelection)))
			// avoid scrolling away from rectangular selection
			textArea.scrollToCaret(false);
	} //}}}

	//{{{ Private members
	private JEditTextArea textArea;
	//}}}
}
