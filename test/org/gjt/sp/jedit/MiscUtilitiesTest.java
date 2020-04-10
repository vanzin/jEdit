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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiscUtilitiesTest
{
	private static final int UNIX = 0x31337;
	private static final int WINDOWS_NT = 0x666;
	private static int os;

	@Mock
	private Buffer buffer;
	@Mock
	private View view;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		os = getOS();
	}

	@After
	public void tearDown() throws Exception
	{
		updateOS(os);
	}

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

	@Test
	public void expandVariablesTilde()
	{
		String userHome = System.getProperty("user.home");
		assertEquals(userHome + "/blabla",  MiscUtilities.expandVariables("~/blabla"));
	}

	@Test
	public void expandVariablesTildeWindows()
	{
		String userHome = System.getProperty("user.home");
		assertEquals(userHome + "\\blabla",  MiscUtilities.expandVariables("~\\blabla"));
	}

	@Test
	public void expandVariablesEnvWindowsAsWindows() throws  Exception
	{
		Map<String,  String> env = System.getenv();
		Map.Entry<String,  String> firstEntry = env.entrySet().iterator().next();
		String  key = firstEntry.getKey();
		String  value = firstEntry.getValue();
		updateOS(WINDOWS_NT);
		assertEquals(value,  MiscUtilities.expandVariables("%"+key + '%'));
	}

	@Test
	public void expandVariablesEnvWindowsAsUnix() throws  Exception
	{
		Map<String,  String> env = System.getenv();
		Map.Entry<String,  String> firstEntry = env.entrySet().iterator().next();
		String  key = firstEntry.getKey();
		String  value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value,  MiscUtilities.expandVariables("%"+key + '%'));
	}

	@Test
	public void expandVariablesEnvUnix() throws  Exception
	{
		Map<String,  String> env = System.getenv();
		Map.Entry<String,  String> firstEntry = env.entrySet().iterator().next();
		String  key = firstEntry.getKey();
		String  value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value,  MiscUtilities.expandVariables("$"+key));
	}

	@Test
	public void expandVariablesEnvUnix2() throws  Exception
	{
		Map<String,  String> env = System.getenv();
		Map.Entry<String,  String> firstEntry = env.entrySet().iterator().next();
		String  key = firstEntry.getKey();
		String  value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value,  MiscUtilities.expandVariables("${"+key+'}'));
	}

	@Test
	public void expandVariablesEnvUnixNoMatch() throws  Exception
	{
		Map<String,  String> env = System.getenv();
		Map.Entry<String,  String> firstEntry = env.entrySet().iterator().next();
		String  key = firstEntry.getKey();
		updateOS(UNIX);
		assertEquals("${"+key,  MiscUtilities.expandVariables("${"+key));
	}

	@Test
	public void abbreviateUserHomeWindows() throws  Exception
	{
		updateOS(WINDOWS_NT);
		MiscUtilities.svc = null;
		String result = MiscUtilities.abbreviate(System.getProperty("user.home"));
		assertTrue("%USERPROFILE%".equals(result) || "%HOME%".equals(result));
	}

	@Test
	public void abbreviateUserHomeUnix() throws  Exception
	{
		updateOS(UNIX);
		MiscUtilities.svc = null;
		assertEquals("~",  MiscUtilities.abbreviate(System.getProperty("user.home")));
	}

	@Test
	public void resolveSymlinksWindowsUrl() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("http://www.jedit.org", MiscUtilities.resolveSymlinks("http://www.jedit.org"));
	}

	@Test
	public void resolveSymlinksWindows1() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:", MiscUtilities.resolveSymlinks("c:"));
	}

	@Test
	public void resolveSymlinksWindows2() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:\\", MiscUtilities.resolveSymlinks("c:\\"));
	}

	@Test
	public void resolveSymlinksWindows3() throws  Exception
	{
		if(OperatingSystem.isWindows())
		{
			// no idea how to test  that on linux
			assertEquals("C:\\bla", MiscUtilities.resolveSymlinks("c:\\bla"));
		}
	}

	@Test
	public void isAbsolutePathUrl()
	{
		assertTrue(MiscUtilities.isAbsolutePath("http://www.jedit.org"));
	}

	@Test
	public void isAbsolutePathTilde()
	{
		assertTrue(MiscUtilities.isAbsolutePath("~/"));
	}

	@Test
	public void isAbsolutePathMinus()
	{
		assertTrue(MiscUtilities.isAbsolutePath("-"));
	}

	@Test
	public void isAbsolutePathWindows() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:"));
	}

	@Test
	public void isAbsolutePathWindows2() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:\\bla"));
	}

	@Test
	public void isAbsolutePathWindows3() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertFalse(MiscUtilities.isAbsolutePath("toto/tutu"));
	}

	@Test
	public void isAbsolutePathWindows4() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:/bla"));
	}

	@Test
	public void isAbsolutePathWindows5() throws  Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("//bla"));
	}

	@Test
	public void isAbsolutePathWindows6() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("\\\\bla"));
	}

	@Test
	public void isAbsolutePathUnix() throws Exception
	{
		updateOS(UNIX);
		assertTrue(MiscUtilities.isAbsolutePath("/toto/tutu"));
	}

	@Test
	public void isAbsolutePathUnix2() throws Exception
	{
		updateOS(UNIX);
		assertFalse(MiscUtilities.isAbsolutePath("toto/tutu"));
	}

	@Test
	public void constructPathAbsolute()
	{
		assertEquals("http://www.jedit.org", MiscUtilities.constructPath(null,  "http://www.jedit.org"));
	}

	@Test
	public void constructPathNullEmpty()
	{
		assertEquals(System.getProperty("user.dir"), MiscUtilities.constructPath(null,  ""));
	}

	@Test
	public void constructPathDot()
	{
		assertEquals("/home/dev/jEdit", MiscUtilities.constructPath("/home/dev/jEdit", "."));
	}

	@Test
	public void constructPathDotDot()
	{
		assertEquals("/home/dev/", MiscUtilities.constructPath("/home/dev/jEdit", ".."));
	}

	@Test
	public void constructPathDotDotPath()
	{
		assertEquals(File.separator + "home"+ File.separator+"dev"+ File.separator+"yolo", MiscUtilities.constructPath("/home/dev/jEdit", "../yolo"));
	}

	@Test
	public void constructPathWindows() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:\\", MiscUtilities.constructPath(null, "c:\\"));
	}

	private static int getOS() throws IllegalAccessException, NoSuchFieldException
	{
		Field os = OperatingSystem.class.getDeclaredField("os");
		os.setAccessible(true);
		int  oldValue = os.getInt(OperatingSystem.class);
		return oldValue;
	}

	private static int updateOS(int newValue) throws IllegalAccessException, NoSuchFieldException
	{
		Field os = OperatingSystem.class.getDeclaredField("os");
		os.setAccessible(true);
		int  oldValue = os.getInt(OperatingSystem.class);
		os.set(OperatingSystem.class,newValue);
		return oldValue;
	}
}