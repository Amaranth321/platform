#!/usr/bin/env bash

./gradlew packJuzz4 \
-Dprefix=platform \
-Dtype=node \
-Dversion=1.2.3.4 \
-Dcloud_host=kaiup.kaisquare.com 