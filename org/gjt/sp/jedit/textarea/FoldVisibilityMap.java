/*
 * FoldVisibilityMap.java - A specialized hash table
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001 Slava Pestov
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

/**
 * Stores which fold start lines are visible, and their associated
 * virtual line numbers.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.0pre1
 */
class FoldVisibilityMap
{
	//{{{ getp() method
	Entry getp(int p)
	{
		Entry e = p2v[p % BUCKETS];
		while(e != null)
		{
			if(e.p == p)
				return e;
			else
				e = e.nextp;
		}

		throw new IllegalArgumentException("Not in map: " + p);
	} //}}}

	//{{{ getv() method
	Entry getv(int v)
	{
		Entry e = v2p[v % BUCKETS];
		while(e != null)
		{
			if(e.v == v)
				return e;
			else
				e = e.nextv;
		}

		throw new IllegalArgumentException("Not in map: " + v);
	} //}}}

	//{{{ put() method
	void put(int p, int v, boolean visible)
	{
		Entry bucketp = p2v[p % BUCKETS];
		while(bucketp != null)
		{
			if(bucketp.p == p)
			{
				bucketp.v = v;
				bucketp.visible = visible;
				break;
			}
			else
				bucketp = bucketp.nextp;
		}

		Entry e = new Entry(p,v,visible);

		bucketp = p2v[p % BUCKETS];
		e.nextp = bucketp;
		p2v[p % BUCKETS] = e;

		Entry bucketv = v2p[v % BUCKETS];
		e.nextv = bucketv;
		v2p[v % BUCKETS] = e;
	} //}}}

	//{{{ removep() method
	void removep(int p)
	{
		Entry prev = null;
		Entry e = p2v[p % BUCKETS];
		int v = -1;

		while(e != null)
		{
			if(e.p == p)
			{
				v = e.v;
				if(prev == null)
					p2v[p % BUCKETS] = e.nextp;
				else
					prev.nextp = e.nextp;
				break;
			}
			else
				e = e.nextp;
		}

		if(v == -1)
			return;

		e = v2p[v % BUCKETS];
		while(e != null)
		{
			if(e.p == p)
			{
				if(prev == null)
					v2p[v % BUCKETS] = e.nextv;
				else
					prev.nextv = e.nextv;
				break;
			}
			else
				e = e.nextv;
		}
	} //}}}

	//{{{ insertLines() method
	/**
	 * Called when a range of lines is inserted.
	 * @param startLine The start line
	 * @param numLines The line count
	 */
	void insertLines(int startLine, int numLines)
	{
		for(int i = 0; i < BUCKETS; i++)
		{
			Entry e = p2v[i];
			while(e != null)
			{
				if(e.p > startLine)
					e.p += numLines;
				e = e.nextp;
			}
		}
	} //}}}

	//{{{ removeLines() method
	/**
	 * Called when a range of lines is removed.
	 * @param startLine The start line
	 * @param numLines The line count
	 */
	void removeLines(int startLine, int numLines)
	{
		for(int i = 0; i < BUCKETS; i++)
		{
			Entry e = p2v[i];
			while(e != null)
			{
				if(e.p > startLine)
				{
					if(e.p <= startLine + numLines)
						e.p = startLine;
					else
						e.p -= numLines;
				}

				e = e.nextp;
			}
		}
	} //}}}

	//{{{ Private members
	private static final int BUCKETS = 20;
	private Entry[] p2v = new Entry[BUCKETS];
	private Entry[] v2p = new Entry[BUCKETS];
	//}}}

	//{{{ Entry class
	static class Entry
	{
		int p;
		int v;
		boolean visible;

		Entry nextp;
		Entry nextv;

		//{{{ Entry constructor
		Entry(int p, int v, boolean visible)
		{
			this.p = p;
			this.v = v;
			this.visible = visible;
		}
	}
}
