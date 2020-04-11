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

import javax.swing.text.Segment;

import static org.junit.Assert.*;

public class StandardUtilitiesTest
{
	private Segment emptySegment;

	@Before
	public void setUp() throws Exception
	{
		emptySegment = new Segment(new char[0],0, 0);
	}

	@Test
	public void charsToEscapes()
	{
		assertEquals("ab\\ncc\\taa\\\\\\\\rr\\\"gg\\'", StandardUtilities.charsToEscapes("ab\ncc\taa\\\\rr\"gg'"));
	}

	@Test
	public void charsToEscapes2()
	{
		assertEquals("\\ab\\a\\a\\c", StandardUtilities.charsToEscapes("abaac","ac"));
	}

	@Test
	public void getIndentString()
	{
		assertEquals("\t  \t\t   ", StandardUtilities.getIndentString("\t  \t\t   a"));
	}

	@Test
	public void getLeadingWhiteSpace()
	{
		assertEquals(8, StandardUtilities.getLeadingWhiteSpace("\t  \t\t   a"));
	}

	@Test
	public void getLeadingWhiteSpaceWidth1()
	{
		assertEquals(8, StandardUtilities.getLeadingWhiteSpaceWidth("\t  \t\t   a", 1));
	}

	@Test
	public void getLeadingWhiteSpaceWidth3()
	{
		assertEquals(12, StandardUtilities.getLeadingWhiteSpaceWidth("\t  \t\t   ta", 3));
	}

	@Test
	public void getTrailingWhiteSpace()
	{
		assertEquals(7, StandardUtilities.getTrailingWhiteSpace("\t  \t\t   a\t  \t\t  "));
	}

	@Test
	public void createWhiteSpace0()
	{
		assertEquals("       ", StandardUtilities.createWhiteSpace(7,0));
	}

	@Test
	public void createWhiteSpace01()
	{
		assertEquals(" ", StandardUtilities.createWhiteSpace(1,1));
	}

	@Test
	public void createWhiteSpace1()
	{
		assertEquals("\t\t\t\t\t\t\t", StandardUtilities.createWhiteSpace(7,1));
	}

	@Test
	public void createWhiteSpace4()
	{
		assertEquals("\t   ", StandardUtilities.createWhiteSpace(7,4));
	}

	@Test
	public void getVirtualWidth()
	{
		String str = "\t  \t\t   ta";
		Segment seg = new Segment(str.toCharArray(),0, str.length());
		assertEquals(17, StandardUtilities.getVirtualWidth(seg,4));
	}

	@Test
	public void getVirtualWidthEmptySegment()
	{
		assertEquals(0, StandardUtilities.getVirtualWidth(emptySegment,4));
	}

	@Test
	public void getOffsetOfVirtualColumn()
	{
		String str = "\t  \t\t   ta";
		Segment seg = new Segment(str.toCharArray(),0, str.length());
		assertEquals(3, StandardUtilities.getOffsetOfVirtualColumn(seg,1, 3, null));
	}

	@Test
	public void getOffsetOfVirtualColumn2()
	{
		String str = "\t  \t\t   ta";
		Segment seg = new Segment(str.toCharArray(),0, str.length());
		assertEquals(1, StandardUtilities.getOffsetOfVirtualColumn(seg,4, 3, null));
	}

	@Test
	public void getOffsetOfVirtualColumn3()
	{
		String str = "\t  \t\t   ta";
		Segment seg = new Segment(str.toCharArray(),0, str.length());
		assertEquals(1, StandardUtilities.getOffsetOfVirtualColumn(seg,4, 4, null));
	}

	@Test
	public void compareStrings()
	{
		assertEquals(0, StandardUtilities.compareStrings(null, null, false));
	}

	@Test
	public void compareStrings2()
	{
		assertEquals(-1, StandardUtilities.compareStrings(null, "null", false));
	}

	@Test
	public void compareStrings3()
	{
		assertEquals(1, StandardUtilities.compareStrings("null", null, false));
	}

	@Test
	public void compareStrings4()
	{
		assertEquals(0, StandardUtilities.compareStrings("null", "null", false));
	}

	@Test
	public void compareStrings5()
	{
		assertEquals(0, StandardUtilities.compareStrings("null", "nuLl", true));
	}

