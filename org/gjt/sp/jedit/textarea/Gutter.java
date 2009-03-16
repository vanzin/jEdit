/*
 * Gutter.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2000 mike dillon
 * Portions copyright (C) 2001, 2002 Slava Pestov
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.Log;
//}}}

/**
 * The gutter is the component that displays folding indicators and line
 * numbers to the left of the text area. The only methods in this class
 * that should be called by plugins are those for adding and removing
 * text area extensions.
 *
 * @see #addExtension(TextAreaExtension)
 * @see #addExtension(int,TextAreaExtension)
 * @see #removeExtension(TextAreaExtension)
 * @see TextAreaExtension
 * @see TextArea
 *
 * @author Mike Dillon and Slava Pestov
 * @version $Id$
 */
public class Gutter extends JComponent implements SwingConstants
{
	//{{{ Layers
	/**
	 * The lowest possible layer.
	 * @see #addExtension(int,TextAreaExtension)
	 * @since jEdit 4.0pre4
	 */
	public static final int LOWEST_LAYER = Integer.MIN_VALUE;

	/**
	 * Default extension layer. This is above the wrap guide but below the
	 * bracket highlight.
	 * @since jEdit 4.0pre4
	 */
	public static final int DEFAULT_LAYER = 0;

	/**
	 * Highest possible layer.
	 * @since jEdit 4.0pre4
	 */
	public static final int HIGHEST_LAYER = Integer.MAX_VALUE;
	//}}}

	//{{{ Fold painters
	/**
	 * Fold painter service.
	 * @since jEdit 4.3pre16
	 */
	public static final String FOLD_PAINTER_PROPERTY = "foldPainter";
	public static final String FOLD_PAINTER_SERVICE = "org.gjt.sp.jedit.textarea.FoldPainter";
	public static final String DEFAULT_FOLD_PAINTER_SERVICE = "Triangle";

	//{{{ setFolderPainter() method
	public void setFoldPainter(FoldPainter painter)
	{
		if (painter == null)
			foldPainter = new TriangleFoldPainter();
		else
			foldPainter = painter;
	}
	//}}}
	
	//}}} Fold painters
	
	//{{{ Gutter constructor
	public Gutter(TextArea textArea)
	{
		this.textArea = textArea;

		setAutoscrolls(true);
		setOpaque(true);
		setRequestFocusEnabled(false);

		extensionMgr = new ExtensionManager();

		mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);

