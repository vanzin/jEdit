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

public class SegmentCharSequenceTest
{

	private String input;
	private SegmentBuffer segment;

	@Before
	public void setUp() throws Exception
	{
		input = "Hello world";
		segment = new SegmentBuffer(10);
		segment.append(input.toCharArray(), 0, input.length());
	}

	@Test
	public void charAt()
	{
		SegmentCharSequence segmentCharSequence = new SegmentCharSequence(segment);
		assertEquals('e', segmentCharSequence.charAt(1));
		assertEquals(input.length(), segmentCharSequence.length());
		assertEquals(input, segmentCharSequence.toString());
	}

	@Test
	public void subSequence()
	{
		SegmentCharSequence segmentCharSequence = new SegmentCharSequence(segment);
		assertEquals("Hello", segmentCharSequence.subSequence(0, 5).toString());
	}
}