/*
 * InputListener.java
 * Copyright (C) 2004
 * 
 * $Id: InputListener.java,v 1.6 2005-06-06 18:22:20 hzi Exp $
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
package jake2.sys;

import com.jogamp.newt.event.*;

import java.util.LinkedList;

/**
 * InputListener
 */
public final class InputListener implements KeyListener, MouseListener, WindowListener {

	// modifications of eventQueue must be thread safe!
	private static LinkedList<Jake2InputEvent> eventQueue = new LinkedList<Jake2InputEvent>();

	static void addEvent(Jake2InputEvent ev) {
		synchronized (eventQueue) {
			eventQueue.addLast(ev);
		}
	}

	static Jake2InputEvent nextEvent() {
		Jake2InputEvent ev;
		synchronized (eventQueue) {
			ev = (!eventQueue.isEmpty())?(Jake2InputEvent)eventQueue.removeFirst():null;
		}
		return ev;
	}

        @Override
	public void keyPressed(KeyEvent e) {
	    addEvent(new Jake2InputEvent(Jake2InputEvent.KeyPress, e));
	}

        @Override
	public void keyReleased(KeyEvent e) {
	    addEvent(new Jake2InputEvent(Jake2InputEvent.KeyRelease, e));
	}

        @Override
	public void mouseClicked(MouseEvent e) {
	}

        @Override
	public void mouseEntered(MouseEvent e) {
	}

        @Override
	public void mouseExited(MouseEvent e) {
	}

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            addEvent(new Jake2InputEvent(Jake2InputEvent.WheelMoved, e));
        }

        @Override
	public void mousePressed(MouseEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.ButtonPress, e));
	}

        @Override
	public void mouseReleased(MouseEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.ButtonRelease, e));
	}

        @Override
	public void mouseDragged(MouseEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.MotionNotify, e));
	}

        @Override
	public void mouseMoved(MouseEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.MotionNotify, e));
	}

        @Override
	public void windowMoved(WindowEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.ConfigureNotify, e));
	}

        @Override
	public void windowResized(WindowEvent e) {
		addEvent(new Jake2InputEvent(Jake2InputEvent.ConfigureNotify, e));
	}

        @Override
        public void windowDestroyNotify(WindowEvent e) {
        }
        
        @Override
        public void windowDestroyed(WindowEvent e) {
        }
        
        @Override
        public void windowGainedFocus(WindowEvent e) {
            addEvent(new Jake2InputEvent(Jake2InputEvent.ConfigureNotify, e));
        }
        
        @Override
        public void windowLostFocus(WindowEvent e) {
        }
        
        @Override
        public void windowRepaint(WindowUpdateEvent e) {
            addEvent(new Jake2InputEvent(Jake2InputEvent.ConfigureNotify, e));
        }	

}

