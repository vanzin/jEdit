; Inno Setup installer script for jEdit
; Björn "Vampire" Kautler <Vampire@jEdit.org>
;

[Setup]
AllowNoIcons=true
AppContact=jedit-devel@lists.sourceforge.net
AppCopyright=Copyright © 1998-@current.year@ Contributors
AppID=jEdit
AppName=jEdit
AppPublisher=Contributors
AppPublisherURL=http://www.jEdit.org
AppReadmeFile={app}\doc\README.txt
AppSupportURL=http://www.jEdit.org
AppUpdatesURL=http://www.jEdit.org
AppVerName=jEdit @jedit.version@
AppVersion=@jedit.version@
ArchitecturesInstallIn64BitMode=x64
ChangesAssociations=true
ChangesEnvironment=true
DefaultDirName={pf}\jEdit
DefaultGroupName=jEdit
FlatComponentsList=false
LicenseFile=@dist.dir@\doc\COPYING.txt
OutputBaseFilename=@win.filename@
OutputDir=@dist.dir@
SetupIconFile=@base.dir@\icons\jedit.ico
ShowTasksTreeLines=true
SolidCompression=true
SourceDir=@dist.dir@
TimeStampsInUTC=true
UninstallDisplayIcon={app}\jedit.exe
UninstallDisplayName=jEdit @jedit.version@
VersionInfoCompany=Contributors
VersionInfoCopyright=Copyright © 1998-@current.year@ Contributors
VersionInfoDescription=Programmer's Text Editor
VersionInfoTextVersion=@jedit.version@
VersionInfoVersion=@jedit.build.number@
WizardImageFile=@base.dir@\icons\WindowsInstallerImage.bmp
WizardSmallImageFile=@base.dir@\icons\WindowsInstallerSmallImage.bmp

[Components]
Name: main; Description: jEdit - Programmer's Text Editor; Flags: fixed; Types: custom compact full
Name: apidoc; Description: {cm:APIDocumentation}; Types: full
Name: macros; Description: {cm:Macros}; Types: compact full
Name: batchfile; Description: {cm:BatchFile}; Types: full

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}
Name: quicklaunchicon; Description: {cm:CreateQuickLaunchIcon}; GroupDescription: {cm:AdditionalIcons}
Name: autostartserver; Description: {cm:AutostartJEditServer}; GroupDescription: Autostart:

[Files]
Source: @jar.filename@; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: jedit.exe; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; AfterInstall: updatePATHVariable; Components: main
Source: classes\package-files\windows\jedit.l4j.ini; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: classes\package-files\windows\jEdit.url; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: doc\*; DestDir: {app}\doc; Excludes: \doc\api\*; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: doc\api\*; DestDir: {app}\doc\api; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: apidoc
Source: jars\QuickNotepad.jar; DestDir: {app}\jars; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: macros\*; DestDir: {app}\macros; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: macros
Source: modes\*; DestDir: {app}\modes; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: properties\*; DestDir: {app}\properties; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: startup\*; DestDir: {app}\startup; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: classes\package-files\windows\jedit.bat; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; AfterInstall: updateBatchfile; Components: batchfile

[Icons]
Name: {group}\jEdit; Filename: {app}\jedit.exe; WorkingDir: {app}; Comment: jEdit - Programmer's Text Editor; HotKey: ctrl+alt+j
Name: {group}\{cm:ProgramOnTheWeb,jEdit}; Filename: {app}\jEdit.url; Comment: jEdit Website
Name: {group}\{cm:LaunchProgram,jEdit Server}; Filename: {app}\jedit.exe; Parameters: "-background -nogui --l4j-dont-wait"; WorkingDir: {app}; Comment: {cm:LaunchProgram,jEdit Server}
Name: {group}\{cm:QuitProgram,jEdit Server}; Filename: {app}\jedit.exe; Parameters: "-quit"; WorkingDir: {app}; Comment: {cm:QuitProgram,jEdit Server}
Name: {group}\{cm:UninstallProgram,jEdit}; Filename: {uninstallexe}; Comment: {cm:UninstallProgram,jEdit}
Name: {userdesktop}\jEdit; Filename: {app}\jedit.exe; Tasks: desktopicon; WorkingDir: {app}; Comment: jEdit - Programmer's Text Editor
Name: {userappdata}\Microsoft\Internet Explorer\Quick Launch\jEdit; Filename: {app}\jedit.exe; Tasks: quicklaunchicon; WorkingDir: {app}; Comment: jEdit - Programmer's Text Editor