		updateBorder();
		setFoldPainter(textArea.getFoldPainter());
	} //}}}

	//{{{ paintComponent() method
	public void paintComponent(Graphics _gfx)
	{
		Graphics2D gfx = (Graphics2D)_gfx;
		gfx.setRenderingHints(textArea.getPainter().renderingHints);
		// fill the background
		Rectangle clip = gfx.getClipBounds();
		gfx.setColor(getBackground());
		gfx.fillRect(clip.x, clip.y, clip.width, clip.height);

		// if buffer is loading, don't paint anything
		if (textArea.getBuffer().isLoading())
			return;

		int lineHeight = textArea.getPainter().getFontMetrics()
			.getHeight();

		if(lineHeight == 0)
			return;

		int firstLine = clip.y / lineHeight;
		int lastLine = (clip.y + clip.height - 1) / lineHeight;

		if(lastLine - firstLine > textArea.getVisibleLines())
		{
			Log.log(Log.ERROR,this,"BUG: firstLine=" + firstLine);
			Log.log(Log.ERROR,this,"     lastLine=" + lastLine);
			Log.log(Log.ERROR,this,"     visibleLines=" + textArea.getVisibleLines());
			Log.log(Log.ERROR,this,"     height=" + getHeight());
			Log.log(Log.ERROR,this,"     painter.height=" + textArea.getPainter().getHeight());
			Log.log(Log.ERROR,this,"     clip.y=" + clip.y);
			Log.log(Log.ERROR,this,"     clip.height=" + clip.height);
			Log.log(Log.ERROR,this,"     lineHeight=" + lineHeight);
		}
	
		int y = clip.y - clip.y % lineHeight;

		extensionMgr.paintScreenLineRange(textArea,gfx,
			firstLine,lastLine,y,lineHeight);

		for (int line = firstLine; line <= lastLine;
			line++, y += lineHeight)
		{
			paintLine(gfx,line,y);
		}
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
		if(textArea.getBuffer().isLoading())
			return null;

		return extensionMgr.getToolTipText(evt.getX(),evt.getY());
	} //}}}

	//{{{ setBorder() method
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
		borderWidth = width;

		focusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color1));
		noFocusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color2));
		updateBorder();
	} //}}}

	//{{{ updateBorder() method
	/**
	 * Sets the border differently if the text area has focus or not.
	 */
	public void updateBorder()
	{
		if (textArea.hasFocus())
			setBorder(focusBorder);
		else
			setBorder(noFocusBorder);
	} //}}}

	//{{{ setBorder() method
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

		revalidate();
	} //}}}

	//{{{ setFont() method
	/*
	 * JComponent.setFont(Font) is overridden here to cache the font
	 * metrics for the font. This avoids having to get the font metrics
	 * during every repaint.
	 */
	public void setFont(Font font)
	{
		super.setFont(font);

		fm = getFontMetrics(font);

		Border border = getBorder();
		if(border != null)
		{
			gutterSize.width = FOLD_MARKER_SIZE
				+ border.getBorderInsets(this).right
				+ fm.stringWidth("12345");
			revalidate();
		}
	} //}}}

	//{{{ Getters and setters

	//{{{ getHighlightedForeground() method
	/**
	 * Get the foreground color for highlighted line numbers
	 * @return The highlight color
	 */
	public Color getHighlightedForeground()
	{
		return intervalHighlight;
	} //}}}

	//{{{ setHighlightedForeground() method
	public void setHighlightedForeground(Color highlight)
	{
		intervalHighlight = highlight;
	} //}}}

	//{{{ getCurrentLineForeground() method
	public Color getCurrentLineForeground()
 	{
		return currentLineHighlight;
	} //}}}

	//{{{ setCurrentLineForeground() method
	public void setCurrentLineForeground(Color highlight)
	{
		currentLineHighlight = highlight;
 	} //}}}

	//{{{ getFoldColor() method
	public Color getFoldColor()
 	{
		return foldColor;
	} //}}}

	//{{{ setFoldColor() method
	public void setFoldColor(Color foldColor)
	{
		this.foldColor = foldColor;
 	} //}}}

	//{{{ getPreferredSize() method
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
	} //}}}

	//{{{ getMinimumSize() method
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	} //}}}

	//{{{ getLineNumberAlignment() method
	/**
	 * Identifies whether the horizontal alignment of the line numbers.
	 * @return Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public int getLineNumberAlignment()
	{
		return alignment;
	} //}}}

	//{{{ setLineNumberAlignment() method
	/**
	 * Sets the horizontal alignment of the line numbers.
	 * @param alignment Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public void setLineNumberAlignment(int alignment)
	{
		if (this.alignment == alignment) return;

		this.alignment = alignment;

		repaint();
	} //}}}

	//{{{ isExpanded() method
	/**
	 * Identifies whether the gutter is collapsed or expanded.
	 * @return true if the gutter is expanded, false if it is collapsed
	 */
	public boolean isExpanded()
	{
		return expanded;
	} //}}}

	//{{{ setExpanded() method
	/**
	 * Sets whether the gutter is collapsed or expanded and force the text
	 * area to update its layout if there is a change.
	 * @param expanded true if the gutter is expanded,
	 *                   false if it is collapsed
	 */
	public void setExpanded(boolean expanded)
	{
		if (this.expanded == expanded) return;

		this.expanded = expanded;

		textArea.revalidate();
	} //}}}

	//{{{ toggleExpanded() method
	/**
	 * Toggles whether the gutter is collapsed or expanded.
	 */
	public void toggleExpanded()
	{
		setExpanded(!expanded);
	} //}}}

	//{{{ getHighlightInterval() method
	/**
	 * Sets the number of lines between highlighted line numbers.
	 * @return The number of lines between highlighted line numbers or
	 *          zero if highlighting is disabled
	 */
	public int getHighlightInterval()
	{
		return interval;
	} //}}}

	//{{{ setHighlightInterval() method
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
	} //}}}

	//{{{ isCurrentLineHighlightEnabled() method
	public boolean isCurrentLineHighlightEnabled()
	{
		return currentLineHighlightEnabled;
	} //}}}

	//{{{ setCurrentLineHighlightEnabled() method
	public void setCurrentLineHighlightEnabled(boolean enabled)
	{
		if (currentLineHighlightEnabled == enabled) return;

		currentLineHighlightEnabled = enabled;

		repaint();
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
	 * @param structureHighlightColor The structure highlight color
	 * @since jEdit 4.2pre3
	 */
	public final void setStructureHighlightColor(Color structureHighlightColor)
	{
		this.structureHighlightColor = structureHighlightColor;
		repaint();
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
		repaint();
	} //}}}

	public void setMouseActionsProvider(MouseActionsProvider mouseActionsProvider)
	{
		mouseHandler.mouseActions = mouseActionsProvider;
	}
	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private static final int FOLD_MARKER_SIZE = 12;

	private final TextArea textArea;
	private MouseHandler mouseHandler;
	private ExtensionManager extensionMgr;

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

	private boolean structureHighlight;
	private Color structureHighlightColor;

	private int borderWidth;
	private Border focusBorder, noFocusBorder;
	
	private FoldPainter foldPainter;
	//}}}

	//{{{ paintLine() method
	private void paintLine(Graphics2D gfx, int line, int y)
	{
		JEditBuffer buffer = textArea.getBuffer();
		if(buffer.isLoading())
			return;

		FontMetrics textAreaFm = textArea.getPainter().getFontMetrics();
		int lineHeight = textAreaFm.getHeight();
		int baseline = textAreaFm.getAscent();

		ChunkCache.LineInfo info = textArea.chunkCache.getLineInfo(line);
		int physicalLine = info.physicalLine;

		// Skip lines beyond EOF
		if(physicalLine == -1)
			return;

		boolean drawFoldMiddle = true;
		//{{{ Paint fold start and end indicators
		if(info.firstSubregion && buffer.isFoldStart(physicalLine))
		{
			drawFoldMiddle = false;
			foldPainter.paintFoldStart(this, gfx, line, physicalLine,
					textArea.displayManager.isLineVisible(physicalLine+1),
					y, lineHeight, buffer);
		}
		else if(info.lastSubregion && buffer.isFoldEnd(physicalLine))
		{
			drawFoldMiddle = false;
			foldPainter.paintFoldEnd(this, gfx, line, physicalLine, y,
					lineHeight, buffer);
		} //}}}
		//{{{ Paint bracket scope
		else if(structureHighlight)
		{
			StructureMatcher.Match match = textArea.getStructureMatch();
			int caretLine = textArea.getCaretLine();

			if(textArea.isStructureHighlightVisible()
				&& physicalLine >= Math.min(caretLine,match.startLine)
				&& physicalLine <= Math.max(caretLine,match.startLine))
			{
				int caretScreenLine;
				if(caretLine > textArea.getLastPhysicalLine())
					caretScreenLine = Integer.MAX_VALUE;
				else if(textArea.displayManager.isLineVisible(
						textArea.getCaretLine()))
				{
					caretScreenLine = textArea
						.getScreenLineOfOffset(
						textArea.getCaretPosition());
				}
				else
				{
					caretScreenLine = -1;
				}

				int structScreenLine;
				if(match.startLine > textArea.getLastPhysicalLine())
					structScreenLine = Integer.MAX_VALUE;
				else if(textArea.displayManager.isLineVisible(
						match.startLine))
				{
					structScreenLine = textArea
						.getScreenLineOfOffset(
						match.start);
				}
				else
				{
					structScreenLine = -1;
				}

				if(caretScreenLine > structScreenLine)
				{
					int tmp = caretScreenLine;
					caretScreenLine = structScreenLine;
					structScreenLine = tmp;
				}

				gfx.setColor(structureHighlightColor);
				drawFoldMiddle = false;
				if(structScreenLine == caretScreenLine)
				{
					// do nothing
					drawFoldMiddle = true;
				}
				// draw |^
				else if(line == caretScreenLine)
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
				// draw |_
				else if(line == structScreenLine)
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
				// draw |
				else if(line > caretScreenLine
					&& line < structScreenLine)
				{
					gfx.fillRect(5,
						y,
						2,
						lineHeight);
				}
			}
		} //}}}
		if(drawFoldMiddle && buffer.getFoldLevel(physicalLine) > 0)
		{
			foldPainter.paintFoldMiddle(this, gfx, line, physicalLine,
					y, lineHeight, buffer);
		}

		//{{{ Paint line numbers
		if(info.firstSubregion && expanded)
		{
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

			if (physicalLine == textArea.getCaretLine() && currentLineHighlightEnabled)
			{
				gfx.setColor(currentLineHighlight);
			}
			else if (interval > 1 && (physicalLine + 1) % interval == 0)
				gfx.setColor(intervalHighlight);
			else
				gfx.setColor(getForeground());

			gfx.drawString(number, FOLD_MARKER_SIZE + offset,
				baseline + y);
		} //}}}
	} //}}}

	//}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseInputAdapter
	{
		MouseActionsProvider mouseActions;
		boolean drag;
		int toolTipInitialDelay, toolTipReshowDelay;

		//{{{ mouseEntered() method
		public void mouseEntered(MouseEvent e)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			toolTipInitialDelay = ttm.getInitialDelay();
			toolTipReshowDelay = ttm.getReshowDelay();
			ttm.setInitialDelay(0);
			ttm.setReshowDelay(0);
		} //}}}

		//{{{ mouseExited() method
		public void mouseExited(MouseEvent evt)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			ttm.setInitialDelay(toolTipInitialDelay);
			ttm.setReshowDelay(toolTipReshowDelay);
		} //}}}

		//{{{ mousePressed() method
		public void mousePressed(MouseEvent e)
		{
			textArea.requestFocus();

			if(TextAreaMouseHandler.isPopupTrigger(e)
				|| e.getX() >= getWidth() - borderWidth * 2)
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mousePressed(e);
				drag = true;
			}
			else
			{
				JEditBuffer buffer = textArea.getBuffer();

				int screenLine = e.getY() / textArea.getPainter()
					.getFontMetrics().getHeight();

				int line = textArea.chunkCache.getLineInfo(screenLine)
					.physicalLine;

				if(line == -1)
					return;

				//{{{ Determine action
				String defaultAction;
				String variant;
				if(buffer.isFoldStart(line))
				{
					defaultAction = "toggle-fold";
					variant = "fold";
				}
				else if(structureHighlight
					&& textArea.isStructureHighlightVisible()
					&& textArea.lineInStructureScope(line))
				{
					defaultAction = "match-struct";
					variant = "struct";
				}
				else
					return;

				String action = null;

				if (mouseActions != null)
					action = mouseActions.getActionForEvent(
						e,variant);

				if(action == null)
					action = defaultAction;
				//}}}

				//{{{ Handle actions
				StructureMatcher.Match match = textArea
					.getStructureMatch();

				if(action.equals("select-fold"))
				{
					textArea.displayManager.expandFold(line,true);
					textArea.selectFold(line);
				}
				else if(action.equals("narrow-fold"))
				{
					int[] lines = buffer.getFoldAtLine(line);
					textArea.displayManager.narrow(lines[0],lines[1]);
				}
				else if(action.startsWith("toggle-fold"))
				{
					if(textArea.displayManager
						.isLineVisible(line + 1))
					{
						textArea.collapseFold(line);
					}
					else
					{
						if(action.endsWith("-fully"))
						{
							textArea.displayManager
								.expandFold(line,
								true);
						}
						else
						{
							textArea.displayManager
								.expandFold(line,
								false);
						}
					}
				}
				else if(action.equals("match-struct"))
				{
					if(match != null)
						textArea.setCaretPosition(match.end);
				}
				else if(action.equals("select-struct"))
				{
					if(match != null)
					{
						match.matcher.selectMatch(
							textArea);
					}
				}
				else if(action.equals("narrow-struct"))
				{
					if(match != null)
					{
						int start = Math.min(
							match.startLine,
							textArea.getCaretLine());
						int end = Math.max(
							match.endLine,
							textArea.getCaretLine());
						textArea.displayManager.narrow(start,end);
					}
				} //}}}
			}
		} //}}}

		//{{{ mouseDragged() method
		public void mouseDragged(MouseEvent e)
		{
			if(drag /* && e.getX() >= getWidth() - borderWidth * 2 */)
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mouseDragged(e);
			}
		} //}}}

		//{{{ mouseReleased() method
		public void mouseReleased(MouseEvent e)
		{
			if(drag && e.getX() >= getWidth() - borderWidth * 2)
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mouseReleased(e);
			}

			drag = false;
		} //}}}
	} //}}}
}
