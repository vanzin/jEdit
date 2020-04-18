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

package org.gjt.sp.jedit.buffer;

import org.gjt.sp.util.SegmentBuffer;
import org.gjt.sp.util.SegmentCharSequence;
import org.junit.Before;
import org.junit.Test;

import javax.swing.text.Segment;

import static org.junit.Assert.*;

public class ContentManagerTest
{
	private ContentManager contentManager;

	@Before
	public void setUp() throws Exception
	{
		contentManager = new ContentManager();
	}

	@Test
	public void setContent()
	{
		String input = "Hello world\nHow are you ?";
		contentManager._setContent(input.toCharArray(), input.length());
		assertEquals(input.length(), contentManager.getLength());
		assertEquals(input, contentManager.getText(0, contentManager.getLength()));
		CharSequence segment = contentManager.getSegment(0, contentManager.getLength());
		assertEquals(contentManager.getLength(), segment.length());
		assertEquals(input, segment.toString());
		Segment segment2 = new Segment();
		contentManager.getText(0, contentManager.getLength() - 14, segment2);
		assertEquals("Hello world", segment2.toString());
		assertEquals("How are you ?", contentManager.getText(12, contentManager.getLength() - 12));
	}

	@Test
	public void insertString()
	{
		String input = "Hello world\nHow are you ?";
		contentManager.insert(0, input);
		assertEquals(input.length(), contentManager.getLength());
		assertEquals(input, contentManager.getText(0, contentManager.getLength()));
		CharSequence segment = contentManager.getSegment(0, contentManager.getLength());
		assertEquals(contentManager.getLength(), segment.length());
		assertEquals(input, segment.toString());
		assertEquals("How are you ?", contentManager.getText(12, contentManager.getLength() - 12));
	}

	@Test
	public void insertSegment()
	{
		SegmentBuffer segmentBuffer = new SegmentBuffer(10);
		String input = "Hello world\nHow are you ?";
		segmentBuffer.append(input.toCharArray(), 0, input.length());
		contentManager.insert(0, segmentBuffer);
		assertEquals(input.length(), contentManager.getLength());
		assertEquals(input, contentManager.getText(0, contentManager.getLength()));
		CharSequence segment = contentManager.getSegment(0, contentManager.getLength());
		assertEquals(contentManager.getLength(), segment.length());
		assertEquals(input, segment.toString());
		assertEquals("How are you ?", contentManager.getText(12, contentManager.getLength() - 12));
	}

	@Test
	public void insertSegmentCharSequence()
	{
		SegmentBuffer segmentBuffer = new SegmentBuffer(10);
		String input = "Hello world\nHow are you ?";
		segmentBuffer.append(input.toCharArray(), 0, input.length());
		SegmentCharSequence segmentCharSequence = new SegmentCharSequence(segmentBuffer);
		contentManager.insert(0, segmentCharSequence);
		assertEquals(input.length(), contentManager.getLength());
		assertEquals(input, contentManager.getText(0, contentManager.getLength()));
		CharSequence segment = contentManager.getSegment(0, contentManager.getLength());
		assertEquals(contentManager.getLength(), segment.length());
		assertEquals(input, segment.toString());
		assertEquals("How are you ?", contentManager.getText(12, contentManager.getLength() - 12));
	}
}