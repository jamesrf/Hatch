;-----------------------------------------------------------------------
; NSIS Installation Script                                              ;
; Kyle Huckins                                                          ;
; khuckins@catalystdevworks.com                                         ;
; This will be a guide to working with NSIS to create an installer.     ;
; It is a heavily commented practice installer, basically.              ;
;                                                                       ;
; Comments are designated by # and ;                                    ;
; # tends to be at the beginning of a line, while ; is                  ;
; primarilly used as end-of-line comments.  However it's                ;
; common to use just ; for comments as well.                            ;
;                                                                       ;
;------------------------------------------------------------------------

;==================
; Basic Information
;--------
; Includes
!include "MUI2.nsh" ;This enables the use of ModernUI, and must go at the top
!include "x64.nsh"
!include "defines.nsh" ;Definitions for our variables
!include "LogicLib.nsh" ;Sparsly documented library, but necessary for any real logic
!include "nsDialogs.nsh" ;Need this to create custom pages
!include StrRep.nsh
!include ReplaceInFile.nsh
;---------------------------------------------------------------

; Installer's filename
Outfile "${APPNAME} Installer.exe"
RequestExecutionLevel admin


;-----------------
; Titlebar Content
Name "${APPNAME}"

;==================================
; Page system
!insertmacro MUI_PAGE_WELCOME
!define MUI_PAGE_CUSTOMFUNCTION_PRE ValidateInstall
!insertmacro MUI_PAGE_LICENSE "license.rtf" ;Loads licence.rtf to show license content
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

;-------------------------------------
; Code to verify if a user is an admin
!macro VerifyUserIsAdmin
UserInfo::GetAccountType
pop $0
${If} $0 != "admin" ;Require admin rights
    messageBox MB_OK|MB_ICONSTOP "Administrator rights required!"
    setErrorLevel 740 ;ERROR_ELEVATION_REQUIRED
    quit
${EndIf}
!macroend

; Find any installed JRE/JDK and return the version or -1
Function DetectJava
    ; 32 bit JRE >= 9
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\JRE" "CurrentVersion"
    StrCmp $0 "" +1 Found

    ; 32 bit JDK >= 9
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\JDK" "CurrentVersion"
    StrCmp $0 "" +1 Found

    ; 64 bit JRE >= 9
    ${If} ${RunningX64}
        SetRegView 64
        ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\JRE" "CurrentVersion"
        SetRegView 32 ; basically SetRegView Default since NSIS only creates 32 bit installers
        StrCmp $0 "" +1 Found
    ${EndIf}

    ; 64 bit JDK >= 9
    ${If} ${RunningX64}
        SetRegView 64
        ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\JDK" "CurrentVersion"
        SetRegView 32
        StrCmp $0 "" +1 Found
    ${EndIf}

    ; 32 bit JRE < 9
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    StrCmp $0 "" +1 Found

    ; 32 bit JDK < 9
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
    StrCmp $0 "" +1 Found

    ; 64 bit JRE < 9
    ${If} ${RunningX64}
        SetRegView 64
        ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
        SetRegView 32
        StrCmp $0 "" +1 Found
    ${EndIf}

    ; 64 bit JDK < 9
    ${If} ${RunningX64}
        SetRegView 64
        ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
        SetRegView 32
        StrCmp $0 "" +1 Found
    ${EndIf}

    ; Nuthin.
    Push "-1"
    Return

    Found:
    Push $0
    Return
FunctionEnd

function .onInit
    setShellVarContext all
    !insertmacro VerifyUserIsAdmin
    Return
functionEnd

;-----------------
;Check our version
function ValidateInstall
    Call DetectJava
    Pop $0
    StrCmp $0 "-1" NoJava
    ${If} $0 S< ${JRE_MIN_VERSION}
        MessageBox MB_OK "Please update Java to version ${JRE_MIN_VERSION} or higher. Setup will now exit." /SD IDOK IDOK QUIT
    ${EndIf}
    ; Hatch Version Check
    ReadRegStr $1 HKLM "SOFTWARE\${COMPANYNAME}\${APPNAME}" "Version"
    StrCmp $1 "" INSTALL
    ${If} $1 S== ${FULLVERSION} ;Same version is installed
        MessageBox MB_OKCANCEL|MB_ICONINFORMATION "You already have this version of ${APPNAME} installed.  You must uninstall the currently installed version to continue." /SD IDOK IDOK UNINSTALL IDCANCEL QUIT
    ${ElseIf} $1 S> ${FULLVERSION} ;Older version is installed
        MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION "You are tring to install an older version of ${APPNAME} than the one you currently have installed. You must uninstall the currently installed verion to continue." /SD IDOK IDOK UNINSTALL IDCANCEL QUIT
    ${ElseIf} $1 S< ${FULLVERSION} ;Newer version is installed
        MessageBox MB_OKCANCEL|MB_ICONINFORMATION "You have a previous version of ${APPNAME} installed. The previous version will be uninrtalled so installation can continue." /SD IDOK IDOK UNINSTALL IDCANCEL QUIT
    ${EndIf}
    UNINSTALL:
        ReadRegStr $1 HKLM "SOFTWARE\${COMPANYNAME}\${APPNAME}" "Install Path"
        ; This leaves the uninstaller behind because it doesn't copy itself to a temp location. There's not a good way to clean that up that doesn't introduce potential race conditions on slow machines. :-/
        ExecWait '"$1\Uninstall ${APPNAME}.exe" /S _?=$1'
        Goto INSTALL
    NoJava:
        MessageBox MB_OK|MB_ICONSTOP "Java Not Detected. Please install a JRE of version ${JRE_MIN_VERSION} or greater." /SD IDOK
    QUIT:
        Quit
    INSTALL:
        Return
