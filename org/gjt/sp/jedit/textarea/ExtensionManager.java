/*
 * ExtensionManager.java - Handles 'layers'
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002, 2003 Slava Pestov
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

import java.awt.Graphics2D;
import java.util.*;
import org.gjt.sp.util.Log;

class ExtensionManager
{
	//{{{ addExtension() method
	void addExtension(int layer, TextAreaExtension ext)
	{
		Entry entry = new Entry(layer,ext);

		int i = 0;
		Iterator iter = extensions.iterator();
		while(iter.hasNext())
		{
			int _layer = ((Entry)iter.next()).layer;
			if(layer < _layer)
			{
				extensions.add(i,entry);
				return;
			}
			i++;
		}

		extensions.add(entry);
	} //}}}

	//{{{ removeExtension() method
	void removeExtension(TextAreaExtension ext)
	{
		Iterator iter = extensions.iterator();
		while(iter.hasNext())
		{
			if(((Entry)iter.next()).ext == ext)
			{
				iter.remove();
				return;
			}
		}
	} //}}}

	//{{{ getExtensions() method
	TextAreaExtension[] getExtensions()
	{
		TextAreaExtension[] retVal = new TextAreaExtension[
			extensions.size()];
		Iterator iter = extensions.iterator();
		int i = 0;
		while(iter.hasNext())
		{
			retVal[i++] = ((Entry)iter.next()).ext;
		}
		return retVal;
	} //}}}

	//{{{ paintScreenLineRange() method
	void paintScreenLineRange(JEditTextArea textArea, Graphics2D gfx,
		int firstLine, int lastLine, int y, int lineHeight)
	{
		try
		{
			int[] physicalLines = new int[lastLine - firstLine + 1];
			int[] start = new int[physicalLines.length];
			int[] end = new int[physicalLines.length];

			for(int i = 0; i < physicalLines.length; i++)
			{
				int screenLine = i + firstLine;
				ChunkCache.LineInfo lineInfo = textArea
					.chunkCache.getLineInfo(screenLine);

				if(lineInfo.physicalLine == -1)
					physicalLines[i] = -1;
				else
				{
					physicalLines[i] = lineInfo.physicalLine;
					start[i] = textArea.getScreenLineStartOffset(screenLine);
					end[i] = textArea.getScreenLineEndOffset(screenLine);
				}
			}

			paintScreenLineRange(gfx,firstLine,lastLine,physicalLines,
				start,end,y,lineHeight);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,"Error repainting line"
				+ " range {" + firstLine + ","
				+ lastLine + "}:");
			Log.log(Log.ERROR,this,e);
		}
	} //}}}

	//{{{ getToolTipText() method
	String getToolTipText(int x, int y)
	{
		for(int i = 0; i < extensions.size(); i++)
		{
			TextAreaExtension ext = ((Entry)extensions.get(i)).ext;
			String toolTip = ext.getToolTipText(x,y);
			if(toolTip != null)
				return toolTip;
		}

		return null;
	} //}}}

	//{{{ Private members
	private List extensions = new LinkedList();

	//{{{ paintScreenLineRange() method
	private void paintScreenLineRange(Graphics2D gfx, int firstLine,
		int lastLine, int[] physicalLines, int[] start, int[] end,
		int y, int lineHeight)
	{
		Iterator iter = extensions.iterator();
		while(iter.hasNext())
		{
			TextAreaExtension ext = ((Entry)iter.next()).ext;
			try
			{
				ext.paintScreenLineRange(gfx,firstLine,lastLine,
					physicalLines,start,end,y,lineHeight);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,t);

				// remove it so editor can continue
				// functioning
				iter.remove();
			}
		}
	} //}}}

	//}}}

	//{{{ Entry class
	static class Entry
	{
		int layer;
		TextAreaExtension ext;

		Entry(int layer, TextAreaExtension ext)
		{
			this.layer = layer;
			this.ext = ext;
		}
	} //}}}
}
