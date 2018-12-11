#!/bin/bash

mkdir log/
mkdir size_log/
rm log/logClient/*
rm log/logServer/*


array=( 4 )

for i in "${array[@]}"
do
    echo "Size: $i KB"
    java -jar jars/WoCoServer.jar localhost 3000 true 8 &
    sleep 2
    for j in `seq 1 8`;
	do 
		java -jar jars/WoCoClient.jar localhost 3000 $i 20 &
	done
	wait
	rm -r size_log/$i
    mkdir size_log/$i
    mkdir size_log/$i/server
    mkdir size_log/$i/client
    mv log/logServer/* size_log/$i/server
    mv log/logClient/clientInterval* size_log/$i/client
    sleep 1
done