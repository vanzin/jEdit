; Inno Setup installer script for jEdit
; Ollie Rutherfurd <oliver@jedit.org>
;
; $Id$
;
; For each release:
;
; * change JEDIT_HOME to location of jEdit files, if 
;   not building from source directory.
; * change AppVerName
;
; TODO: add file association, so one gets context menu -- 'Open with jEdit'
; TODO: create htmlhelp file?
; TODO: check that Java version >= 1.3
; TODO: read version number from source
; TODO: figure out how to generate "jedit.bat" in source dir

#define JEDIT_HOME "." ; Where files for installer are located.

[Setup]
AppName=jEdit
AppVerName=jEdit 4.2pre15
AppVersion=4.2
AppPublisher=Slava Pestov
AppPublisherURL=http://www.jedit.org/
AppSupportURL=http://www.jedit.org/
AppUpdatesURL=http://www.jedit.org/
DefaultDirName={pf}\jEdit
DefaultGroupName=jEdit 4.2
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes
; using orange icon from: http://www.jedit.org/index.php?page=images
SetupIconFile={#JEDIT_HOME}\jedit.ico
LicenseFile={#JEDIT_HOME}\doc\COPYING.txt
UninstallIconFile={#JEDIT_HOME}\jedit.ico

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; May need to copy icon file to JEDIT_HOME
; Using orange icon from: http://www.jedit.org/index.php?page=images
Source: "{#JEDIT_HOME}\jedit.ico"; DestDir: "{app}"; Flags: ignoreversion; Components: main
Source: "{#JEDIT_HOME}\jedit.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: main
Source: "{#JEDIT_HOME}\doc\*"; Excludes: "api\*"; DestDir: "{app}\doc"; Flags: ignoreversion recursesubdirs; Components: main
Source: "{#JEDIT_HOME}\doc\api\*"; DestDir: "{app}\doc\api"; Flags: ignoreversion recursesubdirs; Components: apidoc
Source: "{#JEDIT_HOME}\jars\*"; DestDir: "{app}\jars"; Flags: ignoreversion recursesubdirs; Components: main
Source: "{#JEDIT_HOME}\macros\*"; DestDir: "{app}\macros"; Flags: ignoreversion recursesubdirs; Components: macros
Source: "{#JEDIT_HOME}\modes\*"; DestDir: "{app}\modes"; Flags: ignoreversion recursesubdirs; Components: main
Source: "{#JEDIT_HOME}\properties\*"; DestDir: "{app}\properties"; Flags: ignoreversion recursesubdirs; Components: main
Source: "{#JEDIT_HOME}\startup\*"; DestDir: "{app}\startup"; Flags: ignoreversion recursesubdirs; Components: main
; This batch file can be empty, but it must exist. 
; I couldn't figure out how generate a file without 
; an existing one.
Source: "{#JEDIT_HOME}\jedit.bat"; DestDir: "{win}"; AfterInstall: UpdateBatchFile; Components: batchfile

[Components]
Name: "main"; Description: "jEdit text editor"; Types: full compact custom; Flags: fixed
Name: "apidoc"; Description: "API Documentation (for macro and plugin development)"; Types: full custom
Name: "macros"; Description: "Default set of macros (highly recommended)"; Types: full compact custom
Name: "batchfile"; Description: "Batch file (for command-line usage)"; Types: full compact custom

[Icons]
Name: "{group}\jEdit"; IconFilename: "{app}\jedit.ico"; Filename: "{code:GetJavaPath}"; WorkingDir: "{app}"; Parameters: "-jar ""{app}\jedit.jar"""
Name: "{group}\{cm:UninstallProgram,jEdit}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\jEdit"; IconFilename: "{app}\jedit.ico"; Filename: "{code:GetJavaPath}"; WorkingDir: "{app}"; Parameters: "-jar ""{app}\jedit.jar"""; Tasks: desktopicon
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\jEdit"; IconFilename: "{app}\jedit.ico"; Filename: "{code:GetJavaPath}"; WorkingDir: "{app}"; Parameters: "-jar ""{app}\jedit.jar"""; Tasks: quicklaunchicon

[Run]
Filename: "{code:GetJavaPath}"; WorkingDir: "{app}"; Parameters: "-jar ""{app}\jedit.jar"""; Description: "{cm:LaunchProgram,jEdit}"; Flags: shellexec postinstall skipifsilent

[Code]
(* looks for JRE version, in Registry *)
function getJREVersion(): String;
var
	javaVersion: String;
begin
	javaVersion := '';
	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', javaVersion);
	GetVersionNumbersString(javaVersion, javaVersion);
	Result := javaVersion;
end;

(* looks for JDK version, in Registry *)
function getJDKVersion(): String;
var
	jdkVersion: String;
begin
	jdkVersion := '';
	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Development Kit', 'CurrentVersion', jdkVersion);
	GetVersionNumbersString(jdkVersion, jdkVersion);
	Result := jdkVersion;
end;

(* Finds path to "javaw.exe" by looking up JDK or JRE locations *)
(* in the registry.  Ensures the file actually exists.  If none *)
(* is found, an empty string is returned. 						*)
function GetJavaPath(Default: String): String;
var
	javaVersion: String;
	javaHome: String;
	path: String;
begin
	path := '';
	javaVersion := getJDKVersion();
	if (Length(javaVersion) > 0) and ((javaVersion) >= '1.3.0') then begin
		RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Development Kit\' + javaVersion, 'JavaHome', javaHome);
		path := javaHome + '\bin\' + 'javaw.exe';
		if FileExists(path) then begin
			Result := path;
		end;
	end;
	// if we didn't find a JDK "javaw.exe", try for a JRE one
	if Length(path) = 0 then begin
		javaVersion := getJDKVersion();
	if (Length(javaVersion) > 0) and ((javaVersion) >= '1.3.0') then begin
		RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\' + javaVersion, 'JavaHome', javaHome);
		path := javaHome + '\bin\' + 'javaw.exe';
		if FileExists(path) then
			Result := path;
		end;
	end;
end;

(* generates a batch file for command-line usage *)
procedure UpdateBatchFile();
var
	appDir: String;
	batchFile: String;
	jarPath: String;
	javaPath: String;
	winDir: String;
begin
	winDir := ExpandConstant('{win}');
	appDir := ExpandConstant('{app}');
	javaPath := GetJavaPath('');
	batchFile := winDir + '\' + 'jedit.bat';
	jarPath := appDir + '\' + 'jedit.jar';
	SaveStringToFile(batchFile, javaPath + ' -jar "' + jarPath + '" %1 %2 %3 %4 %5 %6 %7 %8 %9', False);
end;

(* Called on setup startup *)
function InitializeSetup(): Boolean;
var
	javaPath: String;
begin
	javaPath := GetJavaPath('');
	if Length(javaPath) > 0 then begin
		//MsgBox('Found javaw.exe here: ' + javaPath, mbInformation, MB_OK);
		Result := true;
	end
	else begin
		MsgBox('Setup unable to find a Java Development Kit or Java Runtime 1.3, or higher, installed.' + #13 +
			   'You must have installed at least JDK or JRE, 1.3 or higher to continue setup.' + #13 +
			   'Please install one from http://java.sun.com and restart setup.', mbInformation, MB_OK);
		Result := false;
	end;
end;

// :noTabs=false:lineSeparator=\r\n:tabSize=4:
