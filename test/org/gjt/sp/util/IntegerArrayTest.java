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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IntegerArrayTest
{
	private IntegerArray empty;
	private IntegerArray small;

	@Before
	public void setUp() throws Exception
	{
		empty = new IntegerArray(0);
		small = new IntegerArray(10);
	}

	@Test
	public void get()
	{
		small.add(33);
		assertEquals(33, small.get(0));
	}

	@Test
	public void getEmpty()
	{
		empty.add(33);
		assertEquals(33, empty.get(0));
	}

	@Test
	public void isEmpty()
	{
		assertTrue(empty.isEmpty());
	}

	@Test
	public void isEmpty2()
	{
		assertTrue(small.isEmpty());
	}
}