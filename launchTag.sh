#!/bin/bash

OPS=4
mkdir log/
mkdir stored_log/
mkdir stored_log/tag
rm log/logClient/client*
rm log/logServer/*

array=(80)

for i in "${array[@]}";
do
	echo "Clients: $i"
    java -jar jars/WoCoServerTag.jar localhost 3000 true 1 &
    sleep 2
    for j in `seq 1 $i`;
	do
        java -jar jars/WoCoClient.jar localhost 3000 4 $OPS &
	done
	wait
    rm -r stored_log/tag/$i
    mkdir stored_log/tag/$i
    mkdir stored_log/tag/$i/interval
    mkdir stored_log/tag/$i/percentile
    mv log/logClient/clientInterval* stored_log/tag/$i/interval
    mv log/logClient/clientPercent* stored_log/tag/$i/percentile
    sleep 1
done