COMPILING JEDIT FROM SOURCE

To compile jEdit, you will need:

- Jakarta Ant. I use version 1.4; older versions might or might not
  work. Get it from <http://jakarta.apache.org>.

- A Java compiler, such as Sun's javac or IBM's jikes.

- (Optional) To build the HTML version of the documentation:

  - DocBook-XML 1.4.3 DTD and DocBook-XSL 1.48 (or later) stylesheets
    (<http://docbook.sourceforge.net>).
  - An XSLT processor, such as Xalan (<http://xml.apache.org>) or
    xsltproc (<http://xmlsoft.org/XSLT/>).

- (Optional) To build the PDF version of the documentation:

  - DocBook-XML 1.4.3 DTD and DocBook-DSSSL 1.74b (or later) stylesheets
    (<http://docbook.sourceforge.net>).

  - OpenJade 1.3 and OpenSP 1.3.4 (or later)
    (<http://openjade.sourceforge.net>).

  - A TeX implementation that includes PDF output capability.

Once you have all the necessary tools installed, run the 'dist' target
in the build.xml file to compile jEdit. If you want to build the docs,
first edit the `build.properties' file to specify the path to where the
DocBook XSL stylesheets are installed, then run the 'docs-html-xalan' or
'docs-html-xsltproc' target.

* A note about JDK versions

If plan on running jEdit under both 1.3 and 1.4, read the below
information!

When running under JDK 1.4, jEdit needs to load a few workarounds for
keyboard shortcuts to work properly; those are found in the source file
Java14.java. Since this file uses JDK 1.4 APIs, it is not compiled by
default if Ant detects that JDK 1.3 is being used.

Furthermore, when compiled with JDK 1.4, jEdit does not run under 1.3
because Sun made some backwards-incompatible changes in the ABI (ABI,
not API).

Therefore, when compiled under 1.3, jEdit only runs in 1.3, and a when
compiled under 1.4 it only runs in 1.4.

You may notice that the binaries I release work with both Java versions;
what I do is compile most of the sources under 1.3, then use 1.4 to
compile Java14.java.
