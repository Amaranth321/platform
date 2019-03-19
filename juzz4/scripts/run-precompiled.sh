#!/bin/bash
PLAY=play
PLAY_ID=
if [ ! -z "$1" ]; then
    PLAY_ID=$1
else
    echo "Please specify play id"
    exit 1
fi
# Reset the RabbitMQ instance
# Do NOT reset RabbitMQ here, it will wipe out all the messages in queue
# Please run RabbitMQ as a daemon manually
# $ rabbitmq-server -detached
#if [ "$PLAY_ID" != "svc" ]; then
#    rabbitmqctl reset
#    rabbitmqctl start_app
#fi
shift
while echo $1 | grep ^- > /dev/null; do
    eval $( echo $1 | sed 's/-//g' | tr -d '\012')=$2
    shift
    shift
done

# Create logs directory if it doesn't exist
log_dir=`pwd`/logs
if [ ! -d "$log_dir" ]; then
    mkdir $log_dir
fi

supported_play=( \
play-1.2.k.4 \
play-1.2.k.2 \
play-1.2.k.1 \
play-1.2.k \
play-1.2.5 \
)

# Run MongoDB server
#mongod --journal &
if [ $PLAY_ID = "node" ]; then
    #mongod --fork --journal --logpath `pwd`/logs/mongodb.log --dbpath /mnt/data/mongodb
    rootdir=/root/VCABox
    for v in ${supported_play[@]}
    do
        if [ -d "$rootdir/$v" ]; then
            echo "$v exists"
            PLAY=$rootdir/$v/play
            break
        else
            PLAY=
        fi
    done
    if [ -z "$PLAY" ]; then
        echo "Could not find Play! directory"
        exit 2
    fi
#elif [ $PLAY_ID = "uat" ]; then
#    mongod --dbpath /mnt/data/db --journal --fork --logpath `pwd`/logs/mongodb.log
#elif [ "$PLAY_ID" != "svc" ]; then
#    mongod --journal --fork --logpath `pwd`/logs/mongodb.log
fi

# Run the application (target machine should have the framework ID set using 'play id' command, so that conf file settings are correctly picked up)
if test -z "$debug"; then
    $PLAY start -Dprecompiled=true -Duser.timezone=GMT -Djava.awt.headless=true --%$PLAY_ID
else
    $PLAY start -Dprecompiled=true -Duser.timezone=GMT -Djava.awt.headless=true --%$PLAY_ID -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=8000 -Djava.rmi.server.hostname=$debug
fi

# Run the application with OOM core dump enabled
#play run -Dprecompiled=true -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/ec2-user/dump
