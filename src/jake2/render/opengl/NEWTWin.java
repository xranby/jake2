/*
 * NEWTWin.java
 * Copyright (C) 2004
 * 
 */
package jake2.render.opengl;

import jake2.Defines;
import jake2.Globals;
import jake2.SizeChangeListener;
import jake2.client.VID;
import jake2.game.cvar_t;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Cvar;
import jake2.render.Base;
import jake2.sys.NEWTKBD;

import java.io.PrintStream;
import java.util.List;

import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.*;

import jogamp.opengl.FPSCounterImpl;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.*;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.GenericGLCapabilitiesChooser;

public class NEWTWin {
    final static boolean DEBUG = false;
    /** Required due to AWT lock of surface, if Applet! */
    final static boolean FORCE_RELEASE_CTX_VAL = true; 
    
    MonitorMode oldDisplayMode = null;
    volatile Screen screen = null;
    volatile GLWindow window = null;
    volatile GameAnimatorControl animCtrl = null;
    /** Encapsulateed AWT dependency */
    volatile Object newtCanvasObject = null;
    boolean forceReleaseCtx = false;
    volatile boolean shouldQuit = false;
    volatile boolean shouldPause = false;
    volatile boolean shouldReparent = false;
    volatile boolean isAnimating = false;

    public List<MonitorMode> getModeList() {
        if( null != window ) {
            final MonitorDevice mainMonitor = window.getMainMonitor();
            return mainMonitor.getSupportedModes();
        } else {        
            return screen.getMonitorModes();
        }
    }

    public MonitorMode findDisplayMode(DimensionImmutable dim) {
        final List<MonitorMode> sml = MonitorModeUtil.filterByResolution(getModeList(), dim);
        if(sml.size() == 0) {
            return oldDisplayMode;
        }
        return sml.get(0);
    }

    public String getModeString(MonitorMode mm) {
        final SurfaceSize ss = mm.getSurfaceSize();
        final DimensionImmutable m = ss.getResolution();
        final StringBuffer sb = new StringBuffer();
        sb.append(m.getWidth());
        sb.append('x');
        sb.append(m.getHeight());
        sb.append('x');
        sb.append(ss.getBitsPerPixel());
        sb.append('@');
        sb.append(mm.getRefreshRate());
        sb.append("Hz");
        return sb.toString();
    }

