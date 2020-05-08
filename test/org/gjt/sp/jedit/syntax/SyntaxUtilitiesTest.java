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

package org.gjt.sp.jedit.syntax;

import org.junit.Test;

import javax.swing.text.Segment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SyntaxUtilitiesTest
{
	@Test
	public void regionMatches()
	{
		String src = "Hello world";
		Segment segment = new Segment();
		segment.array = src.toCharArray();
		segment.count = src.length();
		assertTrue(SyntaxUtilities.regionMatches(false, segment, 6, "world".toCharArray()));
	}

	@Test
	public void regionMatchesFail()
	{
		String src = "Hello world";
		Segment segment = new Segment();
		segment.array = src.toCharArray();
		segment.count = src.length();
		assertFalse(SyntaxUtilities.regionMatches(false, segment, 6, "worlD".toCharArray()));
	}

	@Test
	public void regionMatchesIgnoreCase()
	{
		String src = "Hello world";
		Segment segment = new Segment();
		segment.array = src.toCharArray();
		segment.count = src.length();
		assertTrue(SyntaxUtilities.regionMatches(true, segment, 6, "worlD".toCharArray()));
	}
}