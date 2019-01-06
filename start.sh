#!/bin/sh
if [ -e ~/.nxt/nxt.pid ]; then
    PID=`cat ~/.nxt/nxt.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Fch server already running"
        exit 1
    fi
fi
mkdir -p ~/.nxt/
java -cp classes:lib_server_packed/*:conf:addons/classes:addons/lib/* nxt.Nxt > /dev/null 2>&1 &
echo $! > ~/.nxt/nxt.pid
