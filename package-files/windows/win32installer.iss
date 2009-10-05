; Inno Setup installer script for jEdit
; Björn "Vampire" Kautler <Vampire@jEdit.org>
;

[Setup]
AppName=jEdit
AppVerName=jEdit @jedit.version@
AppPublisher=Contributors
AppPublisherURL=http://www.jEdit.org
AppSupportURL=http://www.jEdit.org
AppUpdatesURL=http://www.jEdit.org
DefaultDirName={pf}\jEdit
DefaultGroupName=jEdit
AllowNoIcons=true
LicenseFile=@dist.dir@\doc\COPYING.txt
OutputDir=@dist.dir@
OutputBaseFilename=@win.filename@
SetupIconFile=@base.dir@\icons\jedit.ico
SolidCompression=true
SourceDir=@dist.dir@
VersionInfoVersion=@jedit.build.number@
VersionInfoCompany=Contributors
VersionInfoDescription=Programmer's Text Editor
VersionInfoTextVersion=@jedit.version@
VersionInfoCopyright=Copyright © 1998-@current.year@ Contributors
AppCopyright=Copyright © 1998-@current.year@ Contributors
ChangesAssociations=true
TimeStampsInUTC=true
FlatComponentsList=false
ShowTasksTreeLines=true
AppVersion=@jedit.version@
AppID=jEdit
UninstallDisplayName=jEdit @jedit.version@
AppContact=jedit-devel@lists.sourceforge.net
AppReadmeFile={app}\doc\README.txt
UninstallDisplayIcon={app}\jedit.ico
ChangesEnvironment=true
PrivilegesRequired=admin
WizardImageFile=@base.dir@\icons\WindowsInstallerImage.bmp
WizardSmallImageFile=@base.dir@\icons\WindowsInstallerSmallImage.bmp
WizardImageStretch=false
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: english; MessagesFile: compiler:Default.isl
Name: brazilianportuguese; MessagesFile: compiler:Languages\BrazilianPortuguese.isl
Name: catalan; MessagesFile: compiler:Languages\Catalan.isl
Name: czech; MessagesFile: compiler:Languages\Czech.isl
Name: danish; MessagesFile: compiler:Languages\Danish.isl
Name: dutch; MessagesFile: compiler:Languages\Dutch.isl
Name: finnish; MessagesFile: compiler:Languages\Finnish.isl
Name: french; MessagesFile: compiler:Languages\French.isl
Name: german; MessagesFile: compiler:Languages\German.isl
Name: hungarian; MessagesFile: compiler:Languages\Hungarian.isl
Name: italian; MessagesFile: compiler:Languages\Italian.isl
Name: norwegian; MessagesFile: compiler:Languages\Norwegian.isl
Name: polish; MessagesFile: compiler:Languages\Polish.isl
Name: portuguese; MessagesFile: compiler:Languages\Portuguese.isl
Name: russian; MessagesFile: compiler:Languages\Russian.isl
Name: slovak; MessagesFile: compiler:Languages\Slovak.isl
Name: slovenian; MessagesFile: compiler:Languages\Slovenian.isl

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}
Name: quicklaunchicon; Description: {cm:CreateQuickLaunchIcon}; GroupDescription: {cm:AdditionalIcons}
Name: autostartserver; Description: Start jEdit Server automatically on system startup; GroupDescription: Autostart:; Languages: slovenian slovak russian portuguese polish norwegian italian hungarian french finnish dutch danish czech catalan brazilianportuguese english
Name: autostartserver; Description: jEdit Server automatisch beim Hochfahren starten; GroupDescription: Autostart:; Languages: german

[Files]
Source: @jar.filename@; DestDir: {app}; Flags: ignoreversion sortfilesbyextension; Components: main
Source: doc\*; DestDir: {app}\doc; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Excludes: api\*,README.txt; Components: main
Source: doc\api\*; DestDir: {app}\doc\api; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Components: apidoc
Source: jars\LatestVersion.jar; DestDir: {app}\jars; Flags: ignoreversion sortfilesbyextension; Components: main
Source: jars\QuickNotepad.jar; DestDir: {app}\jars; Flags: ignoreversion sortfilesbyextension; Components: main
Source: macros\*; DestDir: {app}\macros; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Components: macros
Source: modes\*; DestDir: {app}\modes; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Components: main
Source: properties\*; DestDir: {app}\properties; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Components: main
Source: startup\*; DestDir: {app}\startup; Flags: ignoreversion recursesubdirs createallsubdirs sortfilesbyextension; Components: main
Source: @base.dir@\icons\jedit.ico; DestDir: {app}; Flags: ignoreversion sortfilesbyextension; Components: main
Source: doc\README.txt; DestDir: {app}\doc; Flags: isreadme ignoreversion sortfilesbyextension; Components: main
Source: classes\package-files\windows\jedit.bat; DestDir: {app}; Flags: ignoreversion sortfilesbyextension; AfterInstall: UpdateBatchfile; Components: batchfile

