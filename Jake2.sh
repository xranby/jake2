#!/bin/bash

cd `dirname $0`

# for jogl and joal
CP=lib/jogl/gluegen-rt.jar:lib/joal/joal.jar:lib/jogl/jogl-all.jar:dist/lib/jake2.jar

exec java -Xmx100M -cp $CP jake2.Jake2 $*
