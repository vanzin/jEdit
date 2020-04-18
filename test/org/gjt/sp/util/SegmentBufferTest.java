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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SegmentBufferTest
{
	private SegmentBuffer segmentBuffer;

	@Before
	public void setUp() throws Exception
	{
		segmentBuffer = new SegmentBuffer(10);
	}

	@Test
	public void testAppendChar()
	{
		segmentBuffer.append('a');

		assertEquals('a', segmentBuffer.charAt(0));
		assertEquals(1, segmentBuffer.count);
	}

	@Test
	public void testAppendCharArray()
	{
		segmentBuffer.append('a');
		String inputString = "hello world";
		char[] charArray = inputString.toCharArray();
		segmentBuffer.append(inputString.toCharArray(), 0, inputString.length());

		assertEquals(charArray.length + 1, segmentBuffer.count);
		assertEquals('a' + inputString, segmentBuffer.toString());
	}
}