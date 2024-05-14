#!/bin/bash

flatc -b -j -o ../abcvlibApp/libs/abcvlib/src/main/java/ episode.fbs
flatc -b -p -o ../python/ --force-empty-vectors episode.fbs
