/*
 * TextAreaPainter.java - Paints the text area
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
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

//{{{ Imports
import javax.swing.text.*;
import javax.swing.JComponent;
import java.awt.event.MouseEvent;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.util.Log;
//}}}

/**
 * The text area repaint manager. It performs double buffering and paints
 * lines of text.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextAreaPainter extends JComponent implements TabExpander
{
	//{{{ TextAreaPainter constructor
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

		highlights = new ArrayList();

		setAutoscrolls(true);
		setDoubleBuffered(true);
		setOpaque(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		fontRenderContext = new FontRenderContext(null,false,false);
	} //}}}

	//{{{ isManagingFocus() method
	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	public boolean isManagingFocus()
	{
		return false;
	} //}}}

	//{{{ getFocusTraversalKeysEnabled() method
	/**
	 * Makes the tab key work in Java 1.4.
	 * @since jEdit 3.2pre4
	 */
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	} //}}}

	//{{{ propertiesChanged() method
	public void propertiesChanged()
	{
		if(textArea.getBuffer() == null)
			return;

		int _tabSize = textArea.getBuffer().getTabSize();
		char[] foo = new char[_tabSize];
		tabSize = (float)getFont().getStringBounds(foo,0,_tabSize,
			fontRenderContext).getWidth();

		int _maxLineLen = textArea.getBuffer()
			.getIntegerProperty("maxLineLen",0);

		if(_maxLineLen <= 0)
			maxLineLen = 0;
		else
		{
			// stupidity
			foo = new char[_maxLineLen];
			for(int i = 0; i < foo.length; i++)
			{
				foo[i] = ' ';
			}
			maxLineLen = (int)getFont().getStringBounds(
				foo,0,_maxLineLen,fontRenderContext).getWidth();
		}
	} //}}}

	//{{{ Getters and setters

	//{{{ getStyles() method
	/**
	 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final SyntaxStyle[] getStyles()
	{
		return styles;
	} //}}}

	//{{{ setStyles() method
	/**
	 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
	 * will be used to paint tokens with id = <i>n</i>.
	 * @param styles The syntax styles
	 * @see org.gjt.sp.jedit.syntax.Token
	 */
	public final void setStyles(SyntaxStyle[] styles)
	{
		this.styles = styles;
		styles[Token.NULL] = new SyntaxStyle(getForeground(),null,getFont());
		repaint();
	} //}}}

	//{{{ getCaretColor() method
	/**
	 * Returns the caret color.
	 */
	public final Color getCaretColor()
	{
		return caretColor;
	} //}}}

	//{{{ setCaretColor() method
	/**
	 * Sets the caret color.
	 * @param caretColor The caret color
	 */
	public final void setCaretColor(Color caretColor)
	{
		this.caretColor = caretColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getCaretLine());
	} //}}}

	//{{{ getSelectionColor() method
	/**
	 * Returns the selection color.
	 */
	public final Color getSelectionColor()
	{
		return selectionColor;
	} //}}}

	//{{{ setSelectionColor() method
	/**
	 * Sets the selection color.
	 * @param selectionColor The selection color
	 */
	public final void setSelectionColor(Color selectionColor)
	{
		this.selectionColor = selectionColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateSelectedLines();
	} //}}}

	//{{{ getLineHighlightColor() method
	/**
	 * Returns the line highlight color.
	 */
	public final Color getLineHighlightColor()
	{
		return lineHighlightColor;
	} //}}}

	//{{{ setLineHighlightColor() method
	/**
	 * Sets the line highlight color.
	 * @param lineHighlightColor The line highlight color
	 */
	public final void setLineHighlightColor(Color lineHighlightColor)
	{
		this.lineHighlightColor = lineHighlightColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getCaretLine());
	} //}}}

	//{{{ isLineHighlightEnabled() method
	/**
	 * Returns true if line highlight is enabled, false otherwise.
	 */
	public final boolean isLineHighlightEnabled()
	{
		return lineHighlight;
	} //}}}

	//{{{ setLineHighlightEnabled() method
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
	} //}}}

	//{{{ getFoldedLineColor() method
	/**
	 * Returns the background color of a collapsed fold line.
	 */
	public final Color getFoldedLineColor()
	{
		return foldedLineColor;
	} //}}}

	//{{{ setFoldedLineColor() method
	/**
	 * Sets the background color of a collapsed fold line.
	 * @param foldedLineColor The folded line color
	 */
	public final void setFoldedLineColor(Color foldedLineColor)
	{
		this.foldedLineColor = foldedLineColor;
		repaint();
	} //}}}

	//{{{ getBracketHighlightColor() method
	/**
	 * Returns the bracket highlight color.
	 */
	public final Color getBracketHighlightColor()
	{
		return bracketHighlightColor;
	} //}}}

	//{{{ setBracketHighlightColor() method
	/**
	 * Sets the bracket highlight color.
	 * @param bracketHighlightColor The bracket highlight color
	 */
	public final void setBracketHighlightColor(Color bracketHighlightColor)
	{
		this.bracketHighlightColor = bracketHighlightColor;
		if(textArea.getBuffer() != null)
			textArea.invalidateLine(textArea.getBracketLine());
	} //}}}

	//{{{ isBracketHighlightEnabled() method
	/**
	 * Returns true if bracket highlighting is enabled, false otherwise.
	 * When bracket highlighting is enabled, the bracket matching the
	 * one before the caret (if any) is highlighted.
	 */
	public final boolean isBracketHighlightEnabled()
	{
		return bracketHighlight;
	} //}}}

	//{{{ setBracketHighlightEnabled() method
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
	} //}}}

	//{{{ isBlockCaretEnabled() method
	/**
	 * Returns true if the caret should be drawn as a block, false otherwise.
	 */
	public final boolean isBlockCaretEnabled()
	{
		return blockCaret;
	} //}}}

	//{{{ setBlockCaretEnabled() method
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
	} //}}}

	//{{{ getEOLMarkerColor() method
	/**
	 * Returns the EOL marker color.
	 */
	public final Color getEOLMarkerColor()
	{
		return eolMarkerColor;
	} //}}}

	//{{{ setEOLMarkerColor() method
	/**
	 * Sets the EOL marker color.
	 * @param eolMarkerColor The EOL marker color
	 */
	public final void setEOLMarkerColor(Color eolMarkerColor)
	{
		this.eolMarkerColor = eolMarkerColor;
		repaint();
	} //}}}

	//{{{ getEOLMarkersPainted() method
	/**
	 * Returns true if EOL markers are drawn, false otherwise.
	 */
	public final boolean getEOLMarkersPainted()
	{
		return eolMarkers;
	} //}}}

	//{{{ setEOLMarkersPainted() method
	/**
	 * Sets if EOL markers are to be drawn.
	 * @param eolMarkers True if EOL markers should be drawn, false otherwise
	 */
	public final void setEOLMarkersPainted(boolean eolMarkers)
	{
		this.eolMarkers = eolMarkers;
		repaint();
	} //}}}

	//{{{ getWrapGuideColor() method
	/**
	 * Returns the wrap guide color.
	 */
	public final Color getWrapGuideColor()
	{
		return wrapGuideColor;
	} //}}}

	//{{{ setWrapGuideColor() method
	/**
	 * Sets the wrap guide color.
	 * @param wrapGuideColor The wrap guide color
	 */
	public final void setWrapGuideColor(Color wrapGuideColor)
	{
		this.wrapGuideColor = wrapGuideColor;
		repaint();
	} //}}}

	//{{{ isWrapGuidePainted()
	/**
	 * Returns true if the wrap guide is drawn, false otherwise.
	 */
	public final boolean getWrapGuidePainted()
	{
		return wrapGuide;
	} //}}}

	//{{{ setWrapGuidePainted() method
	/**
	 * Sets if the wrap guide is to be drawn.
	 * @param wrapGuide True if the wrap guide should be drawn, false otherwise
	 */
	public final void setWrapGuidePainted(boolean wrapGuide)
	{
		this.wrapGuide = wrapGuide;
		repaint();
	} //}}}

	//{{{ setAntiAliasEnabled() method
	/**
	 * Sets if anti-aliasing should be enabled. Has no effect when
	 * running on Java 1.1.
	 * @since jEdit 3.2pre6
	 */
	public void setAntiAliasEnabled(boolean antiAlias)
	{
		this.antiAlias = antiAlias;
		updateRenderingHints();
	} //}}}

	//{{{ isAntiAliasEnabled() method
	/**
	 * Returns if anti-aliasing is enabled.
	 * @since jEdit 3.2pre6
	 */
	public boolean isAntiAliasEnabled()
	{
		return antiAlias;
	} //}}}

	//{{{ setFractionalFontMetricsEnabled() method
	/**
	 * Sets if fractional font metrics should be enabled. Has no effect when
	 * running on Java 1.1.
	 * @since jEdit 3.2pre6
	 */
	public void setFractionalFontMetricsEnabled(boolean fracFontMetrics)
	{
		this.fracFontMetrics = fracFontMetrics;
		updateRenderingHints();
	} //}}}

	//{{{ isFractionalFontMetricsEnabled() method
	/**
	 * Returns if fractional font metrics are enabled.
	 * @since jEdit 3.2pre6
	 */
	public boolean isFractionalFontMetricsEnabled()
	{
		return fracFontMetrics;
	} //}}}

	//}}}

	//{{{ addCustomHighlight() method
	/**
	 * Adds a custom highlight painter.
	 * @param highlight The highlight
	 */
	public void addCustomHighlight(TextAreaHighlight highlight)
	{
		highlights.add(highlight);

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
	} //}}}

	//{{{ removeCustomHighlight() method
	/**
	 * Removes a custom highlight painter.
	 * @param highlight The highlight
	 * @since jEdit 4.0pre1
	 */
	public void removeCustomHighlight(TextAreaHighlight highlight)
	{
		highlights.remove(highlight);
		repaint();
	} //}}}

	//{{{ getToolTipText() method
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
				highlights.get(i);
			String toolTip = highlight.getToolTipText(evt);
			if(toolTip != null)
				return toolTip;
		}

		return null;
	} //}}}

	//{{{ getFontMetrics() method
	/**
	 * Returns the font metrics used by this component.
	 */
	public FontMetrics getFontMetrics()
	{
		return fm;
	} //}}}

	//{{{ setFont() method
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

		propertiesChanged();
	} //}}}

	//{{{ paintComponent() method
	/**
	 * Repaints the text.
	 * @param g The graphics context
	 */
	public void paintComponent(Graphics _gfx)
	{
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setRenderingHints(renderingHints);
		fontRenderContext = gfx.getFontRenderContext();

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

		FoldVisibilityManager foldVisibilityManager
			= textArea.getFoldVisibilityManager();

		int lineCount = foldVisibilityManager.getVirtualLineCount();

		int y = (clipRect.y - clipRect.y % height);

		try
		{
			boolean updateMaxHorizontalScrollWidth = false;

			for(int line = firstInvalid; line <= lastInvalid; line++)
			{
				boolean valid = buffer.isLoaded()
					&& line >= 0 && line < lineCount;

				int physicalLine;
				boolean collapsedFold;

				if(valid)
				{
					physicalLine = textArea.virtualToPhysical(line);
					collapsedFold = (physicalLine < buffer.getLineCount() - 1
						&& buffer.isFoldStart(physicalLine)
						&& !foldVisibilityManager
						.isLineVisible(physicalLine + 1));
				}
				else
				{
					physicalLine = -1;
					collapsedFold = false;
				}

				int width = paintLine(gfx,buffer,valid,line,
					physicalLine,x,y,collapsedFold) - x;

				if(valid)
				{
					// it seems we are repainted before a
					// component event is received
					// (read: recalculateVisibleLines()
					// called)
					int screenLine = line - textArea.getFirstLine();
					if(screenLine < textArea.lineWidths.length)
						textArea.lineWidths[line - textArea.getFirstLine()] = width;
					if(width > textArea.maxHorizontalScrollWidth)
						updateMaxHorizontalScrollWidth = true;

					physicalLine = foldVisibilityManager
						.getNextVisibleLine(physicalLine);
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
	} //}}}

	//{{{ nextTabStop() method
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
		int ntabs = (int)((x - offset) / tabSize);
		return (ntabs + 1) * tabSize + offset;
	} //}}}

	//{{{ getPreferredSize() method
	/**
	 * Returns the painter's preferred size.
	 */
	public Dimension getPreferredSize()
	{
		Dimension dim = new Dimension();
		dim.width = fm.charWidth('w') * 80;
		dim.height = fm.getHeight() * 25;
		return dim;
	} //}}}

	//{{{ getMinimumSize() method
	/**
	 * Returns the painter's minimum size.
	 */
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	} //}}}

	//{{{ Package-private members

	public void timeL2LC()
	{
		Buffer buffer = textArea.getBuffer();
		Segment seg = new Segment();
		java.util.Random r = new java.util.Random();
		long start = System.currentTimeMillis();
		for(int i = 0; i < 1000; i++)
		{
			int l = Math.abs(r.nextInt() % textArea.getBuffer()
				.getLineCount());
			
			buffer.getLineText(l,seg);
			lineToChunkList(seg,buffer.markTokens(l).getFirstToken());
		}
		System.err.println(System.currentTimeMillis() - start);
	} //}}}

	//{{{ lineToChunkList() method
	TextUtilities.Chunk lineToChunkList(Segment seg, Token tokens)
	{
		ArrayList l = new ArrayList();
		TextUtilities.lineToChunkList(seg,tokens,styles,
			fontRenderContext,this,0.0f,l);
		if(l.size() == 0)
			return null;
		else
			return (TextUtilities.Chunk)l.get(0);
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private JEditTextArea textArea;

	private SyntaxStyle[] styles;
	private Color caretColor;
	private Color selectionColor;
	private Color lineHighlightColor;
	private Color foldedLineColor;
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

	private float tabSize;
	private int maxLineLen;

	// should try to use this as little as possible.
	private FontMetrics fm;

	private ArrayList highlights;
	private RenderingHints renderingHints;
	private FontRenderContext fontRenderContext;
	//}}}

	//{{{ updateRenderingHints() method
	private void updateRenderingHints()
	{
		HashMap hints = new HashMap();

		if(antiAlias)
		{
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		else
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
			fracFontMetrics ?
				RenderingHints.VALUE_FRACTIONALMETRICS_ON
				: RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		renderingHints = new RenderingHints(hints);
		fontRenderContext = new FontRenderContext(null,antiAlias,
			fracFontMetrics);
	} //}}}

	//{{{ paintLine() method
	private int paintLine(Graphics2D gfx, Buffer buffer, boolean valid,
		int virtualLine, int physicalLine, int x, int y,
		boolean collapsedFold)
	{
		paintHighlight(gfx,virtualLine,physicalLine,y,valid,collapsedFold);

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

			float baseLine = y + fm.getHeight()
				- fm.getLeading() - fm.getDescent();

			Segment seg = new Segment();
			buffer.getLineText(physicalLine,seg);
			TextUtilities.Chunk chunks = lineToChunkList(seg,buffer.markTokens(
				physicalLine).getFirstToken());

			x += TextUtilities.paintChunkList(chunks,gfx,x,baseLine);

			gfx.setFont(defaultFont);
			gfx.setColor(eolMarkerColor);

			if(collapsedFold)
			{
				int nextLine = textArea.getFoldVisibilityManager()
					.getNextVisibleLine(physicalLine);
				if(nextLine == -1)
					nextLine = buffer.getLineCount();

				int count = nextLine - physicalLine - 1;
				String str = " [" + count + " lines]";
				gfx.drawString(str,x,baseLine);
				x += fm.stringWidth(str);
			}
			else if(eolMarkers)
			{
				gfx.drawString(".",x,baseLine);
				x += fm.charWidth('.');
			}

			if(physicalLine == textArea.getCaretLine()
				&& textArea.isCaretVisible())
				paintCaret(gfx,physicalLine,y,collapsedFold);
		}

		return x;
	} //}}}

	//{{{ paintHighlight() method
	private void paintHighlight(Graphics2D gfx, int virtualLine,
		int physicalLine, int y, boolean valid,
		boolean collapsedFold)
	{
		if(valid)
		{
			boolean paintLineHighlight = lineHighlight
				&& physicalLine == textArea.getCaretLine()
				&& textArea.selection.size() == 0;

			Buffer buffer = textArea.getBuffer();
			if(!paintLineHighlight && collapsedFold)
			{
				gfx.setColor(foldedLineColor);
				gfx.fillRect(0,y,getWidth(),fm.getHeight());
			}

			if(textArea.selection.size() == 0)
			{
				if(paintLineHighlight)
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
				&& textArea.isBracketHighlightVisible())
				paintBracketHighlight(gfx,physicalLine,y);
		}

		if(highlights.size() != 0)
		{
			for(int i = 0; i < highlights.size(); i++)
			{
				TextAreaHighlight highlight = (TextAreaHighlight)
					highlights.get(i);
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
					highlights.remove(i);
					i--;
				}
			}
		}
	} //}}}

	//{{{ paintBracketHighlight() method
	private void paintBracketHighlight(Graphics2D gfx, int physicalLine, int y)
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
	} //}}}

	//{{{ paintCaret() method
	private void paintCaret(Graphics2D gfx, int physicalLine, int y,
		boolean collapsedFold)
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
			if(collapsedFold)
				gfx.setXORMode(foldedLineColor);
			else if(textArea.selection == null && lineHighlight)
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
	} //}}}

	//{{{ paintSelection() method
	private void paintSelection(Graphics2D gfx, int physicalLine, int y,
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
	} //}}}

	//}}}
}
