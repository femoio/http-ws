#!/usr/bin/env bash

git clone https://gitlab.com/xjs/dynamic.git dynamic
cd dynamic
mvn install
cd ..

git clone https://gitlab.com/xjs/http.git http
cd http
mvn install
cd ..