#!/bin/bash
FOLDER="tag"
for i in `seq 1 16`;
do
    mkdir stored_log/$FOLDER/$i/interval
    mkdir stored_log/$FOLDER/$i/percentile
    mv stored_log/$FOLDER/$i/clientInterval* stored_log/$FOLDER/$i/interval
    mv stored_log/$FOLDER/$i/clientPercent* stored_log/$FOLDER/$i/percentile
done