/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
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

import java.awt.font.TextLayout;
import java.util.Vector;

import javax.swing.text.TabExpander;

public class ElasticTabstopsTabExpander implements TabExpander 
{
	TextArea textArea;
	
	//{{{ ElasticTabstopsTabExpander() method
	public ElasticTabstopsTabExpander(TextArea textArea)
	{
		this.textArea = textArea;
	}//}}}
	
	//{{{ nextTabStop() method
	@Override
	public float nextTabStop(float x, int tabOffset) 
	{
		float _tabSize = 0;
		if(textArea.buffer.getBooleanProperty("elasticTabstops")&&textArea.buffer.getColumnBlock()!=null)
		{
			int line = textArea.buffer.getLineOfOffset(tabOffset);
			_tabSize = getTabSize(textArea.buffer.getColumnBlock().getColumnBlock(line, tabOffset),line);
			if(_tabSize<0)
			{
				throw new IllegalArgumentException("Unaccounted tab at line "+textArea.buffer.getLineOfOffset(tabOffset)+" at index "+tabOffset);
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
			Vector lines = columnBlock.getLines();
			if(columnBlock.areTabSizesDirty())
			{
				float colBlockWidth = -1;
				for(int i= 0;i<lines.size();i++)
				{
					ColumnBlockLine colBlockLine = (ColumnBlockLine)lines.elementAt(i);
					int startOffset = colBlockLine.getColumnStartIndex()+textArea.buffer.getLineStartOffset(colBlockLine.getLine());
					String str = textArea.buffer.getText(startOffset,colBlockLine.getColumnEndIndex()-colBlockLine.getColumnStartIndex());
					float width = 0;
					if(str.length()!=0)
					{	
						TextLayout layout = new TextLayout(str,textArea.painter.getFont(),textArea.painter.getFontRenderContext());
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
			ret = columnBlock.columnBlockWidth-((ColumnBlockLine)lines.get(line-columnBlock.startLine)).lineLength;
		}	
		return ret;
	}//}}}

}
