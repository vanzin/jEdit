#!/bin/sh

# Creates the jEdit source tarball.

if [ "$1" = "" ]; then
  echo "Must specify a command line parameter."
  exit 1
fi

sh clean.sh
rm -f doc/users-guide/*.html
rm -f doc/users-guide/toc.xml

cd ..
tar cvfz jedit${1}source.tar.gz `find jEdit -type f \! \( -name Entries \
	-o -name Root -o -name Entries.Static -o -name Repository \
	-o -name \*.class -o -name \*.jar -o -name .\*.marks -o -name .xvpics \
	-o -name \*.exe -o -name \*.dll -o -name \*.dl_ \
	-o -name \*.spec \)`
