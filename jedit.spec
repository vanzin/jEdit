###
### RPM spec file for jEdit
###

### To create the RPM, put the source tarball in the RPM SOURCES
### directory, and invoke:

### rpm -ba jedit.spec

### You will need to have jmk, openjade and DocBook-XML 4.1.2 installed
### for this to work.

Summary: Programmer's text editor written in Java
Name: jedit
Version: 3.2.1
Release: 1
# REMIND: bump this with each RPM
Serial: 21
Copyright: GPL
Group: Application/Editors
Source0: http://prdownloads.sourceforge.net/jedit/jedit321source.tar.gz
Source1: jedit.sh.in
Patch0: jedit32-javac.patch
URL: http://www.jedit.org
Vendor: Slava Pestov <slava@jedit.org>
Packager: Slava Pestov <slava@jedit.org>
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-root

%description
jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires Java 2 (or Java 1.1 with Swing 1.1) in order to work.

%prep
%setup -n jEdit
%patch0 -p1 -b .javac

%build
export CLASSPATH="."

# Build docs
(cd doc/users-guide/ && jmk htmldocs)

# Build jedit.jar
jmk

# Build LatestVersion.jar
(cd jars/LatestVersion && jmk)

# Build LatestVersion.jar
(cd jars/Firewall && jmk)

# Create installer filelists
sh installer/mk_filelist.sh

%install
[ -n "$RPM_BUILD_ROOT" -a "$RPM_BUILD_ROOT" != / ] && rm -rf $RPM_BUILD_ROOT

export CLASSPATH="."

java installer.Install auto $RPM_BUILD_ROOT%{_datadir}/jedit/%{version} \
	$RPM_BUILD_ROOT%{_bindir}

sed -e "s^@JEDIT_HOME@^"%{_datadir}"/jedit/"%{version}"^g" < %{SOURCE1} > \
	$RPM_BUILD_ROOT%{_bindir}/jedit

chmod +x $RPM_BUILD_ROOT%{_bindir}/jedit

%clean
[ -n "$RPM_BUILD_ROOT" -a "$RPM_BUILD_ROOT" != / ] && rm -rf $RPM_BUILD_ROOT

%files
%doc doc/*.txt
%{_bindir}/jedit
%{_datadir}/jedit/%{version}
