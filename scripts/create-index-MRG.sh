#!/bin/bash

# This file creates an index from corpus files

# The first parameter points to the source directory of the corpus.
# All files ending with the suffix .mrg within the source directory or its subdirectories are treated as corpus files.

# The second parameter points is the index directory name.
# The index directory names should be unique for each searchable corpus.

# The third parameter is a name given to the indexed corpus.
# This will be used in the UI

# The fourth parameter specifies a limit on the number of sentences
# Enter -1 if all sentences in the directory should be indexed
if [[ $# -ne 3 && $# -ne 4 ]]
then
    echo "Error in $0 - Invalid Argument Count"
    echo "Syntax: $0 <data_source> <index_dir_name> <index_name> [<number_of_sents>]"
    echo "See README file for more details."
    exit
fi;
NOS=$4;
: ${NOS:="-1"};
java -cp .:./lib/jetty-6.1.26.jar:./lib/jetty-util-6.1.26.jar:./lib/servlet-api-2.5-20081211.jar:./lib/derby.jar:./lib/derbytools.jar:./lib/jsp-2.1/ant-1.6.5.jar:./lib/jsp-2.1/core-3.1.1.jar:./lib/jsp-2.1/jsp-2.1-glassfish-2.1.v20091210.jar:./lib/jsp-2.1/jsp-2.1-jetty-6.1.26.jar:./lib/jsp-2.1/jsp-api-2.1-glassfish-2.1.v20091210.jar:./server/WEB-INF/lib/lucene-core-2.4.jar:./server/WEB-INF/lib/fangorn.jar au.edu.unimelb.csse.CreateIndex $1 $2 $3 $NOS false;

