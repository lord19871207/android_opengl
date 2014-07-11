package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlTexture {

    private final int mTarget;
    private final int[] mTexture = new int[1];

    public GlTexture(int target) {
        mTarget = target;
        GLES31.glGenTextures(1, mTexture, 0);
        GlUtils.checkGLErrors();
    }

    public int getTexture() {
        return mTexture[0];
    }

    public void bind() {
        GLES31.glBindTexture(mTarget, mTexture[0]);
        GlUtils.checkGLErrors();
    }

    public void unbind() {
        GLES31.glBindTexture(mTarget, 0);
        GlUtils.checkGLErrors();
    }

}
