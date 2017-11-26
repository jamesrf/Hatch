;=============================
; All defines go in here

;-----------------
; Base Info

!define APPNAME "Hatch" 
!define COMPANYNAME "Evergreen ILS"
!define DESCRIPTION "Java based Print Service and Local Storage System"
!define EXTENSIONID "ppooibdipmklfichpmkcgplfgdplgahl" ; Chrome extension id
!define EXTENSION_UPDATEURL "https://clients2.google.com/service/update2/crx" ; Chrome Web Store Autoupdate URL
;-----------------------------------
; Version info
; Version numbers should be integers
!define VERSIONMAJOR 0
!define VERSIONMINOR 1
!define VERSIONBUILD 2
!define FULLVERSION "${VERSIONMAJOR}.${VERSIONMINOR}.${VERSIONBUILD}"
;---------------------------
; Add Remove info
; The following will be displayed by the "Click here for support information" link
; in Add/Remove Programs.  You can use mailto: links here.
; Unless there's real info, leave these blank. The registry keys will be skipped if they're blank
!define HELPURL ""
!define UPDATEURL ""
!define ABOUTURL ""
; The size in KB of all the files we'll be installing
!define INSTALLSIZE 332000

;------------------------------
; Java
!define JRE_MIN_VERSION "1.8"

;------------------------------
; Page Info
; This includes the look & feel as well as text on certain pages
!define MUI_COMPONENTSPAGE_SMALLDESC ;No value
!define MUI_INSTFILESPAGE_COLORS "FFFFFF 000000" ;Two colors
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Header\nsis.bmp" ; optional
!define MUI_ABORTWARNING
!define MUI_RIGHTIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\win.bmp"
!define MUI_PAGE_HEADER_TEXT "Hatch Installer"
; Welcome Page Variables
!define MUI_WELCOMEPAGE_TEXT "Welcome to the Hatch install wizard.  This application will guide you through the installation for Hatch."
; License Page Variables
!define MUI_LICENSEPAGE_TEXT_TOP "Read through the license carefully."
!define MUI_LICENSEPAGE_TEXT_BOTTOM "If you accept the terms of the agreement, click I Agree to continue.  You must accept the agreement to install Hatch."
