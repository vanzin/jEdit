#!/bin/sh

find . -name \*~ -exec rm {} \;
find . -name .\*~ -exec rm {} \;
find . -name \*.bak -exec rm {} \;
find . -name \*.orig -exec rm {} \;
find . -name \*.rej -exec rm {} \;
find . -name \#\*\# -exec rm {} \;
find . -name .\*.swp -exec rm {} \;
find org jars gnu com \( -name \*.class -a \! -name TextRenderer2D.class \) -exec rm {} \;
find . -name .\#\* -exec rm {} \;
find . -name .new\* -exec rm {} \;
find . -name .directory -exec rm {} \;
rm -f doc/users-guide/*.{aux,tex,log}
rm -f doc/users-guide/*.out
rm -f doc/users-guide/*.pdf
rm -f installer/jedit-*
