package fi.harism.opengl.lib.model;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlLoaderObj {

    private FloatBuffer mBufferVertices;
    private FloatBuffer mBufferNormals;
    private FloatBuffer mBufferTexture;
    private int mVertexCount;

    public GlLoaderObj(Context context, String path) throws IOException {
        Matcher matcher, matcherIndex;
        Pattern patternEmpty = Pattern.compile("^\\s*$");
        Pattern patternComment = Pattern.compile("^#.*");
        Pattern patternObject = Pattern.compile("^o .*");
        Pattern patternS = Pattern.compile("^s .*");
        Pattern patternVertex = Pattern.compile("^v\\s+([-.0-9]+)\\s+([-.0-9]+)\\s+([-.0-9]+)\\s*$");
        Pattern patternNormal = Pattern.compile("^vn\\s+([-.0-9]+)\\s+([-.0-9]+)\\s+([-.0-9]+)\\s*$");
        Pattern patternFace = Pattern.compile("^f\\s+([/0-9]+)\\s+([/0-9]+)\\s+([/0-9]+)\\s*$");
        Pattern patternFaceIndex = Pattern.compile("^([0-9]*)/([0-9]*)/([0-9]*)$");

        ArrayList<float[]> arrayVertices = new ArrayList<>();
        ArrayList<float[]> arrayNormals = new ArrayList<>();
        ArrayList<float[]> arrayTextures = new ArrayList<>();
        ArrayList<int[]> arrayFaces = new ArrayList<>();

        arrayVertices.add(new float[]{0, 0, 0});
        arrayNormals.add(new float[]{0, 0, 0});
        arrayTextures.add(new float[]{0, 0});

        String currentLine;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getAssets().open(path)));
        while ((currentLine = bufferedReader.readLine()) != null) {
            if (patternEmpty.matcher(currentLine).matches()) {

            } else if (patternComment.matcher(currentLine).matches()) {

            } else if (patternObject.matcher(currentLine).matches()) {

            } else if (patternS.matcher(currentLine).matches()) {

            } else if ((matcher = patternVertex.matcher(currentLine)).matches()) {
                //Log.d("VERTEX", matcher.group(1) + "  " + matcher.group(2) + "  " + matcher.group(3));
                float[] values = new float[3];
                for (int i = 0; i < 3; ++i) {
                    values[i] = Float.parseFloat(matcher.group(i + 1));
                }
                arrayVertices.add(values);
            } else if ((matcher = patternNormal.matcher(currentLine)).matches()) {
                //Log.d("NORMAL", matcher.group(1) + "  " + matcher.group(2) + "  " + matcher.group(3));
                float[] values = new float[3];
                for (int i = 0; i < 3; ++i) {
                    values[i] = Float.parseFloat(matcher.group(i + 1));
                }
                arrayNormals.add(values);
            } else if ((matcher = patternFace.matcher(currentLine)).matches()) {
                //Log.d("FACE", matcher.group(1) + "  " + matcher.group(2) + "  " + matcher.group(3));
                int[] values = new int[9];
                for (int i = 0; i < 3; ++i) {
                    matcherIndex = patternFaceIndex.matcher(matcher.group(i + 1));
                    if (matcherIndex.matches()) {
                        for (int j = 0; j < 3; ++j) {
                            String indexString = matcherIndex.group(j + 1);
                            if (indexString.isEmpty()) {
                                indexString = "0";
                            }
                            values[i * 3 + j] = Integer.parseInt(indexString);
                        }
                    } else {
                        throw new IOException("Face error : " + matcher.group(i + 1));
                    }
                }
                arrayFaces.add(values);
            } else {
                throw new IOException("Obj file error : " + currentLine);
            }
        }

        mVertexCount = 3 * arrayFaces.size();
        mBufferVertices = ByteBuffer.allocateDirect(4 * 3 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferNormals = ByteBuffer.allocateDirect(4 * 3 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mBufferTexture = ByteBuffer.allocateDirect(4 * 2 * mVertexCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int[] face : arrayFaces) {
            for (int i = 0; i < 3; ++i) {
                mBufferVertices.put(arrayVertices.get(face[i * 3 + 0]));
                mBufferTexture.put(arrayTextures.get(face[i * 3 + 1]));
                mBufferNormals.put(arrayNormals.get(face[i * 3 + 2]));
            }
        }
        mBufferVertices.position(0);
        mBufferNormals.position(0);
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
