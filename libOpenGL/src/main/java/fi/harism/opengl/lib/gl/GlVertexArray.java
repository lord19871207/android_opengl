package fi.harism.opengl.lib.gl;

import android.opengl.GLES31;

public class GlVertexArray {

    private final int[] mVertexArray = new int[1];

    public GlVertexArray() {
        GLES31.glGenVertexArrays(1, mVertexArray, 0);
        GlUtils.checkGLErrors();
    }

    public void bind() {
        GLES31.glBindVertexArray(mVertexArray[0]);
        GlUtils.checkGLErrors();
    }

}
