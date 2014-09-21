package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlSampler {

    private final int sampler[] = {0};

    public GlSampler() {
        GLES31.glGenSamplers(1, sampler, 0);
        GlUtils.checkGLErrors();
    }

    public void parameter(int pname, float pvalue) {
        GLES31.glSamplerParameterf(sampler[0], pname, pvalue);
        GlUtils.checkGLErrors();
    }

    public void parameter(int pname, int pvalue) {
        GLES31.glSamplerParameteri(sampler[0], pname, pvalue);
        GlUtils.checkGLErrors();
    }

    public void bind(int textureUnit) {
        GLES31.glBindSampler(textureUnit, sampler[0]);
        GlUtils.checkGLErrors();
    }

    public void unbind(int textureUnit) {
        GLES31.glBindSampler(textureUnit, 0);
        GlUtils.checkGLErrors();
    }

}
