#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
HOME_DIR=`pwd`
cd ..
CONF_BASE_DIR=`pwd`
CONF_DIR=$CONF_BASE_DIR/conf
PID=`ps -ef|grep java|grep $CONF_DIR|awk '{print $2}'`
echo "即将关闭服务:$PID"
if [ -n "$PID" ];
then
   kill -9 $PID
fi
if [ $?==0 ];then
   echo "服务关闭"
else
   echo "服务关闭异常"
fi
exit $?