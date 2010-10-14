/*
 * ColumnBlock.java 
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2010 Anshal Shukla
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

import java.util.Vector;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.Selection.Rect;

public class ColumnBlock extends Rect implements Node
{
	private Node parent;

	private Vector<Node> children = new Vector<Node>();

	private Vector<ColumnBlockLine> lines = new Vector<ColumnBlockLine>();

	float columnBlockWidth;

	private boolean tabSizesDirty = true;

	private JEditBuffer buffer;

	private boolean isDirty = false;

	@Override
	//{{{ addChild() method
	public void addChild(Node node)
	{
		// must add the children in sorted order
		ColumnBlock block = (ColumnBlock) node;
		ColumnBlock blockBelow = searchChildren(block.startLine);
		int index = -1;
		if (blockBelow != null)
		{
			if (blockBelow.isLineWithinThisBlock(block.endLine) >= 0)
			{
				throw new IllegalArgumentException("Overlapping column blocks: "
					+ block + " \n&\n" + blockBelow);
			}
			index = children.indexOf(blockBelow);
			children.add(index, node);
		}
		else
		{
			children.add(node);
		}
	}//}}}

	@Override
	//{{{ getChildren() method
	public Vector getChildren()
	{
		return children;
	}//}}}

	@Override
	//{{{ getParent() method
	public Node getParent()
	{
		return parent;
	}//}}}

	//{{{ setWidth() method
	public void setWidth(int width)
	{
		columnBlockWidth = width;
	}//}}}

	//{{{ setParent() method
	public void setParent(Node parent)
	{
		this.parent = parent;
	}//}}}

	//{{{ setLines() method
	public void setLines(Vector<ColumnBlockLine> lines)
	{
		this.lines = lines;
	}//}}}

	//{{{ getLines() method
	public Vector<ColumnBlockLine> getLines()
	{
		return lines;
	}//}}}

	//{{{ ColumnBlock() method
	public ColumnBlock()
	{

	}//}}}

	//{{{  ColumnBlock() method
	public ColumnBlock(JEditBuffer buffer, int startLine, int startColumn, int endLine,
		int endColumn)
	{
		super(buffer, startLine, startColumn, endLine, endColumn);
		this.buffer = buffer;
	}//}}}

	//{{{  ColumnBlock() method
	public ColumnBlock(JEditBuffer buffer, int startLine, int endLine)
	{
		this.startLine = startLine;
		this.endLine = endLine;
		this.buffer = buffer;
	}//}}}

	//{{{  getStartLine() method
	public int getStartLine()
	{
		return startLine;
	}//}}}

	//{{{  getEndLine() method
	public int getEndLine()
	{
		return endLine;
	}//}}}

	//{{{   getColumnWidth() method
	public int getColumnWidth()
	{
		return (int) columnBlockWidth;
	}//}}}

	//{{{  isLineWithinThisBlock() method
	public int isLineWithinThisBlock(int line)
	{
		if (line < startLine)
		{
			return line - startLine;
		}
		else if (line > endLine)
		{
			return line - endLine;
		}
		else
		{
			return 0;
		}
	}//}}}

	//{{{ getContainingBlock() method
	public ColumnBlock getContainingBlock(int line, int offset)
	{
		ColumnBlock retBlock = null;
		int relativeOffset = -1;
		if ((line >= startLine) && (line <= endLine))
		{
			relativeOffset = offset - buffer.getLineStartOffset(line);
			if ((lines != null) && (lines.size() > 0))
			{
				ColumnBlockLine blockLine = (ColumnBlockLine) (lines.get(line
					- startLine));
				if ((blockLine.getColumnEndIndex() >= relativeOffset)
					&& (blockLine.getColumnStartIndex() <= relativeOffset))
				{
					retBlock = this;
				}
			}
			if ((retBlock == null) && (children != null) && (children.size() > 0))
			{
				ColumnBlock block = searchChildren(line);
				if ((block != null) && (block.isLineWithinThisBlock(line) == 0))
				{
					retBlock = block.getContainingBlock(line, offset);
				}
			}
		}
		return retBlock;
	}//}}}

	//{{{ getContainingBlock() method
	public ColumnBlock getColumnBlock(int line, int offset)
	{
		if (isDirty)
		{
			return null;
		}
		// int tabSize=-5;
		ColumnBlock colBlock = null;
		synchronized (buffer.columnBlockLock)
		{
			if ((line >= startLine) && (line <= endLine))
			{
				if ((lines != null) && (lines.size() > 0))
				{
					ColumnBlockLine blockLine = (ColumnBlockLine) (lines
						.get(line - startLine));
					if ((blockLine.getColumnEndIndex() + buffer
						.getLineStartOffset(line)) == offset)
					{
						// tabSize =
						// blockLine.getTabSize();
						colBlock = this;
					}
				}
				if ((colBlock == null) && (children != null)
					&& (children.size() > 0))
				{
					ColumnBlock block = searchChildren(line, 0,
						children.size() - 1);
					if ((block == null)
						|| (block.isLineWithinThisBlock(line) != 0))
					{
						throwException(offset, line);
					}
					// tabSize =
					// block.getColumnBlock(line,offset);
					colBlock = block.getColumnBlock(line, offset);
				}
			}
			// if(tabSize<0)
			if (colBlock == null)
				throwException(offset, line);
			// return tabSize;
			return colBlock;
		}

	}//}}}

	//{{{ searchChildren() method
	public ColumnBlock searchChildren(int line)
	{
		if ((children != null) && (children.size() > 0))
		{
			return searchChildren(line, 0, children.size() - 1);
		}
		else
		{
			return null;
		}
	}//}}}

	//{{{ searchChildren() method
	/*
	 * binary search on a sorted list searches the children for one
	 * containing the line no. line returns an exact match or the closest
	 * column block just below this line use isLineWithinThisBlock on the
	 * column block returned by this method to determine whether there was
	 * an exact match
	 */
	private ColumnBlock searchChildren(int line, int startIndex, int stopIndex)
	{
		if (children != null)
		{
			if (startIndex > stopIndex)
			{
				// no exact match found return the nearest
				// column block just below this line
				return (ColumnBlock) children.get(startIndex);
			}
			int currentSearchIndex = (startIndex + stopIndex) / 2;
			int found = ((ColumnBlock) children.get(currentSearchIndex))
				.isLineWithinThisBlock(line);
			if (found == 0)
			{
				return (ColumnBlock) children.get(currentSearchIndex);
			}
			else if (found > 0)
			{
				if ((children.size() - 1) > currentSearchIndex)
				{
					return searchChildren(line, currentSearchIndex + 1,
						stopIndex);
				}
				else
				{
					return null;
				}
			}
			else if (found < 0)
			{
				if (currentSearchIndex > 0)
				{
					return searchChildren(line, startIndex,
						currentSearchIndex - 1);
				}
				else
				{
					// no exact match found return the
					// nearest column block just below this
					// line
					return (ColumnBlock) children.get(0);
				}
			}
		}
		return null;
	}//}}}
	
	//{{{ toString() method
	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append("ColumnBlock[startLine : " + startLine + " ,endLine : " + endLine
			+ " ,columnBlockWidth : " + columnBlockWidth + "] LINES:");
		for (int i = 0; i < lines.size(); i++)
		{
			buf.append("\n");
			buf.append("LINE " + i + ":" + lines.elementAt(i));
		}

		for (int i = 0; i < children.size(); i++)
		{
			buf.append("\n");
			buf.append("CHILD " + i + ":" + children.elementAt(i));
		}
		return buf.toString();
	}//}}}

	//{{{ throwException() method
	private void throwException(int offset, int line)
	{
		throw new IllegalArgumentException("{ELSTIC TABSTOP}CORRUPT DATA@{"
			+ System.currentTimeMillis() + "} & Thread : "
			+ Thread.currentThread().getName()
			+ " :Cannot find the size for tab at offset "
			+ (offset - buffer.getLineStartOffset(line)) + "in line " + line
			+ "while searching in \n " + this);
	}//}}}

	//{{{ setDirtyStatus() method
	public void setDirtyStatus(boolean status)
	{
		synchronized (buffer.columnBlockLock)
		{
			isDirty = status;
		}
	}//}}}

	//{{{ updateLineNo() method
	public void updateLineNo(int line)
	{
		// Things to do in this method
		// update line no. in this column block
		// update column block lines in this column block
		// call this method on all children
		this.startLine += line;
		this.endLine += line;

		for (int i = 0; i < lines.size(); i++)
		{
			lines.elementAt(i).updateLineNo(line);
		}

		for (int i = 0; i < children.size(); i++)
		{
			((ColumnBlock) children.elementAt(i)).updateLineNo(line);
		}
	}//}}}

	//{{{ updateColumnBlockLineOffset() method
	public void updateColumnBlockLineOffset(int line, int offsetAdd, boolean increaseStartOffset)
	{
		if ((line >= startLine) && (line <= endLine))
		{
			if ((lines != null) && (lines.size() > 0))
			{
				ColumnBlockLine blockLine = (ColumnBlockLine) (lines.get(line
					- startLine));
				if (increaseStartOffset)
				{
					blockLine.colStartIndex += offsetAdd;
				}
				blockLine.colEndIndex += offsetAdd;
			}
			if ((children != null) && (children.size() > 0))
			{
				ColumnBlock block = searchChildren(line);
				if ((block != null) && (block.isLineWithinThisBlock(line) == 0))
				{
					block.updateColumnBlockLineOffset(line, offsetAdd, true);
				}
			}
		}
	}//}}}
	
	//{{{ setTabSizeDirtyStatus() method
	/*
	 * tab sizes become dirty on font changes or when char is added or
	 * deleted inside ColumnBlock they become clean once they get calculated
	 * again inside the tab expander
	 */
	public void setTabSizeDirtyStatus(boolean dirty, boolean recursive)
	{
		if (dirty)
		{
			tabSizesDirty = true;
		}
		else
		{
			tabSizesDirty = false;
		}
		if (recursive && children != null && children.size() > 0)
		{
			for (int i = 0; i < children.size(); i++)
			{
				((ColumnBlock) children.elementAt(i)).setTabSizeDirtyStatus(true,
					true);
			}
		}
	}//}}}

	//{{{ areTabSizesDirty() method
	public boolean areTabSizesDirty()
	{
		return tabSizesDirty;
	}//}}}
}
