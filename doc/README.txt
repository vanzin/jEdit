JEDIT 3.2 README

* Contents

- About jEdit
- License
- jEdit on the Internet
- Common problems
- Problems that occur with older Java versions
- Libraries
- Credits

* About jEdit

jEdit is a cross platform programmer's text editor written in Java.

jEdit requires Java 2 (or Java 1.1 with Swing 1.1). The recommended Java
version for running jEdit is Java 2 version 1.3.

jEdit comes with full online help; to read it, select 'jEdit Help' from
jEdit's 'Help' menu.

A PDF (Adobe Acrobat) version of the user's guide be downloaded from
<http://www.jedit.org>.

* License

jEdit is free software, and you are welcome to redistribute it under the
terms of the GNU General Public License (either version 2 or any later
version, at the user's election). See the file COPYING.txt for details.

A number of plugins are available for jEdit. Unless otherwise stated in
the plugin's documentation, each of the plugins is licensed for use and
redistribution under the terms of the GNU General Public License (either
version 2 or any later version, at the user's election).

The user's guide is released under the terms of the GNU Free
Documentation License, Version 1.1 or any later version published by the
Free Software Foundation; with no "Invariant Sections", "Front-Cover
Texts" or "Back-Cover Texts", each as defined in the license. A copy of
the license can be found in the file COPYING.DOC.txt.

The class libraries shipped with jEdit (gnu.regexp, AElfred, BeanShell)
each have their own license; see the 'Libraries' section below.

* jEdit on the Internet

The jEdit homepage, located at <http://www.jedit.org> contains the
latest version of jEdit, along with plugin downloads.

There are three mailing lists dedicated to jEdit; for details, visit
<http://www.jedit.org/index.php?page=lists>. The mailing lists are the
preferred place to post feature suggestions.

If you would like to report a bug, first read the `Common Problems'
section below. If that doesn't answer your question, report a bug with
our bug tracker, located at <http://www.jedit.org/index.php?page=bugs>.

When writing a bug report, please try to be as specific as possible. You
should specify your jEdit version, Java version, operating system, any
relevant output from the activity log, and an e-mail address, in case we
need further information from you to fix the bug.

The 'Make Bug Report' macro included with jEdit, which can be found in
Macros->Misc, might be useful when preparing a bug report.

If you would like to discuss the BeanShell scripting language,
subscribe to one of the BeanShell mailing lists by visiting
<http://www.beanshell.org/contact.html>.

You may also contact me directly by e-mailing <slava@jedit.org>.

* Common problems

Before reporting a problem with jEdit, please make sure it is not
actually a Java bug, or a well-known problem:

- If you get an OutOfMemoryError while editing a large file, even if
  your computer has a large amount of RAM present, increase the Java
  virtual machine heap size.

  - On Windows, run "Set jEdit Parameters" from the "jEdit" group in the
    Programs menu. Then, in the resulting dialog box, under "Command
    line options for Java executable", change the option that looks like
    so:

    -mx32m

  - On Unix, edit the `jedit' shell script and change the line that
    looks like so:

    JAVA_HEAP_SIZE=32

  In both cases, replace `32' with the desired heap size, in megabytes.
  For best results, use a heap size of about 2.5 times the largest file
  size you plan to edit.

- Printing doesn't work very well, especially on Java 2. There isn't
  much I can do about this until Sun fixes several outstanding bugs in
  Java.

- With Java versions older than 1.4 on Unix, you might not be able to
  copy and paste between jEdit and other programs. This is mainly
  because X Windows defines two clipboards, CLIPBOARD and PRIMARY.
  Older Java versions can only access the CLIPBOARD, but many X Windows
  programs only use PRIMARY.

- If you experience window positioning problems when running on Unix,
  try using a different window manager or Java version.

- International keyboards, input methods, composed keys, etc. might not
  work properly. As I do not have an international keyboard, this will
  likely remain unfixed until someone submits the necessary code.

- Anti-aliased text might not display correctly with some Java versions.
  If you enabled anti-aliasing and are seeing problems such as text
  being drawn with the wrong font style, try using a different Java
  version or disable anti-aliasing.

- Aborting I/O operations in the I/O Progress Monitor doesn't always
  work.

- Edit modes you write will not be available in the editor until you add
  an entry to the `catalog' file in the appropriate modes directory.

- The Swing HTML component used by jEdit's help viewer is very buggy.
  Although the jEdit online help works around many of the bugs, it still
  renders some HTML incorrectly and runs very slowly.

- Because jEdit is written in Java, it will always be slower than a
  native application. For best performance, use a recent Java version,
  such as Java 2 version 1.3.

* Problems that occur with older Java versions

- If you are having problems such as Alt-key mnemonics not working, or
  keystrokes inserting garbage into the text area, make sure you are
  running the very latest Java version for your platform. Some older
  Java versions, especially on Linux, had buggy key handling.

- If you are using Java 1.1 and get a `ClassNotFoundException:
  javax/swing/JWindow' or similar exception when starting jEdit,
  chances are you don't have Swing installed properly. Download Swing
  from <http://java.sun.com/products/jfc>. Alternatively, upgrade to
  Java 2, which doesn't require you to install Swing separately.

- The following three source files will not compile under Java 1.1:

  bsh/XThis.java
  bsh/reflect/ReflectManagerImpl.java
  org/gjt/sp/jedit/textarea/TextRenderer2D.java

  These files use Java 2-specific features, and are only used when
  running on Java 2. You can safely delete or ignore them if you only
  plan on running jEdit in Java 1.1.

* Libraries

jEdit depends on, and comes bundled with the following libraries:

- gnu.regexp by the Free Software Foundation. jEdit bundles gnu.regexp
  1.1.3, with the following minor change:

  - REMatch.getSubCount() method added

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
  version 1.2beta1, with the bsh.util package removed.

  BeanShell is released under a dual Sun Public License/GNU LGPL
  license. See the BeanShell homepage for details.

  The BeanShell homepage is located at <http://www.beanshell.org>.

- The Sun Java look and Feel icon collection. The license may be found
  in the ICONS.LICENSE.txt file.

* Credits

The following people contributed to the jEdit core:

Alwyn Barry:        - Ada95 edit mode
Andre Kaplan:       - ASP, JavaScript, VBScript, BCEL edit modes
                    - Syntax token background highlighting
Artur Biesiadowski: - Eiffel edit mode
Bruno Tatibouet:    - B formal method edit mode
Dante Fabrizio:     - VHDL edit mode
Denis Lambot:       - Selecting files from the keyboard in the file
                      system browser
Dirk Moebius:       - HTTP firewall plugin
                    - Improved shortcuts option pane
		    - Improved history text fields
                    - EditorExitRequested message code
                    - VariableGridLayout code
                    - Show Free Memory macro
Dominic Stolerman:  - ML edit mode
Ed Stewart:         - PV-WAVE edit mode
Francesc Roses:     - Duplicate Line macro
Gili Tzabari:       - OS/2 start script
Ian Maclean:        - Ruby edit mode
Jason Ginchereau:   - Portions of QuickFile plugin are used in file
                      system browser
Jerry McBride:      - NetRexx edit mode
John Gellene:       - 'Writing macros' section of user's guide
                    - 'Writing plugins' section of user's guide
                    - jEditLauncher for Windows
                    - A large number of sample macros
                    - ... and more
Jonathan Revusky:   - Python edit mode
Juha Lindfors:      - IDL edit mode
Kris Kopicki:       - Objective C syntax highlighting
Kristian Ovaska:    - Intel x86 Assembly, Haskell, Pascal edit modes
Lionel Fiol:        - Omnimark edit mode
Marc Rummel:        - MIPS R2000 Assembly edit mode
Matt Albrecht:      - Inform edit mode
Matt Price:         - Visual Foxpro edit mode
Matthias Schneider: - AWK, COBOL edit modes
Michael Jacobson:   - TCL edit mode
Mike Dillon:        - Syntax highlighting engine ("XMode")
                    - Gutter
                    - New options dialog box
                    - Faster literal search
                    - ColdFusion, CSS, HTML, Java, JSP, LISP, Patch,
                      PHP, SHTML, TSQL, XML, XSL edit modes
                    - http://plugins.jedit.org web master
                    - ... and more
Nitsan Vardi:       - VHDL edit mode
Ollie Rutherfurd:   - C# edit mode
                    - Default set of XSL abbreviations
                    - Some commands in View->Scrolling menu
                    - ... and more
Patrick Tingen:     - Progress 4GL edit mode
Peter Graves:       - Code for preserving file permissions on Unix was
                      taken from Peter's j text editor
                      (http://www.armedbear.org)
Ralf Engels:        - PostScript edit mode
Richard Ashwell:    - SQR edit mode
Rick Owen:          - PL-SQL edit mode
Romain Guy:         - POVRay edit mode
Richard Beton:      - Occam edit mode
Saimon Moore:       - Velocity edit mode
Tal Davidson:       - First implementation of syntax highlighting engine
Tom Locke:          - Reverse search
Will Sargent:       - JHTML edit mode
                    - RelationalView edit mode
Wojciech Stryjewski:- Verilog edit mode

In addition to the above people, I would like to thank the plugin
developers for their plugins, and the users for their feedback and
comments.
