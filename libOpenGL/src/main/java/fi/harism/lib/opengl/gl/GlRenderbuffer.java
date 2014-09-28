package fi.harism.lib.opengl.gl;

import android.opengl.GLES31;

public class GlRenderbuffer {

    private final int[] renderbuffer = {0};

    public GlRenderbuffer() {
        GLES31.glGenRenderbuffers(1, renderbuffer, 0);
        GlUtils.checkGLErrors();
    }

    public GlRenderbuffer bind() {
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, renderbuffer[0]);
        GlUtils.checkGLErrors();
        return this;
    }

    public void unbind() {
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, 0);
        GlUtils.checkGLErrors();
    }

    public GlRenderbuffer storage(int internalFormat, int width, int height) {
        GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, internalFormat, width, height);
        GlUtils.checkGLErrors();
        return this;
    }

    public int name() {
        return renderbuffer[0];
    }

}
