/*
 * Jake2Applet.java
 * Copyright (C)  2008
 * 
 * $Id: Jake2Applet.java,v 1.2 2008-03-02 20:38:04 kbrussel Exp $
 */
/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */
package jake2;

import jake2.game.Cmd;
import jake2.qcommon.*;
import jake2.sys.Timer;

import java.applet.Applet;
import java.awt.*;
import java.util.StringTokenizer;

import com.jogamp.common.os.Platform;

import netscape.javascript.*;

/**
 * Jake2 is the main class of Quake2 for Java.
 */
@SuppressWarnings("serial")
public class Jake2Applet extends Applet {

    private JSObject self;
    private volatile boolean gameShouldShutDown;
    private volatile boolean gameShutDown;
    private volatile boolean gameStarted;
    private Object gameLifecycleLock = new Object();
    
    private static String sz0_args = " +set gl_mode 11 +set vid_width ";
    private static String sz1_args = " +set vid_height ";

    @Override
    public void init() {
        System.err.println("Jake2 Applet Init: "+Thread.currentThread().getName());
        setBackground(new Color(0x33, 0x33, 0x33, 0xff)); // web page
        setLayout(new java.awt.BorderLayout());
    }

    @Override
    public void start() {
        System.err.println("Jake2 Applet Start.0: "+Thread.currentThread().getName());
        synchronized(gameLifecycleLock) {
            gameShouldShutDown = false;
            gameShutDown = false;
            gameStarted = false;
        }
        if( EventQueue.isDispatchThread() ) { // Game thread offloads to AWT-EDT in Applet mode
            new GameThread().start();            
        } else {
            synchronized(gameLifecycleLock) {
                new GameThread().start();
                while (!gameStarted) {
                    try {
                        gameLifecycleLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        System.err.println("Jake2 Applet Start.X: "+Thread.currentThread().getName());
    }

    @Override
    public void stop() {
        System.err.println("Jake2 Applet Stop.0: "+Thread.currentThread().getName());
        synchronized(gameLifecycleLock) {
            gameShouldShutDown = true;
            while (!gameShutDown) {
                try {
                    gameLifecycleLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        System.err.println("Jake2 Applet Stop.X: "+Thread.currentThread().getName());
    }
    
    @Override
    public void destroy() {
        System.err.println("Jake2 Applet Destroy.0: "+Thread.currentThread().getName());
    }
    
    class GameThread extends Thread {
        public GameThread() {
            super("Jake2 Game Thread");
        }

        public void run() {
            synchronized(gameLifecycleLock) {
                // TODO: check if dedicated is set in config file
        	
                System.err.println("Jake2 Applet Game START: "+Thread.currentThread().getName());
                
                Globals.dedicated= Cvar.Get("dedicated", "0", Qcommon.CVAR_NOSET);
        
                // Set things up for applet execution
                Globals.appletMode = true;
                Globals.applet = Jake2Applet.this;
                Globals.sizeChangeListener = new SizeChangeListener() {
                        public void sizeChanged(int width, int height) {
                            try {
                                if (self == null) {
                                    JSObject win = JSObject.getWindow(Jake2Applet.this);
                                    self = (JSObject) win.eval("document.getElementById(\"" +
                                                               getParameter("id") + "\")");
                                }
                                self.setMember("width", new Integer(width));
                                self.setMember("height", new Integer(height));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
    
                // open the q2dialog, if we are not in dedicated mode.
                if (Globals.dedicated.value != 1.0f) {
                    Jake2.initQ2DataTool();
                }
                
                final int a_width = Jake2Applet.this.getWidth();
                final int a_height = (int) ( a_width * 0.75f );
    
                final String cmd_args;
                {
                    final String applet_args = getParameter("jake_args");
                    StringBuffer sb = new StringBuffer();                
                    sb.append("Jake2 ");
                    if( null != applet_args && applet_args.length() > 0 ) {
                        sb.append(applet_args);
                    }
                    sb.append(sz0_args);
                    sb.append(a_width);
                    sb.append(sz1_args);
                    sb.append(a_height);
                    cmd_args = sb.toString();
                }
                System.err.println("Jake2 Applet Cmd: "+cmd_args);
                final String[] c_args;
                StringTokenizer tokens = new StringTokenizer(cmd_args);
                final int argc = tokens.countTokens();
                c_args = new String[argc];
                int i=0;
                while( tokens.hasMoreTokens() ) {
                    c_args[i++] = tokens.nextToken().trim();
                }
                
                Qcommon.Init(c_args);
                if( Platform.OSType.MACOS == Platform.getOSType() ) {
                    // FIXME: Bug on OSX: 1st NewtCanvasAWT added .. causes flickering, so 'do it again'.
                    // Interesting that our JOGLNewtApplet1Run does not suffer from this behavior!
                    Cmd.ExecuteString("quit");
                    Qcommon.Init(c_args);
                }
                Globals.nostdout = Cvar.Get("nostdout", "0", 0);
                
                gameStarted = true;
                gameLifecycleLock.notifyAll();
            }

            try {
                int oldtime = Timer.Milliseconds();
                while (!gameShouldShutDown) {
                    // find time spending rendering last frame
                    final int newtime = Timer.Milliseconds();
                    final int time = newtime - oldtime;

                    if (time > 0) {
                        Qcommon.Frame(time);
                    }
                    oldtime = newtime;
                }
            } catch(Throwable t) {
                System.err.println("Jake2 Applet Game Exception: "+t.getClass().getName()+": "+t.getMessage());
                t.printStackTrace();
            } finally {
                synchronized(gameLifecycleLock) {
                    System.err.println("Jake2 Applet Game STOP.0: "+Thread.currentThread().getName());
                    try {
                        Cmd.ExecuteString("quit");
                    } catch (Exception e) {
                        System.err.println("Jake2 Applet Game STOP Catched:");
                        e.printStackTrace();
                    }
                    gameShutDown = true;
                    gameStarted = false;
                    gameLifecycleLock.notifyAll();
                    System.err.println("Jake2 Applet Game STOP.X: "+Thread.currentThread().getName());
                }
            }
        }
    }
}