[Languages]
Name: en; MessagesFile: compiler:Default.isl
Name: de; MessagesFile: compiler:Languages\German.isl
Name: eu; MessagesFile: compiler:Languages\Basque.isl
Name: pt_BR; MessagesFile: compiler:Languages\BrazilianPortuguese.isl
Name: ca; MessagesFile: compiler:Languages\Catalan.isl
Name: cs; MessagesFile: compiler:Languages\Czech.isl
Name: da; MessagesFile: compiler:Languages\Danish.isl
Name: nl; MessagesFile: compiler:Languages\Dutch.isl
Name: fi; MessagesFile: compiler:Languages\Finnish.isl
Name: fr; MessagesFile: compiler:Languages\French.isl
Name: he; MessagesFile: compiler:Languages\Hebrew.isl
Name: hu; MessagesFile: compiler:Languages\Hungarian.isl
Name: it; MessagesFile: compiler:Languages\Italian.isl
Name: ja; MessagesFile: compiler:Languages\Japanese.isl
Name: no; MessagesFile: compiler:Languages\Norwegian.isl
Name: pl; MessagesFile: compiler:Languages\Polish.isl
Name: pt; MessagesFile: compiler:Languages\Portuguese.isl
Name: ru; MessagesFile: compiler:Languages\Russian.isl
Name: sk; MessagesFile: compiler:Languages\Slovak.isl
Name: sl; MessagesFile: compiler:Languages\Slovenian.isl
Name: es; MessagesFile: compiler:Languages\Spanish.isl

[CustomMessages]
APIDocumentation=API Documentation (for macro and plugin development)
de.APIDocumentation=API Dokumentation (für Macro und Plugin Entwicklung)
Macros=Default set of macros (highly recommended)
de.Macros=Standard Makros (sehr empfohlen)
BatchFile=Batch file (for usage in scripts)
de.BatchFile=Batch file (für die Benutzung in Skripten)
AutostartJEditServer=Start jEdit Server automatically on system startup
de.AutostartJEditServer=jEdit Server automatisch beim Hochfahren starten
QuitProgram=Quit %1
de.QuitProgram=%1 beenden
OpenWithProgram=Open with %1
de.OpenWithProgram=Mit %1 öffnen
pleaseQuitJEdit=The installer will now try to quit a running instance of jEdit.%nPlease save your work and exit jEdit for the installation to continue.
de.pleaseQuitJEdit=Die Installation wird nun versuchen eine laufende Instanz von jEdit zu beenden.%nBitte speichern Sie Ihre Arbeit und beenden Sie jEdit um mit der Installation fortzufahren.
ViewFile=View %1
de.ViewFile=%1 anzeigen

