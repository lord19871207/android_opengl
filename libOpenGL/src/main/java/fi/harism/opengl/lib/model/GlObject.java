package fi.harism.opengl.lib.model;

import android.opengl.GLES31;

import java.nio.FloatBuffer;

import fi.harism.opengl.lib.gl.GlBuffer;

public class GlObject {

    private final int mVertexCount;
    private final GlBuffer mVertexBuffer;
    private final GlBuffer mNormalBuffer;
    private final GlBuffer mTextureBuffer;

    public GlObject(int vertexCount, FloatBuffer vertexBuffer, FloatBuffer normalBuffer, FloatBuffer textureBuffer) {
        mVertexCount = vertexCount;

        mVertexBuffer = new GlBuffer()
                .bind(GLES31.GL_ARRAY_BUFFER)
                .data(GLES31.GL_ARRAY_BUFFER, 4 * 3 * mVertexCount, vertexBuffer, GLES31.GL_STATIC_DRAW)
                .unbind(GLES31.GL_ARRAY_BUFFER);

        mNormalBuffer = new GlBuffer()
                .bind(GLES31.GL_ARRAY_BUFFER)
                .data(GLES31.GL_ARRAY_BUFFER, 4 * 3 * mVertexCount, normalBuffer, GLES31.GL_STATIC_DRAW)
                .unbind(GLES31.GL_ARRAY_BUFFER);

        mTextureBuffer = new GlBuffer()
                .bind(GLES31.GL_ARRAY_BUFFER)
                .data(GLES31.GL_ARRAY_BUFFER, 4 * 2 * mVertexCount, textureBuffer, GLES31.GL_STATIC_DRAW)
                .unbind(GLES31.GL_ARRAY_BUFFER);
    }

    public int getVertexCount() {
        return mVertexCount;
    }

    public GlBuffer getVertexBuffer() {
        return mVertexBuffer;
    }

    public GlBuffer getNormalBuffer() {
        return mNormalBuffer;
    }

    public GlBuffer getTextureBuffer() {
        return mTextureBuffer;
    }

}
