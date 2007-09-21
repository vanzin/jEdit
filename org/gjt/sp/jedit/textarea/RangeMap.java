/*
 * RangeMap.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2005 Slava Pestov
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

import org.gjt.sp.jedit.Debug;
import org.gjt.sp.util.Log;

/**
 * The fold visibility map.
 *
 * All lines from fvm[2*n] to fvm[2*n+1]-1 inclusive are visible.
 * All lines from position fvm[2*n+1] to fvm[2*n+2]-1 inclusive are
 * invisible.
 *
 * Examples:
 * ---------
 * All lines visible: { 0, buffer.getLineCount() }
 * Narrow from a to b: { a, b + 1 }
 * Collapsed fold from a to b: { 0, a + 1, b, buffer.getLineCount() }
 *
 * Note: length is always even.
 */
class RangeMap
{
	//{{{ RangeMap constructor
	RangeMap()
	{
		fvm = new int[2];
		lastfvmget = -1;
	} //}}}

	//{{{ RangeMap constructor
	RangeMap(RangeMap copy)
	{
		this.fvm = copy.fvm.clone();
		this.fvmcount = copy.fvmcount;
	} //}}}

	//{{{ reset() method
	void reset(int lines)
	{
		lastfvmget = -1;
		fvmcount = 2;
		fvm[0] = 0;
		fvm[1] = lines;
	} //}}}

	//{{{ first() method
	int first()
	{
		return fvm[0];
	} //}}}

	//{{{ last() method
	int last()
	{
		return fvm[fvmcount - 1] - 1;
	} //}}}

	//{{{ lookup() method
	int lookup(int index)
	{
		return fvm[index];
	} //}}}

	//{{{ search() method
	/**
	 * Returns the fold visibility map index for the given line.
	 */
	int search(int line)
	{
		if(line < fvm[0])
			return -1;
		if(line >= fvm[fvmcount - 1])
			return fvmcount - 1;

		if(lastfvmget != -1)
		{
			if(line >= fvm[lastfvmget])
			{
				if(lastfvmget == fvmcount - 1
					|| line < fvm[lastfvmget + 1])
				{
					return lastfvmget;
				}
			}
		}

		int start = 0;
		int end = fvmcount - 1;

loop:		for(;;)
		{
			switch(end - start)
			{
			case 0:
				lastfvmget = start;
				break loop;
			case 1:
				int value = fvm[end];
				if(value <= line)
					lastfvmget = end;
				else
					lastfvmget = start;
				break loop;
			default:
				int pivot = (end + start) / 2;
				value = fvm[pivot];
				if(value == line)
				{
					lastfvmget = pivot;
					break loop;
				}
				else if(value < line)
					start = pivot;
				else
					end = pivot - 1;
				break;
			}
		}

		return lastfvmget;
	} //}}}

