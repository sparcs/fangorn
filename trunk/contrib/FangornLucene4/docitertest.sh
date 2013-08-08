#!/bin/bash
indexdir="/opt/wiki-index";
for mem in 1024; do
for trial in 1 2 3; do
echo "Running trial $trial; memory $mem; indexdir: $indexdir";
fileprefix=`date +"%m%d%H%M%S"`;
file=`echo "$fileprefix$mem"`;
for query in `seq 0 44`; do
sudo sh clearcache.sh;
java -Xmx${mem}m -cp .:./fangornL4.jar:./lib/lucene-core-4.2.1.jar au.edu.unimelb.csse.exp.DocIterPerformanceTest $indexdir $query | tee -a $file;
done;
done;
done;
