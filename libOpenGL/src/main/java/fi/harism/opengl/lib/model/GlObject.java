package fi.harism.opengl.lib.model;

import java.nio.FloatBuffer;

public class GlObject {

    private final int mVertexCount;
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mNormalBuffer;
    private final FloatBuffer mTextureBuffer;

    public GlObject(int vertexCount, FloatBuffer vertexBuffer, FloatBuffer normalBuffer, FloatBuffer textureBuffer) {
        mVertexCount = vertexCount;
        mVertexBuffer = vertexBuffer;
        mNormalBuffer = normalBuffer;
        mTextureBuffer = textureBuffer;
    }

    public int getVertexCount() {
        return mVertexCount;
    }

    public FloatBuffer getVertexBuffer() {
        return mVertexBuffer;
    }

    public FloatBuffer getNormalBuffer() {
        return mNormalBuffer;
    }

    public FloatBuffer getTextureBuffer() {
        return mTextureBuffer;
    }

}
