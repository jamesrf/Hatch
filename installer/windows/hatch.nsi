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
!define MUI_PAGE_CUSTOMFUNCTION_PRE VersionChecker
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
    messageBox mb_iconstop "Administrator rights required!"
    setErrorLevel 740 ;ERROR_ELEVATION_REQUIRED
    quit
${EndIf}
!macroend

;---------------------------------------------------------------------
; 1. Check for Java
; 2. Read our current version and check if it's newer, older, or the same
; LogicLib gives S>, S<, and S== for comparing strings.
!macro VersionCheck
    ;JRE Check
    ClearErrors
    ${if} ${RunningX64}
        SetRegView 64 ;So we can read the Registry of 64 bit devices
    ${endif}
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
    IfErrors 0  NoAbort
        MessageBox MB_OK "Java not detected.  Setup will now exit."
        Quit
    NoAbort:
        ${If} $R0 S< ${JRE_MIN_VERSION}
            MessageBox MB_OK "You must update Java.  Setup will now exit."
        ${EndIf}
        ; Hatch Version Check
        ReadRegStr $R2 HKCU "Software\${COMPANYNAME}\${APPNAME}" "Version"
        ${If} $R2 = 0
            Goto INSTALL ;As you were, citizen.
        ${ElseIf} $R2 S== ${FULLVERSION} ;Same version is installed
            MessageBox MB_OKCANCEL|MB_ICONSTOP "You already have this version of ${APPNAME} installed.  You must uninstall the currently installed version to continue." IDOK UNINSTALL IDCANCEL QUIT
        ${ElseIf} $R2 S> ${FULLVERSION} ;Older version is installed
            MessageBox MB_OKCANCEL|MB_ICONSTOP "You are tring to install an older version of ${APPNAME} than the one you currently have installed. You must uninstall the currently installed verion to continue." IDOK UNINSTALL IDCANCEL QUIT
        ${ElseIf} $R2 S< ${FULLVERSION} ;Newer version is installed
            MessageBox MB_OKCANCEL|MB_ICONSTOP "You have a previous version of ${APPNAME} installed. You must uninstall the currently installed version to continue." IDOK UNINSTALL IDCANCEL QUIT
        ${EndIf}
        UNINSTALL:
            ExecWait '"$INSTDIR\Uninstall ${APPNAME}.exe"_?=$INSTDIR'
            Goto INSTALL
        QUIT:
            Quit
        INSTALL:

!macroend

function .onInit
    setShellVarContext all
    !insertmacro VerifyUserIsAdmin
functionEnd
;-----------------
;Check our version
function VersionChecker
    !insertmacro VersionCheck
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
    WriteRegStr HKCU "Software\${COMPANYNAME}\${APPNAME}" "Install Path" $INSTDIR
    WriteRegStr HKCU "Software\${COMPANYNAME}\${APPNAME}" "Version" "${FULLVERSION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${COMPANYNAME} - ${APPNAME} - ${DESCRIPTION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\Uninstall ${APPNAME}.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$\"$INSTDIR\Uninstall ${APPNAME}.exe$\" /S"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$\"$INSTDIR$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\ ${APPNAME}" "DisplayIcon" "$\"$INSTDIR\logo.ico$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "$\"${COMPANYNAME}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "HelpLink" "$\"${HELPURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLUpdateInfo" "$\"${UPDATEURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "URLInfoAbout" "$\"${ABOUTURL}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "$\"${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}$\""
    WriteRegStr HKCU "Software\Google\Chrome\NativeMessagingHosts\org.evergreen_ils.hatch" "" "$INSTDIR\extension\host\org.evergreen_ils.hatch.json"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
    # There is no option for modifying or repairing the install
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
    # Set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "EstimatedSize" ${INSTALLSIZE}

    ;Uncommend when extension is on web store
    ;${if} ${RunningX64}
        ;WriteRegStr HKLM "Software\Wow6432Node\Google\Chrome\Extensions\${EXTENSIONID}" "update_url" "${EXTENSION_UPDATEURL}"
    ;${EndIf}
    ;WriteRegStr HKLM "Software\Google\Chrome\Extensions\${EXTENSIONID}" "update_url" "${EXTENSION_UPDATEURL}"
SectionEnd


#############################
# Uninstaller code

function un.onInit
    SetShellVarContext all
    
    # Verify uninstaller
    MessageBox MB_OKCANCEL "Permanently remove ${APPNAME}?" IDOK next
        Abort
    next:
    !insertmacro VerifyUserIsAdmin
functionEnd

section "uninstall"    
    # Remove the actual files
    delete $INSTDIR\*.*
    rmDir /r $INSTDIR\extension
    rmDir /r $INSTDIR\lib
    # Delete uninstaller last
    delete "$INSTDIR\Uninstall ${APPNAME}.exe"
    
    # Remove installation directory
    rmDir $INSTDIR\
    
    # Remove uninstaller info from registry
    DeleteRegKey HKCU "Software\${COMPANYNAME}\${APPNAME}"
    DeleteRegKey HKCU "Software\${COMPANYNAME}"
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
    DeleteRegKey HKCU "Software\Google\Chrome\NativeMessagingHosts\org.evergreen_ils.hatch"
    DeleteRegKey HKLM "Software\Google\Chrome\Extensions\${EXTENSIONID}"
    ${if} ${RunningX64}
        DeleteRegKey HKLM "Software\Wow6432Node\Google\Chrome\Extensions\${EXTENSIONID}"
    ${EndIf}
    
sectionEnd
