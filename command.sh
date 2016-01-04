#!/bin/bash
echo "Start to execute java"

javac src/messages/engine/*.java src/myMsgLevel/*.java
cd bin
pwd
#The following commands should be executed sequentielly one by each time 
java myMsgLevel.chatMain -host=p1 -port=9991 -connection=ACCEPT
java myMsgLevel.chatMain -host=p2 -port=9993,9991 -connection=ACCEPT,CONNECT
java myMsgLevel.chatMain -host=p3 -port=9995,9991,9993 -connection=ACCEPT,CONNECT,CONNECT
java myMsgLevel.chatMain -host=p3 -port=9997,9991,9993,9995 -connection=ACCEPT,CONNECT,CONNECT,CONNECT
java myMsgLevel.chatMain -host=p3 -port=9999,9991,9993,9995,9997 -connection=ACCEPT,CONNECT,CONNECT,CONNECT,CONNECT

