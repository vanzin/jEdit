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

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;

import static javax.tools.DocumentationTool.Location.DOCUMENTATION_OUTPUT;

public class GenerateTocXML extends StandardDoclet
{
	public static final String OUT = "toc.xml";
	public static final String HEADER = "<?xml version='1.0'?>\n<TOC>\n"
		+ "<ENTRY HREF='overview-summary.html'><TITLE>jEdit API Reference</TITLE>";
	public static final String FOOTER = "</ENTRY></TOC>\n";

	@Override
	public String getName()
	{
		return super.getName() + " with jEdit ToC XML";
	}

	@Override
	public boolean run(DocletEnvironment docEnv)
	{
		if (!super.run(docEnv))
		{
			return false;
		}
		try (Writer out = docEnv.getJavaFileManager()
				.getFileForOutput(DOCUMENTATION_OUTPUT, "", OUT, null)
				.openWriter())
		{
			out.write(HEADER);
			docEnv.getIncludedElements().stream()
					.filter(PackageElement.class::isInstance)
					.map(PackageElement.class::cast)
					.forEach(pkg -> processPackage(
							out,
							pkg,
							docEnv.getIncludedElements().stream()
									.filter(element -> pkg.equals(findPackage(element)))));
			out.write(FOOTER);
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	private static PackageElement findPackage(Element element)
	{
		Element result = element;
		while ((result != null) && !(result instanceof PackageElement))
		{
			result = result.getEnclosingElement();
		}
		return ((PackageElement) result);
	}

	private static void processPackage(Writer out, PackageElement pkg, Stream<? extends Element> includedPkgContents)
	{
		try
		{
			out.write("<ENTRY HREF='");
			String pkgName = pkg.getQualifiedName().toString();
			String pkgPath = pkgName.replace('.','/') + "/";
			out.write(pkgPath);
			out.write("package-summary.html'><TITLE>");
			out.write(pkgName);
			out.write("</TITLE>\n");
			includedPkgContents.filter(TypeElement.class::isInstance)
					.map(TypeElement.class::cast)
					.map(TypeElement::getQualifiedName)
					.map(Name::toString)
					.sorted()
					.forEach(className -> processClass(out, pkgPath, className.substring(pkgPath.length())));
			out.write("</ENTRY>");
		}
		catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}
	}

	private static void processClass(Writer out, String pkgPath, String className)
	{
		try
		{
			out.write("<ENTRY HREF='");
			out.write(pkgPath);
			out.write(className);
			out.write(".html'><TITLE>");
			out.write(className);
			out.write("</TITLE>\n");
			out.write("</ENTRY>");
		}
		catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}
	}
}
