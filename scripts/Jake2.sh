#!/bin/bash

cd `dirname $0`

CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:lib/jake2.jar

exec java -cp $CP jake2.Jake2
