#!/bin/bash
indexdir="/opt/wiki-index";
byteformat="BYTE1111"
for run in 1 2 3; do
fileprefix=`date +"%m%d%H%M%S"`;
settingsfile=`echo "${fileprefix}.settings"`;
echo "Executing Run ${run} of CacheEffectTest with settings: IndexDir: $indexdir ; ByteFormat: $byteformat" | tee $settingsfile;
#for mem in 6144 5120 4096 3072 2048 1024; do
#for mem in 384 512 768; do
for mem in 1024 1536; do
file=`echo "$fileprefix$mem"`;
for query in `seq 0 28`; do
for join in 'BASELINE1' 'BASELINE2' 'MPMG1' 'MPMG2' 'STACKTREE' 'MPMG3' 'MPMG4' 'STAIRCASE' 'LATE' 'LATEMRR' 'TWIGSTACK' 'PATHSTACK'; do
sudo sh clearcache.sh;
java -Xmx${mem}m -cp .:./fangornL4.jar:./lib/lucene-core-4.2.1.jar au.edu.unimelb.csse.exp.CacheEffectTest $indexdir $join $query $byteformat | tee -a $file;
done;
done;
done;
done;

