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

import java.io.*;

/**
 * This is very much of a hack.
 */
public class GenerateTocXML
{
	public static final String PATH = "doc/api/";
	public static final String OUT = PATH + "toc.xml";
	public static final String HEADER = "<?xml version='1.0'?>\n<TOC>\n"
		+ "<ENTRY HREF='overview-summary.html'><TITLE>jEdit API Reference</TITLE>";
	public static final String FOOTER = "</ENTRY></TOC>\n";

	public static boolean start(RootDoc root)
	{
		try
		{
			FileWriter out = new FileWriter(OUT);
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

		for(int i = 0; i < classes.length; i++)
		{
			processClass(out,pkgPath,classes[i]);
		}

		out.write("</ENTRY>");
	}

	private static void processClass(Writer out, String pkgPath, ClassDoc clazz)
		throws IOException
	{
		out.write("<ENTRY HREF='");
		out.write(pkgPath);
		out.write(clazz.name());
		out.write(".html'><TITLE>");
		out.write(clazz.name());
		out.write("</TITLE>\n");
		out.write("</ENTRY>");
	}
}

