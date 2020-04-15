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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FirstLineTest
{
	private static Field preContentInsertedScrollLines;
	private static Field preContentRemovedScrollLines;

	@Mock
	private DisplayManager displayManager;
	@Mock
	private TextArea textArea;

	private FirstLine firstLine;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		Debug.SCROLL_DEBUG = true;
		Debug.SCROLL_VERIFY = true;
		preContentInsertedScrollLines = Anchor.class.getDeclaredField("preContentInsertedScrollLines");
		preContentInsertedScrollLines.setAccessible(true);
		preContentRemovedScrollLines = Anchor.class.getDeclaredField("preContentRemovedScrollLines");
		preContentRemovedScrollLines.setAccessible(true);
	}

	@Before
	public void setUp() throws Exception
	{
		reset(displayManager, textArea);
		firstLine = new FirstLine(displayManager, textArea);
	}

	@Test
	public void preContentInsertedAfter()
	{
		firstLine.setPhysicalLine(3);
		firstLine.setScrollLine(3);
		firstLine.preContentInserted(12, 3);
		assertEquals(3, firstLine.getPhysicalLine());
		assertEquals(3, firstLine.getScrollLine());
		assertEquals(0, firstLine.getSkew());
	}

	@Test
	public void preContentInsertedBefore() throws IllegalAccessException
	{
		firstLine.setPhysicalLine(54);
		firstLine.setScrollLine(54);
		ArgumentCaptor<Integer> physicalLineCaptor = ArgumentCaptor.forClass(Integer.class);
		when(displayManager.isLineVisible(physicalLineCaptor.capture())).thenReturn(true);
		when(displayManager.getScreenLineCount(physicalLineCaptor.capture())).thenReturn(1);

		firstLine.preContentInserted(20, 5);
		assertEquals(34, preContentInsertedScrollLines.get(firstLine));
	}

	@Test
	public void setSkew()
	{
		firstLine.setSkew(42);
		assertEquals(42, firstLine.getSkew());
		assertTrue(firstLine.isCallChanged());
	}
}