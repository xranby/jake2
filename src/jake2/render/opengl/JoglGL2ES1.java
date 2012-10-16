package jake2.render.opengl;


import java.nio.*;

import javax.media.opengl.*;

import com.jogamp.opengl.util.ImmModeSink;

public class JoglGL2ES1 implements QGL {

    private GL2ES1 gl;
    protected final ImmModeSink ims;
    private boolean inBlock = false; // within begin/end

    JoglGL2ES1() {
        // singleton
        ims = ImmModeSink.createFixed(4, 
                3, GL.GL_FLOAT,  // vertex
                0, 0,            // color
                0, 0,            // normal
                2, GL.GL_FLOAT,  // texture
                GL.GL_STATIC_DRAW);
    }

    void setGL(GL2ES1 gl) {
        this.gl = gl;
    }

    public void glBegin(int mode) {
        if(inBlock) {
            throw new GLException("glBegin already called");
        }
        ims.glBegin(mode);
        inBlock = true;
    }

    public void glEnd() {
        if(!inBlock) {
            throw new GLException("glBegin not called");
        }        
        ims.glEnd(gl, true);
        inBlock = false;
    }

    public void glColor3f(float red, float green, float blue) {
        glColor4f(red, green, blue, 1f);
    }

    public void glColor3ub(byte red, byte green, byte blue) {
        glColor4ub(red, green, blue, (byte) 255);
    }

    public void glColor4f(float red, float green, float blue, float alpha) {
        if(inBlock) {
            ims.glColor4f(red, green, blue, alpha);
        } else {
            gl.glColor4f(red, green, blue, alpha);
        }
    }

    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        final float r = (red   & 255) / 255.0f;
        final float g = (green & 255) / 255.0f;
        final float b = (blue  & 255) / 255.0f;
        final float a = (alpha & 255) / 255.0f;
        glColor4f(r, g, b, a);
    }

    public void glTexCoord2f(float s, float t) {
        if(inBlock) {
            ims.glTexCoord2f(s, t);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    public void glVertex2f(float x, float y) {
        if(inBlock) {
            ims.glVertex2f(x, y);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    public void glVertex3f(float x, float y, float z) {
        if(inBlock) {
            ims.glVertex3f(x, y, z);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    public void glAlphaFunc(int func, float ref) {
        gl.glAlphaFunc(func, ref);
    }

    public void glBindTexture(int target, int texture) {
        gl.glBindTexture(target, texture);
    }

    public void glBlendFunc(int sfactor, int dfactor) {
        gl.glBlendFunc(sfactor, dfactor);
    }

    public void glClear(int mask) {
        gl.glClear(mask);
    }

    public void glClearColor(float red, float green, float blue, float alpha) {
        gl.glClearColor(red, green, blue, alpha);
    }

    public void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {
        gl.glColorPointer(size, GL_UNSIGNED_BYTE, stride, pointer);
    }

    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        gl.glColorPointer(size, GL_FLOAT, stride, pointer);
    }

    public void glCullFace(int mode) {
        gl.glCullFace(mode);
    }

    public void glDeleteTextures(IntBuffer textures) {
        gl.glDeleteTextures(textures.limit(), textures);
    }

    public void glDepthFunc(int func) {
        gl.glDepthFunc(func);
    }

    public void glDepthMask(boolean flag) {
        gl.glDepthMask(flag);
    }

    public void glDepthRange(double zNear, double zFar) {
        gl.glDepthRangef((float)zNear, (float)zFar);
    }

    public void glDisable(int cap) {
        gl.glDisable(cap);
    }

    public void glDisableClientState(int cap) {
        gl.glDisableClientState(cap);
    }

    public void glDrawArrays(int mode, int first, int count) {
        switch(mode) {
            case GL_QUAD_STRIP:
                mode=GL.GL_TRIANGLE_STRIP;
                break;
            case GL_POLYGON:
                mode=GL.GL_TRIANGLE_FAN;
                break;
        }
        if ( GL_QUADS == mode && !gl.isGL2() ) {
            for (int j = first; j < count - 3; j += 4) {
                gl.glDrawArrays(GL.GL_TRIANGLE_FAN, j, 4);
            }
        } else {
            gl.glDrawArrays(mode, first, count);
        }
    }

    public void glDrawBuffer(int mode) {
        // FIXME: ignored  
        if(GL.GL_BACK != mode) {
            System.err.println("IGNORED: glDrawBuffer(0x"+Integer.toHexString(mode)+")");
        }
    }

    public void glDrawElements(int mode, ShortBuffer indices) {
        switch(mode) {
            case GL_QUAD_STRIP:
                mode=GL.GL_TRIANGLE_STRIP;
                break;
            case GL_POLYGON:
                mode=GL.GL_TRIANGLE_FAN;
                break;
        }
        final int idxLen = indices.remaining();
        if ( GL_QUADS == mode && !gl.isGL2() ) {
            final int idx0 = indices.position();
            final ShortBuffer b = (ShortBuffer) indices;
            for (int j = 0; j < idxLen; j++) {
                gl.glDrawArrays(GL.GL_TRIANGLE_FAN, (int)(0x0000ffff & b.get(idx0+j)), 4);
            }
        } else {
            gl.glDrawElements(mode, idxLen, GL.GL_UNSIGNED_SHORT, indices);
        }
    }

    public void glEnable(int cap) {
        gl.glEnable(cap);
    }

    public void glEnableClientState(int cap) {
        gl.glEnableClientState(cap);
    }

    public void glFinish() {
        gl.glFinish();
    }

    public void glFlush() {
        gl.glFlush();
    }

    public void glFrustum(double left, double right, double bottom,
            double top, double zNear, double zFar) {
        gl.glFrustum(left, right, bottom, top, zNear, zFar);
    }

    public int glGetError() {
        return gl.glGetError();
    }

    public void glGetFloat(int pname, FloatBuffer params) {
        gl.glGetFloatv(pname, params);
    }

    public String glGetString(int name) {
        if( GL.GL_EXTENSIONS == name ) {
            StringBuilder sb = new StringBuilder();
            sb.append(gl.glGetString(name));
            sb.append(" GL_ARB_multitexture");
            if( gl.isGLES1() ) {
                sb.append(" GL_EXT_point_parameters");
            }
            return sb.toString();
        }
        return gl.glGetString(name);
    }

    public void glHint(int target, int mode) {
        gl.glHint(target, mode);
    }

    public void glInterleavedArrays(int format, int stride, FloatBuffer pointer) {
        // gl.glInterleavedArrays(GL_T2F_V3F, glpoly_t.BYTE_STRIDE, globalPolygonInterleavedBuf);        
        // gl.glInterleavedArrays(format, stride, pointer);
        if(GL_T2F_V3F == format) {
            glInterleavedArraysT2F_V3F(stride, pointer);
            return;
        }
        throw new GLException("Type not supported: 0x"+Integer.toHexString(format));
    }

    private void glInterleavedArraysT2F_V3F(int byteStride, FloatBuffer buf) {
        int pos = buf.position();
        gl.glTexCoordPointer(2, GL.GL_FLOAT, byteStride, buf);
        gl.glEnableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);

        buf.position(pos + 2);
        gl.glVertexPointer(3, GL.GL_FLOAT, byteStride, buf);
        gl.glEnableClientState(GL2ES1.GL_VERTEX_ARRAY);

        buf.position(pos);
    }

    public void glLoadIdentity() {
        gl.glLoadIdentity();
    }

    public void glLoadMatrix(FloatBuffer m) {
        gl.glLoadMatrixf(m);
    }

    public void glMatrixMode(int mode) {
        gl.glMatrixMode(mode);
    }

    public void glOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        gl.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    public void glPixelStorei(int pname, int param) {
        gl.glPixelStorei(pname, param);
    }

    public void glPointSize(float size) {
        gl.glPointSize(size);
    }

    public void glPolygonMode(int face, int mode) {
        if( GL_FRONT_AND_BACK != face || GL_FILL != mode ) { // if !default
            System.err.println("IGNORED: glPolygonMode(0x"+Integer.toHexString(face)+", 0x"+Integer.toHexString(mode)+")");
        }
    }

    public void glPopMatrix() {
        gl.glPopMatrix();
    }

    public void glPushMatrix() {
        gl.glPushMatrix();
    }

    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        gl.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public void glRotatef(float angle, float x, float y, float z) {
        gl.glRotatef(angle, x, y, z);
    }

    public void glScalef(float x, float y, float z) {
        gl.glScalef(x, y, z);
    }

    public void glScissor(int x, int y, int width, int height) {
        gl.glScissor(x, y, width, height);
    }

    public void glShadeModel(int mode) {
        gl.glShadeModel(mode);
    }

    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        gl.glTexCoordPointer(size, GL_FLOAT, stride, pointer);
    }

    public void glTexEnvi(int target, int pname, int param) {
        gl.glTexEnvi(target, pname, param);
    }

    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, 
                             int format, int type, ByteBuffer pixels) {
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, 
                             int format, int type, IntBuffer pixels) {
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public void glTexParameterf(int target, int pname, float param) {
        gl.glTexParameterf(target, pname, param);
    }

    public void glTexParameteri(int target, int pname, int param) {
        gl.glTexParameteri(target, pname, param);
    }

    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, 
                                int format, int type, IntBuffer pixels) {
        gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public void glTranslatef(float x, float y, float z) {
        gl.glTranslatef(x, y, z);
    }

    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        gl.glVertexPointer(size, GL_FLOAT, stride, pointer);
    }

    public void glViewport(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
    }

    public void glColorTable(int target, int internalFormat, int width, int format, int type, ByteBuffer data) {
        // nop / FIXME gl.glColorTable(target, internalFormat, width, format, type, data);
        System.err.println("IGNORED: glColorTable(0x"+Integer.toHexString(target)+", 0x"+Integer.toHexString(internalFormat)+", ..)");
    }

    public void glActiveTextureARB(int texture) {
        gl.glActiveTexture(texture);
    }

    public void glClientActiveTextureARB(int texture) {
        gl.glClientActiveTexture(texture);
    }

    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {
        gl.glPointParameterfv(pname, pfParams);
    }

    public void glPointParameterfEXT(int pname, float param) {
        gl.glPointParameterf(pname, param);
    }

    public void glLockArraysEXT(int first, int count) {
        // nop / FIXME gl.glLockArraysEXT(first, count);
        System.err.println("IGNORED: glLockArraysEXT(0x"+Integer.toHexString(first)+", 0x"+Integer.toHexString(count)+", ..)");
    }

    public void glArrayElement(int index) {
        // nop / FIXME gl.glArrayElement(index);
        System.err.println("IGNORED: glArrayElement(0x"+Integer.toHexString(index)+")");
    }

    public void glUnlockArraysEXT() {
        // nop / FIXME gl.glUnlockArraysEXT();
        System.err.println("IGNORED: glUnlockArraysEXT()");
    }

    public void glMultiTexCoord2f(int target, float s, float t) {
        // nop / FIXME gl.glMultiTexCoord2f(target, s, t);
        System.err.println("IGNORED: glMultiTexCoord2f(0x"+Integer.toHexString(target)+", "+s+", "+t+")");
    }

    /*
     * util extensions
     */
    public void setSwapInterval(int interval) {
        gl.setSwapInterval(interval);
    }

}
