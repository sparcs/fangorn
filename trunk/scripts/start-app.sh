#!/bin/bash
SERVER_PORT=9090
CLASSPATH=".:./lib/jetty-6.1.26.jar:./lib/jetty-util-6.1.26.jar:./lib/servlet-api-2.5-20081211.jar:./lib/derby.jar:./lib/derbytools.jar:./lib/jsp-2.1/ant-1.6.5.jar:./lib/jsp-2.1/core-3.1.1.jar:./lib/jsp-2.1/jsp-2.1-glassfish-2.1.v20091210.jar:./lib/jsp-2.1/jsp-2.1-jetty-6.1.26.jar:./lib/jsp-2.1/jsp-api-2.1-glassfish-2.1.v20091210.jar:./server/WEB-INF/lib/lucene-core-2.4.jar:./server/WEB-INF/lib/fangorn.jar"
LOG_CONFIG="-Djava.util.logging.config.file=app.properties" 
java -cp $CLASSPATH $LOG_CONFIG au.edu.unimelb.csse.StartServer $SERVER_PORT
