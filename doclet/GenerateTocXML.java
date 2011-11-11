/*
 * GenerateTocXML.java
 * Copyright (C) 1999, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package doclet;

import com.sun.javadoc.*;
import com.sun.tools.doclets.standard.Standard;

import java.io.*;
import java.util.Arrays;

public class GenerateTocXML
{
	public static final String OUT = "toc.xml";
	public static final String HEADER = "<?xml version='1.0'?>\n<TOC>\n"
		+ "<ENTRY HREF='overview-summary.html'><TITLE>jEdit API Reference</TITLE>";
	public static final String FOOTER = "</ENTRY></TOC>\n";

	public static boolean start(RootDoc root)
	{
		if (!Standard.start(root))
		{
			return false;
		}
		try
		{
			String destDirName = null;
			for (String[] option : root.options()) {
				if ("-d".equals(option[0].toLowerCase())) {
					destDirName = option[1];
					break;
				}
			}
			FileWriter out = new FileWriter(new File(destDirName, OUT));
			out.write(HEADER);

			PackageDoc[] packages = root.specifiedPackages();
			for(int i = 0; i < packages.length; ++i)
			{
				processPackage(out,packages[i]);
			}

			out.write(FOOTER);
			out.close();

			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static int optionLength(String option)
	{
		return Standard.optionLength(option);
	}

	public static boolean validOptions(String[][] options, DocErrorReporter reporter)
	{
		return Standard.validOptions(options,reporter);
	}

	public static LanguageVersion languageVersion()
	{
		return Standard.languageVersion();
	}

	private static void processPackage(Writer out, PackageDoc pkg)
		throws IOException
	{
		out.write("<ENTRY HREF='");
		String pkgPath = pkg.name().replace('.','/') + "/";
		out.write(pkgPath);
		out.write("package-summary.html'><TITLE>");
		out.write(pkg.name());
		out.write("</TITLE>\n");

		ClassDoc[] classes = pkg.allClasses();
		String[] classNames = new String[classes.length];
		for(int i = 0; i < classes.length; i++)
		{
			classNames[i] = classes[i].name();
		}
		Arrays.sort(classNames);

		for(int i = 0; i < classes.length; i++)
		{
			processClass(out,pkgPath,classNames[i]);
		}

		out.write("</ENTRY>");
	}

	private static void processClass(Writer out, String pkgPath, String clazz)
		throws IOException
	{
		out.write("<ENTRY HREF='");
		out.write(pkgPath);
		out.write(clazz);
		out.write(".html'><TITLE>");
		out.write(clazz);
		out.write("</TITLE>\n");
		out.write("</ENTRY>");
	}
}
