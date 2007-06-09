@echo off
:LOOP
	"{app}\launchstub.exe" "{javaw.exe}" -Xms64M -Xmx192M -jar "{jedit.jar}" -reuseview "%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8" "%9"
	set i=
	:COUNT_LOOP
		set i=%i%-
		shift
	if not "%i%" == "---------" goto COUNT_LOOP
if not "%1" == "" goto LOOP
