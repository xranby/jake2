#!/bin/bash

cd `dirname $0`

# for jogl and joal
CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:dist/lib/jake2.jar

#D_ARGS="-Dnewt.debug.Window.MouseEvent"
#D_ARGS="-Dnewt.debug.Window.KeyEvent"
#D_ARGS="-Djogl.debug=all"
#D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.DebugGL"
#D_ARGS="-Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.FixedFuncImpl"
#D_ARGS="-Djogl.debug.FixedFuncPipeline"

#G_ARGS="+connect 10.1.0.52 +set cl_timeout 12000"

exec java -Xmx100M -cp $CP $D_ARGS jake2.Jake2 $G_ARGS $*
