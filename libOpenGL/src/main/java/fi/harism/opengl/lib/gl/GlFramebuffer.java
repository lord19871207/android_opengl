package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlFramebuffer {

    private final int[] framebuffer = {0};

    public GlFramebuffer() {
        GLES31.glGenFramebuffers(1, framebuffer, 0);
        GlUtils.checkGLErrors();
    }

    public GlFramebuffer bind(int target) {
        GLES31.glBindFramebuffer(target, framebuffer[0]);
        GlUtils.checkGLErrors();
        return this;
    }

    public void unbind(int target) {
        GLES31.glBindFramebuffer(target, 0);
        GlUtils.checkGLErrors();
    }

    public void renderbuffer(int target, int attachment, int renderbuffer) {
        GLES31.glFramebufferRenderbuffer(target, attachment, GLES31.GL_RENDERBUFFER, renderbuffer);
        GlUtils.checkGLErrors();
    }

    public void texture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES31.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        GlUtils.checkGLErrors();
    }

    public int checkStatus(int target) {
        GlUtils.checkGLErrors();
        return GLES31.glCheckFramebufferStatus(target);
    }

    public int name() {
        return framebuffer[0];
    }

}
