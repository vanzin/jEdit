/*
 * Gutter.java
 * Copyright (C) 1999, 2000 mike dillon
 * Portions copyright (C) 2001 Slava Pestov
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

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class Gutter extends JComponent implements SwingConstants
{
	public Gutter(View view, JEditTextArea textArea)
	{
		this.view = view;
		this.textArea = textArea;

		highlights = new Vector();

		setDoubleBuffered(true);

		MouseHandler ml = new MouseHandler();
		addMouseListener(ml);
		addMouseMotionListener(ml);
	}

	public void paintComponent(Graphics gfx)
	{
		// fill the background
		Rectangle clip = gfx.getClipBounds();
		gfx.setColor(getBackground());
		gfx.fillRect(clip.x, clip.y, clip.width, clip.height);

		// if buffer is loading, don't paint anything
		if (!textArea.getBuffer().isLoaded())
			return;

		// paint highlights and line numbers
		int lineHeight = textArea.getPainter().getFontMetrics()
			.getHeight();

		int firstLine = clip.y / lineHeight + textArea.getFirstLine();
		int lastLine = (clip.y + clip.height - 1) / lineHeight
			+ textArea.getFirstLine();

		FontMetrics pfm = textArea.getPainter().getFontMetrics();
		Color fg = getForeground();

		int baseline = (int)((this.baseline + lineHeight
			- pfm.getDescent()) / 2.0);

		boolean highlightCurrentLine = currentLineHighlightEnabled
			&& textArea.selection.size() == 0;

		int y = (clip.y - clip.y % lineHeight);

		Buffer buffer = textArea.getBuffer();

		int firstValidLine = firstLine >= 0 ? firstLine : 0;
		int lastValidLine = (lastLine >= buffer.getVirtualLineCount())
			? buffer.getVirtualLineCount() - 1 : lastLine;

		for (int line = firstLine; line <= lastLine;
			line++, y += lineHeight)
		{
			boolean valid = (line >= firstValidLine && line <= lastValidLine);

			if(highlights.size() != 0)
			{
				for(int i = 0; i < highlights.size(); i++)
				{
					TextAreaHighlight highlight = (TextAreaHighlight)
						highlights.elementAt(i);
					try
					{
						highlight.paintHighlight(gfx,line,
							y - fm.getLeading() - fm.getDescent());
					}
					catch(Throwable t)
					{
						Log.log(Log.ERROR,this,t);
	
						// remove it so editor can continue
						// functioning
						highlights.removeElementAt(i);
						i--;
					}
				}
			}

			if(!valid)
				return;

			// calculate the physical line
			int physicalLine = buffer.virtualToPhysical(line);

			if(physicalLine != buffer.getLineCount() - 1
				&& buffer.isFoldStart(physicalLine))
			{
				int _y = y + lineHeight / 2;
				gfx.setColor(foldColor);
				if(buffer.isLineVisible(physicalLine + 1))
				{
					gfx.drawLine(1,_y - 3,10,_y - 3);
					gfx.drawLine(2,_y - 2,9,_y - 2);
					gfx.drawLine(3,_y - 1,8,_y - 1);
					gfx.drawLine(4,_y,7,_y);
					gfx.drawLine(5,_y + 1,6,_y + 1);
				}
				else
				{
					gfx.drawLine(4,_y - 5,4,_y + 4);
					gfx.drawLine(5,_y - 4,5,_y + 3);
					gfx.drawLine(6,_y - 3,6,_y + 2);
					gfx.drawLine(7,_y - 2,7,_y + 1);
					gfx.drawLine(8,_y - 1,8,_y);
				}
			}
			else
			{
				int bracketLine = textArea.getBracketLine();
				if(bracketLine != -1)
				{
					int caretLine = textArea.getCaretLine();
					if(caretLine != bracketLine)
					{
						if(caretLine > bracketLine)
						{
							int tmp = caretLine;
							caretLine = bracketLine;
							bracketLine = tmp;
						}

						gfx.setColor(bracketHighlightColor);
						if(physicalLine == caretLine)
						{
							gfx.fillRect(5,
								y
								+ lineHeight / 2,
								5,
								2);
							gfx.fillRect(5,
								y
								+ lineHeight / 2,
								2,
								lineHeight - lineHeight / 2);
						}
						else if(physicalLine == bracketLine)
						{
							gfx.fillRect(5,
								y,
								2,
								lineHeight / 2);
							gfx.fillRect(5,
								y + lineHeight / 2,
								5,
								2);
						}
						else if(physicalLine > caretLine
							&& physicalLine < bracketLine)
						{
							gfx.fillRect(5,
								y,
								2,
								lineHeight);
						}
					}
				}
			}

			if (!expanded)
				continue;

			String number = Integer.toString(physicalLine + 1);

			int offset;
			switch (alignment)
			{
			case RIGHT:
				offset = gutterSize.width - collapsedSize.width
					- (fm.stringWidth(number) + 1);
				break;
			case CENTER:
				offset = ((gutterSize.width - collapsedSize.width)
					- fm.stringWidth(number)) / 2;
				break;
			case LEFT: default:
				offset = 0;
				break;
			}

			if (physicalLine == textArea.getCaretLine() && highlightCurrentLine)
			{
				gfx.setColor(currentLineHighlight);
			}
			else if (interval > 1 && (line + 1) % interval == 0)
				gfx.setColor(intervalHighlight);
			else
				gfx.setColor(fg);

			gfx.drawString(number, FOLD_MARKER_SIZE + offset,
				baseline + y);
		}
	}

	/**
	 * Adds a custom highlight painter.
	 * @param highlight The highlight
	 */
	public void addCustomHighlight(TextAreaHighlight highlight)
	{
		highlights.addElement(highlight);

		// handle old highlighters
		Class clazz = highlight.getClass();
		try
		{
			Method method = clazz.getMethod("init",
				new Class[] { JEditTextArea.class,
				TextAreaHighlight.class });
			if(method != null)
			{
				Log.log(Log.WARNING,this,clazz.getName()
					+ " uses old highlighter API");
				method.invoke(highlight,new Object[] { textArea, null });
			}
		}
		catch(Exception e)
		{
			// ignore
		}

		repaint();
	}

	/**
	 * Removes a custom highlight painter.
	 * @param highlight The highlight
	 * @since jEdit 4.0pre1
	 */
	public void removeCustomHighlight(TextAreaHighlight highlight)
	{
		highlights.removeElement(highlight);
		repaint();
	}

	/**
	 * Convenience method for setting a default matte border on the right
	 * with the specified border width and color
	 * @param width The border width (in pixels)
	 * @param color1 The focused border color
	 * @param color2 The unfocused border color
	 * @param color3 The gutter/text area gap color
	 */
	public void setBorder(int width, Color color1, Color color2, Color color3)
	{
		this.borderWidth = width;

		focusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color1));
		noFocusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color2));
		updateBorder();
	}

	/**
	 * Sets the border differently if the text area has focus or not.
	 */
	public void updateBorder()
	{
		// because we are called from the text area's focus handler,
		// we do an invokeLater() so that the view's focus handler
		// has a chance to execute and set the edit pane properly
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(view.getEditPane() == null)
					return;

				if(view.getEditPane().getTextArea() == textArea)
					setBorder(focusBorder);
				else
					setBorder(noFocusBorder);
			}
		});
	}

	/*
	 * JComponent.setBorder(Border) is overridden here to cache the left
	 * inset of the border (if any) to avoid having to fetch it during every
	 * repaint.
	 */
	public void setBorder(Border border)
	{
		super.setBorder(border);

		if (border == null)
		{
			collapsedSize.width = 0;
			collapsedSize.height = 0;
		}
		else
		{
			Insets insets = border.getBorderInsets(this);
			collapsedSize.width = FOLD_MARKER_SIZE + insets.right;
			collapsedSize.height = gutterSize.height
				= insets.top + insets.bottom;
			gutterSize.width = FOLD_MARKER_SIZE + insets.right
				+ fm.stringWidth("12345");
		}
	}

	/*
	 * JComponent.setFont(Font) is overridden here to cache the baseline for
	 * the font. This avoids having to get the font metrics during every
	 * repaint.
	 */
	public void setFont(Font font)
	{
		super.setFont(font);

		fm = getFontMetrics(font);

		baseline = fm.getAscent();
	}

	/**
	 * Get the foreground color for highlighted line numbers
	 * @return The highlight color
	 */
	public Color getHighlightedForeground()
	{
		return intervalHighlight;
	}

	public void setHighlightedForeground(Color highlight)
	{
		intervalHighlight = highlight;
	}

	public Color getCurrentLineForeground()
 	{
		return currentLineHighlight;
	}

	public void setCurrentLineForeground(Color highlight)
	{
		currentLineHighlight = highlight;
 	}

	public Color getFoldColor()
 	{
		return foldColor;
	}

	public void setFoldColor(Color foldColor)
	{
		this.foldColor = foldColor;
 	}

	/*
	 * Component.getPreferredSize() is overridden here to support the
	 * collapsing behavior.
	 */
	public Dimension getPreferredSize()
	{
		if (expanded)
			return gutterSize;
		else
			return collapsedSize;
	}

	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	public String getToolTipText(MouseEvent evt)
	{
		for(int i = 0; i < highlights.size(); i++)
		{
			TextAreaHighlight highlight =
				(TextAreaHighlight)
				highlights.elementAt(i);
			String toolTip = highlight.getToolTipText(evt);
			if(toolTip != null)
				return toolTip;
		}

		return null;
	}

	/**
	 * Identifies whether the horizontal alignment of the line numbers.
	 * @return Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public int getLineNumberAlignment()
	{
		return alignment;
	}

	/**
	 * Sets the horizontal alignment of the line numbers.
	 * @param alignment Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public void setLineNumberAlignment(int alignment)
	{
		if (this.alignment == alignment) return;

		this.alignment = alignment;

		repaint();
	}

	/**
	 * Identifies whether the gutter is collapsed or expanded.
	 * @return true if the gutter is expanded, false if it is collapsed
	 */
	public boolean isExpanded()
	{
		return expanded;
	}

	/**
	 * Sets whether the gutter is collapsed or expanded and force the text
	 * area to update its layout if there is a change.
	 * @param collapsed true if the gutter is expanded,
	 *                   false if it is collapsed
	 */
	public void setExpanded(boolean expanded)
	{
		if (this.expanded == expanded) return;

		this.expanded = expanded;

		textArea.revalidate();
	}

	/**
	 * Toggles whether the gutter is collapsed or expanded.
	 */
	public void toggleExpanded()
	{
		setExpanded(!expanded);
	}

	/**
	 * Sets the number of lines between highlighted line numbers.
	 * @return The number of lines between highlighted line numbers or
	 *          zero if highlighting is disabled
	 */
	public int getHighlightInterval()
	{
		return interval;
	}

	/**
	 * Sets the number of lines between highlighted line numbers. Any value
	 * less than or equal to one will result in highlighting being disabled.
	 * @param interval The number of lines between highlighted line numbers
	 */
	public void setHighlightInterval(int interval)
	{
		if (interval <= 1) interval = 0;
		this.interval = interval;
		repaint();
	}

	public boolean isCurrentLineHighlightEnabled()
	{
		return currentLineHighlightEnabled;
	}

	public void setCurrentLineHighlightEnabled(boolean enabled)
	{
		if (currentLineHighlightEnabled == enabled) return;

		currentLineHighlightEnabled = enabled;

		repaint();
	}

	/**
	 * Returns the bracket highlight color.
	 */
	public final Color getBracketHighlightColor()
	{
		return bracketHighlightColor;
	}

	/**
	 * Sets the bracket highlight color.
	 * @param bracketHighlightColor The bracket highlight color
	 * @since jEdit 4.0pre1
	 */
	public final void setBracketHighlightColor(Color bracketHighlightColor)
	{
		this.bracketHighlightColor = bracketHighlightColor;
		repaint();
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 * @since jEdit 4.0pre1
	 */
	public final boolean isBracketHighlightEnabled()
	{
		return bracketHighlight;
	}

	/**
	 * Enables or disables bracket highlighting.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 * @param bracketHighlight True if bracket highlighting should be
	 * enabled, false otherwise
	 * @since jEdit 4.0pre1
	 */
	public final void setBracketHighlightEnabled(boolean bracketHighlight)
	{
		this.bracketHighlight = bracketHighlight;
		repaint();
	}

	// private members
	private static final int FOLD_MARKER_SIZE = 12;

	private View view;
	private JEditTextArea textArea;

	private Vector highlights;

	private int baseline;

	private Dimension gutterSize = new Dimension(0,0);
	private Dimension collapsedSize = new Dimension(0,0);

	private Color intervalHighlight;
	private Color currentLineHighlight;
	private Color foldColor;

	private FontMetrics fm;

	private int alignment;

	private int interval;
	private boolean currentLineHighlightEnabled;
	private boolean expanded;

	private boolean bracketHighlight;
	private Color bracketHighlightColor;

	private int borderWidth;
	private Border focusBorder, noFocusBorder;

	class MouseHandler implements MouseListener, MouseMotionListener
	{
		boolean drag;
		int toolTipInitialDelay, toolTipReshowDelay;

		public void mouseEntered(MouseEvent e)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			toolTipInitialDelay = ttm.getInitialDelay();
			toolTipReshowDelay = ttm.getReshowDelay();
			ttm.setInitialDelay(0);
			ttm.setReshowDelay(0);
		}

		public void mouseExited(MouseEvent evt)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setInitialDelay(toolTipInitialDelay);
			ttm.setReshowDelay(toolTipReshowDelay);
		}

		public void mousePressed(MouseEvent e)
		{
			if(e.getX() < getWidth() - borderWidth * 2)
			{
				Buffer buffer = textArea.getBuffer();

				int line = e.getY() / textArea.getPainter()
					.getFontMetrics().getHeight()
					+ textArea.getFirstLine();

				if(line > buffer.getVirtualLineCount() - 1)
					return;

				line = buffer.virtualToPhysical(line);
				if(buffer.isFoldStart(line))
				{
					if(e.isControlDown())
					{
						buffer.expandFoldAt(line,true,textArea);
						textArea.selectFoldAt(line);
					}
					else if(buffer.isLineVisible(line + 1))
						buffer.collapseFoldAt(line);
					else
						buffer.expandFoldAt(line,e.isShiftDown(),textArea);
				}
			}
			else
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mousePressed(e);
				drag = true;
			}
		}

		public void mouseDragged(MouseEvent e)
		{
			if(drag && e.getX() >= getWidth() - borderWidth * 2)
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mouseDragged(e);
			}
		}

		public void mouseMoved(MouseEvent e) {}

		public void mouseReleased(MouseEvent e)
		{
			if(drag && e.getX() >= getWidth() - borderWidth * 2)
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mouseReleased(e);
			}

			drag = false;
		}

		public void mouseClicked(MouseEvent e) {}
	}
}
