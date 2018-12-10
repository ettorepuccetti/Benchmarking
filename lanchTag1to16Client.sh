#!/bin/bash
mkdir stored_log/tag
rm log/logClient/client*

for i in `seq 7 9`;
do
	echo "Clients: $i"
    java -jar jars/WoCoServerTag.jar localhost 3000 true 1 &
    sleep 2
    for j in `seq 1 $i`;
	do
        java -jar jars/WoCoClient.jar localhost 3000 4 100 &
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