#!/bin/sh
CP=conf/:classes/:lib_server_packed/*
SP=src/java/

/bin/rm -rf classes
/bin/mkdir -p classes/

javac -sourcepath ${SP} -classpath ${CP} -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java || exit 1

echo "fch class files compiled successfully"
