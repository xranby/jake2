#!/bin/bash

cd `dirname $0`

# for jogl and joal
if [ -f "target/jake2.jar" ] ; then
  CP=target/jake2.jar
else
  CP=lib/jogamp/gluegen-rt.jar:lib/jogamp/joal.jar:lib/jogamp/jogl-all.jar:dist/lib/jake2.jar
fi

#breaks VM's like avian
X_ARGS="-Xmx100M"

# should be redundant (AWT not used anyways), however, due to a bug in avian, it's required
#D0_ARGS="-Djava.awt.headless=true"

#D_ARGS="-Dnewt.debug.Window.MouseEvent"
#D_ARGS="-Dnewt.debug.Window.KeyEvent"
#D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.Window"
#D_ARGS="-Dnewt.debug.Window"
#D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
#D_ARGS="-Dnewt.debug=all -Dnativewindow.debug.NativeWindow"
#D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.DebugGL"
#D_ARGS="-Djogl.debug.TraceGL"
#D_ARGS="-Djogl.debug.FixedFuncImpl"
#D_ARGS="-Djogl.debug.FixedFuncPipeline"
#D_ARGS="-Djogl.debug.GLSLCode"
#D_ARGS="-Djogl.debug.ImmModeSink.Buffer"
#D_ARGS="-Dnativewindow.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser"
#D_ARGS="-Djogl.debug.GLDrawable -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup=true"

#
# See README: JOGL2 Port for JOGL flags
#
#G_ARGS="+connect 10.1.0.52 +set cl_timeout 12000"
#G_ARGS="+set s_impl joal +set timeout 12000 +set cl_timeout 12000 +set gl_mode 0"
#G_ARGS="+set s_impl joal +set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogldummy"
#G_ARGS="+set s_impl joal +set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref joglgl2"
#G_ARGS="+set s_impl joal +set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogles2"
#G_ARGS="+set s_impl joal +set timeout 12000 +set cl_timeout 12000 +set gl_mode 0 +set vid_ref jogles1"

# G_ARGS2="+set vid_fullscreen 1"
# G_ARGS2="+set jogl_gl2 0 +set jogl_gl2es1 1 +set jogl_gl2es2 1 +set jogl_rgb565 1"
# G_ARGS2="+set gl_texturemode GL_LINEAR_MIPMAP_NEAREST"
# G_ARGS2="+set r_shadows 0 +set gl_shadows 0 +set gl_dynamic 0"
# G_ARGS2="+connect 10.1.0.52"

exec java $X_ARGS -cp $CP $D0_ARGS $D_ARGS jake2.Jake2 $G_ARGS $G_ARGS2 $* 2>&1 | tee Jake2.log
