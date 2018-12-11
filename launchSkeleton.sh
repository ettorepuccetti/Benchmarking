#!/bin/bash

OPS=150

mkdir stored_log/skeleton
rm log/logClient/client*
rm log/logServer/*

array=(16)

for i in `seq 1 5`;
do
	echo "Clients: $i"
    java -jar jars/WoCoServerSkeleton.jar localhost 3000 false 1 &
    sleep 2
    for j in `seq 1 $i`;
	do
        java -jar jars/WoCoClient.jar localhost 3000 4 $OPS &
	done
	wait
    rm -r stored_log/skeleton/$i
    mkdir stored_log/skeleton/$i
    mkdir stored_log/skeleton/$i/interval
    mkdir stored_log/skeleton/$i/percentile
    mv log/logClient/clientInterval* stored_log/skeleton/$i/interval
    mv log/logClient/clientPercent* stored_log/skeleton/$i/percentile
    sleep 1
done