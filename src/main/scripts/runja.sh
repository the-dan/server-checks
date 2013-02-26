#!/bin/bash

APP_ROOT=`dirname $0`
JAVA_OPTS=

CONFIG_FILE="$APP_ROOT"/runja.conf

if [[ ! -e $CONFIG_FILE ]]; then
    echo "Create config file $CONFIG_FILE first"
    exit 1
else
    . $CONFIG_FILE
fi


if [[ -z $JAVA_HOME ]]; then
	JAVA_HOME=/opt/java
fi
LIB_ROOT="$APP_ROOT"/libs
APP_PID="$APP_ROOT"/$APP.pid

LOG_DIR="$APP_ROOT"/logs

##
#
# TODO:
# - restart log
# - watching based on following & on number of lines
#
##

echo "App root: $APP_ROOT"
echo "Lib root: $LIB_ROOT"
echo "Pidfile: $APP_PID"
echo ""

function check() {
    if [[ ! -e "$LIB_ROOT" ]]; then
		echo "Libs dir $LIB_ROOT not found"
		return 1
    fi
    
    if [[ -n "$RUNAS_USER" && "$USER" != "$RUNAS_USER" ]]; then
		echo "Should be run under $RUNAS_USER"
		return 1
    fi
    
    if [[ -e "$APP_PID" && ! -w "$APP_PID" ]]; then
		echo "Pid file $APP_PID exists but not writable. Exiting"
		return 1
    fi
    
    
    if [[ ! -e "$APP_PID" && ! -w `dirname "$APP_PID"` ]]; then
		echo "Pid file $APP_PID DOES NOT exist. Directory is not writable. Exiting"
        return  2
    fi
    
    if [[ ! -e "$LOG_DIR" || ! -w "$LOG_DIR" ]]; then
		echo "Log directory $LOG_DIR DOES NOT exist or not writable. Exiting";
		return 3
    fi
}

function status() {
    if [[ ! -e "$APP_PID" ]]; then
		echo "Pidfile $APP_PID not found. Assuming process is not running"
		return 1
    fi

    if [[ -n "$RUNAS_USER" && "$USER" != "$RUNAS_USER" ]]; then
		echo "Should be run under $RUNAS_USER"
		return 1
    fi

    
    read PID < "$APP_PID"

    kill -0 $PID > /dev/null 2>&1
    RETVAL=$?

    if [[ $RETVAL -eq 0 ]]; then
		echo "Process is active. PID $PID"
		return 0
    else
		echo "Process is dead"
		return 2
    fi
}

function thread_dump() {

    if [[ -n "$RUNAS_USER" && "$USER" != "$RUNAS_USER" ]]; then
		echo "Should be run under $RUNAS_USER"
		return 1
    fi
    
    status
    RETVAL=$?
    [[ $RETVAL -ne 0 ]] && echo "Process is not running according to status. Exiting" && exit 1
    
    read PID < $APP_PID
    
    kill -QUIT $PID

}

function force_stop() {

    if [[ -n "$RUNAS_USER" && "$USER" != "$RUNAS_USER" ]]; then
		echo "Should be run under $RUNAS_USER"
		return 1
    fi
    
    status
    RETVAL=$?
    [[ $RETVAL -ne 0 ]] && echo "Process is not running according to status. Exiting" && exit 1
    
    read PID < $APP_PID
    
    kill -9 $PID

    echo "KILL signal sent"
}

function stop() {

    if [[ -n "$RUNAS_USER" && "$USER" != "$RUNAS_USER" ]]; then
		echo "Should be run under $RUNAS_USER"
		return 1
    fi
    
    status
    RETVAL=$?
    [[ $RETVAL -ne 0 ]] && echo "Process is not running according to status. Exiting" && exit 1
    
    read PID < $APP_PID
    
    kill $PID

    echo "TERM signal sent"
    
}

function watch() {
    # TODO: follow and/or numoflines options
    tail -f $LOG_DIR/out.log $LOG_DIR/err.log
}

function start_java() {

    CLASSPATH=
    PID=

    if [[ -z $CLASS ]]; then
		export CLASSPATH="$APP_ROOT"/"$JAR":$(find $LIB_ROOT -name '*.jar' -exec printf {}: ';')
		shift
		"$JAVA_HOME"/bin/java $JAVA_OPTS -jar "$JAR" $PROC_OPTS $* >> "$LOG_DIR/out.log" 2>> "$LOG_DIR/err.log" &
		PID=$!
    else
		export CLASSPATH="$APP_ROOT"/"$JAR":$(find $LIB_ROOT -name '*.jar' -exec printf {}: ';')
		shift
		"$JAVA_HOME"/bin/java $JAVA_OPTS $CLASS $PROC_OPTS $* >> "$LOG_DIR/out.log" 2>> "$LOG_DIR/err.log" &
		PID=$!
    fi

    echo $PID > $APP_PID

}

function cron() {
    # same as start but quiet

    check
    RETVAL=$?
    [[ $RETVAL -ne 0 ]] && exit $RETVAL

    status
    RETVAL=$?
    
    if [[ $RETVAL -eq 0 ]]; then
	exit 0
    fi

    start_java

    read PID < $APP_PID

    echo "Started (pid= $PID)"
    
}

function start() {

    check
    RETVAL=$?
    [[ $RETVAL -ne 0 ]] && exit $RETVAL

    status
    RETVAL=$?
    
    if [[ $RETVAL -eq 0 ]]; then
		echo "Process is running already. Exiting"
		exit 1
    fi

    start_java $*
    
    read PID < $APP_PID
    
    echo "Started (pid= $PID)"
    
}
function run() {
	if [[ -z $CLASS ]]; then
		export CLASSPATH="$APP_ROOT"/"$JAR":$(find $LIB_ROOT -name '*.jar' -exec printf {}: ';')
		exec -a "$APP" "$JAVA_HOME"/bin/java $JAVA_OPTS -jar "$JAR" $PROC_OPTS $*
    else
		export CLASSPATH="$APP_ROOT"/"$JAR":$(find $LIB_ROOT -name '*.jar' -exec printf {}: ';')
		case `uname` in
			CYGWIN*)
				CLASSPATH=`cygpath -w -p $CLASSPATH`
				;;
		esac
		exec -a "$APP" "$JAVA_HOME"/bin/java $JAVA_OPTS $CLASS $*
    fi
}


if [[ $# -lt 1 ]]; then
    echo "usage: $0 {start|stop|restart|status|td|kill|watch|cron|run}"
    exit 1
fi

case $1 in
	run)
		shift
		run $*
		;;
    start)
		start $*
		;;
    stop)
		stop
		;;
    status)
		status
		;;
    restart)
		stop
		start
		;;
    td)
		thread_dump
		;;
    watch)
		watch
		;;
    cron)
		cron
		;;
    "kill")
		force_stop
		;;
	*)
		echo "command not found"
		exit 1
		;;
esac
