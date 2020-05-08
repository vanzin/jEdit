/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2010 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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

import org.gjt.sp.jedit.buffer.JEditBuffer;

import java.awt.font.TextLayout;
import java.util.Vector;

import javax.swing.text.TabExpander;

/** A Swing TabExpander for Elastic Tabstops. */

public class ElasticTabstopsTabExpander implements TabExpander 
{
	private final TextArea textArea;
	private final TextAreaPainter painter;

	//{{{ ElasticTabstopsTabExpander() method
	public ElasticTabstopsTabExpander(TextArea textArea)
	{
		this.textArea = textArea;
		painter = textArea.getPainter();
	}//}}}
	
	//{{{ nextTabStop() method
	@Override
	public float nextTabStop(float x, int tabOffset) 
	{
		float _tabSize = 0;
		JEditBuffer buffer = textArea.getBuffer();
		if(buffer.getBooleanProperty("elasticTabstops")&& buffer.getColumnBlock()!=null)
		{
			int line = buffer.getLineOfOffset(tabOffset);
			_tabSize = getTabSize(buffer.getColumnBlock().getColumnBlock(line, tabOffset),line);
			if(_tabSize<0)
			{
				throw new IllegalArgumentException("Unaccounted tab at line "+ buffer.getLineOfOffset(tabOffset)+" at index "+tabOffset);
			}
		}
		//keep minimum tab size of  textArea.tabSize
		_tabSize+= textArea.tabSize;
		return (x+_tabSize);
	}//}}}
	
	//{{{ getTabSize() method
	private float getTabSize(ColumnBlock columnBlock, int line) 
	{
		float ret = -5;
		if(columnBlock!=null)
		{	
			Vector<ColumnBlockLine> lines = columnBlock.getLines();
			if(columnBlock.areTabSizesDirty())
			{
				float colBlockWidth = -1;
				JEditBuffer buffer = textArea.getBuffer();
				for(int i= 0;i<lines.size();i++)
				{
					ColumnBlockLine colBlockLine = lines.elementAt(i);
					int startOffset = colBlockLine.getColumnStartIndex()+ buffer.getLineStartOffset(colBlockLine.getLine());
					String str = buffer.getText(startOffset,colBlockLine.getColumnEndIndex()-colBlockLine.getColumnStartIndex());
					float width = 0;
					if(!str.isEmpty())
					{	
						TextLayout layout = new TextLayout(str, painter.getFont(), painter.getFontRenderContext());
						width = layout.getAdvance();
					}
					colBlockLine.lineLength = width;
					//colBlockLine.lineLength = textArea.painter.getFontMetrics().stringWidth(str);
					if((colBlockWidth<0)||(colBlockLine.lineLength>colBlockWidth))
					{
						colBlockWidth = colBlockLine.lineLength;
					}
				}
				columnBlock.columnBlockWidth = colBlockWidth;
				columnBlock.setTabSizeDirtyStatus(false, false);
			}
			ret = columnBlock.columnBlockWidth- lines.get(line-columnBlock.startLine).lineLength;
		}	
		return ret;
	}//}}}
}
