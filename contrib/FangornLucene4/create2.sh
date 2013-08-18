#!/bin/bash
echo "creating subset indexes"
for i in 1 2 3; do
echo "Index ${i} start time `date`";
java -cp .:./fangornL4.jar:./lib/lucene-core-4.2.1.jar -Xmx6144m au.edu.unimelb.csse.CreateIndex /home/sumukh/data/wiki-char-john-T50-proc-${i} /opt/wiki-index-${i} > >(tee stdout${i}.log) 2> >(tee stderr${i}.log >&2)
echo "Index ${i} end time `date`";
done;

