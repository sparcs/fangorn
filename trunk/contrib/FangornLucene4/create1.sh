#!/bin/bash
echo `date`;
java -cp .:./fangornL4.jar:./lib/lucene-core-4.0-SNAPSHOT.jar -Xmx6144m au.edu.unimelb.csse.CreateIndex /home/sumukh/data/wiki-char-john-T50-proc /opt/wiki-index
echo `date`;

