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

package org.gjt.sp.jedit.textarea;

import org.gjt.sp.jedit.Debug;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class RangeMapTest
{
	private RangeMap rangeMap;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		Debug.FOLD_VIS_DEBUG = true;
	}

	@Before
	public void setUp() throws Exception
	{
		rangeMap = new RangeMap();
		rangeMap.reset(40);
	}

	@Test
	public void copy() throws NoSuchFieldException, IllegalAccessException
	{
		rangeMap.contentInserted(0, 40);
		rangeMap.hide(3, 5);
		rangeMap.hide(9, 10);
		RangeMap rangeMap = new RangeMap(this.rangeMap);
		Field fvm = RangeMap.class.getDeclaredField("fvm");
		fvm.setAccessible(true);
		Field fvmcount = RangeMap.class.getDeclaredField("fvmcount");
		fvmcount.setAccessible(true);
		assertArrayEquals((int[]) fvm.get(this.rangeMap), (int[]) fvm.get(rangeMap));
		assertEquals(fvmcount.get(this.rangeMap), fvmcount.get(rangeMap));
	}

	@Test
	public void hide()
	{
		rangeMap.hide(10, 20);
		assertTrue(isVisibleLine(rangeMap, 0));
		assertTrue(isVisibleLine(rangeMap, 1));
		assertTrue(isVisibleLine(rangeMap, 2));
		assertTrue(isVisibleLine(rangeMap, 3));
		assertTrue(isVisibleLine(rangeMap, 4));
		assertTrue(isVisibleLine(rangeMap, 5));
		assertTrue(isVisibleLine(rangeMap, 6));
		assertTrue(isVisibleLine(rangeMap, 7));
		assertTrue(isVisibleLine(rangeMap, 8));
		assertTrue(isVisibleLine(rangeMap, 9));
		assertFalse(isVisibleLine(rangeMap, 10));
		assertFalse(isVisibleLine(rangeMap, 11));
		assertFalse(isVisibleLine(rangeMap, 12));
		assertFalse(isVisibleLine(rangeMap, 13));
		assertFalse(isVisibleLine(rangeMap, 14));
		assertFalse(isVisibleLine(rangeMap, 15));
		assertFalse(isVisibleLine(rangeMap, 16));
		assertFalse(isVisibleLine(rangeMap, 17));
		assertFalse(isVisibleLine(rangeMap, 18));
		assertFalse(isVisibleLine(rangeMap, 19));
		assertFalse(isVisibleLine(rangeMap, 20));
		assertTrue(isVisibleLine(rangeMap, 21));
		assertTrue(isVisibleLine(rangeMap, 30));
		assertTrue(isVisibleLine(rangeMap, 39));
	}

	@Test
	public void show()
	{
		rangeMap.hide(10, 20);
		rangeMap.show(13, 18);
		assertTrue(isVisibleLine(rangeMap, 0));
		assertTrue(isVisibleLine(rangeMap, 1));
		assertTrue(isVisibleLine(rangeMap, 2));
		assertTrue(isVisibleLine(rangeMap, 3));
		assertTrue(isVisibleLine(rangeMap, 4));
		assertTrue(isVisibleLine(rangeMap, 5));
		assertTrue(isVisibleLine(rangeMap, 6));
		assertTrue(isVisibleLine(rangeMap, 7));
		assertTrue(isVisibleLine(rangeMap, 8));
		assertTrue(isVisibleLine(rangeMap, 9));
		assertFalse(isVisibleLine(rangeMap, 10));
		assertFalse(isVisibleLine(rangeMap, 11));
		assertFalse(isVisibleLine(rangeMap, 12));
		assertTrue(isVisibleLine(rangeMap, 13));
		assertTrue(isVisibleLine(rangeMap, 14));
		assertTrue(isVisibleLine(rangeMap, 15));
		assertTrue(isVisibleLine(rangeMap, 16));
		assertTrue(isVisibleLine(rangeMap, 17));
		assertTrue(isVisibleLine(rangeMap, 18));
		assertFalse(isVisibleLine(rangeMap, 19));
		assertFalse(isVisibleLine(rangeMap, 20));
		assertTrue(isVisibleLine(rangeMap, 21));
		assertTrue(isVisibleLine(rangeMap, 30));
		assertTrue(isVisibleLine(rangeMap, 39));
	}

	@Test
	public void preContentRemoved()
	{
		rangeMap.hide(10, 20);
		rangeMap.show(13, 18);
		assertFalse(rangeMap.preContentRemoved(5, 5));
		assertTrue(isVisibleLine(rangeMap, 0));
		assertTrue(isVisibleLine(rangeMap, 1));
		assertTrue(isVisibleLine(rangeMap, 2));
		assertTrue(isVisibleLine(rangeMap, 3));
		assertTrue(isVisibleLine(rangeMap, 4));
		assertFalse(isVisibleLine(rangeMap, 5));
		assertFalse(isVisibleLine(rangeMap, 6));
		assertFalse(isVisibleLine(rangeMap, 7));
		assertTrue(isVisibleLine(rangeMap, 8));
		assertTrue(isVisibleLine(rangeMap, 9));
		assertTrue(isVisibleLine(rangeMap, 10));
		assertTrue(isVisibleLine(rangeMap, 11));
		assertTrue(isVisibleLine(rangeMap, 12));
		assertTrue(isVisibleLine(rangeMap, 13));
		assertFalse(isVisibleLine(rangeMap, 14));
		assertFalse(isVisibleLine(rangeMap, 15));
		assertTrue(isVisibleLine(rangeMap, 16));
		assertTrue(isVisibleLine(rangeMap, 25));
		assertTrue(isVisibleLine(rangeMap, 34));
	}

	@Test
	public void first()
	{
		rangeMap.hide(0, 5);
		assertEquals(6, rangeMap.first());
	}

	@Test
	public void last()
	{
		rangeMap.hide(0, 5);
		assertEquals(39, rangeMap.last());
	}

	@Test
	public void last2()
	{
		rangeMap.hide(0, 5);
		rangeMap.hide(10, 40);
		assertEquals(9, rangeMap.last());
	}

	@Test
	public void search()
	{
		rangeMap.hide(0, 5);
		assertEquals(-1, rangeMap.search(0));
	}

	private static boolean isVisibleLine(RangeMap rangeMap, int line)
	{
		return rangeMap.search(line) % 2 == 0;
	}
}