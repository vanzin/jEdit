#!/bin/sh

function print_size() {
	echo -n "$1: "
	ls -l `cat installer/$1` | awk 'BEGIN { size=0 } { disk_size+=(int($5/4096+1)*4); size+=$5/1024 } END { print disk_size " " size }'
}

# This script must be run from the jEdit directory, *not* the installer
# directory!!!

# jedit-program fileset
echo jedit.jar > installer/jedit-program
echo jars/LatestVersion.jar >> installer/jedit-program
echo jars/QuickNotepad.jar >> installer/jedit-program
echo properties/README.txt >> installer/jedit-program
echo startup/README.txt >> installer/jedit-program
find modes -name \*.xml >> installer/jedit-program
echo modes/catalog >> installer/jedit-program
find doc \( -name \*.txt -o -name \*.png \) >> installer/jedit-program
find doc/users-guide doc/FAQ \( -name \*.html -o -name toc.xml \) >> installer/jedit-program

print_size jedit-program

# jedit-macros fileset
find macros -name \*.bsh > installer/jedit-macros

print_size jedit-macros

# jedit-api fileset
find doc/api \( -name \*.html -o -name toc.xml \) > installer/jedit-api

print_size jedit-api

# jedit-windows fileset
echo jeshlstb.dl_ > installer/jedit-windows
echo ltslog.dll >> installer/jedit-windows
echo jeditsrv.exe >> installer/jedit-windows
echo jedit.exe >> installer/jedit-windows
echo jedinit.exe >> installer/jedit-windows
echo unlaunch.exe >> installer/jedit-windows
echo jedinstl.dll >> installer/jedit-windows
echo jeservps.dll >> installer/jedit-windows
echo jedidiff.exe >> installer/jedit-windows
echo jEdit_IE.reg.txt >> installer/jedit-windows

print_size jedit-windows

# jedit-mac fileset
echo jars/MacOS.jar > installer/jedit-mac

print_size jedit-mac

# jedit-os2 fileset
echo jedit.cmd > installer/jedit-os2

print_size jedit-os2

# jedit-source fileset
#find . \( -name \*.java -o -name \*.props -o -name \*.xml -o -name \*.png -o -name \*.gif \) -print > installer/jedit-source

#print_size jedit-source

rm installer/jedit-*.tar.bz2

for file in installer/jedit-*
do
	tar cfj $file.tar.bz2 `cat $file`
	rm $file
done
