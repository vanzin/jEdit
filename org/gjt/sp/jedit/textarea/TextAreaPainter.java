/*
 * TextAreaPainter.java - Paints the text area
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000, 2001, 2002 Slava Pestov
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
import java.util.ArrayList;
import java.util.HashMap;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;
//}}}

/**
 * The text area painter is the component responsible for displaying the
 * text of the current buffer. The only methods in this class that should
 * be called by plugins are those for adding and removing
 * text area extensions.
 *
 * @see #addExtension(TextAreaExtension)
 * @see #addExtension(int,TextAreaExtension)
 * @see #removeExtension(TextAreaExtension)
 * @see TextAreaExtension
 * @see JEditTextArea
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class TextAreaPainter extends JComponent implements TabExpander
{
	//{{{ Layers
	/**
	 * The lowest possible layer.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre4
	 */
	public static final int LOWEST_LAYER = Integer.MIN_VALUE;

	/**
	 * Below selection layer. The JDiff plugin will use this.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre4
	 */
	public static final int BELOW_SELECTION_LAYER = -40;

	/**
	 * Selection layer. Most extensions will be above this layer, but some
	 * (eg, JDiff) will want to be below the selection.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre4
	 */
	public static final int SELECTION_LAYER = -30;

	/**
	 * Wrap guide layer. Most extensions will be above this layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int WRAP_GUIDE_LAYER = -20;

	/**
	 * Below most extensions layer.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre4
	 */
	public static final int BELOW_MOST_EXTENSIONS_LAYER = -10;

	/**
	 * Default extension layer. This is above the wrap guide but below the
	 * bracket highlight.
	 * @since jEdit 4.0pre4
	 */
	public static final int DEFAULT_LAYER = 0;

	/**
	 * Bracket highlight layer. Most extensions will be below this layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int BRACKET_HIGHLIGHT_LAYER = 100;

	/**
	 * Highest possible layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int HIGHEST_LAYER = Integer.MAX_VALUE;
	//}}}

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

		extensionMgr = new ExtensionManager();

		setAutoscrolls(true);
		setOpaque(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		fontRenderContext = new FontRenderContext(null,false,false);

		returnValue = new Point();

		addExtension(SELECTION_LAYER,new PaintSelection());
		addExtension(WRAP_GUIDE_LAYER,new WrapGuide());
		addExtension(BRACKET_HIGHLIGHT_LAYER,new BracketHighlight());
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

	//{{{ isWrapGuidePainted() method
	/**
	 * Returns true if the wrap guide is drawn, false otherwise.
	 * @since jEdit 4.0pre4
	 */
	public final boolean isWrapGuidePainted()
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

	//{{{ getFontRenderContext() method
	/**
	 * Returns the font render context.
	 * @since jEdit 4.0pre4
	 */
	public FontRenderContext getFontRenderContext()
	{
		return fontRenderContext;
	} //}}}

	//}}}

	//{{{ addCustomHighlight() method
	/**
	 * @deprecated Write a <code>TextAreaExtension</code> instead.
	 */
	public void addCustomHighlight(TextAreaHighlight highlight)
	{
		Log.log(Log.WARNING,this,"Old highlighter API not supported: "
			+ highlight);
	} //}}}

	//{{{ removeCustomHighlight() method
	/**
	 * @deprecated Write a <code>TextAreaExtension</code> instead.
	 */
	public void removeCustomHighlight(TextAreaHighlight highlight)
	{
		Log.log(Log.WARNING,this,"Old highlighter API not supported: "
			+ highlight);
	} //}}}

	//{{{ addExtension() method
	/**
	 * Adds a text area extension, which can perform custom painting and
	 * tool tip handling.
	 * @param extension The extension
	 * @since jEdit 4.0pre4
	 */
	public void addExtension(TextAreaExtension extension)
	{
		extensionMgr.addExtension(DEFAULT_LAYER,extension);
		repaint();
	} //}}}

	//{{{ addExtension() method
	/**
	 * Adds a text area extension, which can perform custom painting and
	 * tool tip handling.
	 * @param layer The layer to add the extension to. Note that more than
	 * extension can share the same layer.
	 * @param extension The extension
	 * @since jEdit 4.0pre4
	 */
	public void addExtension(int layer, TextAreaExtension extension)
	{
		extensionMgr.addExtension(layer,extension);
		repaint();
	} //}}}

	//{{{ removeExtension() method
	/**
	 * Removes a text area extension. It will no longer be asked to
	 * perform custom painting and tool tip handling.
	 * @param extension The extension
	 * @since jEdit 4.0pre4
	 */
	public void removeExtension(TextAreaExtension extension)
	{
		extensionMgr.removeExtension(extension);
		repaint();
	} //}}}

	//{{{ getToolTipText() method
	/**
	 * Returns the tool tip to display at the specified location.
	 * @param evt The mouse event
	 */
	public String getToolTipText(MouseEvent evt)
	{
		if(!textArea.getBuffer().isLoaded())
			return null;

		return extensionMgr.getToolTipText(evt.getX(),evt.getY());
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

		Rectangle clipRect = gfx.getClipBounds();

		gfx.setColor(getBackground());
		gfx.fillRect(clipRect.x,clipRect.y,clipRect.width,clipRect.height);

		Buffer buffer = textArea.getBuffer();
		if(!buffer.isLoaded())
			return;

		int x = textArea.getHorizontalOffset();

		int height = fm.getHeight();
		int firstInvalid = clipRect.y / height;
		// Because the clipRect's height is usually an even multiple
		// of the font height, we subtract 1 from it, otherwise one
		// too many lines will always be painted.
		int lastInvalid = (clipRect.y + clipRect.height - 1) / height;

		textArea.chunkCache.updateChunksUpTo(lastInvalid);

		int lineCount = textArea.getVirtualLineCount();

		int y = (clipRect.y - clipRect.y % height);

		try
		{
			boolean updateMaxHorizontalScrollWidth = false;

			for(int line = firstInvalid; line <= lastInvalid; line++)
			{
				ChunkCache.LineInfo lineInfo = textArea.chunkCache
					.getLineInfo(line);
				if(!lineInfo.chunksValid)
					System.err.println("text area painter: not valid");

				lineInfo.width = paintLine(gfx,buffer,lineInfo,line,x,y) - x;
				if(lineInfo.width > textArea.maxHorizontalScrollWidth)
					updateMaxHorizontalScrollWidth = true;

				y += height;
			}

			if(buffer.isNextLineRequested())
			{
				int h = clipRect.y + clipRect.height;
				textArea.chunkCache.invalidateChunksFrom(lastInvalid + 1);
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
		int ntabs = (int)(x / textArea.tabSize);
		return (ntabs + 1) * textArea.tabSize;
	} //}}}

	//{{{ getPreferredSize() method
	/**
	 * Returns the painter's preferred size.
	 */
	public Dimension getPreferredSize()
	{
		Dimension dim = new Dimension();

		char[] foo = new char[80];
		for(int i = 0; i < foo.length; i++)
			foo[i] = ' ';
		dim.width = (int)(getFont().getStringBounds(foo,0,foo.length,
			fontRenderContext).getWidth());
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

	// should try to use this as little as possible.
	private FontMetrics fm;

	private ExtensionManager extensionMgr;

	private RenderingHints renderingHints;
	private FontRenderContext fontRenderContext;

	// used to store offsetToXY() results
	private Point returnValue;
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
	private int paintLine(Graphics2D gfx, Buffer buffer,
		ChunkCache.LineInfo lineInfo, int screenLine,
		int x, int y)
	{
		int physicalLine = lineInfo.physicalLine;

		if(physicalLine == -1)
			extensionMgr.paintInvalidLine(gfx,screenLine,y);
		else
		{
			boolean collapsedFold = (physicalLine < buffer.getLineCount() - 1
				&& buffer.isFoldStart(physicalLine)
				&& !textArea.getFoldVisibilityManager()
				.isLineVisible(physicalLine + 1)
				&& lineInfo.firstSubregion);
			int start = textArea.getScreenLineStartOffset(screenLine);
			int end = textArea.getScreenLineEndOffset(screenLine);

			int caret = textArea.getCaretPosition();
			boolean paintLineHighlight = lineHighlight
				&& caret >= start && caret < end
				&& textArea.selection.size() == 0;

			Color bgColor;

			if(paintLineHighlight)
				bgColor = lineHighlightColor;
			else if(collapsedFold)
				bgColor = foldedLineColor;
			else
				bgColor = getBackground();

			if(paintLineHighlight || collapsedFold)
			{
				gfx.setColor(bgColor);
				gfx.fillRect(0,y,getWidth(),fm.getHeight());
			}

			extensionMgr.paintValidLine(gfx,screenLine,physicalLine,
				start,end,y);

			Font defaultFont = getFont();
			Color defaultColor = getForeground();

			gfx.setFont(defaultFont);
			gfx.setColor(defaultColor);

			float baseLine = y + fm.getHeight()
				- fm.getLeading() - fm.getDescent();

			if(lineInfo.chunks != null)
			{
				x += ChunkCache.paintChunkList(
					lineInfo.chunks,gfx,x,baseLine,
					getWidth(),bgColor,true);
			}

			gfx.setFont(defaultFont);
			gfx.setColor(eolMarkerColor);

			if(!lineInfo.lastSubregion)
			{
				gfx.drawString(":",Math.max(x,textArea.wrapMargin),
					baseLine);
				x += textArea.charWidth;
			}
			else if(collapsedFold)
			{
				int nextLine = textArea.getFoldVisibilityManager()
					.getNextVisibleLine(physicalLine);
				if(nextLine == -1)
					nextLine = buffer.getLineCount();

				int count = nextLine - physicalLine - 1;
				String str = " [" + count + " lines]";
				gfx.drawString(str,x,baseLine);
				x += (int)(getFont().getStringBounds(
					str,fontRenderContext)
					.getWidth());
			}
			else if(eolMarkers)
			{
				gfx.drawString(".",x,baseLine);
				x += textArea.charWidth;
			}

			paintCaret(gfx,physicalLine,start,end,y,bgColor);
		}

		return x;
	} //}}}

	//{{{ paintCaret() method
	private void paintCaret(Graphics2D gfx, int physicalLine,
		int start, int end, int y, Color bgColor)
	{
		if(!textArea.isCaretVisible())
			return;

		int caret = textArea.getCaretPosition();
		if(caret < start || caret >= end)
			return;

		int offset = caret - textArea.getLineStartOffset(physicalLine);
		textArea.offsetToXY(physicalLine,offset,returnValue);
		int caretX = returnValue.x;
		int height = fm.getHeight();

		gfx.setColor(caretColor);

		if(textArea.isOverwriteEnabled())
		{
			gfx.drawLine(caretX,y + height - 1,
				caretX + textArea.charWidth,y + height - 1);
		}
		else if(blockCaret)
		{
			// Workaround for bug in Graphics2D in JDK1.4 under
			// Windows; calling setPaintMode() does not reset
			// graphics mode.
			Graphics2D blockgfx = (Graphics2D)gfx.create();
			blockgfx.setXORMode(bgColor);
			blockgfx.fillRect(caretX,y,textArea.charWidth,height);
			blockgfx.dispose();
		}
		else
		{
			gfx.drawLine(caretX,y,caretX,y + height - 1);
		}
	} //}}}

	//}}}

	//{{{ PaintSelection class
	class PaintSelection extends TextAreaExtension
	{
		//{{{ paintValidLine() method
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(textArea.selection.size() == 0)
				return;

			gfx.setColor(getSelectionColor());
			for(int i = textArea.selection.size() - 1;
				i >= 0; i--)
			{
				paintSelection(gfx,screenLine,
					physicalLine,start,end,y,
					(Selection)textArea.selection
					.get(i));
			}
		} //}}}

		//{{{ paintSelection() method
		private void paintSelection(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y, Selection s)
		{
			if(end <= s.start || start > s.end)
				return;

			int selStartScreenLine = textArea.getScreenLineOfOffset(s.start);
			int selEndScreenLine = textArea.getScreenLineOfOffset(s.end);

			int lineStart = textArea.getLineStartOffset(physicalLine);
			int x1, x2;

			if(s instanceof Selection.Rect)
			{
				int lineLen = textArea.getLineLength(physicalLine);
				x1 = textArea.offsetToXY(physicalLine,Math.min(lineLen,
					s.start - textArea.getLineStartOffset(
					s.startLine)),returnValue).x;
				x2 = textArea.offsetToXY(physicalLine,Math.min(lineLen,
					s.end - textArea.getLineStartOffset(
					s.endLine)),returnValue).x;

				if(x1 > x2)
				{
					int tmp = x2;
					x2 = x1;
					x1 = tmp;
				}
			}
			else if(selStartScreenLine == selEndScreenLine
				&& selStartScreenLine != -1)
			{
				x1 = textArea.offsetToXY(physicalLine,
					s.start - lineStart,returnValue).x;
				x2 = textArea.offsetToXY(physicalLine,
					s.end - lineStart,returnValue).x;
			}
			else if(screenLine == selStartScreenLine)
			{
				x1 = textArea.offsetToXY(physicalLine,
					s.start - lineStart,returnValue).x;
				x2 = getWidth();
			}
			else if(screenLine == selEndScreenLine)
			{
				x1 = 0;
				x2 = textArea.offsetToXY(physicalLine,
					s.end - lineStart,returnValue).x;
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
	} //}}}

	//{{{ WrapGuide class
	class WrapGuide extends TextAreaExtension
	{
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			paintInvalidLine(gfx,screenLine,y);
		}

		public void paintInvalidLine(Graphics2D gfx, int screenLine, int y)
		{
			if(!textArea.wrapToWidth && textArea.wrapMargin != 0
				&& isWrapGuidePainted())
			{
				gfx.setColor(getWrapGuideColor());
				int x = textArea.getHorizontalOffset() + textArea.wrapMargin;
				gfx.drawLine(x,y,x,y + fm.getHeight());
			}
		}

		public String getToolTipText(int x, int y)
		{
			if(!textArea.wrapToWidth && textArea.wrapMargin != 0
				&& isWrapGuidePainted())
			{
				int wrapGuidePos = textArea.wrapMargin
					+ textArea.getHorizontalOffset();
				if(Math.abs(x - wrapGuidePos) < 5)
				{
					return String.valueOf(textArea.getBuffer()
						.getProperty("maxLineLen"));
				}
			}

			return null;
		}
	} //}}}

	//{{{ BracketHighlight class
	class BracketHighlight extends TextAreaExtension
	{
		// try to minimise access$n() methods
		Point returnValue = new Point();

		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(!isBracketHighlightEnabled() || !textArea.isBracketHighlightVisible())
				return;

			int bracketLine = textArea.getBracketLine();
			int bracketOffset = textArea.getBracketPosition();
			if(bracketLine == -1 || bracketOffset == -1)
				return;

			int bracketLineStart = textArea.getLineStartOffset(bracketLine);
			if(bracketOffset + bracketLineStart < start
				|| bracketOffset + bracketLineStart >= end)
				return;

			textArea.offsetToXY(bracketLine,bracketOffset,returnValue);
			gfx.setColor(getBracketHighlightColor());
			// Hack!!! Since there is no fast way to get the character
			// from the bracket matching routine, we use ( since all
			// brackets probably have the same width anyway
			gfx.drawRect(returnValue.x,y,(int)gfx.getFont().getStringBounds(
				"(",getFontRenderContext()).getWidth() - 1,
				fm.getHeight() - 1);
		}
	} //}}}
}
