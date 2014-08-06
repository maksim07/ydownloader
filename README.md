# Downloader

  Current version support http sources loading without following redirects. 
  
# Build

  1. Pull from git:
  
  ```
  git clone git@github.com:maksim07/ydownloader.git
  ```
  
  2. Build it with maven:
  
  ```
  mvn package 
  ```
  
  You can find assembled tool in `target/downloader-1.0-SNAPSHOT-distr/downloader-1.0-SNAPSHOT` directory or packed in
  `downloader-1.0-SNAPSHOT-distr.tar.gz` archive.
  
# Command line tool

  Command line tool allows to download several URLs listed in job description file. Each line of the file is either url
  or a blank line. Those blank lines separate download requests. 
  
  Example of such file with two requests:
  
  ```
  http://www.example.com/file1.txt
  http://www.example.com/file2.txt
  
  http://www.example.com/file3.txt
  ```
  
  To launch the tool use following command:
  
  ```
  download.sh <job-descriptor-file>
  ```