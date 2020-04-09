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

package org.gjt.sp.jedit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiscUtilitiesTest
{
	@Mock
	private Buffer buffer;
	@Mock
	private View view;

	@Test
	public void canonPathEmpty()
	{
		assertEquals("",  MiscUtilities.canonPath(""));
	}

	@Test
	public void canonPathHome()
	{
		assertEquals(System.getProperty("user.home"),  MiscUtilities.canonPath("~"));
	}

	@Test
	public void canonPathHomeStart()
	{
		assertEquals(System.getProperty("user.home") + File.separator,  MiscUtilities.canonPath("~/"));
	}

	@Test
	public void canonPathHomeStart2()
	{
		assertEquals(System.getProperty("user.home") + File.separator + "toto",  MiscUtilities.canonPath("~/toto"));
	}

	@Test
	public void canonPathHomeMinus()
	{
		jEdit.getViewManager().setActiveView(view);
		when(view.getBuffer()).thenReturn(buffer);
		when(buffer.getPath()).thenReturn("/home/jedit-dev/blabla/myfile.txt");
		assertEquals("/home/jedit-dev/blabla/",  MiscUtilities.canonPath("-"));
	}
	@Test
	public void canonPathHomeUrl()
	{
		assertEquals("http://jedit.org/",  MiscUtilities.canonPath("http://jedit.org/"));
	}

	@Test
	public void canonPathFile()
	{
		assertEquals("C:\\Users\\jedi-dev\\",  MiscUtilities.canonPath("file://C:\\Users\\jedi-dev\\"));
	}

	@Test
	public void canonPathFileSlash()
	{
		assertEquals("C:\\Users\\jedi-dev\\",  MiscUtilities.canonPath("file:C:\\Users\\jedi-dev\\"));
	}
}