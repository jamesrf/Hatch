;---------------------------------------------
;   Supplemental Function
;   ReplaceInFile
;   http://nsis.sourceforge.net/ReplaceInFile

!macro _ReplaceInFile SOURCE_FILE SEARCH_TEXT REPLACEMENT
  Push "${SOURCE_FILE}"
  Push "${SEARCH_TEXT}"
  Push "${REPLACEMENT}"
  Call RIF
!macroend

Function RIF
 
  ClearErrors
 
  Exch $0      ; REPLACEMENT
  Exch
  Exch $1      ; SEARCH_TEXT
  Exch 2
  Exch $2      ; SOURCE_FILE
 
  Push $R0     ; SOURCE_FILE file handle
  Push $R1     ; temporary file handle
  Push $R2     ; unique temporary file name
  Push $R3     ; a line to sar/save
  Push $R4     ; shift puffer
 
  IfFileExists $2 +1 RIF_error
  FileOpen $R0 $2 "r"
 
  GetTempFileName $R2
  FileOpen $R1 $R2 "w"

  ;Loop through each line
  RIF_loop:
    FileRead $R0 $R3
    IfErrors RIF_leaveloop

    ;Search and Replace
    RIF_sar:
      Push "$R3"
      Push "$1"
      Push "$0"
      Call StrRep
      StrCpy $R4 "$R3"
      Pop $R3
      StrCmp "$R3" "$R4" +1 RIF_sar
    FileWrite $R1 "$R3"
  Goto RIF_loop
 
  RIF_leaveloop:
    FileClose $R1
    FileClose $R0
 
    ;Clean up $2
    Delete "$2"
    Rename "$R2" "$2"
 
    ClearErrors
    Goto RIF_out
 
  RIF_error:
    SetErrors
 
  RIF_out:
  Pop $R4
  Pop $R3
  Pop $R2
  Pop $R1
  Pop $R0
  Pop $2
  Pop $0
  Pop $1
 
FunctionEnd