[INI]
Filename: {app}\jEdit.url; Section: InternetShortcut; Key: URL; String: http://www.jEdit.org

[Icons]
Name: {group}\jEdit; Filename: {code:javaPath}; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: jEdit - Programmer's Text Editor; HotKey: ctrl+alt+j; IconIndex: 0; Parameters: "{code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview"
Name: {group}\{cm:ProgramOnTheWeb,jEdit}; Filename: {app}\jEdit.url; Comment: jEdit Website
Name: {group}\Start jEdit Server; Filename: {code:javaPath}; Parameters: "{code:jvmOptions} -jar ""{app}\@jar.filename@"" -background -nogui"; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: Start jEdit Server; IconIndex: 0; Languages: slovenian slovak russian portuguese polish norwegian italian hungarian french finnish dutch danish czech catalan brazilianportuguese english
Name: {group}\Quit jEdit Server; Filename: {code:javaPath}; Parameters: "-jar ""{app}\@jar.filename@"" -quit"; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: Quit jEdit Server; IconIndex: 0; Languages: slovenian slovak russian portuguese polish norwegian italian hungarian french finnish dutch danish czech catalan brazilianportuguese english
Name: {group}\jEdit Server Starten; Filename: {code:javaPath}; Parameters: "{code:jvmOptions}  -jar ""{app}\@jar.filename@"" -background -nogui"; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: jEdit Server Starten; IconIndex: 0; Languages: german
Name: {group}\jEdit Server Beenden; Filename: {code:javaPath}; Parameters: "-jar ""{app}\@jar.filename@"" -quit"; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: jEdit Server Beenden; IconIndex: 0; Languages: german
Name: {group}\{cm:UninstallProgram,jEdit}; Filename: {uninstallexe}; Comment: {cm:UninstallProgram,jEdit}
Name: {userdesktop}\jEdit; Filename: {code:javaPath}; Tasks: desktopicon; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: jEdit - Programmer's Text Editor; IconIndex: 0; Parameters: "{code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview"
Name: {userappdata}\Microsoft\Internet Explorer\Quick Launch\jEdit; Filename: {code:javaPath}; Tasks: quicklaunchicon; WorkingDir: {app}; IconFilename: {app}\jedit.ico; Comment: jEdit - Programmer's Text Editor; IconIndex: 0; Parameters: "{code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview"

[Run]
Filename: {code:javaPath}; Description: {cm:LaunchProgram,jEdit}; Flags: postinstall skipifsilent nowait; WorkingDir: {app}; Parameters: "{code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview"

[Components]
Name: main; Description: jEdit - Programmer's Text Editor; Flags: fixed; Types: custom compact full
Name: apidoc; Description: API Documentation (for macro and plugin development); Types: custom full
Name: macros; Description: Default set of macros (highly recommended); Types: custom compact full
Name: batchfile; Description: Batch file (for command-line usage); Types: custom compact full

