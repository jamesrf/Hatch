@echo off
REM Windows Hatch Execution Script
REM @echo off required for STDIO to work with the browser.

REM NOTE: Do not EVER set ERRORLEVEL for any reason; it's a passthrough variable that takes on the current value
REM of the errorlevel return status but if you assign to it that magic passthrough is broken and it's just set to that value.
REM This is necessary to do sensible comparisons against it. (such as EQU) The 'if errorlevel' construct is completely bananas.
REM Also: automatic path testing only looks for 'java' - if you don't have the JDK in your path you'll need to fix that yourself.

REM Assume java executables are in our path
SET JAVA=java
SET JAVAC=javac
SET JAR=jar

REM Optionally override the java path
REM SET JAVA_HOME="C:\Program Files\Java\jdk1.8.0_111"
REM SET JAVA=%JAVA_HOME%\bin\java
REM SET JAVAC=%JAVA_HOME%\bin\javac
REM SET JAR=%JAVA_HOME%\bin\jar

REM Is anyone there?
%JAVA% --dry-run -cp "lib\*" org.evergreen_ils.hatch.Hatch 2>nul

IF %ERRORLEVEL% EQU 0 GOTO Huzzah

REM Are you still there?
SET JAVA=%PROGRAMDATA%\Oracle\Java\javapath\java
%JAVA% --dry-run -cp "lib\*" org.evergreen_ils.hatch.Hatch 2>nul

IF %ERRORLEVEL% EQU 0 GOTO Huzzah

REM I don't blame you
EXIT %ERRORLEVEL%

REM There you are.
:Huzzah

IF "%1" == "compile" (

    %JAVAC% -cp "lib\*" -Xdiags:verbose^
        -d lib src\org\evergreen_ils\hatch\*.java

    %JAR% cf lib\hatch.jar -C lib org
    rd /s /q lib\org

) ELSE (

    IF "%1" == "test" (

        %JAVA% -cp "lib\*"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.TestHatch | %JAVA% -cp "lib\*"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.Hatch | %JAVA% -cp "lib\*"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.TestHatch receive

    ) ELSE ( REM No options means run Hatch

        %JAVA% -cp "lib\*"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.Hatch

    )
)
