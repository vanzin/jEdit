#!/bin/sh

# Creates the jEdit source tarball.

if [ "$1" = "" ]; then
  echo "Must specify a command line parameter."
  exit 1
fi

ant clean
rm -f doc/{FAQ,users-guide}/{toc.xml,word-index.xml,*.html}

cd
tar cvfz jedit${1}source.tar.gz `find jEdit -type f \! \( -name Entries \
	-o -name Root -o -name Entries.Static -o -name Repository \
	-o -name Baserev \
	-o -name \*.class -o -name \*.jar -o -name .\*.marks -o -name .xvpics \
	-o -name \*.exe -o -name \*.dll -o -name \*.dl_ \
	-o -name .cvsignore -o -name tags \)`