[Registry]
Root: HKCR; Subkey: *\Shell; Flags: uninsdeletekeyifempty
Root: HKCR; Subkey: *\Shell\{cm:OpenWithProgram,jEdit}; Flags: uninsdeletekey
Root: HKCR; Subkey: *\Shell\{cm:OpenWithProgram,jEdit}\Command; ValueType: string; ValueData: """{app}\jedit.exe"" ""%1"""
Root: HKLM; Subkey: SOFTWARE\Microsoft\Windows\CurrentVersion\Run; ValueType: string; ValueName: jEdit Server; ValueData: """{app}\jedit.exe"" -background -nogui --l4j-dont-wait"; Flags: uninsdeletevalue; Tasks: autostartserver

[Run]
Filename: {app}\jedit.exe; Description: {cm:ViewFile,README}; Parameters: "--l4j-dont-wait -nosettings {app}\doc\README.txt"; WorkingDir: {app}; Flags: nowait postinstall skipifsilent
Filename: {app}\jedit.exe; Description: {cm:ViewFile,CHANGES}; Parameters: "--l4j-dont-wait -nosettings {app}\doc\CHANGES.txt"; WorkingDir: {app}; Flags: nowait postinstall skipifsilent
Filename: {app}\jedit.exe; Description: {cm:LaunchProgram,jEdit}; Parameters: "--l4j-dont-wait"; WorkingDir: {app}; Flags: nowait postinstall skipifsilent; Tasks: not autostartserver
Filename: {app}\jedit.exe; Description: {cm:LaunchProgram,jEdit}; Parameters: "--l4j-dont-wait -background"; WorkingDir: {app}; Flags: nowait postinstall skipifsilent; Tasks: autostartserver

[Code]
var
	javawExePath: String;

// looks for current JRE version, in Registry
function getJREVersion: String;
var
	jreVersion : String;
begin
	jreVersion := '';
	RegQueryStringValue(HKLM,'SOFTWARE\JavaSoft\Java Runtime Environment','CurrentVersion',jreVersion);
	if (Length(jreVersion) = 0) and IsWin64 then begin
		RegQueryStringValue(HKLM32,'SOFTWARE\JavaSoft\Java Runtime Environment','CurrentVersion',jreVersion);
	end;
	Result := jreVersion;
end;

// looks for current JDK version, in Registry
function getJDKVersion: String;
var
	jdkVersion : String;
begin
	jdkVersion := '';
	RegQueryStringValue(HKLM,'SOFTWARE\JavaSoft\Java Development Kit', 'CurrentVersion', jdkVersion);
	if (Length(jdkVersion) = 0) and IsWin64 then begin
		RegQueryStringValue(HKLM32,'SOFTWARE\JavaSoft\Java Development Kit', 'CurrentVersion', jdkVersion);
	end;
	Result := jdkVersion;
end;

// Finds path to "javaw.exe" by looking up System directory or JDK/JRE
// locations in the registry.  Ensures the file actually exists.  If
// none is found, an empty string is returned.
function javaPath: String;
var
	javaVersion : String;
	javaHome : String;
	sysWow64 : String;
	path : String;
begin
	if Length(javawExePath) > 0 then begin
		Result := javawExePath;
		exit;
	end;

	// Workaround for 64-bit Windows.
	// GetSysWow64Dir must be checked before GetSystemDir because of
	// the following reasons.
	//   - Sun's JRE 1.6.0_03 puts javaw.exe in SysWOW64 even if user
	//     chose the 64-bit installer. This is not documented. But an
	//     user reports this. See SF.net bug #1849762 for more detail.
	//   - "File System Redirector"
	//     http://msdn2.microsoft.com/en-us/library/aa384187.aspx
	//   - This installer is a 32-bit program while installed paths are
	//     used by 64-bit programs.
	// Without this workaround, the installer finds javaw.exe in
	// System32 which is mimiced by the redirector. But all links
	// pointing there doesn't work for 64-bit programs (including
	// Windows Explorer) because javaw.exe really is in SysWOW64.
	sysWow64 := GetSysWow64Dir;
	if sysWow64 <> '' then begin
		path := sysWow64 + '\javaw.exe';
		if FileExists(path) then begin
			Log('(SysWow64Dir) found javaw.exe: ' + path);
			javawExePath := path;
			Result := javawExePath;
			exit;
		end;
	end;

	path := GetSystemDir + '\javaw.exe';
	if FileExists(path) then begin
		Log('(SystemDir) found javaw.exe: ' + path);
		javawExePath := path;
		Result := javawExePath;
		exit;
	end;

	// try to find JDK "javaw.exe"
	javaVersion := getJDKVersion;
	if (Length(javaVersion) > 0) and (javaVersion >= '1.6') then begin
		RegQueryStringValue(HKLM,'SOFTWARE\JavaSoft\Java Development Kit\' + javaVersion,'JavaHome',javaHome);
		if (Length(javaHome) = 0) and IsWin64 then begin
			RegQueryStringValue(HKLM32,'SOFTWARE\JavaSoft\Java Development Kit\' + javaVersion,'JavaHome',javaHome);
		end;
		path := javaHome + '\bin\javaw.exe';
		if FileExists(path) then begin
			Log('(JDK) found javaw.exe: ' + path);
			javawExePath := path;
			Result := javawExePath;
			exit;
		end;
	end;

	// if we didn't find a JDK "javaw.exe", try for a JRE one
	Log('(JRE) JDK not found or too old, looking for JRE');
	javaVersion := getJREVersion;
	if (Length(javaVersion) > 0) and (javaVersion >= '1.6') then begin
		RegQueryStringValue(HKLM,'SOFTWARE\JavaSoft\Java Runtime Environment\' + javaVersion,'JavaHome',javaHome);
		if (Length(javaHome) = 0) and IsWin64 then begin
			RegQueryStringValue(HKLM32,'SOFTWARE\JavaSoft\Java Runtime Environment\' + javaVersion,'JavaHome',javaHome);
		end;
		path := javaHome + '\bin\javaw.exe';
		if FileExists(path) then begin
			Log('(JRE) found javaw.exe: ' + path);
			javawExePath := path;
			Result := javawExePath;
			exit;
		end;
	end;

	// we didn't find a suitable "javaw.exe"
	Result := '';
end;

procedure updateBatchFile;
var
	tmpSAnsi : AnsiString;
	tmpS : String;
begin
	LoadStringFromFile(ExpandConstant('{app}\jedit.bat'),tmpSAnsi);
	tmpS := tmpSAnsi;

	StringChangeEx(tmpS,'{jvmOptions}','-Xmx192M',False);
	StringChangeEx(tmpS,'{jedit.jar}','{app}\@jar.filename@',False);
	SaveStringToFile(ExpandConstant('{app}\jedit.bat'),ExpandConstantEx(tmpS,'javaw.exe',javaPath),false);
end;

function InitializeSetup: Boolean;
begin
	// check if java >= 1.6 is installed
	if Length(javaPath) > 0 then begin
		Result := true;
	end else begin
		MsgBox('Setup was unable to find an installed Java Runtime Environment or Java Development Kit of version 1.6, or higher.' + #13 +
			   'You must have installed at least JDK or JRE 1.6 to continue setup.' + #13 +
			   'Please install one from http://www.java.com/download and restart setup.',mbInformation,MB_OK);
		Result := false;
	end;
end;

function findPathLine(lines: Array Of String): Integer;
var
	i : Integer;
begin
	for i := 0 to GetArrayLength(lines) - 1 do begin
		if Uppercase(Copy(Trim(lines[i]),1,5)) = 'PATH=' then begin
			Result := i;
		end;
	end;
end;

function appendAppDirIfNecessary(var path: String): Boolean;
var
	appDir : String;
begin
	appDir := ExpandConstant('{app}');
	if Pos(Uppercase(appDir),Uppercase(path)) = 0 then begin
		if Copy(path,Length(path),1) <> ';' then begin
			path := path + ';';
		end;
		path := path + appDir;
		Result := true;
	end else begin
		Result := false;
	end;
end;

procedure updatePATHVariableInRegistry;
var
	path : String;
begin
	RegQueryStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',path);
	if appendAppDirIfNecessary(path) then begin
		RegWriteStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',path);
	end;
end;

procedure updatePATHVariableInAutoexecBat;
var
	lines : Array Of String;
	pathLine : Integer;
	path : String;
begin
	LoadStringsFromFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),lines);
	pathLine := findPathLine(lines);
	if pathLine = -1 then begin
		pathLine := GetArrayLength(lines);
		SetArrayLength(lines,pathLine + 1);
		lines[pathLine] := 'PATH=';
	end;
	path := Trim(lines[pathLine]);
	if appendAppDirIfNecessary(path) then begin
		lines[pathLine] := path;
		SaveStringsToFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),lines,false);
	end;
end;

procedure updatePATHVariable;
begin
	if UsingWinNT then begin
		updatePATHVariableInRegistry;
	end else begin
		updatePATHVariableInAutoexecBat;
	end;
end;

function removeAppDirIfNecessary(var path: String): Boolean;
var
	appDir : String;
	position : Integer;
begin
	appDir := ExpandConstant('{app}');
	position := Pos(Uppercase(appDir),Uppercase(path));
	if position = 0 then begin
		Result := false;
	end else begin
		Delete(path,position,Length(appDir));
		if Copy(path,position,1) = ';' then begin
			Delete(path,position,1);
		end;
		Result := true;
	end;
end;

procedure cleanPATHVariableInRegistry;
var
	path : String;
begin
	RegQueryStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',path);
	if removeAppDirIfNecessary(path) then begin
		RegWriteStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',path);
	end;
end;

procedure cleanPATHVariableInAutoexecBat;
var
	lines : Array Of String;
	pathLine : Integer;
	path : String;
begin
	LoadStringsFromFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),lines);
	pathLine := findPathLine(lines);
	if pathLine <> -1 then begin
		path := Trim(lines[pathLine]);
		if removeAppDirIfNecessary(path) then begin
			lines[pathLine] := path;
			SaveStringsToFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),lines,false);
		end;
	end;
end;

procedure cleanPATHVariable;
begin
	if UsingWinNT then begin
		cleanPATHVariableInRegistry;
	end else begin
		cleanPATHVariableInAutoexecBat;
	end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
	if CurUninstallStep = usPostUninstall then begin
		cleanPATHVariable;
	end;
end;

procedure quitJEdit;
var
	resultCode : Integer;
begin
	case SuppressibleMsgBox(CustomMessage('pleaseQuitJEdit'),mbConfirmation,MB_OKCANCEL or MB_DEFBUTTON2, IDYES) of
		IDOK: begin
			ExtractTemporaryFile('@jar.filename@');
			ExtractTemporaryFile('jedit.exe');
			ExecAsOriginalUser(ExpandConstant('{tmp}/jedit.exe'),'-quit',ExpandConstant('{tmp}'),SW_SHOW,ewWaitUntilTerminated,resultCode);
		end;
		IDCANCEL: Abort;
	end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
	if CurStep = ssInstall then begin
		quitJEdit;
	end;
end;
