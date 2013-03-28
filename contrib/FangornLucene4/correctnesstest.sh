#!/bin/bash
echo `date`;
java -cp .:./fangornL4.jar:./lib/lucene-core-4.0-SNAPSHOT.jar:./lib/lucene-analyzers-common-4.0-SNAPSHOT.jar -Xmx6144m au.edu.unimelb.csse.exp.CorrectnessTest
echo `date`;

