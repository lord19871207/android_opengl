package fi.harism.app.opengl3x.fragment.basic;

import android.opengl.GLES30;

import java.nio.ByteBuffer;

import fi.harism.app.opengl3x.fragment.RendererFragment;
import fi.harism.lib.opengl.gl.GlBuffer;
import fi.harism.lib.opengl.gl.GlVertexArray;

public abstract class BasicRendererFragment extends RendererFragment {

    private GlBuffer glBufferVertices;
    private GlBuffer glBufferNormals;
    private GlVertexArray glVertexArrayCube;

    @Override
    public void onSurfaceCreated() {
        // Vertex and normal data plus indices arrays.
        final byte[][] CUBEVERTICES = {
                {-1, 1, 1}, {-1, -1, 1}, {1, 1, 1}, {1, -1, 1},
                {-1, 1, -1}, {-1, -1, -1}, {1, 1, -1}, {1, -1, -1}};
        final byte[][] CUBENORMALS = {
                {0, 0, 1}, {0, 0, -1}, {-1, 0, 0},
                {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};
        final int[][][] CUBEFILLED = {
                {{0, 1, 2, 1, 3, 2}, {0}},
                {{6, 7, 4, 7, 5, 4}, {1}},
                {{0, 4, 1, 4, 5, 1}, {2}},
                {{3, 7, 2, 7, 6, 2}, {3}},
                {{4, 0, 6, 0, 2, 6}, {4}},
                {{1, 5, 3, 5, 7, 3}, {5}}};

        ByteBuffer bufferVertices = ByteBuffer.allocateDirect(3 * 6 * 6);
        ByteBuffer bufferNormals = ByteBuffer.allocateDirect(3 * 6 * 6);

        for (int i = 0; i < CUBEFILLED.length; ++i) {
            for (int j = 0; j < CUBEFILLED[i][0].length; ++j) {
                bufferVertices.put(CUBEVERTICES[CUBEFILLED[i][0][j]]);
                bufferNormals.put(CUBENORMALS[CUBEFILLED[i][1][0]]);
            }
        }

        glVertexArrayCube = new GlVertexArray().bind();

        glBufferVertices = new GlBuffer()
                .bind(GLES30.GL_ARRAY_BUFFER)
                .data(GLES30.GL_ARRAY_BUFFER, 3 * 6 * 6, bufferVertices.position(0), GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_BYTE, false, 0, 0);
        GLES30.glEnableVertexAttribArray(0);

        glBufferNormals = new GlBuffer()
                .bind(GLES30.GL_ARRAY_BUFFER)
                .data(GLES30.GL_ARRAY_BUFFER, 3 * 6 * 6, bufferNormals.position(0), GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_BYTE, false, 0, 0);
        GLES30.glEnableVertexAttribArray(1);

        glVertexArrayCube.unbind();
    }

    protected void renderCube() {
        glVertexArrayCube.bind();
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        glVertexArrayCube.unbind();
    }

}
