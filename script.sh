#!/bin/bash
echo "server"
#Usage:         <listenaddress> <listenport> <cleaning> <threadcount> [<nclients> default:1]
java -jar jars/WoCoServer.jar localhost 3000 true 8 &
sleep 3
for i in `seq 0 15`;
do
	echo "Clients: $i"
    # Usage:           <servername> <serverport> <documentsize(KiB)> <opcount(x1000)> [<seed>]
	java -jar jars/WoCoClient.jar localhost 3000 4 100 &	#>> outclient$i.csv
done   



