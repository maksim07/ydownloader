#!/bin/bash

sdir=`dirname $0`

java -cp $sdir/downloader-${project.version}.jar yand.downloader.DownloadTool $@
