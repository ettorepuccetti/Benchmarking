#!/bin/bash

OPS=4

mkdir log/
mkdir stored_log/
mkdir stored_log/multi
rm log/logClient/client*
rm log/logServer/*

array=(80)

for i in "${array[@]}";
do
	echo "Clients: $i"
    java -jar jars/WoCoServer.jar localhost 3000 true 8 &
    sleep 2
    for j in `seq 1 $i`;
	do
        java -jar jars/WoCoClient.jar localhost 3000 4 $OPS &
	done
	wait
    rm -r stored_log/multi/$i
    mkdir stored_log/multi/$i
    mkdir stored_log/multi/$i/interval
    mkdir stored_log/multi/$i/percentile
    mv log/logClient/clientInterval* stored_log/multi/$i/interval
    mv log/logClient/clientPercent* stored_log/multi/$i/percentile
    sleep 1
done