import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Convert {

    public static void main(String args[]) {
        for (String arg : args) {
            String outputName = arg.substring(0, arg.lastIndexOf('.')) + ".dat";
            try {
                System.out.println(arg + " --> " + outputName);
                convert(arg, outputName);
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
        }
    }

    private static void convert(String inPath, String outPath) throws IOException {
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
        InputStream inputStream = new FileInputStream(inPath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
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
                        throw new IOException("Obj face error : " + matcher.group(i + 1));
                    }
                }
                arrayFaces.add(values);
            } else {
                throw new IOException("Obj file error : " + currentLine);
            }
        }

        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outPath));
        outputStream.writeInt(3 * arrayFaces.size());

        for (int[] face : arrayFaces) {
            for (int i = 0; i < 3; ++i) {
                final float[] vertex = arrayVertices.get(face[i * 3]);
                outputStream.writeFloat(vertex[0]);
                outputStream.writeFloat(vertex[1]);
                outputStream.writeFloat(vertex[2]);
            }
        }

        for (int[] face : arrayFaces) {
            for (int i = 0; i < 3; ++i) {
                final float[] normal = arrayNormals.get(face[i * 3 + 2]);
                outputStream.writeFloat(normal[0]);
                outputStream.writeFloat(normal[1]);
                outputStream.writeFloat(normal[2]);
            }
        }

        for (int[] face : arrayFaces) {
            for (int i = 0; i < 3; ++i) {
                final float[] texture = arrayTextures.get(face[i * 3 + 1]);
                outputStream.writeFloat(texture[0]);
                outputStream.writeFloat(texture[1]);
            }
        }

        outputStream.flush();
        outputStream.close();
    }

}