	@Test
	public void compareStrings6()
	{
		assertEquals(32, StandardUtilities.compareStrings("null", "nuLl", false));
	}

	@Test
	public void compareStrings7()
	{
		assertEquals(-13, StandardUtilities.compareStrings("anull", "nuLl", true));
	}

	@Test
	public void compareStringsDigit1()
	{
		assertEquals(-1, StandardUtilities.compareStrings("0123", "01123", true));
	}

	@Test
	public void globToRE()
	{
		assertEquals("(aa|bb)", StandardUtilities.globToRE("{aa,bb}"));
	}

	@Test
	public void globToRE2()
	{
		assertEquals("(aa|bb)", StandardUtilities.globToRE("(re)(aa|bb)"));
	}

	@Test
	public void globToRE3()
	{
		assertEquals("(CHANGELOG|CHANGES|INSTALL|LICENSE|NEWS|README|TODO)(|\\.txt)", StandardUtilities.globToRE("{CHANGELOG,CHANGES,INSTALL,LICENSE,NEWS,README,TODO}{,.txt}"));
	}

	@Test
	public void globToRE4()
	{
		assertEquals(".*\\.(gz|jar|zip|tgz|z|war|ear)", StandardUtilities.globToRE("*.{gz,jar,zip,tgz,z,war,ear}"));
	}

	@Test
	public void globToRE5()
	{
		assertEquals("(CVS|#.*|.*~|\\\\\\..*)", StandardUtilities.globToRE("{CVS,#*,*~,\\\\.*}"));
	}

	@Test
	public void globToRE6()
	{
		assertEquals("CVS.", StandardUtilities.globToRE("CVS?"));
	}

	@Test
	public void globToRE7()
	{
		assertEquals("CVS|SVN", StandardUtilities.globToRE("CVS|SVN"));
	}

	@Test
	public void regionMatches()
	{
		assertTrue(StandardUtilities.regionMatches("CVS|SVN", 4, "SVN", 0, 3));
	}

	@Test
	public void regionMatches2()
	{
		assertFalse(StandardUtilities.regionMatches("CVS|SVN", -1, "SVN", 0, 3));
	}

	@Test
	public void startsWith()
	{
		assertTrue(StandardUtilities.startsWith("CVS|SVN", "CVS"));
	}

	@Test
	public void startsWith2()
	{
		assertFalse(StandardUtilities.startsWith("CVS|SVN", "SVN"));
	}

	@Test
	public void getBoolean()
	{
		assertTrue(StandardUtilities.getBoolean("true", false));
	}

	@Test
	public void getBoolean2()
	{
		assertFalse(StandardUtilities.getBoolean(null, false));
	}

	@Test
	public void getBoolean3()
	{
		assertTrue(StandardUtilities.getBoolean(Boolean.TRUE, false));
	}

	@Test
	public void getBoolean4()
	{
		assertTrue(StandardUtilities.getBoolean("yes", false));
	}

	@Test
	public void getBoolean5()
	{
		assertTrue(StandardUtilities.getBoolean("on", false));
	}

	@Test
	public void getBoolean6()
	{
		assertFalse(StandardUtilities.getBoolean("false", true));
	}

	@Test
	public void getBoolean7()
	{
		assertFalse(StandardUtilities.getBoolean("no", true));
	}

	@Test
	public void getBoolean8()
	{
		assertFalse(StandardUtilities.getBoolean("off", true));
	}

	@Test
	public void getBoolean9()
	{
		assertFalse(StandardUtilities.getBoolean("bla", false));
	}

	@Test
	public void formatFileSize()
	{
		assertEquals("345 Bytes", StandardUtilities.formatFileSize(345));
	}

	@Test
	public void formatFileSize2()
	{
		assertEquals("1000 Bytes", StandardUtilities.formatFileSize(1000));
	}

	@Test
	public void formatFileSize3()
	{
		assertEquals("1 kB", StandardUtilities.formatFileSize(1024));
	}

	@Test
	public void formatFileSize4()
	{
		assertEquals("1 kB", StandardUtilities.formatFileSize(1025));
	}

	@Test
	public void formatFileSize5()
	{
		char decimalSeparator = StandardUtilities.MB_FORMAT.getDecimalFormatSymbols().getDecimalSeparator();
		assertEquals("9"+decimalSeparator+"7 MB", StandardUtilities.formatFileSize(10201012));
	}
}