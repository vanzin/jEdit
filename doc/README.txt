JEDIT 4.0 README

* Contents

- About jEdit
- License
- jEdit on the Internet
- Common problems
- Libraries

* About jEdit

jEdit is a cross platform programmer's text editor written in Java.

jEdit requires Java 2 version 1.3. It should also work with version 1.4,
but has not been tested as extensively with that version.

jEdit comes with full online help; to read it, select 'jEdit Help' from
jEdit's 'Help' menu.

A PDF (Adobe Acrobat) version of the user's guide be downloaded from
<http://www.jedit.org>.

A list of people who contributed to jEdit, either by submitting code or
edit modes to the core, or writing plugins can be viewed by selecting
'About jEdit' from jEdit's 'Help' menu.

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
latest version of jEdit, along with plugin downloads. There is also a
user-oriented site, <http://community.jedit.org>. Check it out.

There are several mailing lists dedicated to jEdit; for details, visit
<http://www.jedit.org/?page=lists>. The mailing lists are the preferred
place to post feature suggestions, questions, and the like.

If you would like to report a bug, first read the `Common Problems'
section below. If that doesn't answer your question, report a bug with
our bug tracker, located at <http://www.jedit.org/?page=bugs>.

When writing a bug report, please try to be as specific as possible. You
should specify your jEdit version, Java version, operating system, any
relevant output from the activity log, and an e-mail address, in case we
need further information from you to fix the bug.

The 'Make Bug Report' macro included with jEdit, which can be found in
the 'Misc' submenu of the 'Macros' menu, might be useful when preparing
a bug report.

If you would like to discuss the BeanShell scripting language,
subscribe to one of the BeanShell mailing lists by visiting
<http://www.beanshell.org/contact.html>.

You may also contact me directly by e-mailing <slava@jedit.org>.

Finally, if you want to chat about jEdit with other users and
developers, come join the #jedit channel on irc.openprojects.net. You
can use the IRC plugin, available from http://plugins.jedit.org, for
this purpose.

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

- Printing doesn't work very well on some Java implementations. There
  isn't much I can do about this until Sun fixes several outstanding
  bugs in Java.

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

- The Swing HTML component used by jEdit's help viewer is very buggy.
  Although the jEdit online help works around many of the bugs, it still
  renders some HTML incorrectly and runs very slowly.

- Because jEdit is written in Java, it will always be slower than a
  native application. For best performance, use a recent Java version,
  such as Java 2 version 1.4.

* Libraries

jEdit depends on, and comes bundled with the following libraries:

- gnu.regexp by the Free Software Foundation. jEdit bundles gnu.regexp
  1.1.4.

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
  version 1.2b3 with the bsh.util and bsh.classpath packages removed.

  BeanShell is released under a dual Sun Public License/GNU LGPL
  license. See the BeanShell homepage for details.

  The BeanShell homepage is located at <http://www.beanshell.org>.

- The Sun Java look and Feel icon collection. The license may be found
  in the ICONS.LICENSE.txt file.
