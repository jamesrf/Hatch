#!/bin/bash
#
# Linux/Mac Hatch Execution Script

JAVA_HOME=jdk1.8
JAVA=$JAVA_HOME/bin/java
JAVAC=$JAVA_HOME/bin/javac
LOGS=-Djava.util.logging.config.file=logging.properties

COMMAND="$1"

if [ "$COMMAND" == "compile" ]; then

    $JAVAC -Xdiags:verbose -Xlint:unchecked \
        -cp lib:lib/\* -d lib src/org/evergreen_ils/hatch/*.java

elif [ "$COMMAND" == "test" ]; then

    # 1. Run TestHatch in (default) send mode, which emits JSON requests
    # 2. Run Hatch and process messages emitted from #1.
    # 3. Run TestHatch in receive mode to log the responses.

    $JAVA "$LOGS" -cp lib:lib/\* org.evergreen_ils.hatch.TestHatch \
        | $JAVA "$LOGS" -cp lib:lib/\* org.evergreen_ils.hatch.Hatch \
        | $JAVA "$LOGS" -cp lib:lib/\* org.evergreen_ils.hatch.TestHatch receive

else

    # run Hatch
    $JAVA "$LOGS" -cp lib:lib/\* org.evergreen_ils.hatch.Hatch
fi;