/*
 * TextAreaPainter.java - Paints the text area
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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
import java.awt.*;
import java.util.HashMap;
import org.gjt.sp.jedit.buffer.IndentFoldHandler;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.OperatingSystem;
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
	public static final int BACKGROUND_LAYER = -60;

	/**
	 * The line highlight and collapsed fold highlight layer.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre7
	 */
	public static final int LINE_BACKGROUND_LAYER = -50;

	/**
	 * Below selection layer.
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
	 * structure highlight.
	 * @since jEdit 4.0pre4
	 */
	public static final int DEFAULT_LAYER = 0;

	/**
	 * Block caret layer. Most extensions will be below this layer.
	 * @since jEdit 4.2pre1
	 */
	public static final int BLOCK_CARET_LAYER = 50;

	/**
	 * Bracket highlight layer. Most extensions will be below this layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int BRACKET_HIGHLIGHT_LAYER = 100;

	/**
	 * Text layer. Most extensions will be below this layer.
	 * @since jEdit 4.2pre1
	 */
	public static final int TEXT_LAYER = 200;

	/**
	 * Caret layer. Most extensions will be below this layer.
	 * @since jEdit 4.2pre1
	 */
	public static final int CARET_LAYER = 300;

	/**
	 * Highest possible layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int HIGHEST_LAYER = Integer.MAX_VALUE;
	//}}}

	//{{{ setBounds() method
	/**
	 * It is a bad idea to override this, but we need to get the component
	 * event before the first repaint.
	 */
	public void setBounds(int x, int y, int width, int height)
	{
		if(x == getX() && y == getY() && width == getWidth()
			&& height == getHeight())
		{
			return;
		}

		super.setBounds(x,y,width,height);

		textArea.recalculateVisibleLines();
		if(textArea.getBuffer().isLoaded())
			textArea.recalculateLastPhysicalLine();
		textArea.propertiesChanged();
		textArea.scrollBarsInitialized = true;
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
		// assumed this is called after a font render context is set up.
		// changing font render context settings without a setStyles()
		// call will not reset cached monospaced font info.
		fonts.clear();

		this.styles = styles;
		styles[Token.NULL] = new SyntaxStyle(getForeground(),null,getFont());
		for(int i = 0; i < styles.length; i++)
		{
			styles[i].setCharWidth(getCharWidth(styles[i].getFont()));
		}
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

	//{{{ getMultipleSelectionColor() method
	/**
	 * Returns the multiple selection color.
	 * @since jEdit 4.2pre1
	 */
	public final Color getMultipleSelectionColor()
	{
		return multipleSelectionColor;
	} //}}}

	//{{{ setMultipleSelectionColor() method
	/**
	 * Sets the multiple selection color.
	 * @param multipleSelectionColor The multiple selection color
	 * @since jEdit 4.2pre1
	 */
	public final void setMultipleSelectionColor(Color multipleSelectionColor)
	{
		this.multipleSelectionColor = multipleSelectionColor;
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

	//{{{ getStructureHighlightColor() method
	/**
	 * Returns the structure highlight color.
	 * @since jEdit 4.2pre3
	 */
	public final Color getStructureHighlightColor()
	{
		return structureHighlightColor;
	} //}}}

	//{{{ setStructureHighlightColor() method
	/**
	 * Sets the structure highlight color.
	 * @param structureHighlightColor The bracket highlight color
	 * @since jEdit 4.2pre3
	 */
	public final void setStructureHighlightColor(
		Color structureHighlightColor)
	{
		this.structureHighlightColor = structureHighlightColor;
		StructureMatcher.Match match = textArea.getStructureMatch();
		if(match != null)
		{
			textArea.invalidateLineRange(
				match.startLine,match.endLine
			);
		}
	} //}}}

	//{{{ isStructureHighlightEnabled() method
	/**
	 * Returns true if structure highlighting is enabled, false otherwise.
	 * @since jEdit 4.2pre3
	 */
	public final boolean isStructureHighlightEnabled()
	{
		return structureHighlight;
	} //}}}

	//{{{ setStructureHighlightEnabled() method
	/**
	 * Enables or disables structure highlighting.
	 * @param structureHighlight True if structure highlighting should be
	 * enabled, false otherwise
	 * @since jEdit 4.2pre3
	 */
	public final void setStructureHighlightEnabled(boolean structureHighlight)
	{
		this.structureHighlight = structureHighlight;
		StructureMatcher.Match match = textArea.getStructureMatch();
		if(match != null)
		{
			textArea.invalidateLineRange(
				match.startLine,
				match.endLine
			);
		}
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
		extensionMgr.removeExtension(caretExtension);
		if(blockCaret)
			addExtension(BLOCK_CARET_LAYER,caretExtension);
		else
			addExtension(CARET_LAYER,caretExtension);
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

	//{{{ getFoldLineStyle() method
	/**
	 * Returns the fold line style. The first element is the style for
	 * lines with a fold level greater than 3. The remaining elements
	 * are for fold levels 1 to 3.
	 */
	public final SyntaxStyle[] getFoldLineStyle()
	{
		return foldLineStyle;
	} //}}}

	//{{{ setFoldLineStyle() method
	/**
	 * Sets the fold line style. The first element is the style for
	 * lines with a fold level greater than 3. The remaining elements
	 * are for fold levels 1 to 3.
	 * @param foldLineStyle The fold line style
	 */
	public final void setFoldLineStyle(SyntaxStyle[] foldLineStyle)
	{
		this.foldLineStyle = foldLineStyle;
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

	//{{{ getExtensions() method
	/**
	 * Returns an array of registered text area extensions. Useful for
	 * debugging purposes.
	 * @since jEdit 4.1pre5
	 */
	public TextAreaExtension[] getExtensions()
	{
		return extensionMgr.getExtensions();
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
		textArea.propertiesChanged();
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

		if(Debug.PAINT_TIMER && lastInvalid - firstInvalid >= 1)
			Log.log(Log.DEBUG,this,"repainting " + (lastInvalid - firstInvalid) + " lines");

		int y = (clipRect.y - clipRect.y % height);

		textArea.updateMaxHorizontalScrollWidth = false;

		extensionMgr.paintScreenLineRange(textArea,gfx,
			firstInvalid,lastInvalid,y,height);

		if(textArea.updateMaxHorizontalScrollWidth)
			textArea.updateMaxHorizontalScrollWidth();

		textArea.displayManager._notifyScreenLineChanges();
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

	//{{{ Package-private members

	//{{{ Instance variables
	/* package-private since they are accessed by inner classes and we
	 * want this to be fast */
	JEditTextArea textArea;

	SyntaxStyle[] styles;
	Color caretColor;
	Color selectionColor;
	Color multipleSelectionColor;
	Color lineHighlightColor;
	Color structureHighlightColor;
	Color eolMarkerColor;
	Color wrapGuideColor;

	SyntaxStyle[] foldLineStyle;

	boolean blockCaret;
	boolean lineHighlight;
	boolean structureHighlight;
	boolean eolMarkers;
	boolean wrapGuide;
	boolean antiAlias;
	boolean fracFontMetrics;

	// should try to use this as little as possible.
	FontMetrics fm;
	//}}}

	//{{{ TextAreaPainter constructor
	/**
	 * Creates a new painter. Do not create instances of this class
	 * directly.
	 */
	TextAreaPainter(JEditTextArea textArea)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK
			| AWTEvent.KEY_EVENT_MASK
			| AWTEvent.MOUSE_EVENT_MASK);

		this.textArea = textArea;

		fonts = new HashMap();
		extensionMgr = new ExtensionManager();

		setAutoscrolls(true);
		setOpaque(true);
		setRequestFocusEnabled(false);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		fontRenderContext = new FontRenderContext(null,false,false);

		addExtension(LINE_BACKGROUND_LAYER,new PaintLineBackground());
		addExtension(SELECTION_LAYER,new PaintSelection());
		addExtension(WRAP_GUIDE_LAYER,new PaintWrapGuide());
		addExtension(BRACKET_HIGHLIGHT_LAYER,new StructureMatcher
			.Highlight(textArea));
		addExtension(TEXT_LAYER,new PaintText());
		caretExtension = new PaintCaret();
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private ExtensionManager extensionMgr;
	private PaintCaret caretExtension;
	private RenderingHints renderingHints;
	private FontRenderContext fontRenderContext;
	private HashMap fonts;
	//}}}

	//{{{ updateRenderingHints() method
	private void updateRenderingHints()
	{
		HashMap hints = new HashMap();

		if(antiAlias)
		{
			//hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		else
		{
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		}

		hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
			fracFontMetrics ?
				RenderingHints.VALUE_FRACTIONALMETRICS_ON
				: RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		renderingHints = new RenderingHints(hints);
		fontRenderContext = new FontRenderContext(null,antiAlias,
			fracFontMetrics);
	} //}}}

	//{{{ getCharWidth() method
	private int getCharWidth(Font font)
	{
		Integer returnValue = (Integer)fonts.get(font);
		if(returnValue == null)
		{
			int minWidth = Integer.MAX_VALUE;
			int maxWidth = Integer.MIN_VALUE;
			FontMetrics fm = getFontMetrics(font);
			int[] widths = fm.getWidths();
			for(int i = 0; i < widths.length; i++)
			{
				int width = widths[i];
				if(width == 0 || !font.canDisplay((char)i))
					continue;
				minWidth = Math.min(width,minWidth);
				maxWidth = Math.max(width,maxWidth);
			}

			String str = "iwiwiwiau1234";
			double width1 = font.createGlyphVector(textArea.getPainter()
				.getFontRenderContext(),str).getLogicalBounds()
				.getWidth();
			double width2 = str.length() * maxWidth;
			if(minWidth == maxWidth
				&& width1 == width2)
			{
				Log.log(Log.DEBUG,this,"Using monospaced font optimization: " + font);
				returnValue = new Integer(maxWidth);
			}
			else
			{
				Log.log(Log.DEBUG,this,"Not using monospaced font optimization: " + font);
				Log.log(Log.DEBUG,this,"Minimum width = " + minWidth
					+ ", maximum width = " + maxWidth);
				returnValue = new Integer(0);
			}

			fonts.put(font,returnValue);
		}
		return returnValue.intValue();
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ PaintLineBackground class
	class PaintLineBackground extends TextAreaExtension
	{
		//{{{ paintValidLine() method
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			// minimise access$ methods
			JEditTextArea textArea = TextAreaPainter.this.textArea;
			Buffer buffer = textArea.getBuffer();

			//{{{ Paint line highlight and collapsed fold highlight
			boolean collapsedFold =
				(physicalLine < buffer.getLineCount() - 1
				&& buffer.isFoldStart(physicalLine)
				&& !textArea.displayManager
				.isLineVisible(physicalLine + 1));

			SyntaxStyle foldLineStyle = null;
			if(collapsedFold)
			{
				int level = buffer.getFoldLevel(physicalLine + 1);
				if(buffer.getFoldHandler() instanceof IndentFoldHandler)
					level = Math.max(1,level / buffer.getIndentSize());
				if(level > 3)
					level = 0;
				foldLineStyle = TextAreaPainter.this.foldLineStyle[level];
			}

			int caret = textArea.getCaretPosition();
			boolean paintLineHighlight = isLineHighlightEnabled()
				&& caret >= start && caret < end
				&& textArea.selection.size() == 0;

			Color bgColor;
			if(paintLineHighlight)
				bgColor = lineHighlightColor;
			else if(collapsedFold)
			{
				bgColor = foldLineStyle.getBackgroundColor();
				if(bgColor == null)
					bgColor = getBackground();
			}
			else
				bgColor = getBackground();

			if(paintLineHighlight || collapsedFold)
			{
				gfx.setColor(bgColor);
				gfx.fillRect(0,y,getWidth(),fm.getHeight());
			} //}}}

			//{{{ Paint token backgrounds
			ChunkCache.LineInfo lineInfo = textArea.chunkCache
				.getLineInfo(screenLine);

			if(lineInfo.chunks != null)
			{
				float baseLine = y + fm.getHeight()
					- fm.getLeading() - fm.getDescent();
				Chunk.paintChunkBackgrounds(
					lineInfo.chunks,gfx,
					textArea.getHorizontalOffset(),
					baseLine);
			} //}}}
		} //}}}
	} //}}}

	//{{{ PaintSelection class
	class PaintSelection extends TextAreaExtension
	{
		//{{{ paintValidLine() method
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(textArea.selection.size() == 0)
				return;

			gfx.setColor(textArea.isMultipleSelectionEnabled()
				? getMultipleSelectionColor()
				: getSelectionColor());
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

			Buffer buffer = textArea.getBuffer();

			int lineStart = buffer.getLineStartOffset(physicalLine);

			int x1, x2;

			if(s instanceof Selection.Rect)
			{
				start -= lineStart;
				end -= lineStart;

				Selection.Rect rect = (Selection.Rect)s;
				int _start = rect.getStartColumn(buffer);
				int _end = rect.getEndColumn(buffer);

				int lineLen = buffer.getLineLength(physicalLine);

				int[] total = new int[1];

				int rectStart = buffer.getOffsetOfVirtualColumn(
					physicalLine,_start,total);
				if(rectStart == -1)
				{
					x1 = (_start - total[0]) * textArea.charWidth;
					rectStart = lineLen;
				}
				else
					x1 = 0;

				int rectEnd = buffer.getOffsetOfVirtualColumn(
					physicalLine,_end,total);
				if(rectEnd == -1)
				{
					x2 = (_end - total[0]) * textArea.charWidth;
					rectEnd = lineLen;
				}
				else
					x2 = 0;

				if(end <= rectStart || start > rectEnd)
					return;

				x1 = (rectStart < start ? 0
					: x1 + textArea.offsetToXY(physicalLine,rectStart,textArea.returnValue).x);
				x2 = (rectEnd > end ? getWidth()
					: x2 + textArea.offsetToXY(physicalLine,rectEnd,textArea.returnValue).x);
			}
			else if(selStartScreenLine == selEndScreenLine
				&& selStartScreenLine != -1)
			{
				x1 = textArea.offsetToXY(physicalLine,
					s.start - lineStart,textArea.returnValue).x;
				x2 = textArea.offsetToXY(physicalLine,
					s.end - lineStart,textArea.returnValue).x;
			}
			else if(screenLine == selStartScreenLine)
			{
				x1 = textArea.offsetToXY(physicalLine,
					s.start - lineStart,textArea.returnValue).x;
				x2 = getWidth();
			}
			else if(screenLine == selEndScreenLine)
			{
				x1 = 0;
				x2 = textArea.offsetToXY(physicalLine,
					s.end - lineStart,textArea.returnValue).x;
			}
			else
			{
				x1 = 0;
				x2 = getWidth();
			}

			if(x1 < 0)
				x1 = 0;
			if(x2 < 0)
				x2 = 0;

			if(x1 == x2)
				x2++;

			gfx.fillRect(x1,y,x2 - x1,fm.getHeight());
		} //}}}
	} //}}}

	//{{{ PaintWrapGuide class
	class PaintWrapGuide extends TextAreaExtension
	{
		public void paintScreenLineRange(Graphics2D gfx, int firstLine,
			int lastLine, int[] physicalLines, int[] start,
			int[] end, int y, int lineHeight)
		{
			if(textArea.getDisplayManager().wrapMargin != 0
				&& isWrapGuidePainted())
			{
				gfx.setColor(getWrapGuideColor());
				int x = textArea.getHorizontalOffset()
					+ textArea.getDisplayManager()
					.wrapMargin;
				gfx.drawLine(x,y,x,y + (lastLine - firstLine
					+ 1) * lineHeight);
			}
		}

		public String getToolTipText(int x, int y)
		{
			if(textArea.getDisplayManager().wrapMargin != 0 && isWrapGuidePainted())
			{
				int wrapGuidePos = textArea.getDisplayManager().wrapMargin
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

	//{{{ PaintText class
	class PaintText extends TextAreaExtension
	{
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			ChunkCache.LineInfo lineInfo = textArea.chunkCache
				.getLineInfo(screenLine);

			Font defaultFont = getFont();
			Color defaultColor = getForeground();

			gfx.setFont(defaultFont);
			gfx.setColor(defaultColor);

			int x = textArea.getHorizontalOffset();
			int originalX = x;

			float baseLine = y + fm.getHeight()
				- fm.getLeading() - fm.getDescent();

			if(lineInfo.chunks != null)
			{
				x += Chunk.paintChunkList(lineInfo.chunks,
					gfx,textArea.getHorizontalOffset(),
					baseLine,!Debug.DISABLE_GLYPH_VECTOR);
			}

			Buffer buffer = textArea.getBuffer();

			if(!lineInfo.lastSubregion)
			{
				gfx.setFont(defaultFont);
				gfx.setColor(eolMarkerColor);
				gfx.drawString(":",Math.max(x,
					textArea.getHorizontalOffset()
					+ textArea.getDisplayManager().wrapMargin + textArea.charWidth),
					baseLine);
				x += textArea.charWidth;
			}
			else if(physicalLine < buffer.getLineCount() - 1
				&& buffer.isFoldStart(physicalLine)
				&& !textArea.displayManager
				.isLineVisible(physicalLine + 1))
			{
				int level = buffer.getFoldLevel(physicalLine + 1);
				if(buffer.getFoldHandler() instanceof IndentFoldHandler)
					level = Math.max(1,level / buffer.getIndentSize());
				if(level > 3)
					level = 0;
				SyntaxStyle foldLineStyle = TextAreaPainter.this.foldLineStyle[level];

				Font font = foldLineStyle.getFont();
				gfx.setFont(font);
				gfx.setColor(foldLineStyle.getForegroundColor());

				int nextLine;
				int nextScreenLine = screenLine + 1;
				if(nextScreenLine < textArea.getVisibleLines())
				{
					nextLine = textArea.chunkCache.getLineInfo(nextScreenLine)
						.physicalLine;
				}
				else
				{
					nextLine = textArea.displayManager
						.getNextVisibleLine(physicalLine);
				}

				if(nextLine == -1)
					nextLine = textArea.getLineCount();

				int count = nextLine - physicalLine - 1;
				String str = " [" + count + " lines]";

				float width = (float)font.getStringBounds(
					str,fontRenderContext).getWidth();

				gfx.drawString(str,x,baseLine);
				x += width;
			}
			else if(eolMarkers)
			{
				gfx.setFont(defaultFont);
				gfx.setColor(eolMarkerColor);
				gfx.drawString(".",x,baseLine);
				x += textArea.charWidth;
			}

			lineInfo.width = (x - originalX);
			if(lineInfo.width > textArea.maxHorizontalScrollWidth)
				textArea.updateMaxHorizontalScrollWidth = true;
		}
	} //}}}

	//{{{ PaintCaret class
	class PaintCaret extends TextAreaExtension
	{
		public void paintValidLine(Graphics2D gfx, int screenLine,
			int physicalLine, int start, int end, int y)
		{
			if(!textArea.isCaretVisible())
				return;

			int caret = textArea.getCaretPosition();
			if(caret < start || caret >= end)
				return;

			int offset = caret - textArea.getLineStartOffset(physicalLine);
			textArea.offsetToXY(physicalLine,offset,textArea.returnValue);
			int caretX = textArea.returnValue.x;
			int height = fm.getHeight();

			gfx.setColor(caretColor);

			if(textArea.isOverwriteEnabled())
			{
				gfx.drawLine(caretX,y + height - 1,
					caretX + textArea.charWidth,y + height - 1);
			}
			else if(blockCaret)
				gfx.drawRect(caretX,y,textArea.charWidth - 1,height - 1);
			else
				gfx.drawLine(caretX,y,caretX,y + height - 1);
		}
	} //}}}

	//}}}
}
