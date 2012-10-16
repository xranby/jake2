@echo off
SET PATH=lib\windows;%PATH%
SET CP=lib/jogamp/gluegen-rt.jar;lib/jogamp/joal.jar;lib/jogamp/jogl-all.jar;lib/jake2.jar
start javaw -Dsun.java2d.noddraw=true -cp %CP% jake2.Jake2
