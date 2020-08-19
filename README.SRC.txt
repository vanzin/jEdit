SOURCE CODE NOTES

This file only contains information about compiling from source.
Scroll to the bottom if you are trying to use an IDE like Eclipse.
General jEdit documentation can be found in the 'doc' directory.

* Requirements

 For all tasks:

  - Apache Ant. Get it from <http://ant.apache.org>.
    1.9.10 or newer has to be used.
  - The Ant Optional package. This is included in the download from ants website,
    as well as the AntPlugin from the Plugin Manager,
    but not in some default installs on various *nix systems.
    There you should get the ant-optional package through your
    package management system.

 For building jEdit, the API documentation or any of the
 distribution files except of the source package:

  - A Java compiler of version 11, such as OpenJDK's javac which is
    included in the JDK. Get it from <http://www.java.com/download>
    or from your package manager - e. g. openjdk-11-jdk.

 For building the API documentation:

  - Sun's javadoc tool, which is included in the JDK.
    Get it from <http://www.java.com/download> or from your package
    manager - e. g. openjdk-11-jdk.

 For building the windows installer (for the final step):

  - Inno Setup 6.0.0 or newer. Get it from <http://www.jrsoftware.org/isdl.php>
  - A box running windows or wine, e. g. on *nix. If Inno Setup should be
    run via wine, a wine version of at least 1.3.10 is required because
    of a bug in earlier wine versions.

 For building the OS X disk image (DMG) for easy distribution
 (for the final step):

  - A box running OS X


* Tasks

 If all necessary tools are installed and set up, you can use ant with a couple of targets.
 The default target if you just run "ant" is "build", like running "ant build".

 - retrieve                retrieve the dependencies
  retrieves almost all dependencies needed for building jEdit, only InnoSetup for the
  windows installer and wine, if InnoSetup should be run on *nix or OS X, have to
  be installed and configured in build.properties manually.

 - build                   build the jEdit JAR-file with full debug-information
  builds jEdit in the build-folder, configured in build.properties,
  with full debug information included.

 - build-exe-launcher      build the EXE launcher
  builds the EXE launcher in the build-folder, configured in build.properties.

 - run                     run jEdit
  runs the jEdit-version in the build-folder, configured in build.properties.
  If there isn't any, it builds it in front of execution.

 - run-debug               run jEdit with debug listening enabled
  runs the jEdit-version in the build-folder, configured in build.properties.
  If there isn't any, it builds it in front of execution.
  The debug listening for this JVM is enabled.

 - test                    run unit tests
  runs the available unit tests.

 - docs-html               generate HTML docs
  builds the online help in HTML-format in the build-folder, configured in build.properties.

 - docs-javadoc            generate JavaDoc API docs
  builds the API documentation in the build-folder, configured in build.properties.

 - docs-pdf-USletter       generate PDF users-guide with US letter paper size
  builds the User's Guide in PDF-format with US letter page size
  in the build-folder, configured in build.properties.

 - docs-pdf-a4             generate PDF users-guide with A4 paper size
  builds the User's Guide in PDF-format with A4 page size
  in the build-folder, configured in build.properties.

 - dist                    build all distribution files
  builds all distribution files or prepares the final step for some of them (Win and Mac)
  in the dist-folder, configured in build.properties.

 - dist-deb                build the DEB Package
  builds the DEB Debian package in the dist-folder, configured in build.properties.

 - dist-sign-deb-Release   sign the DEB Release file
  signs the Debian repository metadata

 - dist-java               build the Java-installer
  builds the Java installer in the dist-folder, configured in build.properties.

 - dist-mac                build the OS X disk image (DMG-file)
  builds the OS X internet-enabled disk image (DMG-file) if building on a box
  running OS X. If building on a box running something else, there will be a file
  called jedit<version_here>-dist-mac-finish.tar.bz2 in the dist-folder,
  configured in build.properties. Give that to someone running OS X and ask him
  to extract the archive and to execute "ant dist-mac-finish".
  The only thing that needs to be installed for this final step is Apache Ant.

 - dist-mac-finish         finish building the OS X disk image (DMG-file) on OS X
  builds the OS X internet-enabled disk image (DMG-file) in the dist-folder,
  configured in build.properties if building on a box running OS X.
  This target is normally only run directly, if someone just has to do
  the final step that was prepared by "dist-mac" or "dist".

 - dist-manuals            build the PDF-manuals
  builds the User's Guide in PDF-format with both, USletter and A4 page size
  in the dist-folder, configured in build.properties.

 - dist-slackware          build the Slackware Package
  builds the Slackware TGZ package in the dist-folder, configured in build.properties.

 - dist-src                build the src-archive
  builds the source package in the dist-folder, configured in build.properties.

 - dist-win                build the Windows installer (EXE-file)
  builds the windows installer in the dist-folder, configured in build.properties,
  on a box running Windows. If building on a box running something else, there will be
  a file called jedit<version_here>-dist-win-finish.tar.bz2 in the dist-folder,
  configured in build.properties. Give that to someone running Windows and ask him
  to extract the archive and to execute "ant dist-win-finish".
  The only things that need to be installed for this final step is Apache Ant
  and Inno Setup. Prior to running "ant dist-win-finish", the helper has to set
  up the build.properties file with the path to his Inno Setup installation.

 - dist-win-finish         finish building the Windows installer (EXE-file) on Windows or via wine
  build the windows installer in the dist-folder, configured in build.properties if
  building on a box running Windows or via wine. This target is normally only run directly, if
  someone just has to do the final step that was prepared by "dist-win" or "dist".

 - clean                   clean up intermediate files
  cleans up the temporary files from the build- and dist-folder, configured in build.properties.
  Leaves the runnable jEdit, and the distribution files in place.

 - clean-all               clean up lib.dir, build.dir and dist.dir completely
  cleans up all files from the lib-, build- and dist-folder, configured in build.properties,
  and the folders itself too.



