#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
HOME_DIR=`pwd`
cd ..
CONF_BASE_DIR=`pwd`
LIB_DIR=$HOME_DIR/lib
CONF_DIR=$CONF_BASE_DIR/conf/
LOG_DIR=$HOME_DIR/logs
GC_DIR=$HOME_DIR/gc
DUMP_DIR=$HOME_DIR/dump
DYLIB_DIR=$HOME_DIR/dylib/
PID=`ps -f|grep java|grep $CONF_DIR|awk '{print $2}'`
if [ -n "$PID" ];
then
   echo '服务已经启动！'
   exit 1
fi
if [ ! -d $LOG_DIR ];
then
   mkdir $LOG_DIR
fi
if [ ! -d $GC_DIR ];
then
   mkdir $GC_DIR
fi
if [ ! -d $DUMP_DIR ];
then
   mkdir $DUMP_DIR
fi
STD_OUT_FILE=$LOG_DIR/stdout.log

## set java path
if [ -z "$JAVA" ] ; then
  JAVA=$(which java)
fi
JAVA_DATA=" -Dlog.dir=$LOG_DIR -Dcom.sun.management.jmxremote.port=8090 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
LIB_JARS=`ls $LIB_DIR| grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`
JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
    JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:G1HeapRegionSize=16m -XX:HeapDumpPath=$DUMP_DIR/java_dump.hprof -Xloggc:$GC_DIR/gc.log -XX:+PrintGCDetails"
else
    JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:G1HeapRegionSize=16m -XX:HeapDumpPath=$DUMP_DIR/java_dump.hprof -Xloggc:$GC_DIR/gc.log -XX:+PrintGCDetails"
fi
echo "starting server....."
$JAVA $JAVA_MEM_OPTS $JAVA_DATA  -classpath $CONF_DIR:$LIB_JARS -Dlogback.home=$LOG_DIR -Djava.library.path=$DYLIB_DIR com.kanven.leech.starter.Starter 1>>$STD_OUT_FILE 2>&1 &
if [ $?==0 ];then
   echo "服务正常启动"
else
   echo "服务异常退出"
fi
exit $?