functionEnd

;--------------------------
; Where our install files go
InstallDir "$PROGRAMFILES\${APPNAME}" ;This is where our variables come in.
;----------------------------------------------

section "install"
    ; Install directory files - keep these in the same directory
    ; as the script before compiling.
    SetOutPath $INSTDIR\ ;Sets output path to our InstallDir
    File /r ..\..\lib
    File /r ..\..\extension
    File ..\..\hatch.bat
    File ..\..\hatch.properties
    File ..\..\logging.properties
    
    ; Set path variable in org.ils_evergreen.hatch.json to $INSTDIR\hatch.bat
    ${StrRep} '$0' '$INSTDIR' '\' '\\'
    !insertmacro _ReplaceInFile  "$INSTDIR\extension\host\org.evergreen_ils.hatch.json" "/path/to/hatch.sh" "$0\\hatch.bat"

    ; Uninstaller
    writeUninstaller "$INSTDIR\Uninstall ${APPNAME}.exe"

    ; Registry info for Add/Remove Programs
    WriteRegStr HKLM "SOFTWARE\${COMPANYNAME}\${APPNAME}" "Install Path" $INSTDIR
    WriteRegStr HKLM "SOFTWARE\${COMPANYNAME}\${APPNAME}" "Version" "${FULLVERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${COMPANYNAME} - ${APPNAME} - ${DESCRIPTION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\Uninstall ${APPNAME}.exe"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$INSTDIR\Uninstall ${APPNAME}.exe /S"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\logo.ico"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "${COMPANYNAME}"
    StrCmp "${HELPURL}" "" +2
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "HelpLink" "${HELPURL}"
    StrCmp "${UPDATEURL}" "" +2
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLUpdateInfo" "${UPDATEURL}"
    StrCmp "${ABOUTURL}" "" +2
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLInfoAbout" "${ABOUTURL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}"
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
    # There is no option for modifying or repairing the install
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
    # Set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "EstimatedSize" ${INSTALLSIZE}

    ; Connect Hatch to Chrome and install the Hatch extension from the Chrome Web Store
    WriteRegStr HKLM "SOFTWARE\Google\Chrome\NativeMessagingHosts\org.evergreen_ils.hatch" "" "$INSTDIR\extension\host\org.evergreen_ils.hatch.json"
    WriteRegStr HKLM "Software\Google\Chrome\Extensions\${EXTENSIONID}" "update_url" "${EXTENSION_UPDATEURL}"
SectionEnd


#############################
# Uninstaller code

function un.onInit
    SetShellVarContext all
    
    # Verify uninstaller
    MessageBox MB_OKCANCEL "Permanently remove ${APPNAME}?" /SD IDOK IDOK next
        Abort
    next:
    !insertmacro VerifyUserIsAdmin
functionEnd

section "uninstall"    
    # Remove the actual files
    Delete /REBOOTOK $INSTDIR\hatch.bat
    Delete /REBOOTOK $INSTDIR\hatch.properties
    Delete /REBOOTOK $INSTDIR\logging.properties
    ; blindly using /r isn't ideal but the extreme unlikelyhood of there being \lib or \extension folders under $PROGRAMFILES makes it low risk.
    RmDir /r /REBOOTOK $INSTDIR\extension
    RmDir /r /REBOOTOK $INSTDIR\lib
    # Delete uninstaller last
    Delete /REBOOTOK "$INSTDIR\Uninstall ${APPNAME}.exe"
    
    # Remove installation directory
    RmDir /REBOOTOK $INSTDIR
    
    # Remove uninstaller info from registry
    DeleteRegKey HKLM "SOFTWARE\${COMPANYNAME}\${APPNAME}"
    DeleteRegKey HKLM "SOFTWARE\${COMPANYNAME}"
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
    DeleteRegKey HKLM "SOFTWARE\Google\Chrome\NativeMessagingHosts\org.evergreen_ils.hatch"
    DeleteRegKey HKLM "SOFTWARE\Google\Chrome\Extensions\${EXTENSIONID}"
    ${If} ${RunningX64}
        DeleteRegKey HKLM "SOFTWARE\Wow6432Node\Google\Chrome\Extensions\${EXTENSIONID}"
    ${EndIf}

    IfRebootFlag 0 Done
    MessageBox MB_YESNO "A reboot is required to finish the installation. Do you wish to reboot now?" /SD IDNO IDNO Done
    Reboot

    Done:
    
sectionEnd
