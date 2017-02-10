@echo off
REM Windows Hatch Execution Script
REM @echo off required for STDIO to work with the browser.

REM Assume java executables are in our path
SET JAVA=java
SET JAVAC=javac
SET JAR=jar

REM Optionally override the java path
REM SET JAVA_HOME="C:\Program Files\Java\jdk1.8.0_111"
REM SET JAVA=%JAVA_HOME%\bin\java
REM SET JAVAC=%JAVA_HOME%\bin\javac
REM SET JAR=%JAVA_HOME%\bin\jar


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

