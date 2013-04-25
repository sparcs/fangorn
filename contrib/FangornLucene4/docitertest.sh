#!/bin/bash
indexdir="/opt/wiki-index";
fileprefix=`date +"%m%d%H%M%S"`;
for mem in 4096 1024; do
file=`echo "$fileprefix$mem"`;
for query in `seq 0 28`; do
sudo sh clearcache.sh;
java -Xmx${mem}m -cp .:./fangornL4.jar:./lib/lucene-core-4.0-SNAPSHOT.jar:./lib/lucene-analyzers-common-4.0-SNAPSHOT.jar au.edu.unimelb.csse.exp.DocIterPerformanceTest $indexdir $query | tee -a $file;
done;
done;
