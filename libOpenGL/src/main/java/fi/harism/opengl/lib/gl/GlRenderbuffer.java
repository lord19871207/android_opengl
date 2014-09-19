package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlRenderbuffer {

    private final int[] mRenderbuffer = {0};

    public GlRenderbuffer() {
        GLES31.glGenRenderbuffers(1, mRenderbuffer, 0);
        GlUtils.checkGLErrors();
    }

    public GlRenderbuffer bind() {
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, mRenderbuffer[0]);
        GlUtils.checkGLErrors();
        return this;
    }

    public void unbind() {
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, 0);
        GlUtils.checkGLErrors();
    }

    public void storage(int internalFormat, int width, int height) {
        GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, internalFormat, width, height);
        GlUtils.checkGLErrors();
    }

    public int getRenderbuffer() {
        return mRenderbuffer[0];
    }

}
