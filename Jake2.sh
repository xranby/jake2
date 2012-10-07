#!/bin/bash

cd `dirname $0`

# for jogl and joal
CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:dist/lib/jake2.jar

exec java -Xmx100M -cp $CP jake2.Jake2 $*
