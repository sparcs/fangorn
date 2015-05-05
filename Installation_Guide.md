# Fangorn Installation Guide #

## Requirements ##
  * Java 5 and above
  * Preferably more than 1 GB RAM
  * Apache Ant (if compiling from source)

## Installing from binaries ##

  1. Download one of the archived binaries provided in the Downloads page
  1. Decompress the archive
  1. To start the app run the following command in the project's main directory
    * $ sh start-app.sh
  1. Point a browser to http://localhost:9090 to view the search page

## Installing from source ##

  1. Checkout the source code (lets call the checkout out directory PROJECT\_ROOT)
  1. Build the software by running ant from PROJECT\_ROOT:
    * $ cd PROJECT\_ROOT
    * $ ant dist
  1. If the build is successful, the above command will create a 'fangorn' folder in the 'PROJECT\_ROOT/dist' directory
  1. Change to the fangorn dist directory:
    * $ cd PROJECT\_ROOT/dist/fangorn
    * Note that you can copy the PROJECT\_ROOT/dist/fangorn directory to any other directory on your computer and run the app from there
  1. Start the app by running the following command in PROJECT\_ROOT/dist/fangorn
    * $ sh start-app.sh
  1. Point a browser to http://localhost:9090 to view the search page

## Adding and removing corpora ##

  * There are two files in the same directory as start-app.sh which support the addition and deletion of corpora
  * create-index-GZ.sh indexes gzipped corpora
  * create-index-MRG.sh indexes corpora formatted as plain text files with the extension .mrg

Further details on how to use fangorn are available in the README file.