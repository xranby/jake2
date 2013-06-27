package jake2.render.opengl;

import java.util.List;

import javax.media.nativewindow.util.Dimension;

import jake2.qcommon.xcommand_t;

import com.jogamp.newt.MonitorMode;

public interface GLDriver {
    
    boolean init(int xpos, int ypos);
    
    int setMode(Dimension dim, int mode, boolean fullscreen);
    
    void shutdown();
    
    void beginFrame(float camera_separation);
    
    /** Performs <code>swapBuffers()</code>, ticks the fps counter and performs <code>QUIT</code> if requested. */ 
    void endFrame();

    void appActivate(boolean activate);
    
    void enableLogging(boolean enable);
    
    void logNewFrame();
    
    List<MonitorMode> getModeList();

    void updateScreen(xcommand_t callback);

    void screenshot();
    
}
