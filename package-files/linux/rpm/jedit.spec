###
### RPM spec file for jEdit
###

### To create the RPM, put the source tarball in the RPM SOURCES
### directory, and invoke:

### rpm -ba jedit.spec

### You will need to have ant, xsltproc, and DocBook-XML 4.1.2 installed
### for this to work.

Summary: Programmer's text editor written in Java
Name: jedit
Version: 4.1pre9
Release: 1
# REMIND: bump this with each RPM; 32 == 4.1pre9
Serial: 32
Copyright: GPL
Group: Applications/Editors
Source0: http://prdownloads.sourceforge.net/jedit/jedit41pre9source.tar.gz
Source1: jedit.sh.in
URL: http://www.jEdit.org
Vendor: Slava Pestov <slava@jedit.org>
Packager: Slava Pestov <slava@jedit.org>
BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-root

%description
jEdit is an Open Source, cross platform text editor written in Java. It
has an extensive feature set that includes syntax highlighting, auto indent,
folding, word wrap, abbreviation expansion, multiple clipboards, powerful search
and replace, and much more.

Futhermore, jEdit is extremely customizable, and extensible, using either macros
written in the BeanShell scripting language, or plugins written in Java.

jEdit requires Java 2 version 1.3.

%prep
%setup -n jEdit

%build
export CLASSPATH="."

ant docs-html-xsltproc javadoc dist

# Build LatestVersion.jar
(cd jars/LatestVersion && ant)

# Build QuickNotepad.jar
(cd jars/QuickNotepad && ant)

# Create installer filelists
sh installer/mk_filelist.sh

%install
[ -n "$RPM_BUILD_ROOT" -a "$RPM_BUILD_ROOT" != / ] && rm -rf $RPM_BUILD_ROOT

export CLASSPATH="."

java installer.Install auto $RPM_BUILD_ROOT%{_datadir}/jedit/%{version}

sed -e "s^@JEDIT_HOME@^"%{_datadir}"/jedit/"%{version}"^g" < %{SOURCE1} > \
	$RPM_BUILD_ROOT%{_bindir}/jedit

chmod +x $RPM_BUILD_ROOT%{_bindir}/jedit

cp jedit.1 %{_mandir}/man1/jedit.1

%clean
[ -n "$RPM_BUILD_ROOT" -a "$RPM_BUILD_ROOT" != / ] && rm -rf $RPM_BUILD_ROOT

%files
%{_bindir}/jedit
%{_datadir}/jedit/%{version}
%{_mandir}/man1/jedit.1
