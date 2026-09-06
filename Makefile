.PHONY: all main clean

all: main

main:
	mvn package && java \
	  --enable-native-access=ALL-UNNAMED \
	  -jar target/codecrafters-claude-code.jar

clean:
	mvn clean
