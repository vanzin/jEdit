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
; TODO: create htmlhelp file?
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
ChangesAssociations=yes

[Registry]
Root: HKCR; Subkey: "*\Shell"; flags: uninsdeletekeyifempty
Root: HKCR; Subkey: "*\Shell\Open with jEdit"; flags: uninsdeletekey
Root: HKCR; Subkey: "*\Shell\Open with jEdit\command"; ValueType: string; ValueData: """{code:GetJavaPath}"" -jar ""{app}\jedit.jar"" -reuseview ""%1"""; flags: uninsdeletekey

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
var
	javawExePath: String;

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
	if Length(javawExePath) > 0 then begin
		Result := javawExePath;
		path := javawExePath;
	end;

	// try to find JDK "javaw.exe"
	javaVersion := getJDKVersion();
	if (Length(path) = 0) and (Length(javaVersion) > 0) and ((javaVersion) >= '1.3.0') then begin
		RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Development Kit\' + javaVersion, 'JavaHome', javaHome);
		path := javaHome + '\bin\' + 'javaw.exe';
		if FileExists(path) then begin
			Log('(JDK) found javaw.exe: ' + path);
			Result := path;
		end;
	end;

	// if we didn't find a JDK "javaw.exe", try for a JRE one
	if Length(path) = 0 then begin
		Log('(JRE) JDK not found, looking for JRE');
		javaVersion := getJDKVersion();
		if (Length(javaVersion) > 0) and ((javaVersion) >= '1.3.0') then begin
			RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Runtime Environment\' + javaVersion, 'JavaHome', javaHome);
			path := javaHome + '\bin\' + 'javaw.exe';
			if FileExists(path) then begin
				Log('(JRE) found javaw.exe: ' + path);
				Result := path;
			end
		end;
	end;

	(*
	// if unable to find Java path in registry, try %JAVA_HOME%
	// (maybe not a good idea)
	if Length(path) = 0 then begin
		javaHome := GetEnv('JAVA_HOME');
		if Length(javaHome) > 0 then begin
			path := javaHome + '\bin\' + 'javaw.exe';
			if FileExists(path) then begin
				Log('(JAVA_HOME) found java.exe: ' + path);
				Result := path;
			end;
		end;
	end;
	*)

	(*
	// let user browse for Java -- maybe not a good idea
	if Length(path) = 0 then begin
		MsgBox('Setup was unable to automatically find "javaw.exe".' + #13  
			    + #13 +
			   'If you have a JDK or JRE, version 1.3 or greater, installed' + #13
			   'please locate "javaw.exe".  If you don''t have a JDK or JRE' + #13
			   'installed, download and install from http://java.sun.com/ and' + #13
			   'restart setup.', mbError, MB_OK);
		Log('unable to find javaw.exe, prompting for path');
		if GetOpenFileName('Browse for javaw.exe', path, '.', 'EXE files (*.exe)|*.exe', '.exe') then begin
			Log('(BROWSE): user selected: ' + path);
			Result := path;
		end;
	end;
	*)

	javawExePath := Result;	// save found value as global
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
	SaveStringToFile(batchFile, javaPath + ' -jar "' + jarPath + '" -reuseview %1 %2 %3 %4 %5 %6 %7 %8 %9', False);
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

// :noTabs=false:tabSize=4:
