#!/bin/bash

#本启动脚本支持在启动后，监测程序是否启动成功
#原理是yafw在启动时，在系统的临时目录下写一个文件，记录当前进程的PID
#启动成功后，再写一个文件，记录当前进程的PID
#脚本里用循环不断地检测这2个临时文件及对应的PID来判断启动成功/失败
#这样执行启动脚本，就能通过返回值是否为0（表示成功）判断是否启动成功了

#cygwin下需要特别设置/tmp的位置，否则将无法检测成功，按这样的步骤进行
#1、在java程序中通过System.getProperty("java.io.tmpdir")获取本机临时目录位置
#   例如获取到的是C:\Users\BlackCat\AppData\Local\Temp\
#2、在cygwin中，将临时目录链接到Windows系统的目录
#   rm -rf /tmp
#   ln -s /cygdrive/c/Users/BlackCat/AppData/Local/Temp /tmp

#中文系统下的cygwin如果想调用Windows命令和linux命令都不乱码就得把显示编码设置为GBK
#具体的做法是：
#    对于本机运行的cygwin，标题栏右键 -> Options -> Text
#    设置locale为zh_CN, Character set为GBK
#    关掉当前窗口，重新进入
#
#    对于SSH登录的cygwin，请在~/.bashrc中加入
#    export LANG="zh_CN.GBK"
#    并且使用的SSH客户端要把显示字符集改为GBK
#
#脚本输出的编码为UTF-8，这里要保证在linux和cygwin下输出中文都不会乱码
#这里需要针对这种情况特殊处理一下，先识别编码，然后在输出时增加转换机制
PRINT_ENCODING="UTF-8"
if echo $LANG|grep -iq GBK;then
    PRINT_ENCODING="GBK"
fi

#==============================================================================
#增强的print支持在不同的系统环境下中文正常输出显示
#==============================================================================
function print() {
    echo "$@" | iconv -f "UTF-8" -t $PRINT_ENCODING /dev/stdin
}

export LANG="zh_CN.UTF-8"
export LC_ALL="zh_CN.UTF-8"

if [ -z "$JAVA_MAIN_CLASS" ]; then
    print "请不要直接调用本脚本，并确保调用脚本中设置了JAVA_MAIN_CLASS"
    exit 1
fi

if [ -z "$SERVICE_NAME" ]; then
    print "请在调用脚本中设置SERVICE_NAME"
    exit 2
fi

if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-Xmx512m -Xms256m"
fi

if [ -z "$DEBUG_PORT" ]; then
    DEBUG_PORT=2222
fi

if [ -z "$JCONSOLE_PORT" ]; then
    JCONSOLE_PORT=3333
fi


#判断系统是否是cygwin，部分情况下要做特殊处理
#ulimit修改默认打开的文件句柄数，用于增加服务的并发连接数
CYGWIN_MODE=`cat /proc/version | grep -i 'cygwin' | wc -l`
if [ $CYGWIN_MODE -gt 0 ]; then
    ulimit -SHn 3200
    cp_split=";"
else
    ulimit -SHn 65535
    cp_split=":"
fi

PIDS=""
DEBUG_OPTS=""

#==============================================================================
#根据启动类类名搜索服务进程的pid
#==============================================================================
function get_pids() {
    #默认使用JDK自带的jps，如果命令无效就采用针对不同平台的替代实现
    jps > /dev/null 2>&1
    JPS_STATUS=$?
    if [ $JPS_STATUS -ne 0 ]; then
        if [ $CYGWIN_MODE -gt 0 ]; then
            PIDS=`wmic process get commandline, processid | grep -vi ' grep ' | grep -i 'java' | grep "$JAVA_MAIN_CLASS" | awk '{print $NF}'`
        else
            PIDS=`ps -e -o pid -o command | grep -vi ' grep ' | grep -i 'java' | grep "$JAVA_MAIN_CLASS" | awk '{print $1}'`
        fi
    else
        PIDS=`jps -l|grep "$JAVA_MAIN_CLASS" | awk '{print $1}'`
    fi

    if [ "$1" != "quiet" ]; then
        if [ -n "$SERVICE_NAME" ]; then
            print "SERVICE_NAME    = "$SERVICE_NAME
        fi
        print "JAVA_MAIN_CLASS = "$JAVA_MAIN_CLASS
        print ""
        if [ -z "$PIDS" ]; then
            print "没有找到服务进程"
        fi
    fi
}

#==============================================================================
#终止服务进程
#==============================================================================
function svr_stop() {
    get_pids
    if [ -n "$PIDS" ]; then
        for p in $PIDS
        do
            if [ $CYGWIN_MODE -gt 0 ]; then
                print "taskkill /f /t /pid "$p
                taskkill /f /t /pid $p
            else
                print "kill -9 "$p
                kill -9 $p
            fi
        done
        print ""
        print "服务进程已终止"
    fi
}

#==============================================================================
#启动服务前进行环境准备
#==============================================================================
function svr_test() {
    print ""
    java $JAVA_OPTS -version
    JAVA_STATUS=$?
    print ""
    if [ $JAVA_STATUS -ne 0 ]; then
        print "没有在PATH下找到java，请确定JDK已经正确安装并配置"
        exit 3
    fi

    JAVA_CLASSPATH="./conf"

    #支持遍历lib目录下的子目录，最后的结果按字母表升序排列
    #注意目录名不能有空格/;/:等符号
    scan_path[1]="./lib"
    for d in ${scan_path[@]}; do
        for f in `find $d -type d | sort`; do
            JAVA_CLASSPATH=$JAVA_CLASSPATH$cp_split$f"/*"
        done
    done

    if [ "$1" = "debug" ]; then
        DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address="$DEBUG_PORT" "
        DEBUG_OPTS="${DEBUG_OPTS} -Dcom.sun.management.jmxremote.port=${JCONSOLE_PORT} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false "
    fi

    if [ -n "$SERVICE_NAME" ]; then
        print "SERVICE_NAME    = "$SERVICE_NAME
    fi
    print "JAVA_MAIN_CLASS = "$JAVA_MAIN_CLASS
    print "JAVA_OPTS       = "$JAVA_OPTS
    print "DEBUG_OPTS      = "$DEBUG_OPTS
    print "CUSTOM_PARAM    = "$@
    print "HOME_DIR        = "`pwd`
    print "JAVA_CLASSPATH  = "$JAVA_CLASSPATH
    print ""

    print "LAUNCH_COMMAND  = java -cp "$JAVA_CLASSPATH" "$JAVA_OPTS" "$DEBUG_OPTS$JAVA_MAIN_CLASS" "$@" &"
    print ""

    #输出了classpth就不需要在启动命令中再加-cp了，这样用ps查看进程时，命令显示更简短
    export CLASSPATH=$JAVA_CLASSPATH
}

#==============================================================================
#启动服务，并监控是否启动成功
#==============================================================================
function svr_start() {
    launch_pid="/tmp/$JAVA_MAIN_CLASS.pid.launch"
    started_pid="/tmp/$JAVA_MAIN_CLASS.pid.started"

    #删除已有的文件，如果是其他终端用户也在运行启动脚本，那么先启动的PID信息就会被删除
    #这样先启动的用户的脚本在后续循环检测时，发现PID变化就知道是别的终端用户也在启动
    #将直接报错启动失败，让后启动的服务能启动成功
    rm -rf $launch_pid
    rm -rf $started_pid

    java $JAVA_OPTS $DEBUG_OPTS$JAVA_MAIN_CLASS $@ &
    #1s的时间按服务器的配置来说，是足够把进程起起来了（只是启动，不一定初始化完毕）
    #如果还不足以等到临时文件写入的话，那真的有必要换机器了，不该这么慢的
    sleep 1

    get_pids quiet
    #找不到进程，说明根本就没启动起来，直接认为失败
    if [ -z "$PIDS" ]; then
        exit 4
    fi

    java_pid=`cat $launch_pid`

    #循环检测判断启动成功/失败
    while :
    do
        #有时候启动比较慢，或者是临时目录设置有问题，这里无限循环尽量能让启动慢的能启动，不断的报错提示也能暴露临时目录的设置问题
        if [ -z "$java_pid" ];then
            java_pid=`cat $launch_pid`
            sleep 1
            continue
        fi
        curr_java_pid=`cat $launch_pid`
        if [ "$curr_java_pid" != "$java_pid" ]; then
            print "当前有其他的终端用户运行了启动脚本$SCRIPT_NAME，本次启动将被放弃"
            if [ $CYGWIN_MODE -gt 0 ]; then
                print "当前是cygwin环境，请确认已按$0开头部分的注释对临时目录进行设置了"
                print "taskkill /f /t /pid "$java_pid
                taskkill /f /t /pid $java_pid
            else
                print "kill -9 "$java_pid
                kill -9 $java_pid
            fi
            exit 5
        fi
        #检测进程还在不在，用来判断是否失败了（yafw启动失败会自动终止自身进程）
        get_pids quiet
        if [ -n "$PIDS" ]; then
            existing=0
            for p in $PIDS
            do
                if [ "$p" = "$java_pid" ]; then
                    existing=1
                fi
            done
            #说明进程都不在了，直接认为是启动失败
            if [ "$existing" = "0" ]; then
                exit 6
            fi
        else
            exit 7
        fi
        #判断是否启动成功，只要文件存在并且pid匹配就行
        if [ -f "$started_pid" ]; then
            started_java_pid=`cat $started_pid`
            if [ "$java_pid" = "$started_java_pid" ]; then
                exit 0
            fi
        fi
        sleep 1
    done
}

