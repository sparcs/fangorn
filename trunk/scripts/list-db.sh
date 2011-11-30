#!/bin/bash
java -cp .:./lib/derby.jar:./lib/derbytools.jar:./server/WEB-INF/lib/fangorn.jar au.edu.unimelb.csse.DB 1
