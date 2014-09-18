package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlSampler {

    private final int mSampler[] = new int[1];

    public GlSampler() {
        GLES31.glGenSamplers(1, mSampler, 0);
        GlUtils.checkGLErrors();
    }

    public void parameter(int pname, float pvalue) {
        GLES31.glSamplerParameterf(mSampler[0], pname, pvalue);
        GlUtils.checkGLErrors();
    }

    public void parameter(int pname, int pvalue) {
        GLES31.glSamplerParameteri(mSampler[0], pname, pvalue);
        GlUtils.checkGLErrors();
    }

    public void bind(int textureUnit) {
        GLES31.glBindSampler(textureUnit, mSampler[0]);
        GlUtils.checkGLErrors();
    }

    public void unbind(int textureUnit) {
        GLES31.glBindSampler(textureUnit, 0);
        GlUtils.checkGLErrors();
    }

}
