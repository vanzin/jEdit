/*
 * TextAreaPainter.java - Paints the text area
 * Copyright (C) 1999, 2000, 2001 Slava Pestov
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

import javax.swing.text.*;
import javax.swing.JComponent;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Vector;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.util.Log;

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextAreaPainter extends JComponent implements TabExpander
{
	/**
	 * Creates a new painter. Do not create instances of this class
	 * directly.
	 */
	public TextAreaPainter(JEditTextArea textArea)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK
			| AWTEvent.KEY_EVENT_MASK
			| AWTEvent.MOUSE_EVENT_MASK);

		this.textArea = textArea;

		highlights = new Vector();

		setAutoscrolls(true);
		setDoubleBuffered(true);
		setOpaque(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	public boolean isManagingFocus()
	{
		return false;
	}

	/**
	 * Makes the tab key work in Java 1.4.
	 * @since jEdit 3.2pre4
	 */
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	}

	/**
	 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final SyntaxStyle[] getStyles()
	{
		return styles;
	}

	/**
	 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @param styles The syntax styles
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final void setStyles(SyntaxStyle[] styles)
	{
		this.styles = styles;
		repaint();
	}

	/**
	 * Returns the caret color.
	 */
	public final Color getCaretColor()
	{
		return caretColor;
	}

	/**
	 * Sets the caret color.
	 * @param caretColor The caret color
	 */
	public final void setCaretColor(Color caretColor)
	{
		this.caretColor = caretColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getCaretLine());
	}

	/**
	 * Returns the selection color.
	 */
	public final Color getSelectionColor()
	{
		return selectionColor;
	}

	/**
	 * Sets the selection color.
	 * @param selectionColor The selection color
	 */
	public final void setSelectionColor(Color selectionColor)
	{
		this.selectionColor = selectionColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateSelectedLines();
	}

	/**
	 * Returns the line highlight color.
	 */
	public final Color getLineHighlightColor()
	{
		return lineHighlightColor;
	}

	/**
	 * Sets the line highlight color.
	 * @param lineHighlightColor The line highlight color
	 */
	public final void setLineHighlightColor(Color lineHighlightColor)
	{
		this.lineHighlightColor = lineHighlightColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getCaretLine());
	}

	/**
	 * Returns true if line highlight is enabled, false otherwise.
	 */
	public final boolean isLineHighlightEnabled()
	{
		return lineHighlight;
	}

	/**
	 * Enables or disables current line highlighting.
	 * @param lineHighlight True if current line highlight should be enabled,
	 * false otherwise
	 */
	public final void setLineHighlightEnabled(boolean lineHighlight)
	{
		this.lineHighlight = lineHighlight;
		if(textArea.getBuffer() != null)
			textArea.invalidateSelectedLines();
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
	 */
	public final void setBracketHighlightColor(Color bracketHighlightColor)
	{
		this.bracketHighlightColor = bracketHighlightColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
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
	 */
	public final void setBracketHighlightEnabled(boolean bracketHighlight)
	{
		this.bracketHighlight = bracketHighlight;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getBracketLine());
	}

	/**
	 * Returns true if the caret should be drawn as a block, false otherwise.
	 */
	public final boolean isBlockCaretEnabled()
	{
		return blockCaret;
	}

	/**
	 * Sets if the caret should be drawn as a block, false otherwise.
	 * @param blockCaret True if the caret should be drawn as a block,
	 * false otherwise.
	 */
	public final void setBlockCaretEnabled(boolean blockCaret)
	{
		this.blockCaret = blockCaret;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getCaretLine());
	}

	/**
	 * Returns the EOL marker color.
	 */
	public final Color getEOLMarkerColor()
	{
		return eolMarkerColor;
	}

	/**
	 * Sets the EOL marker color.
	 * @param eolMarkerColor The EOL marker color
	 */
	public final void setEOLMarkerColor(Color eolMarkerColor)
	{
		this.eolMarkerColor = eolMarkerColor;
		repaint();
	}

	/**
	 * Returns true if EOL markers are drawn, false otherwise.
	 */
	public final boolean getEOLMarkersPainted()
	{
		return eolMarkers;
	}

	/**
	 * Sets if EOL markers are to be drawn.
	 * @param eolMarkers True if EOL markers should be drawn, false otherwise
	 */
	public final void setEOLMarkersPainted(boolean eolMarkers)
	{
		this.eolMarkers = eolMarkers;
		repaint();
	}

	/**
	 * Returns the wrap guide color.
	 */
	public final Color getWrapGuideColor()
	{
		return wrapGuideColor;
	}

	/**
	 * Sets the wrap guide color.
	 * @param wrapGuideColor The wrap guide color
	 */
	public final void setWrapGuideColor(Color wrapGuideColor)
	{
		this.wrapGuideColor = wrapGuideColor;
		repaint();
	}

	/**
	 * Returns true if the wrap guide is drawn, false otherwise.
	 */
	public final boolean getWrapGuidePainted()
	{
		return wrapGuide;
	}

	/**
	 * Sets if the wrap guide is to be drawn.
	 * @param wrapGuide True if the wrap guide should be drawn, false otherwise
	 */
	public final void setWrapGuidePainted(boolean wrapGuide)
	{
		this.wrapGuide = wrapGuide;
		repaint();
	}

	/**
	 * Sets if anti-aliasing should be enabled. Has no effect when
	 * running on Java 1.1.
	 * @since jEdit 3.2pre6
	 */
	public void setAntiAliasEnabled(boolean antiAlias)
	{
		this.antiAlias = antiAlias;
		textArea.getTextRenderer().configure(antiAlias,fracFontMetrics);
	}

	/**
	 * Returns if anti-aliasing is enabled.
	 * @since jEdit 3.2pre6
	 */
	public boolean isAntiAliasEnabled()
	{
		return antiAlias;
	}

	/**
	 * Sets if fractional font metrics should be enabled. Has no effect when
	 * running on Java 1.1.
	 * @since jEdit 3.2pre6
	 */
	public void setFractionalFontMetricsEnabled(boolean fracFontMetrics)
	{
		this.fracFontMetrics = fracFontMetrics;
		textArea.getTextRenderer().configure(antiAlias,fracFontMetrics);
	}

	/**
	 * Returns if fractional font metrics are enabled.
	 * @since jEdit 3.2pre6
	 */
	public boolean isFractionalFontMetricsEnabled()
	{
		return fracFontMetrics;
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
	 * Returns the tool tip to display at the specified location.
	 * @param evt The mouse event
	 */
	public String getToolTipText(MouseEvent evt)
	{
		if(maxLineLen != 0)
		{
			int wrapGuidePos = maxLineLen + textArea.getHorizontalOffset();
			if(Math.abs(evt.getX() - wrapGuidePos) < 5)
			{
				return String.valueOf(textArea.getBuffer()
					.getProperty("maxLineLen"));
			}
		}

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
	 * Returns the font metrics used by this component.
	 */
	public FontMetrics getFontMetrics()
	{
		return fm;
	}

	/**
	 * Sets the font for this component. This is overridden to update the
	 * cached font metrics and to recalculate which lines are visible.
	 * @param font The font
	 */
	public void setFont(Font font)
	{
		super.setFont(font);
		fm = getFontMetrics(font);
		textArea.recalculateVisibleLines();

		updateTabSize();
	}

	/**
	 * Repaints the text.
	 * @param g The graphics context
	 */
	public void paintComponent(Graphics gfx)
	{
		updateTabSize();

		textArea.getTextRenderer().setupGraphics(gfx);

		Buffer buffer = textArea.getBuffer();

		Rectangle clipRect = gfx.getClipBounds();

		gfx.setColor(getBackground());
		gfx.fillRect(clipRect.x,clipRect.y,clipRect.width,clipRect.height);

		int x = textArea.getHorizontalOffset();

		int height = fm.getHeight();
		int firstLine = textArea.getFirstLine();
		int firstInvalid = firstLine + clipRect.y / height;
		// Because the clipRect's height is usually an even multiple
		// of the font height, we subtract 1 from it, otherwise one
		// too many lines will always be painted.
		int lastInvalid = firstLine + (clipRect.y + clipRect.height - 1) / height;
		int lineCount = textArea.getVirtualLineCount();

		int y = (clipRect.y - clipRect.y % height);

		try
		{
			int maxWidth = textArea.maxHorizontalScrollWidth;

			boolean updateMaxHorizontalScrollWidth = false;
			for(int line = firstInvalid; line <= lastInvalid; line++)
			{
				boolean valid = buffer.isLoaded()
					&& line >= 0 && line < lineCount;

				int physicalLine;
				if(valid)
					physicalLine = buffer.virtualToPhysical(line);
				else
				{
					int virtualLineCount = buffer.getVirtualLineCount();
					physicalLine = buffer.virtualToPhysical(
						virtualLineCount - 1)
						+ (line - virtualLineCount);
				}

				int width = paintLine(gfx,buffer,valid,line,
					physicalLine,x,y)
					- x + 5 /* Yay */;

				if(valid)
				{
					buffer.setLineWidth(physicalLine,width);
					if(width > maxWidth)
						updateMaxHorizontalScrollWidth = true;
				}

				y += height;
			}

			if(buffer.isNextLineRequested())
			{
				int h = clipRect.y + clipRect.height;
				repaint(0,h,getWidth(),getHeight() - h);
			}

			if(updateMaxHorizontalScrollWidth)
				textArea.updateMaxHorizontalScrollWidth();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Error repainting line"
				+ " range {" + firstInvalid + ","
				+ lastInvalid + "}:");
			Log.log(Log.ERROR,this,e);
		}
	}

	/**
	 * Implementation of TabExpander interface. Returns next tab stop after
	 * a specified point.
	 * @param x The x co-ordinate
	 * @param tabOffset Ignored
	 * @return The next tab stop after <i>x</i>
	 */
	public float nextTabStop(float x, int tabOffset)
	{
		int offset = textArea.getHorizontalOffset();
		int ntabs = ((int)x - offset) / tabSize;
		return (ntabs + 1) * tabSize + offset;
	}

	/**
	 * Returns the painter's preferred size.
	 */
	public Dimension getPreferredSize()
	{
		Dimension dim = new Dimension();
		dim.width = fm.charWidth('w') * 80;
		dim.height = fm.getHeight() * 25;
		return dim;
	}

	/**
	 * Returns the painter's minimum size.
	 */
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	// package-private members
	void updateTabSize()
	{
		if(textArea.getBuffer() == null)
			return;

		tabSize = fm.charWidth(' ') * ((Integer)textArea
			.getBuffer().getProperty(
			PlainDocument.tabSizeAttribute)).intValue();

		int _maxLineLen = ((Integer)textArea.getBuffer()
			.getProperty("maxLineLen")).intValue();

		if(_maxLineLen <= 0)
			maxLineLen = 0;
		else
			maxLineLen = fm.charWidth(' ') * _maxLineLen;
	}

	// private members
	private JEditTextArea textArea;

	private SyntaxStyle[] styles;
	private Color caretColor;
	private Color selectionColor;
	private Color lineHighlightColor;
	private Color bracketHighlightColor;
	private Color eolMarkerColor;
	private Color wrapGuideColor;

	private boolean blockCaret;
	private boolean lineHighlight;
	private boolean bracketHighlight;
	private boolean eolMarkers;
	private boolean wrapGuide;
	private boolean antiAlias;
	private boolean fracFontMetrics;

	private int tabSize;
	private int maxLineLen;
	private FontMetrics fm;

	private Vector highlights;

	private int paintLine(Graphics gfx, Buffer buffer, boolean valid,
		int virtualLine, int physicalLine, int x, int y)
	{
		paintHighlight(gfx,virtualLine,physicalLine,y,valid);

		if(maxLineLen != 0 && wrapGuide)
		{
			gfx.setColor(wrapGuideColor);
			gfx.drawLine(x + maxLineLen,y,x + maxLineLen,
				y + fm.getHeight());
		}

		if(valid)
		{
			Font defaultFont = getFont();
			Color defaultColor = getForeground();

			gfx.setFont(defaultFont);
			gfx.setColor(defaultColor);

			int baseLine = y + fm.getHeight()
				- fm.getLeading() - fm.getDescent();

			x = buffer.paintSyntaxLine(physicalLine,gfx,x,baseLine,
				this,true,true,defaultFont,defaultColor,
				(lineHighlight
				&& textArea.getSelectionCount() == 0
				&& physicalLine == textArea.getCaretLine()
				? lineHighlightColor
				: getBackground()),styles,
				textArea.getTextRenderer());

			if(eolMarkers)
			{
				gfx.setFont(defaultFont);
				gfx.setColor(eolMarkerColor);
				gfx.drawString(".",x,baseLine);
			}

			if(physicalLine == textArea.getCaretLine()
				&& textArea.isCaretVisible())
				paintCaret(gfx,physicalLine,y);

			if(buffer.isFoldStart(physicalLine)
				&& !buffer.isLineVisible(physicalLine + 1))
			{
				gfx.setColor(defaultColor);
				int start = textArea.getHorizontalOffset()
					+ fm.charWidth(' ') * buffer.getFoldLevel(physicalLine);

				gfx.drawLine(start,y + fm.getHeight() - 1,
					x - 1,y + fm.getHeight() - 1);
			}
		}

		return x;
	}

	private void paintHighlight(Graphics gfx, int virtualLine,
		int physicalLine, int y, boolean valid)
	{
		if(valid)
		{
			if(textArea.selection.size() == 0)
			{
				if(lineHighlight && physicalLine == textArea.getCaretLine())
				{
					gfx.setColor(lineHighlightColor);
					gfx.fillRect(0,y,getWidth(),fm.getHeight());
				}
			}
			else
			{
				gfx.setColor(selectionColor);
				for(int i = textArea.selection.size() - 1;
					i >= 0; i--)
				{
					paintSelection(gfx,physicalLine,y,
						(Selection)textArea.selection
						.elementAt(i));
				}
			}

			if(bracketHighlight
				&& physicalLine == textArea.getBracketLine()
				&& textArea.isHighlightVisible())
				paintBracketHighlight(gfx,physicalLine,y);
		}

		if(highlights.size() != 0)
		{
			for(int i = 0; i < highlights.size(); i++)
			{
				TextAreaHighlight highlight = (TextAreaHighlight)
					highlights.elementAt(i);
				try
				{
					highlight.paintHighlight(gfx,virtualLine,
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
	}

	private void paintBracketHighlight(Graphics gfx, int physicalLine, int y)
	{
		int position = textArea.getBracketPosition();
		if(position == -1)
			return;

		int x = textArea.offsetToX(physicalLine,position);
		gfx.setColor(bracketHighlightColor);
		// Hack!!! Since there is no fast way to get the character
		// from the bracket matching routine, we use ( since all
		// brackets probably have the same width anyway
		gfx.drawRect(x,y,fm.charWidth('(') - 1,
			fm.getHeight() - 1);
	}

	private void paintCaret(Graphics gfx, int physicalLine, int y)
	{
		int offset = textArea.getCaretPosition()
			- textArea.getLineStartOffset(physicalLine);
		int caretX = textArea.offsetToX(physicalLine,offset);
		int height = fm.getHeight();

		gfx.setColor(caretColor);

		if(textArea.isOverwriteEnabled())
		{
			gfx.drawLine(caretX,y + height - 1,
				caretX + fm.charWidth('w'),y + height - 1);
		}
		else if(blockCaret)
		{
			if(textArea.selection == null && lineHighlight)
				gfx.setXORMode(lineHighlightColor);
			else
				gfx.setXORMode(getBackground());

			gfx.fillRect(caretX,y,fm.charWidth('w'),height);
			gfx.setPaintMode();
		}
		else
		{
			gfx.drawLine(caretX,y,caretX,y + height - 1);
		}
	}

	private void paintSelection(Graphics gfx, int physicalLine, int y,
		Selection s)
	{
		if(physicalLine < s.startLine || physicalLine > s.endLine)
			return;

		int lineStart = textArea.getLineStartOffset(physicalLine);
		int x1, x2;

		if(s instanceof Selection.Rect)
		{
			int lineLen = textArea.getLineLength(physicalLine);
			x1 = textArea.offsetToX(physicalLine,Math.min(lineLen,
				s.start - textArea.getLineStartOffset(
				s.startLine)));
			x2 = textArea.offsetToX(physicalLine,Math.min(lineLen,
				s.end - textArea.getLineStartOffset(
				s.endLine)));

			if(x1 > x2)
			{
				int tmp = x2;
				x2 = x1;
				x1 = tmp;
			}
		}
		else if(s.startLine == s.endLine)
		{
			x1 = textArea.offsetToX(physicalLine,
				s.start - lineStart);
			x2 = textArea.offsetToX(physicalLine,
				s.end - lineStart);
		}
		else if(physicalLine == s.startLine)
		{
			x1 = textArea.offsetToX(physicalLine,
				s.start - lineStart);
			x2 = getWidth();
		}
		else if(physicalLine == s.endLine)
		{
			x1 = 0;
			x2 = textArea.offsetToX(physicalLine,
				s.end - lineStart);
		}
		else
		{
			x1 = 0;
			x2 = getWidth();
		}

		if(x1 == x2)
			x2++;

		gfx.fillRect(x1,y,x2 - x1,fm.getHeight());
	}
}
