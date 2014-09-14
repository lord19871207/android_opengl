package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

import java.nio.Buffer;

public class GlTexture {

    private final int[] mTexture = new int[1];

    public GlTexture() {
        GLES31.glGenTextures(1, mTexture, 0);
        GlUtils.checkGLErrors();
    }

    public int getTexture() {
        return mTexture[0];
    }

    public void bind(int target) {
        GLES31.glBindTexture(target, mTexture[0]);
        GlUtils.checkGLErrors();
    }

    public void unbind(int target) {
        GLES31.glBindTexture(target, 0);
        GlUtils.checkGLErrors();
    }

    public void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, Buffer data) {
        GLES31.glTexImage2D(target, level, internalFormat, width, height, border, format, type, data);
        GlUtils.checkGLErrors();
    }

    public void parameter(int target, int pname, float pvalue) {
        GLES31.glTexParameterf(target, pname, pvalue);
        GlUtils.checkGLErrors();
    }

    public void parameter(int target, int pname, int pvalue) {
        GLES31.glTexParameteri(target, pname, pvalue);
        GlUtils.checkGLErrors();
    }

}
