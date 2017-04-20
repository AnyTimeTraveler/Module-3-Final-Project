# Module-3-Final-Project

This project focuses on creating a relieable ad-hoc network.
As a proof of concept, a file transfer system and a chat was implemented using it.

## Running

### Prerequisites
* Java 8 (Not Java 7, Not Java 9)
* Maven2

### Building instructions
* Enter the root of the repository in a command-line.
* Enter: ```mvn clean compile assembly:single```
* Your jar should be deposited in ```./target/Module-3-Final-Project-<version>-jar-with-dependencies.jar``` where <version> is replaced with the release version.

### To execute
* Enter: ```java -jar ./target/Module-3-Final-Project-<version>-jar-with-dependencies.jar``` Where you replace <version> with the release version.
