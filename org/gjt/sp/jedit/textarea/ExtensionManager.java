/*
 * ExtensionManager.java - Handles 'layers'
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2002 Slava Pestov
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
import java.util.ArrayList;
import org.gjt.sp.util.Log;

class ExtensionManager
{
	//{{{ addExtension() method
	void addExtension(int layer, TextAreaExtension ext)
	{
		Entry entry = new Entry(layer,ext);

		for(int i = 0; i < extensions.size(); i++)
		{
			int _layer = ((Entry)extensions.get(i)).layer;
			if(layer < _layer)
			{
				extensions.add(i,entry);
				return;
			}
		}

		extensions.add(entry);
	} //}}}

	//{{{ removeExtension() method
	void removeExtension(TextAreaExtension ext)
	{
		for(int i = 0; i < extensions.size(); i++)
		{
			Entry entry = (Entry)extensions.get(i);
			if(entry.ext == ext)
			{
				extensions.remove(i);
				return;
			}
		}
	} //}}}

	//{{{ paintValidLine() method
	void paintValidLine(Graphics2D gfx, int screenLine,
		int physicalLine, int start, int end, int y)
	{
		for(int i = 0; i < extensions.size(); i++)
		{
			TextAreaExtension ext = ((Entry)extensions.get(i)).ext;
			try
			{
				ext.paintValidLine(gfx,screenLine,
					physicalLine,start,end,y);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,t);

				// remove it so editor can continue
				// functioning
				extensions.remove(i);
				i--;
			}
		}
	} //}}}

	//{{{ paintInvalidLine() method
	void paintInvalidLine(Graphics2D gfx, int screenLine,
		int y)
	{
		for(int i = 0; i < extensions.size(); i++)
		{
			TextAreaExtension ext = ((Entry)extensions.get(i)).ext;
			try
			{
				ext.paintInvalidLine(gfx,screenLine,y);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,t);

				// remove it so editor can continue
				// functioning
				extensions.remove(i);
				i--;
			}
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
	private ArrayList extensions = new ArrayList();
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
