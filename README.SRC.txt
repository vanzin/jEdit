SOURCE CODE NOTES

This file only contains information about compiling from source.
General jEdit documentation can be found in the 'doc' directory.

* Compiling jEdit

You will need:

- A Java compiler, such as Sun's javac or IBM's jikes.

- Jakarta Ant. I use version 1.5; older versions might or might not
  work. Get it from <http://jakarta.apache.org>.

Once you have all the necessary tools installed, run Ant with the 'dist'
target in the build.xml file to compile jEdit:

$ cd ~/jEdit
$ ant dist

* Building the online help in HTML format

You will need:

- DocBook-XML 4.1.2 DTD and DocBook-XSL 1.60.1 (or later) stylesheets
  (<http://docbook.sourceforge.net>).
- An XSLT processor, such as Xalan (<http://xml.apache.org>) or
  xsltproc (<http://xmlsoft.org/XSLT/>).

To build the HTML format docs, first edit the 'build.properties' file to
specify the path to where the DocBook XSL stylesheets are installed,
then run the 'docs-html-xalan' or 'docs-html-xsltproc' target.

* Building the online help in PDF format

You will need:

- DocBook-XML 4.1.2 DTD and DocBook-DSSSL 1.76 (or later) stylesheets
  (<http://docbook.sourceforge.net>).

- OpenJade 1.3 and OpenSP 1.3.4 (or later)
  (<http://openjade.sourceforge.net>).

- A TeX implementation that includes PDF output capability.

To build the PDF format docs, you will most likely have to edit the
build.xml to set up various paths. Then run the 'docs-pdf-openjade-a4'
or 'docs-pdf-openjade-letter' target, depending on the desired paper
size.

* Building the API documentation

To build the API documentation, you will need Sun's javadoc, which is
bundled with the JDK. You will also need to edit 'build.properties' to
point to the location of your 'tools.jar', which is needed to compile a
custom doclet. After everything is set up, run the 'javadoc' target,
which will create API documentation and a 'toc.xml' file for jEdit's
help viewer.

Note that you will need to run the 'doclet/Clean_Up_Javadoc.bsh' script
in jEdit to make the API documentation display better in jEdit's help
viewer.

* A note about JDK versions

The Jikes compiler from IBM seems to have a problem where code compiled
against JDK 1.4 does not work under JDK 1.3. Sun's javac does not have
this problem.

If plan on running jEdit under both 1.3 and 1.4, I recommend you compile
it using javac.
