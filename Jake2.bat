@echo off

REM for jogl and joal
SET CP=lib/jogl/gluegen-rt.jar;lib/joal/joal.jar;lib/jogl/jogl-all.jar;dist/lib/jake2.jar
java -Xmx100M -Dsun.java2d.noddraw=true -Djava.library.path=%LIB% -cp %CP% jake2.Jake2
