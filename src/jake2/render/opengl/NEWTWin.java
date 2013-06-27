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

import java.util.List;

import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

import jogamp.opengl.FPSCounterImpl;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.*;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorModeUtil;
import com.jogamp.opengl.GenericGLCapabilitiesChooser;

public class NEWTWin {
    MonitorMode oldDisplayMode = null;
    volatile Screen screen = null;
    volatile GLWindow window = null;
    volatile boolean shouldQuit = false;
    final FPSCounterImpl fpsCounter = new FPSCounterImpl();

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
     * @param driverName TODO
     * @return enum Base.rserr_t
     */
    public int setMode(GLProfile glp, Dimension dim, int mode, boolean fullscreen, String driverName) {
        final boolean isARM = Platform.CPUFamily.ARM == Platform.getCPUFamily();

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

        final MonitorDevice mainMonitor = window.getMainMonitor();
        
        if (oldDisplayMode == null) {
            oldDisplayMode = mainMonitor.getCurrentMode();
        }

        // We need to feed the NEWT Window to the NEWTKBD
        NEWTKBD.Init(window);
        
        window.addWindowListener(NEWTKBD.listener);
        window.addKeyListener(NEWTKBD.listener);
        window.addMouseListener(NEWTKBD.listener);

        if (fullscreen) {
            MonitorMode mm = findDisplayMode(newDim);
            final DimensionImmutable smDim = mm.getSurfaceSize().getResolution();
            newDim.setWidth( smDim.getWidth() );
            newDim.setHeight( smDim.getHeight() );
            mainMonitor.setCurrentMode(mm);
            window.setFullscreen(true);
        } else {
            window.setSize(newDim.getWidth(), newDim.getHeight());
            if (Globals.appletMode) {
                // Notify the size listener about the change
                final SizeChangeListener listener = Globals.sizeChangeListener;
                if (listener != null) {
                    listener.sizeChanged(newDim.getWidth(), newDim.getHeight());
                }
            }
        }
        window.setVisible(true);

        if (!Globals.appletMode) {
            while ( !window.isNativeValid()|| !window.isRealized() ) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
        }
        window.requestFocus();
        window.display(); // force GL resource validation
        window.setAutoSwapBufferMode(false);

        VID.Printf(Defines.PRINT_ALL, "...reques GLCaps "+window.getRequestedCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...chosen GLCaps "+window.getChosenGLCapabilities()+'\n');
        VID.Printf(Defines.PRINT_ALL, "...size "+window.getWidth()+" x "+window.getHeight()+'\n');
        fpsCounter.setUpdateFPSFrames(isARM ? 60 : 4*60, System.err);

        // propagateNewSize("init");
        activateGLContext();
        
        return Base.rserr_ok;
    }
    
    private void propagateNewSize() {
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

    protected final void activateGLContext() {
        final GLContext ctx = window.getContext();
        if ( null != ctx && GLContext.getCurrent() != ctx ) {                
            ctx.makeCurrent();
        }
    }
    
    protected final void deactivateGLContext() {
        final GLContext ctx = window.getContext();
        if ( null != ctx && GLContext.getCurrent() == ctx) {
            ctx.release();
        }        
    }
    
    /** Performs {@link GLWindow#swapBuffers()}, ticks the fps counter and performs <code>QUIT</code> if requested. */
    public final void endFrame() {
        window.swapBuffers();
        fpsCounter.tickFPS();
        if( shouldQuit ) {
            deactivateGLContext();
            Cbuf.ExecuteText(Defines.EXEC_APPEND, "quit");
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
            _window.destroy(); // same thing
        }
        if( withScreen && null != screen ) {
            screen.destroy();
            screen = null;
        }
    }    
    
}
