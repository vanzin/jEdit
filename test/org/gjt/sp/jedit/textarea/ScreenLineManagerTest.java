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

	@Test
	public void contentInserted()
	{
		int maxLines = 5;
		when(buffer.getLineCount()).thenReturn(maxLines);
		screenLineManager.reset();
		initLines(maxLines);
		screenLineManager.contentInserted(1, 0);
		assertTrue(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertTrue(screenLineManager.isScreenLineCountValid(2));
		assertTrue(screenLineManager.isScreenLineCountValid(3));
		assertTrue(screenLineManager.isScreenLineCountValid(4));
	}

	@Test
	public void contentInserted3lines()
	{
		int maxLines = 5;
		when(buffer.getLineCount()).thenReturn(maxLines);
		screenLineManager.reset();
		initLines(maxLines);
		/*
		 * 0 : 1
		 * 1 : 1
		 * 2 : 1
		 * 3 : 1
		 * 4 : 1
		 */
		screenLineManager.contentInserted(1, 3);
		/* should be
		 * 0 : 1
		 * 1 : 0
		 * 2 : 0
		 * 3 : 0
		 * 4 : 1
		 * 5 : 1
		 * 6 : 1
		 * 7 : 1
		 */
		assertTrue(screenLineManager.isScreenLineCountValid(0));
		assertFalse(screenLineManager.isScreenLineCountValid(1));
		assertFalse(screenLineManager.isScreenLineCountValid(2));
		assertFalse(screenLineManager.isScreenLineCountValid(3));
//		in my opinion next 4 lines are the old lines 1 to 4 and they should be valid
//		assertTrue(screenLineManager.isScreenLineCountValid(4));
//		assertTrue(screenLineManager.isScreenLineCountValid(5));
//		assertTrue(screenLineManager.isScreenLineCountValid(6));
//		assertTrue(screenLineManager.isScreenLineCountValid(7));
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
			screenLineManager.setScreenLineCount(i, 1);
	}
}