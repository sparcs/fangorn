#!/bin/bash
echo `date`;
java -cp .:./fangornL4.jar:./lib/lucene-core-4.2.1.jar -Xmx768m au.edu.unimelb.csse.exp.CorrectnessTest
echo `date`;

