#!/bin/bash

cd `dirname $0`

# for jogl and joal
CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:dist/lib/jake2.jar

#breaks VM's like avian
#X_ARGS="-Xmx100M"

# should be redundant (AWT not used anyways), however, due to a bug in avian, it's required
D0_ARGS="-Djava.awt.headless=true"

#D_ARGS="-Dnewt.debug.Window.MouseEvent"
#D_ARGS="-Dnewt.debug.Window.KeyEvent"
#D_ARGS="-Djogl.debug=all"
#D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.DebugGL"
#D_ARGS="-Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.FixedFuncImpl"
#D_ARGS="-Djogl.debug.FixedFuncPipeline"
#D_ARGS="-Djogl.debug.GLSLCode"
#D_ARGS="-Djogl.debug.ImmModeSink.Buffer"

#G_ARGS="+connect 10.1.0.52 +set cl_timeout 12000"
#G_ARGS="+set timeout 12000 +set cl_timeout 12000 +set gl_mode 0"
#G_ARGS="+set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref joglgl2"
#G_ARGS="+set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogles1"
G_ARGS="+set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogles2"
#G_ARGS="+set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogldummy"

exec java $X_ARGS -cp $CP $D0_ARGS $D_ARGS jake2.Jake2 $G_ARGS $*
