@echo off
REM Windows Hatch Execution Script
REM @echo off required for STDIO to work with the browser.
SET JAVA_HOME="C:\Program Files\Java\jdk1.8.0_111"
SET JAVA=%JAVA_HOME%\bin\java
SET JAVAC=%JAVA_HOME%\bin\javac

IF "%1" == "compile" (

    %JAVAC% -cp "lib\*" -Xdiags:verbose^
        -d lib src\org\evergreen_ils\hatch\*.java

) ELSE (

    IF "%1" == "test" (

        %JAVA% -cp "lib\*;lib"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.TestHatch | %JAVA% -cp "lib\*;lib"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.Hatch | %JAVA% -cp "lib\*;lib"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.TestHatch receive

    ) ELSE ( REM No options means run Hatch

        %JAVA% -cp "lib\*;lib"^
            -Djava.util.logging.config.file=logging.properties^
            org.evergreen_ils.hatch.Hatch

    )
)