if [ "$1" = "stop" ]; then
    svr_stop
elif [ "$1" = "start" -o "$1" = "force" ]; then
    svr_test $@
    svr_start $@
elif [ "$1" = "restart" -o "$1" = "debug" ]; then
    svr_stop
    sleep 2
    svr_test $@
    svr_start $@
elif [ "$1" = "monitor" ]; then
    get_pids
    if [ -n "$PIDS" ]; then
        for p in $PIDS
        do
            print "当前服务进程PID = "$p
        done
    else
        svr_test $@
        svr_start $@
    fi
elif [ "$1" = "dump" ]; then
    get_pids
    if [ -n "$PIDS" ]; then
        for p in $PIDS
        do
            print "当前服务进程PID = "$p
            print ""
            print "jinfo "$p" > "$SERVICE_NAME"."$p".jinfo.log"
            jinfo $p > $SERVICE_NAME.$p.jinfo.log
            print ""
            print "jstack "$p" > "$SERVICE_NAME"."$p".jstack.log"
            jstack $p > $SERVICE_NAME.$p.jstack.log
            print ""
            print "jmap -heap "$p" > "$SERVICE_NAME"."$p".heap.log"
            jmap -heap $p > $SERVICE_NAME.$p.heap.log
            print ""
            print "jmap -histo "$p" > "$SERVICE_NAME"."$p".histo.log"
            jmap -histo $p > $SERVICE_NAME.$p.histo.log
            print ""
            print "jmap -dump:format=b,file="$SERVICE_NAME"."$p".dump "$p
            jmap -dump:format=b,file=$SERVICE_NAME.$p.dump $p
            print ""
            TAR_NAME=$SERVICE_NAME".dump."$p"."`date +%Y%m%d.%H%M`".tar.gz"
            print "tar --remove-files --exclude=\"*.gz\" -zcvf "$TAR_NAME" "$SERVICE_NAME"."$p".*"
            tar --remove-files --exclude="*.gz" -zcvf $TAR_NAME $SERVICE_NAME.$p.*
            print ""
            print "生成打包文件 "`du -h $TAR_NAME | awk '{print $1}'`
            print `pwd`"/"$TAR_NAME
        done
    fi
elif [ "$1" = "test" ]; then
    svr_test $@
    get_pids
    if [ -n "$PIDS" ]; then
        for p in $PIDS
        do
            print "当前服务进程PID = "$p
        done
    fi
else
    print ""
    print "用法: "$SCRIPT_NAME" [参数]"
    print ""
    print "  start      正常启动服务，服务器首次启动时使用，绑定端口时如果端口已经被占用"
    print "             会直接报错退出"
    print ""
    print "  stop       使用直接终止进程的方式关闭服务"
    print ""
    print "  force      替换重启服务：先另起一个进程，初始化完成后，在绑定端口时发现端口"
    print "             被占用了，就向原来的进程发送关闭指令，原进程关闭后端口释放，再由"
    print "             现进程占用端口完成重启，这种重启方式能尽可能减少重启的停顿期"
    print "             推荐重启服务时使用"
    print ""
    print "  restart    重启服务，先将原来的服务进程关闭，然后启动服务，建议只有改换了绑"
    print "             定端口，或是原服务进程卡死不能响应等情况时才用restart"
    print "             一般重启建议使用force"
    print ""
    print "  debug      关闭当前的服务并用debug模式重启，用于远程调试"
    print ""
    print "  monitor    检查服务进程是否存在，如果存在就输出进程信息，不存在就启动服务"
    print "             用于监控脚本监控服务进程的状态"
    print ""
    print "  dump       保存服务进程的内存堆栈等信息，用于排查问题"
    print ""
    print "  test       查看服务启动配置信息，不会启动服务"
fi
