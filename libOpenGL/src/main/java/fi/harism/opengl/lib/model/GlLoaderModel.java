package fi.harism.opengl.lib.model;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GlLoaderModel {

    private FloatBuffer mBufferVertices;
    private FloatBuffer mBufferNormals;
    private FloatBuffer mBufferTexture;
    private int mVertexCount;

    public GlLoaderModel(Context context, String path) throws IOException {
        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(context.getAssets().open(path)));

        mVertexCount = inputStream.readInt();
        mBufferVertices = ByteBuffer.allocateDirect(4 * 3 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferNormals = ByteBuffer.allocateDirect(4 * 3 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferTexture = ByteBuffer.allocateDirect(4 * 2 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();

        mBufferVertices.position(0);
        for (int i = 0; i < mVertexCount * 3; ++i) {
            mBufferVertices.put(inputStream.readFloat());
        }
        mBufferVertices.position(0);

        mBufferNormals.position(0);
        for (int i = 0; i < mVertexCount * 3; ++i) {
            mBufferNormals.put(inputStream.readFloat());
        }
        mBufferNormals.position(0);

        mBufferTexture.position(0);
        for (int i = 0; i < mVertexCount * 2; ++i) {
            mBufferTexture.put(inputStream.readFloat());
        }
        mBufferTexture.position(0);
    }

    public int getVertexCount() {
        return mVertexCount;
    }

    public FloatBuffer getBufferVertices() {
        return mBufferVertices;
    }

    public FloatBuffer getBufferNormals() {
        return mBufferNormals;
    }

    public FloatBuffer getBufferTexture() {
        return mBufferTexture;
    }

}