* Interesting algorithms and tricks

 - org.gjt.sp.jedit.browser.VFSDirectoryEntryTable: a tree table control.

 - org.gjt.sp.jedit.buffer.LineManager: the "gap" optimization allows
   update operations to be performed in O(1) time in certain
   circumstances.

 - org.gjt.sp.jedit.buffer.KillRing: uses a hash to speed up comparisons
   with sets of strings.

 - org.gjt.sp.jedit.search.BoyerMooreSearchMatcher: fast text search.

 - org.gjt.sp.jedit.syntax.TokenMarker: generic tokenizer driven by rules
   defined in an XML file.

 - org.gjt.sp.jedit.textarea.DisplayManager: the fold visibility map
   looks like an RLE-compressed bit set but does lookups in O(log n).

 - org.gjt.sp.util.WorkThreadPool: a pool of threads executing requests
   from a queue, enforcing various concurrency requirements.



* Tips for Eclipse/NetBeans/IDE users:

A file "jsr305.jar" contains definitions of the annotations
used in jEdit source code. It is downloaded automatically by ivy
as part of the ant build process, to your lib/compile subfolder.
If you add that jar to the
project properties - java build path - libraries, that will
get rid of the compiler errors on the annotations.

A file "tools.jar" from the JDK is also needed in your
java build path.

The ant build process creates a subfolder called "build" for its work.
Eclipse does the same and may pick the same folder.
It is recommended you check/ensure that a different build
directory ("Default Ouptut Folder") for Eclipse is used.

Some of the source directories are only needed for building packages on
certain platforms. If you add jEdit source to an IDE like Eclipse that tries
to compile every .java file, you'll get some errors from these directories unless
you have the right libraries, which are automatically downloaded by ivy.
However, if you tell Eclipse to exclude these directories,
you can still build and run/debug jEdit from source. Follow these steps:

 - Project properties - Java build path
 - Source - Excluded dirs - Edit - Add multiple
 - Add these subdirs: net, de, build, test, misc
 - Default output folder: /build-eclipse

The "eclipse-formatting.xml" file can be used to set the
code format style to be the same as what is currently used here.
