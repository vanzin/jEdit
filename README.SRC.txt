COMPILING JEDIT FROM SOURCE

To compile jEdit, you will need:

- Jakarta Ant. I use version 1.5; older versions might or might not
  work. Get it from <http://jakarta.apache.org>.

- A Java compiler, such as Sun's javac or IBM's jikes.

- (Optional) To build the HTML version of the documentation:

  - DocBook-XML 4.1.2 DTD and DocBook-XSL 1.58.1 (or later) stylesheets
    (<http://docbook.sourceforge.net>).
  - An XSLT processor, such as Xalan (<http://xml.apache.org>) or
    xsltproc (<http://xmlsoft.org/XSLT/>).

- (Optional) To build the PDF version of the documentation:

  - DocBook-XML 4.1.2 DTD and DocBook-DSSSL 1.76 (or later) stylesheets
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

The Jikes compiler from IBM seems to have a problem where code compiled
against JDK 1.4 does not work under JDK 1.3. Sun's javac does not have
this problem.

If plan on running jEdit under both 1.3 and 1.4, I recommend you compile
it using javac.

* Libraries

jEdit depends on, and comes bundled with the following libraries:

- gnu.regexp by the Free Software Foundation. jEdit bundles the
  gnu.regexp 1.1.5 CVS snapshot.

  gnu.regexp is released under the 'GNU Lesser General Public License'.
  The gnu.regexp homepage is <http://www.cacas.org/java/gnu/regexp/>.

- AElfred XML parser by Microstar corporation. This class library is
  released under its own, non-GPL license, which reads as follows:

  "AElfred is free for both commercial and non-commercial use and
  redistribution, provided that Microstar's copyright and disclaimer are
  retained intact.  You are free to modify AElfred for your own use and
  to redistribute AElfred with your modifications, provided that the
  modifications are clearly documented."

  The AElfred home page is located at <http://www.microstar.com>.

- BeanShell scripting language, by Pat Niemeyer. jEdit bundles BeanShell
  version 1.2b6 with the bsh.util and bsh.classpath packages removed.

  BeanShell is released under a dual Sun Public License/GNU LGPL
  license. See the BeanShell homepage for details.

  The BeanShell homepage is located at <http://www.beanshell.org>.
