/*
 * Renderer.java
 * Copyright (C) 2003
 *
 * $Id: Renderer.java,v 1.13 2008-03-02 16:01:27 cawe Exp $
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
package jake2.render;

import jake2.client.refexport_t;

import java.util.Vector;

/**
 * Renderer
 * 
 * @author cwei
 */
public class Renderer {

    static RenderAPI fastRenderer = new jake2.render.fast.Misc();
    static RenderAPI basicRenderer = new jake2.render.basic.Misc();

    static Vector<Ref> drivers = new Vector<Ref>(4);

    static {
        try {
            try {
                Class.forName("jake2.render.JoglDummyRenderer");
            } catch (Throwable t) {
                // ignore the new jogl driver if runtime not in classpath
                System.err.println("Catched exception: "+t.getMessage());
                // t.printStackTrace();
            }
            try {
                Class.forName("javax.media.opengl.GL2");
                Class.forName("jake2.render.JoglGL2Renderer");
            } catch (Throwable t) {
                // ignore the new jogl driver if runtime not in classpath
                System.err.println("Catched exception: "+t.getMessage());
                // t.printStackTrace();
            }
            try {
                Class.forName("javax.media.opengl.GL2ES2");
                Class.forName("jake2.render.JoglES2Renderer");
            } catch (Throwable t) {
                // ignore the new jogl driver if runtime not in classpath
                System.err.println("Catched exception: "+t.getMessage());
                // t.printStackTrace();
            }
            try {
                Class.forName("javax.media.opengl.GL2ES1");
                Class.forName("jake2.render.JoglES1Renderer");
            } catch (Throwable t) {
                // ignore the new jogl driver if runtime not in classpath
                System.err.println("Catched exception: "+t.getMessage());
                // t.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public static void register(Ref impl) {
        if (impl == null) {
            throw new IllegalArgumentException(
                    "Ref implementation can't be null");
        }
        if (!drivers.contains(impl)) {
            System.err.println("Add driver: "+impl+", "+impl.getName());
            drivers.add(impl);
        }
    }

    /**
     * Factory method to get the Renderer implementation.
     * 
     * @return refexport_t (Renderer singleton)
     */
    public static refexport_t getDriver(String driverName) {
        return getDriver(driverName, true);
    }

    /**
     * Factory method to get the Renderer implementation.
     * 
     * @return refexport_t (Renderer singleton)
     */
    public static refexport_t getDriver(String driverName, boolean fast) {
        // find a driver
        Ref driver = null;
        int count = drivers.size();
        for (int i = 0; i < count; i++) {
            driver = drivers.get(i);
            if (driver.getName().equals(driverName)) {
                return driver.GetRefAPI((fast) ? fastRenderer : basicRenderer);
            }
        }
        // null if driver not found
        return null;
    }

    public static String getDefaultName() {
        return (drivers.isEmpty()) ? null : drivers.firstElement().getName();
    }

    public static String getPreferedName() {
        return (drivers.isEmpty()) ? null : drivers.lastElement().getName();
    }

    public static String[] getDriverNames() {
        if (drivers.isEmpty()) {
            return new String[0];
        }

        int count = drivers.size();
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = drivers.get(i).getName();
        }
        return names;
    }

}
