#!/bin/bash
echo "Start to execute java"

javac  -cp ./SAR-RICM5-Chatroom/src/messages/engine/*.java ./SAR-RICM5-Chatroom/src/myMsgLevel/*.java
cd ./SAR-RICM5-Chatroom/bin
pwd
#The following commands should be executed sequentielly one by each time in independant terminal
java myMsgLevel.chatMain -host=chatServer -port=9991 -connection=ACCEPT
java myMsgLevel.chatMain -host=client_1 -port=9991 -connection=CONNECT
java myMsgLevel.chatMain -host=client_2 -port=9991 -connection=CONNECT
java myMsgLevel.chatMain -host=client_3 -port=9991 -connection=CONNECT
java myMsgLevel.chatMain -host=client_4 -port=9991 -connection=CONNECT