	//{{{ put() method
	/**
	 * Replaces from <code>start</code> to <code>end-1</code> inclusive with
	 * <code>put</code>. Update <code>fvmcount</code>.
	 */
	void put(int start, int end, int[] put)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			StringBuilder buf = new StringBuilder(50);
			buf.append("fvmput(").append(start).append(',');
			buf.append(end).append(',');
			buf.append('{');
			if(put != null)
			{
				for(int i = 0; i < put.length; i++)
				{
					if(i != 0)
						buf.append(',');
					buf.append(put[i]);
				}
			}
			buf.append("})");
			Log.log(Log.DEBUG,this,buf.toString());
		}
		int putl = put == null ? 0 : put.length;

		int delta = putl - (end - start);
		if(fvmcount + delta > fvm.length)
		{
			int[] newfvm = new int[(fvm.length << 1) + 1];
			System.arraycopy(fvm,0,newfvm,0,fvmcount);
			fvm = newfvm;
		}

		if(delta != 0)
		{
			System.arraycopy(fvm,end,fvm,start + putl,
				fvmcount - end);
		}

		if(putl != 0)
		{
			System.arraycopy(put,0,fvm,start,put.length);
		}

		fvmcount += delta;

		dump();

		if(fvmcount == 0)
			throw new InternalError();
	} //}}}

	//{{{ put2() method
	/**
	 * Merge previous and next entry if necessary.
	 */
	void put2(int starti, int endi, int start, int end)
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			Log.log(Log.DEBUG,this,"*fvmput2(" + starti + ","
				+ endi + "," + start + "," + end + ")");
		}
		if(starti != -1 && fvm[starti] == start)
		{
			if(endi <= fvmcount - 2 && fvm[endi + 1]
				== end + 1)
			{
				put(starti,endi + 2,null);
			}
			else
			{
				put(starti,endi + 1,
					new int[] { end + 1 });
			}
		}
		else
		{
			if(endi != fvmcount - 1 && fvm[endi + 1]
				== end + 1)
			{
				put(starti + 1,endi + 2,
					new int[] { start });
			}
			else
			{
				put(starti + 1,endi + 1,
					new int[] { start,
					end + 1 });
			}
		}
	} //}}}

	//{{{ next() method
	int next(int line)
	{
		int index = search(line);
		/* in collapsed range */
		if(index % 2 != 0)
		{
			/* beyond last visible line */
			if(fvmcount == index + 1)
				return - 1;
			/* start of next expanded range */
			else
				return fvm[index + 1];
		}
		/* last in expanded range */
		else if(line == fvm[index + 1] - 1)
		{
			/* equal to last visible line */
			if(fvmcount == index + 2)
				return -1;
			/* start of next expanded range */
			else
				return fvm[index + 2];
		}
		/* next in expanded range */
		else
			return line + 1;
	} //}}}

	//{{{ prev() method
	int prev(int line)
	{
		int index = search(line);
		/* before first visible line */
		if(index == -1)
			return -1;
		/* in collapsed range */
		else if(index % 2 == 1)
		{
			/* end of prev expanded range */
			return fvm[index] - 1;
		}
		/* first in expanded range */
		else if(line == fvm[index])
		{
			/* equal to first visible line */
			if(index == 0)
				return -1;
			/* end of prev expanded range */
			else
				return fvm[index - 1] - 1;
		}
		/* prev in expanded range */
		else
			return line - 1;
	} //}}}

	//{{{ show() method
	void show(int start, int end)
	{
		int starti = search(start);
		int endi = search(end);

		if(starti % 2 == 0)
		{
			if(endi % 2 == 0)
				put(starti + 1,endi + 1,null);
			else
			{
				if(endi != fvmcount - 1
					&& fvm[endi + 1] == end + 1)
					put(starti + 1,endi + 2,null);
				else
				{
					put(starti + 1,endi,null);
					fvm[starti + 1] = end + 1;
				}
			}
		}
		else
		{
			if(endi % 2 == 0)
			{
				if(starti != -1 && fvm[starti] == start)
					put(starti,endi + 1,null);
				else
				{
					put(starti + 1,endi,null);
					fvm[starti + 1] = start;
				}
			}
			else
				put2(starti,endi,start,end);
		}

		lastfvmget = -1;
	} //}}}
	
	//{{{ hide() method
	void hide(int start, int end)
	{
		int starti = search(start);
		int endi = search(end);

		if(starti % 2 == 0)
		{
			if(endi % 2 == 0)
				put2(starti,endi,start,end);
			else
			{
				if(start == fvm[0])
					put(starti,endi + 1,null);
				else
				{
					put(starti + 1,endi,null);
					fvm[starti + 1] = start;
				}
			}
		}
		else
		{
			if(endi % 2 == 0)
			{
				if(end + 1 == fvm[fvmcount - 1])
					put(starti + 1,endi + 2,null);
				else
				{
					put(starti + 1,endi,null);
					fvm[starti + 1] = end + 1;
				}
			}
			else
				put(starti + 1,endi + 1,null);
		}

		lastfvmget = -1;
	} //}}}

	//{{{ count() method
	int count()
	{
		return fvmcount;
	} //}}}

	//{{{ dump() method
	void dump()
	{
		if(Debug.FOLD_VIS_DEBUG)
		{
			StringBuilder buf = new StringBuilder("{");
			for(int i = 0; i < fvmcount; i++)
			{
				if(i != 0)
					buf.append(',');
				buf.append(fvm[i]);
			}
			buf.append('}');
			Log.log(Log.DEBUG,this,"fvm = " + buf);
		}
	} //}}}

	//{{{ contentInserted() method
	void contentInserted(int startLine, int numLines)
	{
		if(numLines != 0)
		{
			int index = search(startLine);
			int start = index + 1;

			for(int i = start; i < fvmcount; i++)
				fvm[i] += numLines;

			lastfvmget = -1;
			dump();
		}
	} //}}}

	//{{{ preContentRemoved() method
	/**
	 * @return If the anchors should be reset.
	 */
	boolean preContentRemoved(int startLine, int numLines)
	{
		boolean returnValue = false;

		int endLine = startLine + numLines;

		/* update fold visibility map. */
		int starti = search(startLine);
		int endi = search(endLine);

		/* both have same visibility; just remove
		 * anything in between. */
		if(Math.abs(starti % 2) == Math.abs(endi % 2))
		{
			if(endi - starti == fvmcount)
			{
				// we're removing from before
				// the first visible to after
				// the last visible
				returnValue = true;
				starti = 1;
			}
			else
			{
				put(starti + 1,endi + 1,null);
				starti++;
			}
		}
		/* collapse 2 */
		else if(starti != -1 && fvm[starti] == startLine)
		{
			if(endi - starti == fvmcount - 1)
			{
				// we're removing from
				// the first visible to after
				// the last visible
				returnValue = true;
				starti = 1;
			}
			else
				put(starti,endi + 1,null);
		}
		/* shift */
		else
		{
			put(starti + 1,endi,null);
			fvm[starti + 1] = startLine;
			starti += 2;
		}

		/* update */
		for(int i = starti; i < fvmcount; i++)
			fvm[i] -= numLines;

		lastfvmget = -1;
		dump();

		return returnValue;
	} //}}}

	//{{{ Private members
	private int[] fvm;
	private int fvmcount;
	private int lastfvmget;
	//}}}
}
