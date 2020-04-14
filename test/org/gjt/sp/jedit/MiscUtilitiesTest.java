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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MiscUtilitiesTest
{
	private static final int UNIX = 0x31337;
	private static final int WINDOWS_NT = 0x666;
	private static int os;
	private static String userHome;

	@Mock
	private Buffer buffer;
	@Mock
	private View view;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		os = getOS();
		userHome = System.getProperty("user.home");
	}

	@After
	public void tearDown() throws Exception
	{
		updateOS(os);
		System.setProperty("user.home", userHome);
	}

	@Test
	public void canonPathEmpty()
	{
		assertEquals("", MiscUtilities.canonPath(""));
	}

	@Test
	public void canonPathHome()
	{
		assertEquals(System.getProperty("user.home"), MiscUtilities.canonPath("~"));
	}

	@Test
	public void canonPathHomeStart()
	{
		assertEquals(System.getProperty("user.home") + File.separator, MiscUtilities.canonPath("~/"));
	}

	@Test
	public void canonPathHomeStart2()
	{
		assertEquals(System.getProperty("user.home") + File.separator + "toto", MiscUtilities.canonPath("~/toto"));
	}

	@Test
	public void canonPathHomeMinus()
	{
		jEdit.setActiveView(view);
//		jEdit.getViewManager().setActiveView(view);
		when(view.getBuffer()).thenReturn(buffer);
		when(buffer.getPath()).thenReturn("/home/jedit-dev/blabla/myfile.txt");
		assertEquals("/home/jedit-dev/blabla/", MiscUtilities.canonPath("-"));
	}

	@Test
	public void canonPathHomeUrl()
	{
		assertEquals("http://jedit.org/", MiscUtilities.canonPath("http://jedit.org/"));
	}

	@Test
	public void canonPathFile()
	{
		assertEquals("C:\\Users\\jedit-dev\\", MiscUtilities.canonPath("file://C:\\Users\\jedit-dev\\"));
	}

	@Test
	public void canonPathFileWindows()
	{
		// this test doesn't work on linux
		if (OperatingSystem.isWindows())
			assertEquals("C:\\Users\\jedit-dev\\blabla\\", MiscUtilities.canonPath("file://C:\\Users\\jedit-dev/blabla/"));
	}

	@Test
	public void canonPathFileSlash()
	{
		assertEquals("C:\\Users\\jedit-dev\\", MiscUtilities.canonPath("file:C:\\Users\\jedit-dev\\"));
	}

	@Test
	public void canonPathTildeHomeWithFileSeparator()
	{
		String tmpUserHome = userHome;
		if (!userHome.endsWith(File.separator))
		{
			tmpUserHome = userHome + File.separator;
			System.setProperty("user.home", tmpUserHome);
		}
		assertEquals(tmpUserHome, MiscUtilities.canonPath("~/"));
	}

	@Test
	public void canonPathTildeHomeWithoutSeparator()
	{
		String tmpUserHome = userHome;
		if (userHome.endsWith(File.separator))
		{
			System.setProperty("user.home", userHome.substring(0, userHome.length() - 2));
		}
		else
		{
			tmpUserHome = userHome + File.separator;
		}
		assertEquals(tmpUserHome, MiscUtilities.canonPath("~/"));
	}

	@Test
	public void expandVariablesTilde()
	{
		String userHome = System.getProperty("user.home");
		assertEquals(userHome + "/blabla", MiscUtilities.expandVariables("~/blabla"));
	}

	@Test
	public void expandVariablesTildeWindows()
	{
		String userHome = System.getProperty("user.home");
		assertEquals(userHome + "\\blabla", MiscUtilities.expandVariables("~\\blabla"));
	}

	@Test
	public void expandVariablesEnvWindowsAsWindows() throws Exception
	{
		Map<String, String> env = System.getenv();
		Map.Entry<String, String> firstEntry = env.entrySet().iterator().next();
		String key = firstEntry.getKey();
		String value = firstEntry.getValue();
		updateOS(WINDOWS_NT);
		assertEquals(value, MiscUtilities.expandVariables("%" + key + '%'));
	}

	@Test
	public void expandVariablesEnvWindowsAsUnix() throws Exception
	{
		Map<String, String> env = System.getenv();
		Map.Entry<String, String> firstEntry = env.entrySet().iterator().next();
		String key = firstEntry.getKey();
		String value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value, MiscUtilities.expandVariables("%" + key + '%'));
	}

	@Test
	public void expandVariablesEnvUnix() throws Exception
	{
		Map<String, String> env = System.getenv();
		Map.Entry<String, String> firstEntry = env.entrySet().iterator().next();
		String key = firstEntry.getKey();
		String value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value, MiscUtilities.expandVariables("$" + key));
	}

	@Test
	public void expandVariablesEnvUnix2() throws Exception
	{
		Map<String, String> env = System.getenv();
		Map.Entry<String, String> firstEntry = env.entrySet().iterator().next();
		String key = firstEntry.getKey();
		String value = firstEntry.getValue();
		updateOS(UNIX);
		assertEquals(value, MiscUtilities.expandVariables("${" + key + '}'));
	}

	@Test
	public void expandVariablesEnvUnixNoMatch() throws Exception
	{
		Map<String, String> env = System.getenv();
		Map.Entry<String, String> firstEntry = env.entrySet().iterator().next();
		String key = firstEntry.getKey();
		updateOS(UNIX);
		assertEquals("${" + key, MiscUtilities.expandVariables("${" + key));
	}

	@Test
	public void abbreviateUserHomeWindows() throws Exception
	{
		updateOS(WINDOWS_NT);
		MiscUtilities.svc = null;
		String result = MiscUtilities.abbreviate(System.getProperty("user.home"));
		assertTrue("%USERPROFILE%".equals(result) || "%HOME%".equals(result));
	}

	@Test
	public void abbreviateUserHomeUnix() throws Exception
	{
		updateOS(UNIX);
		MiscUtilities.svc = null;
		assertEquals("~", MiscUtilities.abbreviate(System.getProperty("user.home")));
	}

	@Test
	public void resolveSymlinksWindowsUrl() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("http://www.jedit.org", MiscUtilities.resolveSymlinks("http://www.jedit.org"));
	}

	@Test
	public void resolveSymlinksWindows1() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:", MiscUtilities.resolveSymlinks("c:"));
	}

	@Test
	public void resolveSymlinksWindows2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:\\", MiscUtilities.resolveSymlinks("c:\\"));
	}

	@Test
	public void resolveSymlinksWindows3() throws Exception
	{
		if (OperatingSystem.isWindows())
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
	public void isAbsolutePathWindows() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:"));
	}

	@Test
	public void isAbsolutePathWindows2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:\\"));
	}

	@Test
	public void isAbsolutePathWindows3() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:\\bla"));
	}

	@Test
	public void isAbsolutePathWindows4() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertFalse(MiscUtilities.isAbsolutePath("toto/tutu"));
	}

	@Test
	public void isAbsolutePathWindows5() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("c:/bla"));
	}

	@Test
	public void isAbsolutePathWindows6() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("//bla"));
	}

	@Test
	public void isAbsolutePathWindows7() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertTrue(MiscUtilities.isAbsolutePath("\\\\bla"));
	}

	@Test
	public void isAbsolutePathWindows8() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertFalse(MiscUtilities.isAbsolutePath("c:blabla.txt"));
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
		assertEquals("http://www.jedit.org", MiscUtilities.constructPath(null, "http://www.jedit.org"));
	}

	@Test
	public void constructPathNullEmpty()
	{
		assertEquals(System.getProperty("user.dir"), MiscUtilities.constructPath(null, ""));
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
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "yolo"), MiscUtilities.constructPath("/home/dev/jEdit", "../yolo"));
	}

	@Test
	public void constructPathWindows() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:", MiscUtilities.constructPath(null, "c:"));
	}

	@Test
	public void constructPathWindows2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:\\", MiscUtilities.constructPath(null, "c:\\"));
	}

	@Test
	public void constructPathWindows3() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("c:\\blabla.txt", MiscUtilities.constructPath(null, "c:blabla.txt"));
	}

	@Test
	public void constructPath3Args()
	{
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "file.txt"),
			MiscUtilities.constructPath("/home/dev/jEdit", "..", "file.txt"));
	}

	@Test
	public void concatPath()
	{
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "jEdit", "test.txt"),
			MiscUtilities.concatPath("/home/dev/jEdit", "/test.txt"));
	}

	@Test
	public void concatPath2()
	{
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "jEdit", "test.txt"),
			MiscUtilities.concatPath("/home/dev/jEdit", "test.txt"));
	}

	@Test
	public void concatPath3()
	{
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "jEdit", "c","test.txt"),
			MiscUtilities.concatPath("/home/dev/jEdit", "c:test.txt"));
	}

	@Test
	public void concatPath4()
	{
		assertEquals(File.separator + String.join(File.separator, "home", "dev", "jEdit", "c","test.txt"),
			MiscUtilities.concatPath("/home/dev/jEdit/", "c:test.txt"));
	}

	@Test
	public void getFirstSeparatorIndexAbsolute()
	{
		assertEquals(0, MiscUtilities.getFirstSeparatorIndex("/yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFirstSeparatorIndexRelative()
	{
		assertEquals(4, MiscUtilities.getFirstSeparatorIndex("yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFirstSeparatorWindows1() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(6, MiscUtilities.getFirstSeparatorIndex("c:yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFirstSeparatorWindows2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(7, MiscUtilities.getFirstSeparatorIndex("c:/yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFirstSeparatorWindows3() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(7, MiscUtilities.getFirstSeparatorIndex("c:\\yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFirstSeparatorWindows4() throws Exception
	{
		// this test doesn't work on linux
		if (OperatingSystem.isWindows())
			assertEquals(7, MiscUtilities.getFirstSeparatorIndex("c:\\yoyo\\tata\\aaa.txt"));
	}

	@Test
	public void getLastSeparatorIndexAbsolute()
	{
		assertEquals(10, MiscUtilities.getLastSeparatorIndex("/yoyo/tata/aaa.txt"));
	}

	@Test
	public void getLastSeparatorIndexRelative()
	{
		assertEquals(9, MiscUtilities.getLastSeparatorIndex("yoyo/tata/aaa.txt"));
	}

	@Test
	public void getLastSeparatorIndexNo()
	{
		assertEquals(-1, MiscUtilities.getLastSeparatorIndex("yoyotata"));
	}

	@Test
	public void getLastSeparatorWindows1() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(11, MiscUtilities.getLastSeparatorIndex("c:yoyo/tata/aaa.txt"));
	}

	@Test
	public void getLastSeparatorWindows2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(12, MiscUtilities.getLastSeparatorIndex("c:/yoyo/tata/aaa.txt"));
	}

	@Test
	public void getLastSeparatorWindows3() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(12, MiscUtilities.getLastSeparatorIndex("c:\\yoyo/tata/aaa.txt"));
	}

	@Test
	public void getFileExtension() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(".txt", MiscUtilities.getFileExtension("c:\\yoyo/ta.ta/aaa.txt"));
	}

	@Test
	public void getFileExtension2() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals(".gz", MiscUtilities.getFileExtension("c:\\yoyo/ta.ta/aaa.tar.gz"));
	}

	@Test
	public void getFileExtensionNoExtension() throws Exception
	{
		updateOS(WINDOWS_NT);
		assertEquals("", MiscUtilities.getFileExtension("c:\\yoyo/ta.ta/aaa"));
	}

	@Test
	public void getFileName()
	{
		assertEquals("aaa.txt", MiscUtilities.getFileName("c:\\yoyo/ta.ta/aaa.txt"));
	}

	@Test
	public void getCompleteBaseName()
	{
		assertEquals("aaa", MiscUtilities.getCompleteBaseName("c:\\yoyo/ta.ta/aaa"));
	}

	@Test
	public void getCompleteBaseName2()
	{
		assertEquals("IP-192.168.1.1-data.tar", MiscUtilities.getCompleteBaseName("/net/log/IP-192.168.1.1-data.tar.gz"));
	}

	@Test
	public void getBaseName()
	{
		assertEquals("aaa", MiscUtilities.getBaseName("c:\\yoyo/ta.ta/aaa"));
		assertEquals("aaa", MiscUtilities.getFileNameNoExtension("c:\\yoyo/ta.ta/aaa"));
	}

	@Test
	public void getBaseName2()
	{
		assertEquals("IP-192", MiscUtilities.getBaseName("/net/log/IP-192.168.1.1-data.tar.gz"));
	}

	@Test
	public void getProtocolOfURL()
	{
		assertEquals("http", MiscUtilities.getProtocolOfURL("http://www.jedit.org"));
	}

	@Test
	public void isURL()
	{
		assertTrue(MiscUtilities.isURL("http://www.jedit.org"));
	}

	@Test
	public void isURLEtcPassword()
	{
		assertFalse(MiscUtilities.isURL("/etc/passwd"));
	}

	@Test
	public void isURLWindowsPath()
	{
		assertFalse(MiscUtilities.isURL("C:\\AUTOEXEC.BAT"));
	}

	@Test
	public void isURLFileUrl()
	{
		assertTrue(MiscUtilities.isURL("file:///etc/passwd"));
	}

	@Test
	public void getNthBackupFileNoPrefix()
	{
		assertEquals(new File(File.separatorChar + String.join(File.separator, "tmp", "toto.txt0")),
			MiscUtilities.getNthBackupFile("toto.txt", 0, 2, null, "", "/tmp"));
	}

	@Test
	public void getNthBackupFileNoPrefix1()
	{
		assertEquals(new File(File.separatorChar + String.join(File.separator, "tmp", "toto.txt1")),
			MiscUtilities.getNthBackupFile("toto.txt", 1, 2, "", null, "/tmp"));
	}

	@Test
	public void getNthBackupFileNoPrefixButSuffix()
	{
		assertEquals(new File(File.separatorChar + String.join(File.separator, "tmp", "toto.txtsuf")),
			MiscUtilities.getNthBackupFile("toto.txt", 1, 0, "", "suf", "/tmp"));
	}

	@Test
	public void getNthBackupFileNoPrefix1ButSuffix()
	{
		assertEquals(new File(File.separatorChar + String.join(File.separator, "tmp", "toto.txtsuf")),
			MiscUtilities.getNthBackupFile("toto.txt", 1, 1, "", "suf", "/tmp"));
	}

	@Test
	public void getNthBackupFileNoSuffix()
	{
		assertEquals(new File(File.separatorChar + String.join(File.separator, "tmp","_toto.txt1")), MiscUtilities.getNthBackupFile("toto.txt", 1, 2, "_", "", "/tmp"));
	}

	@Test
	public void getBackupDirectory()
	{
		assertNull(MiscUtilities.getBackupDirectory());
	}

	@Test
	public void fileToClass()
	{
		assertEquals("org.git.sp.jedit.MiscUtilitiesTest", MiscUtilities.fileToClass("org/git/sp/jedit/MiscUtilitiesTest.class"));
	}

	@Test
	public void classToFile()
	{
		assertEquals("org/git/sp/jedit/MiscUtilitiesTest.class", MiscUtilities.classToFile("org.git.sp.jedit.MiscUtilitiesTest"));
	}

	@Test
	public void pathsEqual()
	{
		assertTrue(MiscUtilities.pathsEqual("http://www.jedit.org","http://www.jedit.org"));
	}

	@Test
	public void pathsEqual2()
	{
		assertTrue(MiscUtilities.pathsEqual("http://www.jedit.org","http://www.jedit.org/"));
	}

	@Test
	public void pathsEqual3()
	{
		assertTrue(MiscUtilities.pathsEqual("http://www.jedit.org/","http://www.jedit.org"));
	}

	@Test
	public void pathsEqual4()
	{
		assertTrue(MiscUtilities.pathsEqual("http://www.jedit.org/","http://www.jedit.org/"));
	}

	@Test
	public void pathsEqualFalse()
	{
		assertFalse(MiscUtilities.pathsEqual("http://www.jedit.org","c:/test.txt"));
	}

	@Test
	public void escapesToChars()
	{
		assertEquals("bl\\a\t\tbo\t\n\naa\t\n\tff", MiscUtilities.escapesToChars("bl\\\\a\\t\\tbo\\t\\n\\naa\\t\\n\\tff"));
	}

	@Test
	public void getLongestPrefix()
	{
		assertEquals("blabla", MiscUtilities.getLongestPrefix(List.of("blabla", "blabladsf", "blaBladsfdddd"), true));
	}

	@Test
	public void getLongestPrefix2()
	{
		assertEquals("bla", MiscUtilities.getLongestPrefix(List.of("blabla", "blabladsf", "blaBladsfdddd"), false));
	}

	@Test
	public void getLongestPrefix3()
	{
		assertEquals("blabla", MiscUtilities.getLongestPrefix(new String[]{"blabla", "blabladsf", "blaBladsfdddd"}, true));
	}

	@Test
	public void buildToVersion()
	{
		assertEquals("5.6pre1", MiscUtilities.buildToVersion("05.06.01.00"));
	}

	@Test
	public void isToolsJarAvailable()
	{
		assertTrue(MiscUtilities.isToolsJarAvailable());
	}

	private static int getOS() throws IllegalAccessException, NoSuchFieldException
	{
		Field os = OperatingSystem.class.getDeclaredField("os");
		os.setAccessible(true);
		int oldValue = os.getInt(OperatingSystem.class);
		return oldValue;
	}

	private static int updateOS(int newValue) throws IllegalAccessException, NoSuchFieldException
	{
		Field os = OperatingSystem.class.getDeclaredField("os");
		os.setAccessible(true);
		int oldValue = os.getInt(OperatingSystem.class);
		os.set(OperatingSystem.class, newValue);
		return oldValue;
	}
}