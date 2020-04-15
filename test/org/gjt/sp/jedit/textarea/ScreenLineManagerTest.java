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
		initLines(maxLines);
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

	/**
	 * Test editing the second line
	 */
	@Test
	public void contentInsertedExtendArray()
	{
		int numLines = 0;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(1, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(1));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as this screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	private void initForInsert(int lineCountBeforeInsert, int numLines)
	{
		when(buffer.getLineCount()).thenReturn(lineCountBeforeInsert + numLines);
		screenLineManager.reset();
		initLines(lineCountBeforeInsert);
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted1lineOnLine1ExtendArray()
	{
		int numLines = 1;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(1, numLines);
		/* should be
		 * 0 : 1
		 * 1 : 2 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 3
		 * 4 : 4
		 * 5 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
//		assertEquals(0, screenLineManager.getScreenLineCount(2));


		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine0ExtendArray()
	{
		int numLines = 4;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(0, numLines);
		/* should be
		 * 0 : 1 <- but it is 0 until the display manager updates it
		 * 1 : 1 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 2
		 * 6 : 3
		 * 7 : 4
		 * 8 : 5
		 */
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

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine1ExtendArray()
	{
		int numLines = 4;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(1, numLines);
		/* should be
		 * 0 : 1
		 * 1 : 2 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 1 <- but it is 0 until the display manager updates it
		 * 6 : 3
		 * 7 : 4
		 * 8 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
//		assertEquals(0, screenLineManager.getScreenLineCount(2));
//		assertEquals(0, screenLineManager.getScreenLineCount(3));
//		assertEquals(0, screenLineManager.getScreenLineCount(4));
//		assertEquals(0, screenLineManager.getScreenLineCount(5));


		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine2ExtendArray()
	{
		int numLines = 4;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(2, numLines);
		/* should be
		 * 0 : 1
		 * 1 : 2
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 1 <- but it is 0 until the display manager updates it
		 * 6 : 1 <- but it is 0 until the display manager updates it
		 * 7 : 4
		 * 8 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertFalse(screenLineManager.isScreenLineCountValid(6));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(2));
//		assertEquals(0, screenLineManager.getScreenLineCount(3));
//		assertEquals(0, screenLineManager.getScreenLineCount(4));
//		assertEquals(0, screenLineManager.getScreenLineCount(5));
//		assertEquals(0, screenLineManager.getScreenLineCount(6));

		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted2linesOnLastLineExtendArray()
	{
		int numLines = 2;
		initForInsert(5, numLines);
		screenLineManager.contentInserted(4, numLines);
		/* should be
		 * 0 : 1
		 * 1 : 2
		 * 2 : 3
		 * 3 : 4
		 * 4 : 0 <- but it is 0 until the display manager updates it
		 * 5 : 0 <- but it is 0 until the display manager updates it
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
	}

	/**
	 * Test editing the second line
	 */
	@Test
	public void contentInserted()
	{
		int numLines = 0;
		initForInsertAndExtendArray(5, numLines);
		screenLineManager.contentInserted(1, numLines);

		assertFalse(screenLineManager.isScreenLineCountValid(1));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as this screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
		assertEquals(5, screenLineManager.getScreenLineCount(4));
	}

	private void initForInsertAndExtendArray(int maxLines, int numLines)
	{
		when(buffer.getLineCount()).thenReturn(maxLines + numLines);
		screenLineManager.reset();
		screenLineManager.contentInserted(1, numLines);
		try
		{
			for (int i = maxLines + numLines; i < Integer.MAX_VALUE; i++)
				screenLineManager.setScreenLineCount(i, 99);
		}
		catch (Exception e)
		{
			// to fill the array with 99 value
		}
		initLines(maxLines + numLines);
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted1lineOnLine1()
	{
		int numLines = 1;
		initForInsertAndExtendArray(5, numLines);
		screenLineManager.contentInserted(1, numLines);
		/* should be
		 * 0 : 1
		 * 1 : 2 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 3
		 * 4 : 4
		 * 5 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
//		assertEquals(0, screenLineManager.getScreenLineCount(2));


		assertEquals(3, screenLineManager.getScreenLineCount(3));
		assertEquals(4, screenLineManager.getScreenLineCount(4));
		assertEquals(5, screenLineManager.getScreenLineCount(5));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine0()
	{
		int nbLines = 4;
		initForInsertAndExtendArray(5, nbLines);
		screenLineManager.contentInserted(0, 4);
		/* should be
		 * 0 : 1 <- but it is 0 until the display manager updates it
		 * 1 : 1 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 2
		 * 6 : 3
		 * 7 : 4
		 * 8 : 5
		 */
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

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine1()
	{
		int nbLines = 4;
		initForInsertAndExtendArray(5, nbLines);
		screenLineManager.contentInserted(1, 4);
		/* should be
		 * 0 : 1
		 * 1 : 2 <- but it is 0 until the display manager updates it
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 1 <- but it is 0 until the display manager updates it
		 * 6 : 3
		 * 7 : 4
		 * 8 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(1));
//		assertEquals(0, screenLineManager.getScreenLineCount(2));
//		assertEquals(0, screenLineManager.getScreenLineCount(3));
//		assertEquals(0, screenLineManager.getScreenLineCount(4));
//		assertEquals(0, screenLineManager.getScreenLineCount(5));


		assertEquals(3, screenLineManager.getScreenLineCount(6));
		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted4linesOnLine2()
	{
		int nbLines = 4;
		initForInsertAndExtendArray(5, nbLines);
		screenLineManager.contentInserted(2, 4);
		/* should be
		 * 0 : 1
		 * 1 : 2
		 * 2 : 1 <- but it is 0 until the display manager updates it
		 * 3 : 1 <- but it is 0 until the display manager updates it
		 * 4 : 1 <- but it is 0 until the display manager updates it
		 * 5 : 1 <- but it is 0 until the display manager updates it
		 * 6 : 1 <- but it is 0 until the display manager updates it
		 * 7 : 4
		 * 8 : 5
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));
		assertFalse(screenLineManager.isScreenLineCountValid(6));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
//		cannot call this as those screenline count is invalid
//		assertEquals(0, screenLineManager.getScreenLineCount(2));
//		assertEquals(0, screenLineManager.getScreenLineCount(3));
//		assertEquals(0, screenLineManager.getScreenLineCount(4));
//		assertEquals(0, screenLineManager.getScreenLineCount(5));
//		assertEquals(0, screenLineManager.getScreenLineCount(6));

		assertEquals(4, screenLineManager.getScreenLineCount(7));
		assertEquals(5, screenLineManager.getScreenLineCount(8));
	}

	/**
	 * Test editing the second line and adding a new line
	 */
	@Test
	public void contentInserted2linesOnLastLine()
	{
		int nbLines = 4;
		initForInsertAndExtendArray(5, nbLines);
		screenLineManager.contentInserted(4, 2);
		/* should be
		 * 0 : 1
		 * 1 : 2
		 * 2 : 3
		 * 3 : 4
		 * 4 : 0 <- but it is 0 until the display manager updates it
		 * 5 : 0 <- but it is 0 until the display manager updates it
		 */
		assertFalse(screenLineManager.isScreenLineCountValid(4));
		assertFalse(screenLineManager.isScreenLineCountValid(5));

		assertEquals(1, screenLineManager.getScreenLineCount(0));
		assertEquals(2, screenLineManager.getScreenLineCount(1));
		assertEquals(3, screenLineManager.getScreenLineCount(2));
		assertEquals(4, screenLineManager.getScreenLineCount(3));
	}

	@Test
	public void contentRemoved1Line()
	{
		int maxLines = 5;
		when(buffer.getLineCount()).thenReturn(maxLines);
		screenLineManager.reset();
		initLines(maxLines);
		screenLineManager.contentRemoved(1, 1);
		assertTrue(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertTrue(screenLineManager.isScreenLineCountValid(2));
		assertTrue(screenLineManager.isScreenLineCountValid(3));
		assertTrue(screenLineManager.isScreenLineCountValid(4));
	}

	@Test
	public void contentRemoved2Line()
	{
		int maxLines = 5;
		when(buffer.getLineCount()).thenReturn(maxLines);
		screenLineManager.reset();
		initLines(maxLines);
		screenLineManager.contentRemoved(1, 2);
		assertTrue(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertTrue(screenLineManager.isScreenLineCountValid(2));
		assertTrue(screenLineManager.isScreenLineCountValid(3));
	}

	private void initLines(int nb)
	{
		for (int i = 0; i < nb; i++)
			screenLineManager.setScreenLineCount(i, i+1);
	}
}