#!/bin/sh

# This script must be run from the jEdit directory, *not* the installer
# directory!!!

if [ "$1" = "" ]; then
  echo "Must specify a command line parameter."
  exit 1
fi

# By default, put it in your home directory
DESTDIR=$HOME

jar cf0m $DESTDIR/jedit${1}install.jar installer/install.mf \
	installer/install.props \
	installer/*.html \
	installer/*.class \
	installer/jedit-*.tar.bz2 \
	jedit.1
