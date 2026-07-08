#define MyAppName "Rocket Client"
#define MyAppVersion "0.5.0"
#define MyAppExeName "RocketClient.exe"
#define MyAppDir "build\app-image\RocketClient"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=Pernoise
DefaultDirName={autopf}\RocketClient
DefaultGroupName=Rocket Client
OutputDir=build\installer
OutputBaseFilename=RocketClient-0.5.0-Setup
SetupIconFile=src\main\resources\icons\icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
WizardSizePercent=120
DisableWelcomePage=no
DisableDirPage=no
DisableProgramGroupPage=yes

; Dark themed colors
BackColor=$040408
BackColor2=$040408
BackSolid=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[CustomMessages]
english.WelcomeLabel1=Welcome to Rocket Client
english.WelcomeLabel2=A performance-focused Minecraft client.%n%nThis will install Rocket Client Beta v0.5.0 on your computer.%n%nClick Next to continue.

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Files]
Source: "{#MyAppDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Rocket Client"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\Rocket Client"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\RocketClient.ico"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch Rocket Client"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
