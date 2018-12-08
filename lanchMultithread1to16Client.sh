#!/bin/bash
for i in `seq 1 16`;
do
	echo "Clients: $i"
    java -jar jars/WoCoServer.jar localhost 3000 true 8 &
    sleep 2
    for j in `seq 1 $i`;
	do
        java -jar jars/WoCoClient.jar localhost 3000 4 100 &
	done
	wait
    mkdir stored_log/multi/$i
    mv log/logClient/* stored_log/multi/$i
    sleep 1
done