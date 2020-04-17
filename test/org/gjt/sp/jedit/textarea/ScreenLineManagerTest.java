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
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScreenLineManagerTest
{
	/*
	 * Text example used to validate things with soft wrap
	 * a
	 * a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b
	 * a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c
	 * a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b c
	 * a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca bb c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a b ca b c a b c a a
	 */
	@Mock
	private JEditBuffer buffer;

	private ScreenLineManager screenLineManager;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		Debug.SCREEN_LINES_DEBUG = true;
	}

	@Before
	public void setUp() throws Exception
	{
		reset(buffer);
		screenLineManager = new ScreenLineManager(buffer);
	}

	@Test
	public void isScreenLineCountValidNull()
	{
		assertFalse(screenLineManager.isScreenLineCountValid(0));
	}

	@Test
	public void isScreenLineCountValidNegative()
	{
		assertFalse(screenLineManager.isScreenLineCountValid(-1));
	}

	@Test
	public void isScreenLineCountValidOverLimit()
	{
		when(buffer.getLineCount()).thenReturn(10);
		screenLineManager.reset();
		assertFalse(screenLineManager.isScreenLineCountValid(40));
		screenLineManager.setScreenLineCount(2, 3);
		assertEquals(3, screenLineManager.getScreenLineCount(2));
	}

	@Test
	public void setScreenLineCount()
	{
		when(buffer.getLineCount()).thenReturn(10);
		screenLineManager.reset();
		screenLineManager.setScreenLineCount(2, 3);
		assertEquals(3, screenLineManager.getScreenLineCount(2));
	}

	@Test
	public void invalidateScreenLineCounts()
	{
		int maxLines = 5;
		when(buffer.getLineCount()).thenReturn(maxLines);
		screenLineManager.reset();
		resetLines(screenLineManager, maxLines);
		assertTrue(screenLineManager.isScreenLineCountValid(0));
		assertTrue(screenLineManager.isScreenLineCountValid(1));
		assertTrue(screenLineManager.isScreenLineCountValid(2));
		assertTrue(screenLineManager.isScreenLineCountValid(3));
		assertTrue(screenLineManager.isScreenLineCountValid(4));
		screenLineManager.invalidateScreenLineCounts();
		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
	}

	@Test
	public void contentInserted0LineAtStartSmallArray()
	{
		int numLines = 0;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	/**
	 * Test editing the second line
	 */
	@Test
	public void contentInserted0LineInMiddleSmallArray()
	{
		int numLines = 0;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	@Test
	public void contentInserted0LineAtEndSmallArray()
	{
		int numLines = 0;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
	}

	@Test
	public void contentInserted1lineAtStartSmallArray()
	{
		int numLines = 1;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(2, screenLineManager.getScreenLineCount(2));
		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
	}

	@Test
	public void contentInserted1InMiddleSmallArray()
	{
		int numLines = 1;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
	}

	@Test
	public void contentInserted1AtEndSmallArray()
	{
		int numLines = 1;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
	}

	@Test
	public void contentInserted4lineAtStartSmallArray()
	{
		int numLines = 4;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertEquals(2, screenLineManager.getScreenLineCount(5));
		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	@Test
	public void contentInserted4lineInMiddleSmallArray()
	{
		int numLines = 4;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	@Test
	public void contentInserted4lineAtEndSmallArray()
	{
		int numLines = 4;
		initForInsert(numLines, 5);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertFalse(screenLineManager.isScreenLineCountValid(6));
		assertFalse(screenLineManager.isScreenLineCountValid(7));
		assertFalse(screenLineManager.isScreenLineCountValid(8));
	}

	@Test
	public void contentInserted0LineAtStartBigArray()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
		assertEquals(99, screenLineManager.getScreenLineCount(5));
	}

	/**
	 * Test editing the second line
	 */
	@Test
	public void contentInserted0LineInMiddleBigArray()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
		assertEquals(99, screenLineManager.getScreenLineCount(5));
	}

	@Test
	public void contentInserted0LineAtEndBigArray()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertEquals(99, screenLineManager.getScreenLineCount(5));
	}

	@Test
	public void contentInserted1lineAtStartBigArray()
	{
		int numLines = 1;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(2, screenLineManager.getScreenLineCount(2));
		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
		assertEquals(99, screenLineManager.getScreenLineCount(6));
	}

	@Test
	public void contentInserted1InMiddleBigArray()
	{
		int numLines = 1;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
		assertEquals(99, screenLineManager.getScreenLineCount(6));
	}

	@Test
	public void contentInserted1AtEndBigArray()
	{
		int numLines = 1;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertEquals(99, screenLineManager.getScreenLineCount(6));
	}

	@Test
	public void contentInserted4lineAtStartBigArray()
	{
		int numLines = 4;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertEquals(2, screenLineManager.getScreenLineCount(5));
		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
		assertEquals(99, screenLineManager.getScreenLineCount(9));
	}

	@Test
	public void contentInserted4lineInMiddleBigArray()
	{
		int numLines = 4;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
		assertEquals(99, screenLineManager.getScreenLineCount(9));
	}

	@Test
	public void contentInserted4lineAtEndBigArray()
	{
		int numLines = 4;
		initForInsert(numLines, 100);
		screenLineManager.contentInserted(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertFalse(screenLineManager.isScreenLineCountValid(6));
		assertFalse(screenLineManager.isScreenLineCountValid(7));
		assertFalse(screenLineManager.isScreenLineCountValid(8));
		assertEquals(99, screenLineManager.getScreenLineCount(9));
	}

	@Test
	public void contentRemoved0LineAtStart()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	/**
	 * Test editing the second line
	 */
	@Test
	public void contentRemoved0LineInMiddle()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	@Test
	public void contentRemoved0LineAtEnd()
	{
		int numLines = 0;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(4, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
	}

	@Test
	public void contentRemoved1lineAtStart()
	{
		int numLines = 1;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertEquals(3, screenLineManager.getScreenLineCount(1));
		assertEquals(4, screenLineManager.getScreenLineCount(2));
		assertEquals(5, screenLineManager.getScreenLineCount(3));
	}

	@Test
	public void contentRemoved1InMiddle()
	{
		int numLines = 1;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(4, screenLineManager.getScreenLineCount(2));
		assertEquals(5, screenLineManager.getScreenLineCount(3));
	}

	@Test
	public void contentRemoved2lineAtStart()
	{
		int numLines = 2;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(0, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(0));
		assertEquals(4, screenLineManager.getScreenLineCount(1));
		assertEquals(5, screenLineManager.getScreenLineCount(2));
	}

	@Test
	public void contentRemoved2lineInMiddle()
	{
		int numLines = 2;
		initForInsert(numLines, 100);
		screenLineManager.contentRemoved(1, numLines);

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertEquals(5, screenLineManager.getScreenLineCount(2));
	}

	private void initForInsert(int numLines, int arraySize)
	{
		int lineCountBeforeInsert = 5;
		when(buffer.getLineCount())
			.thenReturn(arraySize)
			.thenReturn(lineCountBeforeInsert + numLines);
		//First return is for the reset()
		screenLineManager.reset();
		resetLines(screenLineManager, lineCountBeforeInsert);
	}

	private void resetLines(ScreenLineManager screenLineManager, int nb)
	{
		try
		{
			for (int i = 0; i < Integer.MAX_VALUE; i++)
				screenLineManager.setScreenLineCount(i, 99);
		}
		catch (Exception e)
		{
			// to fill the array
		}
		for (int i = 0; i < nb; i++)
			screenLineManager.setScreenLineCount(i, i + 1);
	}
}