#!/bin/bash

# This file deletes an index from the system

# The index directory name is a required parameter to the script.
# If you are unsure about the directory name run list-db.sh to view the complete listing of all indexes.

if [ $# -ne 1 ]
then
    echo "Error in $0 - Invalid Argument Count"
    echo "Syntax: $0 <index_dir_name>"
    exit
fi;
java -cp .:./lib/jetty-6.1.26.jar:./lib/jetty-util-6.1.26.jar:./lib/servlet-api-2.5-20081211.jar:./lib/derby.jar:./lib/derbytools.jar:./lib/jsp-2.1/ant-1.6.5.jar:./lib/jsp-2.1/core-3.1.1.jar:./lib/jsp-2.1/jsp-2.1-glassfish-2.1.v20091210.jar:./lib/jsp-2.1/jsp-2.1-jetty-6.1.26.jar:./lib/jsp-2.1/jsp-api-2.1-glassfish-2.1.v20091210.jar:./server/WEB-INF/lib/lucene-core-2.4.jar:./server/WEB-INF/lib/fangorn.jar au.edu.unimelb.csse.DeleteIndex $1;