    /**
     * @param dim
     * @param mode
     * @param fullscreen
     * @param driverName
     * @return enum Base.rserr_t
     */
    public int setMode(GLProfile glp, Dimension dim, int mode, boolean fullscreen, String driverName) {
        final Dimension newDim = new Dimension();

        VID.Printf(Defines.PRINT_ALL, "Initializing OpenGL display for profile "+glp+"\n");

        if(null == screen) {
            screen = NewtFactory.createScreen(NewtFactory.createDisplay(null), 0);
            screen.addReference(); // trigger native creation
        } else if( !screen.isNativeValid() ) {
            screen.addReference(); // trigger native creation
        }
        
        if (!VID.GetModeInfo(newDim, mode)) {
            VID.Printf(Defines.PRINT_ALL, " invalid mode\n");
            return Base.rserr_invalid_mode;
        }

        VID.Printf(Defines.PRINT_ALL, "...setting mode " + mode + ", " + newDim.getWidth() + " x " + newDim.getHeight() + ", fs " + fullscreen + ", driver " + driverName + "\n");

        // destroy the existing window, not screen
        shutdownImpl(false);
        
        if(null != window) {
            throw new InternalError("XXX");            
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        CapabilitiesChooser chooser = null; // default
        {
            final cvar_t v = Cvar.Get("jogl_rgb565", "0", 0);
            if( v.value != 0f ) {
                caps.setRedBits(5);
                caps.setGreenBits(6);
                caps.setBlueBits(5);
                chooser = new GenericGLCapabilitiesChooser(); // don't trust native GL-TK chooser
            }
        }
        
        window = GLWindow.create(screen, caps);
        window.setAutoSwapBufferMode(false);
        window.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE); // we do handle QUIT on our own, no GLWindow.display() called.
        window.setCapabilitiesChooser(chooser);
        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyNotify(WindowEvent e) {
                shouldQuit = !Globals.appletMode && null != window; // not applet and not already in shutdown ?
            }

            public void windowResized(WindowEvent e) {
                propagateNewSize();
            }
        });
        window.setTitle("Jake2 ("+driverName+"-newt-"+glp.getName().toLowerCase()+")");
        
        animCtrl = new GameAnimatorControl();
        window.setAnimator(animCtrl);

        final MonitorDevice mainMonitor = window.getMainMonitor();
        
        if (oldDisplayMode == null) {
            oldDisplayMode = mainMonitor.getCurrentMode();
        }

        // We need to feed the NEWT Window to the NEWTKBD
        NEWTKBD.Init(window);
        
        window.addWindowListener(NEWTKBD.listener);
        window.addKeyListener(NEWTKBD.listener);
        window.addMouseListener(NEWTKBD.listener);
        window.setSize(newDim.getWidth(), newDim.getHeight());
        
        isAnimating = true; // no display() invocation on other thread!
                
        if( !fullscreen && Globals.appletMode ) {
            forceReleaseCtx = FORCE_RELEASE_CTX_VAL;
            
            // Notify the size listener about the change
            final SizeChangeListener listener = Globals.sizeChangeListener;
            if (listener != null) {
                listener.sizeChanged(newDim.getWidth(), newDim.getHeight());
            }
            window.addKeyListener( new ReparentKeyListener() );
            
            final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(window);
            final java.applet.Applet applet = (java.applet.Applet) Globals.applet;
            final Runnable appletAddAction = new Runnable() {
                public void run() {
                    applet.add(newtCanvasAWT, java.awt.BorderLayout.CENTER);
                    applet.validate();
                    newtCanvasAWT.setFocusable(true);
                    newtCanvasAWT.requestFocus();
                    if( Platform.OSType.MACOS == Platform.getOSType() && newtCanvasAWT.isOffscreenLayerSurfaceEnabled() ) {
                        System.err.println("XXX Relayout");
                        // force relayout
                        final int cW = newtCanvasAWT.getWidth();
                        final int cH = newtCanvasAWT.getHeight();
                        newtCanvasAWT.setSize(cW+1, cH+1);
                        newtCanvasAWT.setSize(cW, cH);
                    }
                } };
                if( java.awt.EventQueue.isDispatchThread() ) {
                    System.err.println("XXX Adding on AWT EDT - same thread");
                    appletAddAction.run();
                } else {
                    System.err.println("XXX Adding on AWT EDT - off thread");
                    try {
                        java.awt.EventQueue.invokeAndWait(appletAddAction);
                    } catch (Exception e) {
                        throw new RuntimeException("NEWT Exception during NewtCanvasAWT on AWT-EDT", e);
                    }
                }
            newtCanvasObject = newtCanvasAWT;
            int w=0;
            while ( w<10 && !window.isNativeValid()|| !window.isRealized() ) {
                w++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            System.err.println("XXX waited = "+w+" * 100 ms");
        } else {
            forceReleaseCtx = false;            
            newtCanvasObject = null;
            
            if (fullscreen) {
                MonitorMode mm = findDisplayMode(newDim);
                final DimensionImmutable smDim = mm.getSurfaceSize().getResolution();
                newDim.setWidth( smDim.getWidth() );
                newDim.setHeight( smDim.getHeight() );
                mainMonitor.setCurrentMode(mm);
                VID.Printf(Defines.PRINT_ALL, "...MonitorMode "+mm+'\n');
                window.setFullscreen(true);
            }
            
            window.setVisible(true);
            window.requestFocus();
        }
        if( !window.isNativeValid()|| !window.isRealized() ) {
            throw new RuntimeException("NEWT window didn't not realize: "+window);
        }
        window.display(); // force GL creation
        final GLContext ctx = window.getContext(); 
        if( !ctx.isCreated() ) {
            System.err.println("Warning: GL context not created: "+ctx);
        }
        if( ctx.isCurrent() ) {
            throw new RuntimeException("NEWT GL context still current: "+ctx);
        }

        VID.Printf(Defines.PRINT_ALL, "...reques GLCaps "+window.getRequestedCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...chosen GLCaps "+window.getChosenGLCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...size "+window.getWidth()+" x "+window.getHeight()+'\n');

        // propagateNewSize("init");
        activateGLContext(true);
        
        return Base.rserr_ok;
    }
    
    private void propagateNewSize() {
        if( null != window ) {
            final int width = window.getWidth();
            final int height = window.getHeight();
            final int _width;
            final int mask = ~0x03;
            if ((width & 0x03) != 0) {
                _width = ( width & mask ) + 4;
            } else {
                _width = width;
            }
            VID.Printf(Defines.PRINT_ALL, "Resize: " + width + " x " + height + ", masked " + _width + "x" + height + "\n");  
    
            Base.setVid(_width, height);
            // let the sound and input subsystems know about the new window
            VID.NewWindow(_width, height);
        }
    }

    protected final boolean activateGLContext(boolean force) {
        boolean ctxCurrent = false;
        if( force || !shouldPause ) {
            final GLContext ctx = window.getContext();
            if ( null != ctx && GLContext.getCurrent() != ctx ) {
                if( DEBUG ) {
                    System.err.println("GLCtx Current pause "+shouldPause+": "+Thread.currentThread().getName());
                }
                ctxCurrent = GLContext.CONTEXT_NOT_CURRENT < ctx.makeCurrent();
            } else {
                ctxCurrent = true;
            }
            isAnimating = ctxCurrent;
        }
        return ctxCurrent;
    }
    
    protected final void deactivateGLContext() {
        final GLContext ctx = window.getContext();
        if ( null != ctx && GLContext.getCurrent() == ctx) {
            if( DEBUG ) {
                System.err.println("GLCtx Release pause "+shouldPause+": "+Thread.currentThread().getName());
            }
            ctx.release();
        }        
    }
    
    /** 
     * Performs {@link GLWindow#swapBuffers()}, ticks the fps counter and performs <code>QUIT</code> if requested. 
     */
    public final void endFrame() {
        window.swapBuffers();
        animCtrl.fpsCounter.tickFPS();
        if( shouldQuit ) {
            deactivateGLContext();
            Cbuf.ExecuteText(Defines.EXEC_APPEND, "quit");
        } else if( shouldReparent  ) {
            shouldReparent  = false;
            deactivateGLContext();
            if( null != newtCanvasObject && null != window ) {
                isAnimating = false; // don't let GLDrawableHelper.invoke(..) defer the GLRunnable (preserving GLState that is on OSX/CALayer) 
                final NewtCanvasAWT newtCanvasAWT = (NewtCanvasAWT) newtCanvasObject;
                if(null == window.getParent()) {
                    forceReleaseCtx = FORCE_RELEASE_CTX_VAL; // Applet
                    window.reparentWindow( newtCanvasAWT.getNativeWindow() );
                } else {
                    window.reparentWindow(null);
                    forceReleaseCtx = false;
                }
            }            
        } else if( forceReleaseCtx || shouldPause ) {
            deactivateGLContext();
        }
    }
    
    /** Performs <code>QUIT</code> if requested. */
    public final void checkQuit() {
        if( shouldQuit ) {
            deactivateGLContext();
            Cbuf.ExecuteText(Defines.EXEC_APPEND, "quit");
        }
    }
    
    void shutdown() {
        shutdownImpl(true);
    }
    
    private void shutdownImpl(boolean withScreen) {
        if ( null != window ) {
            deactivateGLContext();
            final GLWindow _window = window;
            window = null;
            _window.destroy();
            if( null != Globals.applet && null != newtCanvasObject ) {
                final java.applet.Applet applet = (java.applet.Applet) Globals.applet;
                final NewtCanvasAWT newtCanvasAWT = (NewtCanvasAWT) newtCanvasObject;
                final Runnable appletRemoveAction = new Runnable() {
                    public void run() {
                        applet.remove(newtCanvasAWT);
                        applet.validate();
                    } };
                    if( java.awt.EventQueue.isDispatchThread() ) {
                        appletRemoveAction.run();
                    } else {
                        try {
                            java.awt.EventQueue.invokeAndWait(appletRemoveAction);
                        } catch (Throwable e) {
                            System.err.println("Catched "+e.getClass().getName()+": "+e.getMessage());
                            e.printStackTrace();
                        }
                    }
                newtCanvasAWT.setNEWTChild(null);
                newtCanvasObject = null;
            }
        }
        if( withScreen && null != screen ) {
            try {
                screen.destroy();
            } catch (Throwable e) {
                System.err.println("Catched "+e.getClass().getName()+": "+e.getMessage());
                e.printStackTrace();
            }
            screen = null;
        }
    }    
    
    class GameAnimatorControl implements GLAnimatorControl {
        final FPSCounterImpl fpsCounter;
        final Thread thread;
        
        GameAnimatorControl() {
            final boolean isARM = Platform.CPUFamily.ARM == Platform.getCPUFamily();
            fpsCounter = new FPSCounterImpl();
            fpsCounter.setUpdateFPSFrames(isARM ? 60 : 4*60, System.err);
            thread = Thread.currentThread();
        }
        
        @Override
        public final boolean start() {
            return false;
        }

        @Override
        public final boolean stop() {
            shouldQuit = true;
            return true;
        }

        @Override
        public final boolean pause() {
            if( DEBUG ) {
                System.err.println("GLCtx Pause Anim: "+Thread.currentThread().getName());
                Thread.dumpStack();
            }
            shouldPause = true;
            return true;
        }

        @Override
        public final boolean resume() {
            shouldPause = false;
            return true;
        }

        @Override
        public final boolean isStarted() {
            return null != window;
        }

        @Override
        public final boolean isAnimating() {
            return isAnimating; // null != window && !shouldPause;
        }

        @Override
        public final boolean isPaused() {
            return null == window || shouldPause;
        }

        @Override
        public final Thread getThread() {
            return thread;
        }
        
        @Override
        public final void add(GLAutoDrawable drawable) {}

        @Override
        public final void remove(GLAutoDrawable drawable) {}
        
        @Override
        public final void setUpdateFPSFrames(int frames, PrintStream out) {
            fpsCounter.setUpdateFPSFrames(frames, out);
        }

        @Override
        public final void resetFPSCounter() {
            fpsCounter.resetFPSCounter();
        }

        @Override
        public final int getUpdateFPSFrames() {
            return fpsCounter.getUpdateFPSFrames();
        }

        @Override
        public final long getFPSStartTime() {
            return fpsCounter.getFPSStartTime();
        }

        @Override
        public final long getLastFPSUpdateTime() {
            return fpsCounter.getLastFPSUpdateTime();
        }

        @Override
        public final long getLastFPSPeriod() {
            return fpsCounter.getLastFPSPeriod();
        }

        @Override
        public final float getLastFPS() {
            return fpsCounter.getLastFPS();
        }

        @Override
        public final int getTotalFPSFrames() {
            return fpsCounter.getTotalFPSFrames();
        }

        @Override
        public final long getTotalFPSDuration() {
            return fpsCounter.getTotalFPSDuration();
        }

        @Override
        public final float getTotalFPS() {
            return fpsCounter.getTotalFPS();
        }
    }
    
    class ReparentKeyListener implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
           System.err.println(e);
           if( !e.isAutoRepeat() ) {
               int keyCode = e.getKeyCode();
               // FIXME: Workaround JOGL/NEWT Bug 798
               if( 0 == keyCode ) {
                   keyCode = e.getKeySymbol();
               }
               if( KeyEvent.VK_HOME == keyCode ) {
                   shouldReparent = true;
               }
           }
        }
        @Override
        public void keyReleased(KeyEvent e) { 
            System.err.println(e);
        }
    }
    
}
