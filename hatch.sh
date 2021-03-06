#!/bin/bash
#
# Linux/Mac Hatch Execution Script

# Use this for openjdk
JAVA_HOME=/usr
# Use this for local extract of Java libs
#JAVA_HOME=jdk1.8

JAVA=$JAVA_HOME/bin/java
JAVAC=$JAVA_HOME/bin/javac
JAR=$JAVA_HOME/bin/jar
LOGS=-Djava.util.logging.config.file=logging.properties
JSON_BUILD="20160810"
JSON_JAR="json-$JSON_BUILD.jar"
JSON_URL="https://search.maven.org/remotecontent?filepath=org/json/json/$JSON_BUILD/$JSON_JAR"

COMMAND="$1"

if [ "$COMMAND" == "compile" ]; then

    mkdir -p lib
    if [ ! -f lib/$JSON_JAR ]; then
        echo "Fetching JSON libs..."
        wget -O lib/$JSON_JAR $JSON_URL
    fi;

    $JAVAC -Xdiags:verbose -Xlint:unchecked \
        -cp lib/\* -d lib src/org/evergreen_ils/hatch/*.java

    # Create a JAR file from the compiled class files them remove them.
    $JAR cf lib/hatch.jar -C lib org
    rm -r lib/org

elif [ "$COMMAND" == "test" ]; then

    # 1. Run TestHatch in (default) send mode, which emits JSON requests
    # 2. Run Hatch and process messages emitted from #1.
    # 3. Run TestHatch in receive mode to log the responses.

    $JAVA "$LOGS" -cp lib/\* org.evergreen_ils.hatch.TestHatch \
        | $JAVA "$LOGS" -cp lib/\* org.evergreen_ils.hatch.Hatch \
        | $JAVA "$LOGS" -cp lib/\* org.evergreen_ils.hatch.TestHatch receive

else

    # run Hatch
    $JAVA "$LOGS" -cp lib/\* org.evergreen_ils.hatch.Hatch
fi;
