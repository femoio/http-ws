#!/usr/bin/env bash

git clone https://gitlab.com/xjs/dynamic.git dynamic
cd dynamic
mvn install -Dmaven.javadoc.skip=true -DskipTests=true -B -V
cd ..

git clone https://github.com/femoio/http.git http
cd http
mvn install -Dmaven.javadoc.skip=true -DskipTests=true -B -V
cd ..