package fi.harism.opengl.lib.gl;

import android.content.Context;
import android.opengl.GLES31;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class GlUtils {

    public static void checkGLErrors() {
        int error;
        while ((error = GLES31.glGetError()) != GLES31.GL_NO_ERROR) {
            for (int i = 3; i <= 4; ++i) {
                String stackTrace = Thread.currentThread().getStackTrace()[i].toString();
                Log.d("OpenGL", stackTrace + "  error=0x" + Integer.toHexString(error));
            }
        }
    }

    public static String loadString(Context context, String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        Scanner scanner = new Scanner(is).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

}
