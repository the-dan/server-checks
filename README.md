Different utilities to primarily debug JVM behavior on Linux. Useful while rolling out your own script to run Java daemon or just to check how OS settings affect Java.


Build
======

[![Build Status](https://travis-ci.org/the-dan/server-checks.svg?branch=master)](https://travis-ci.org/the-dan/server-checks)

For zip package:

$ mvn clean package assembly:single

For real executable file:
$ mvn clean package
$ cat src/main/scripts/rex.sh target/server-checks.jar > ./sc
$ chmod u+x ./sc

While in developement mode, it's more convenient (although not very convenient, but better than running from IDE)
to use mvn exec plugin to run all this stuff:

$ mvn -Dexec.args="" exec:java

Socket testing
================
If you don't want to delay sending till the end of the line (i.e. send entered character right away), you may use telnet in 'character mode'.

E.g.:
telnet localhost 5050
mode character

Usage
======

Usage: black-hole [options]
  Options:
    -d, --delay        Delays N ms after each iteration
                       Default: 100
    -f, --file         Read this file into memory
    -i, --iterations   Adds iterations count of "0123456789" into array
                       Default: 1000000

Usage: date [options]
  Options:
    -d, --date   Date in dd.mm.yyyy format to get GMT offset at

Usage: max-threads [options]
  Options:
    -c, --count   Number of threads to create
                  Default: 100
    -s, --sleep   How many seconds created thread will sleep for. Long.MAX_LONG
                  if absent
                  Default: 9223372036854775807

Usage: nslookup [options]
  Options:
    -a, --address   Address to resolve

Usage: sleepy [options]
  Options:
    -s, --sleep   Be active for, secs. Use 0 for infinity. Default: 60 secs
                  Default: -1

Usage: max-fd [options]

Usage: env [options]

Usage: slowpoke [options]
  Options:
    -p, --port   Port to bind to. Default: 10000
                 Default: 10000
    -u, --uri    URI to create info servlet at. Default: /info
                 Default: /info
