# RuntimeSamp

RuntimeSamp displays examples of variable values at the end of each line.

## Building

To build both the Java agent and the IDE plugin, run the command:

    ./gradlew build

## Running

First, [Redis](https://redis.io) must be running on localhost.

To open IntelliJ IDEA with the plugin installed, use the command:

    ./gradlew runIde

To enable data collection, add the following to the "VM options" of the selected run configuration (Run / Edit Configurations...):

    -javaagent:RUNTIMESAMP_PATH/dist/runtimesamp-agent-1.0-SNAPSHOT.jar=INSTRUMENT_REGEX

where RUNTIMESAMP_PATH is the root path of RuntimeSamp source code and INSTRUMENT_REGEX is a regular expression matching all classes which should be instrumented. The class names use slashes instead of dots (e.g., com/company/MyClass).
