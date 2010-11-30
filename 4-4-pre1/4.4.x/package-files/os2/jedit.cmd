/* MAINTAINERS: Robert Henschel (os2info@gmx.net)
 *              Christoph vogt (ch.vogt@gmx.net)
 *              Gili Tzabari (junk@bbs.darktech.org)
 * PLATFORM: OS/2, eCS
 * 
 * Used to run JEdit 3.1
 */


rc = QueryJavaVersion()
if (rc = -1) then
do
  say "java.exe cannot be found"
  return -1;
end
if (rc = -2) then
  say "Unexpected response to JAVA -VERSION. Assuming newest version is being used."

arguments = "-settings=%HOME%\.jedit -server=%HOME%\.jedit\server"
jedit_dir = GetExecPath()

if (iMajor = 1 & iMid <= 1) then
    'java -Xmx192M -classpath %classpath%;'jedit_dir'\jedit.jar org.gjt.sp.jedit.jEdit 'arguments
  else
    'java -Xmx192M -jar 'jedit_dir'\jedit.jar 'arguments

return 0





 /*
  * Java version detector.
  * Assumes java -version return quoted ("") version number.
  * Written by os2bird on #netlabs
  *
  * Returns -1 on no java or failed to execute
  *         -2 on invalid java version string.
  *         version number.
  */
QueryJavaVersion: procedure expose iMajor iMid iMinor
cQueued =  queued();
'@echo off'
'java -version 2>&1 | rxqueue /LIFO'
i = queued();
do while i > cQueued
    pull sStr
    if (pos("JAVA VERSION ", sStr) > 0) | (pos("JAVA.EXE VERSION ", sStr) > 0) then
    do
        do while(queued() > 0) 
          pull sStrIngore; /* flush input stream */
        end
        parse var sStr sStuff '"'iMajor'.'iMid'.'iMinor'"'
        if (iMinor <> '') then
            return iMajor*100 + iMid * 10 + iMinor;
         else
             return -2
     end
     i = i - 1;
 end
 return -1;


/*
 * Returns the path of the script being executed.
 */
GetExecPath: procedure
parse source result
parse var result 'OS/2 ' dummy result                        /* Get full path of script */
result=filespec("drive", result) || filespec("path", result) /* strip away filename */
result=substr(result, 1, length(result)-1)                   /* remove backslash */
return result