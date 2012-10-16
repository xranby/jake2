#!/bin/bash

cd `dirname $0`/..

CP=lib/jake2.jar

exec java -Xmx64M -cp $CP $D_ARGS jake2.Jake2 +set dedicated 1 +set timeout 12000 $*
