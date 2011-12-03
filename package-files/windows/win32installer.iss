; Inno Setup installer script for jEdit
; encoding=UTF-8Y
; (BOM seems the only way to let Inno Setup know the encoding)
;
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
LicenseFile=@dist.dir.for.innosetup@\doc\COPYING.txt
OutputBaseFilename=@win.filename@
OutputDir=@dist.dir.for.innosetup@
SetupIconFile=@base.dir.for.innosetup@\icons\jedit.ico
ShowTasksTreeLines=true
SolidCompression=true
SourceDir=@dist.dir.for.innosetup@
TimeStampsInUTC=true
UninstallDisplayIcon={app}\jedit.exe
UninstallDisplayName=jEdit @jedit.version@
VersionInfoCompany=Contributors
VersionInfoCopyright=Copyright © 1998-@current.year@ Contributors
VersionInfoDescription=Programmer's Text Editor
VersionInfoTextVersion=@jedit.version@
VersionInfoVersion=@jedit.build.number@
WizardImageFile=@base.dir.for.innosetup@\icons\WindowsInstallerImage.bmp
WizardSmallImageFile=@base.dir.for.innosetup@\icons\WindowsInstallerSmallImage.bmp

[Components]
Name: main; Description: jEdit - Programmer's Text Editor; Flags: fixed; Types: custom compact full
Name: apidoc; Description: {cm:APIDocumentation}; Types: full
Name: macros; Description: {cm:Macros}; Types: compact full

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}
Name: quicklaunchicon; Description: {cm:CreateQuickLaunchIcon}; GroupDescription: {cm:AdditionalIcons}
Name: autostartserver; Description: {cm:AutostartJEditServer}; GroupDescription: Autostart:

[Files]
Source: @jar.filename@; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: jedit.exe; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; AfterInstall: updatePATHVariable; Components: main
Source: classes\package-files\windows\jEdit.url; DestDir: {app}; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: doc\*; DestDir: {app}\doc; Excludes: \doc\api\*; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: doc\api\*; DestDir: {app}\doc\api; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: apidoc
Source: jars\QuickNotepad.jar; DestDir: {app}\jars; Flags: ignoreversion sortfilesbyextension sortfilesbyname; Components: main
Source: macros\*; DestDir: {app}\macros; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: macros
Source: modes\*; DestDir: {app}\modes; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: properties\*; DestDir: {app}\properties; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main
Source: startup\*; DestDir: {app}\startup; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension sortfilesbyname; Components: main

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
