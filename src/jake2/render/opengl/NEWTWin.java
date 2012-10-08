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
import jake2.qcommon.Cbuf;
import jake2.render.Base;
import jake2.sys.NEWTKBD;

import java.util.List;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.SurfaceSize;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import jogamp.opengl.FPSCounterImpl;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.MonitorMode;
import com.jogamp.newt.util.ScreenModeUtil;

public class NEWTWin {
    ScreenMode oldDisplayMode = null;
    volatile Screen screen = null;
    volatile GLWindow window = null;
    final FPSCounterImpl fpsCounter = new FPSCounterImpl();

    public ScreenMode[] getModeList() {
        final List<ScreenMode> sml = screen.getScreenModes();
        ScreenMode[] sma = new ScreenMode[sml.size()];
        sml.toArray(sma);
        return sma;
    }

    public ScreenMode findDisplayMode(DimensionImmutable dim) {
        final List<ScreenMode> sml = ScreenModeUtil.filterByResolution(screen.getScreenModes(), dim);
        if(sml.size() == 0) {
            return oldDisplayMode;
        }
        return sml.get(0);
    }

    public String getModeString(ScreenMode sm) {
        final MonitorMode mm = sm.getMonitorMode();
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
     * @return enum Base.rserr_t
     */
    public int setMode(Dimension dim, int mode, boolean fullscreen) {

        final Dimension newDim = new Dimension();

        VID.Printf(Defines.PRINT_ALL, "Initializing OpenGL display\n");

        VID.Printf(Defines.PRINT_ALL, "...setting mode " + mode + ":");

        /**
                if (Globals.appletMode && container == null) {
                    container = (Container) Globals.applet;
                } */

        final boolean screenRemRef;
        if(null == screen) {
            screen = NewtFactory.createScreen(NewtFactory.createDisplay(null), 0);
            screen.addReference(); // trigger native creation
            screenRemRef = true;
        } else {
            screenRemRef = false;
        }
        
        if (!VID.GetModeInfo(newDim, mode)) {
            VID.Printf(Defines.PRINT_ALL, " invalid mode\n");
            return Base.rserr_invalid_mode;
        }

        VID.Printf(Defines.PRINT_ALL, " " + newDim.getWidth() + " " + newDim.getHeight() + '\n');

        if (!Globals.appletMode) {
            // destroy the existing window
            if (window != null) shutdown();
        }
        
        if(null == window) {
            window = GLWindow.create(screen, new GLCapabilities(GLProfile.get(GLProfile.GL2)));
            window.setTitle("Jake2 (jogl-gl2-newt)");
        }

        if (oldDisplayMode == null) {
            oldDisplayMode = window.getScreen().getCurrentScreenMode();
        }

        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyNotify(WindowEvent e) {
                if (!Globals.appletMode) {
                    Cbuf.ExecuteText(Defines.EXEC_APPEND, "quit");
                }
            }

            public void windowResized(WindowEvent e) {
                int width = window.getWidth();
                int height = window.getHeight();
                final int mask = ~0x03;
                if ((width & 0x03) != 0) {
                    width &= mask;
                    width += 4;
                }

                Base.setVid(width, height);
                // let the sound and input subsystems know about the new window
                VID.NewWindow(width, height);            
            }
        });

        if (Globals.appletMode) {
            // Destroy the previous display if there is one
            shutdown();

            // We don't support full-screen mode
            fullscreen = false;
        }

        // We need to feed the NEWT Window to the NEWTKBD
        NEWTKBD.Init(window);
        
        window.setSize(newDim.getWidth(), newDim.getHeight());

        // D I F F E R E N T   J A K E 2   E V E N T   P R O C E S S I N G
        window.addWindowListener(NEWTKBD.listener);
        window.addKeyListener(NEWTKBD.listener);
        window.addMouseListener(NEWTKBD.listener);

        if (fullscreen) {
            window.setFullscreen(true);

            ScreenMode sm = findDisplayMode(newDim);
            final DimensionImmutable smDim = sm.getMonitorMode().getSurfaceSize().getResolution();
            newDim.setWidth( smDim.getWidth() );
            newDim.setHeight( smDim.getHeight() );
            window.getScreen().setCurrentScreenMode(sm);
            window.setFullscreen(true);
            window.setVisible(true);

            VID.Printf(Defines.PRINT_ALL, "...setting fullscreen " + sm.toString() + '\n');

        } else {
            if (!Globals.appletMode) {
                window.setVisible(true);
            } else {
                // Notify the size listener about the change
                final SizeChangeListener listener = Globals.sizeChangeListener;
                if (listener != null) {
                    listener.sizeChanged(newDim.getWidth(), newDim.getHeight());
                }
            }
        }

        if (!Globals.appletMode) {
            while ( !window.isNativeValid()|| !window.isRealized() ) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
        }
        window.requestFocus();
        window.display(); // force GL resource validation
        
        if(screenRemRef) {
            screen.removeReference();
        }

        fpsCounter.setUpdateFPSFrames(5*60, System.err);

        return Base.rserr_ok;
    }

    void shutdown() {
        if (!Globals.appletMode) {
            if ( null != window ) {
                window.destroy();
            }
        } else {
            if ( null != window ) {
                window.destroy(); // same thing
            }
        }
        window = null;
    }    
    
}