[Registry]
Root: HKCR; Subkey: *\Shell; Flags: uninsdeletekeyifempty
Root: HKCR; Subkey: *\Shell\Open with jEdit; Flags: uninsdeletekey; Languages: slovenian slovak russian portuguese polish norwegian italian hungarian french finnish dutch danish czech catalan brazilianportuguese english
Root: HKCR; Subkey: *\Shell\Open with jEdit\Command; ValueType: string; ValueData: """{code:javaPath}"" {code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview ""%1"""; Flags: uninsdeletekey; Languages: slovenian slovak russian portuguese polish norwegian italian hungarian french finnish dutch danish czech catalan brazilianportuguese english
Root: HKCR; Subkey: *\Shell\Mit jEdit Öffnen; Flags: uninsdeletekey; Languages: german
Root: HKCR; Subkey: *\Shell\Mit jEdit Öffnen\Command; ValueType: string; ValueData: """{code:javaPath}"" {code:jvmOptions} -jar ""{app}\@jar.filename@"" -reuseview ""%1"""; Flags: uninsdeletekey; Languages: german
Root: HKLM; Subkey: SOFTWARE\Microsoft\Windows\CurrentVersion\Run; ValueType: string; ValueData: """{code:javaPath}"" {code:jvmOptions} -jar ""{app}\@jar.filename@"" -background -nogui"; Tasks: autostartserver; ValueName: jEdit Server; Flags: uninsdeletevalue

[UninstallDelete]
Type: files; Name: {app}\jEdit.url

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
function javaPath(Param: String): String;
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
	if (Length(javaVersion) > 0) and (javaVersion >= '1.5') then begin
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
	if (Length(javaVersion) > 0) and (javaVersion >= '1.5') then begin
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

// Returns JVM options to be used for launch jEdit.
function jvmOptions(Param: String): String;
begin
	Result := '-Xmx192M';
end;

// updates the batch file for commandline usage
// and includes its path in %PATH
procedure UpdateBatchFile;
var
	tmpSAnsi : AnsiString;
	tmpS : String;
	tmpSA : Array Of String;
	i, pathLine : Integer;
begin
	LoadStringFromFile(ExpandConstant('{app}\jedit.bat'),tmpSAnsi);
	tmpS := tmpSAnsi;

	StringChangeEx(tmpS,'{jvmOptions}',jvmOptions(''),False);
	StringChangeEx(tmpS,'{jedit.jar}','{app}\@jar.filename@',False);
	SaveStringToFile(ExpandConstant('{app}\jedit.bat'),ExpandConstantEx(tmpS,'javaw.exe',javaPath('')),false);
	if UsingWinNT then begin
		tmpS := '';
		RegQueryStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',tmpS);
		if Pos(Uppercase(ExpandConstant('{app}')),Uppercase(tmpS)) = 0 then begin
			if not (Copy(tmpS,Length(tmpS),1) = ';') then begin
				tmpS := tmpS + ';';
			end;
			tmpS := tmpS + ExpandConstant('{app}');
		end;
		RegWriteStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',tmpS);
	end else begin
		LoadStringsFromFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),tmpSA);
		pathLine := -1;
		for i := 0 to GetArrayLength(tmpSA) - 1 do begin
			tmpS := Trim(tmpSA[i]);
			if Uppercase(Copy(tmpS,1,5)) = 'PATH=' then begin
				pathLine := i;
				if not (Pos(Uppercase(ExpandConstant('{app}')),Uppercase(tmpS)) = 0) then begin
					exit;
				end;
			end;
		end;
		if pathLine = -1 then begin
			SetArrayLength(tmpSA,GetArrayLength(tmpSA) + 1);
			tmpSA[GetArrayLength(tmpSA) - 1] := 'PATH=' + ExpandConstant('{app}');
		end else begin
			tmpS := Trim(tmpSA[pathLine]);
			if not (Copy(tmpS,Length(tmpS),1) = ';') then begin
				tmpS := tmpS + ';';
			end;
			tmpSA[pathLine] := tmpS + ExpandConstant('{app}');
		end;
		SaveStringsToFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),tmpSA,false);
	end;
end;

// Called on setup startup
function InitializeSetup: Boolean;
begin
	// check if java >= 1.4 is installed
	if Length(javaPath('')) > 0 then begin
		Result := true;
	end	else begin
		MsgBox('Setup was unable to find an installed Java Runtime Environment or Java Development Kit of version 1.4, or higher.' + #13 +
			   'You must have installed at least JDK or JRE 1.4 to continue setup.' + #13 +
			   'Please install one from http://java.sun.com and restart setup.', mbInformation, MB_OK);
		Result := false;
	end;
end;

// Called on entering a new step while uninstalling
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
	tmpS : String;
	tmpSA : Array Of String;
	i, position : Integer;
begin
	if CurUninstallStep = usPostUninstall then begin
		if UsingWinNT then begin
			tmpS := '';
			RegQueryStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',tmpS);
			position := Pos(Uppercase(ExpandConstant('{app}')),Uppercase(tmpS));
			if not (position = 0) then begin
				Delete(tmpS,position,Length(ExpandConstant('{app}')));
				if Copy(tmpS,position,1) = ';' then begin
					Delete(tmpS,position,1);
				end;
			end;
			RegWriteStringValue(HKLM,'SYSTEM\CurrentControlSet\Control\Session Manager\Environment','Path',tmpS);
		end else begin
			LoadStringsFromFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),tmpSA);
			for i := 0 to GetArrayLength(tmpSA) - 1 do begin
				tmpS := Trim(tmpSA[i]);
				if Uppercase(Copy(tmpS,1,5)) = 'PATH=' then begin
					position := Pos(Uppercase(ExpandConstant('{app}')),Uppercase(tmpS));
					if not (position = 0) then begin
						Delete(tmpS,position,Length(ExpandConstant('{app}')));
						if Copy(tmpS,position,1) = ';' then begin
							Delete(tmpS,position,1);
						end;
					end;
					tmpSA[i] := tmpS;
				end;
			end;
			SaveStringsToFile(ExpandConstant('{sd}\AUTOEXEC.BAT'),tmpSA,false);
		end;
	end else if CurUninstallStep = usUninstall then begin
		// Delete Plugins that were downloaded to application directory
		DelTree(ExpandConstant('{app}\jars'),true,true,true);
	end;
end;

