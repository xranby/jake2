#!/bin/bash

cd `dirname $0`

# for jogl and joal
CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:dist/lib/jake2.jar

#D_ARGS="-Dnewt.debug.Window.MouseEvent"
#D_ARGS="-Dnewt.debug.Window.KeyEvent"

exec java -Xmx100M -cp $CP $D_ARGS jake2.Jake2 $*
