/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2020 jEdit contributors
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

package org.gjt.sp.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StringListTest
{
	public static final String str = "a,b,c,d,e,f";
	public static final String[] ARRAY = {"a", "b", "c", "d", "e", "f"};

	@Test
	public void split()
	{
		StringList result = new StringList();
		result.addAll(ARRAY);
		assertEquals(result, StringList.split(str, ","));
	}

	@Test
	public void split2()
	{
		assertEquals(new StringList(), StringList.split(null, ","));
	}

	@Test
	public void split3()
	{
		assertEquals(new StringList(), StringList.split("", ","));
	}

	@Test
	public void join()
	{
		assertEquals(str, StringList.join(ARRAY, ","));
	}

	@Test
	public void join2()
	{
		assertEquals(str, StringList.join(List.of(ARRAY), ","));
	}

	@Test
	public void join3()
	{
		assertEquals("", StringList.join(StandardUtilities.EMPTY_STRING_ARRAY, ","));
	}

	@Test
	public void join4()
	{
		assertEquals("a", StringList.join(List.of("a"), ","));
	}

	@Test
	public void toArray()
	{
		assertArrayEquals(ARRAY, new StringList(ARRAY).toArray());
	}

	@Test
	public void testToString()
	{
		assertEquals("a\nb\nc\nd\ne\nf", new StringList(ARRAY).toString());
	}
}